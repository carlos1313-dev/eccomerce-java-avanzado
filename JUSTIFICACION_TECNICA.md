# Documento de Justificación Técnica

**Proyecto:** E-Commerce API — Java Backend Avanzado  
**Stack:** Spring Boot 3 · Spring Security · JPA/Hibernate · PostgreSQL

---

## 1. Estrategia de Concurrencia

### El problema: sobreventa

Cuando múltiples clientes intentan comprar el mismo producto simultáneamente, se produce una condición de carrera (_race condition_). Si dos hilos leen el stock = 1 al mismo tiempo, ambos pueden considerar que hay inventario disponible y proceder con la venta, resultando en un stock final de -1 (sobreventa).

### Estrategia elegida: Bloqueo Pesimista (Pessimistic Locking)

Se eligió **control pesimista** (`PESSIMISTIC_WRITE`) sobre el control optimista por las siguientes razones:

| Criterio | Control Optimista | Control Pesimista (elegido) |
|---|---|---|
| Mecanismo | `@Version` + retry en conflicto | `SELECT FOR UPDATE` en PostgreSQL |
| Conflictos esperados | Pocos | Muchos (e-commerce real) |
| Rendimiento bajo alta contención | Muchos reintentos → degradación | Espera ordenada → predecible |
| Experiencia del cliente | Errores 409 frecuentes | Cola ordenada, sin errores |
| Complejidad | Requiere lógica de reintento | Manejada por la BD |

En un escenario de e-commerce donde decenas de usuarios pueden comprar el mismo producto popular al mismo tiempo, la tasa de conflictos sería alta. El bloqueo optimista generaría muchos `OptimisticLockException` que el cliente tendría que reintentar, degradando la experiencia. El bloqueo pesimista hace que los hilos esperen en cola, garantizando que cada transacción vea el stock real.

### Prevención de Deadlocks

Para evitar deadlocks cuando una orden tiene múltiples productos, los IDs de producto se **ordenan numéricamente antes de adquirir los bloqueos**. Esto garantiza que dos transacciones concurrentes que quieran los productos A y B siempre los bloqueen en el mismo orden (A → B), nunca en orden cruzado.

```java
List<Long> sortedProductIds = request.items().stream()
    .map(Dtos.OrderItemRequest::productId)
    .distinct()
    .sorted()   // ← orden consistente
    .toList();
```

### Nivel de Aislamiento

Se usa `Isolation.REPEATABLE_READ` en la transacción de creación de órdenes. Esto garantiza que si la misma fila se lee dos veces dentro de la misma transacción, siempre devuelve el mismo valor, evitando lecturas fantasma durante el proceso de validación de stock.

### Evidencia de la prueba

La clase `ConcurrencyTest` simula **20 compradores simultáneos** intentando adquirir un producto con **5 unidades** de stock. El test verifica que:
1. El stock final nunca sea negativo.
2. Las compras exitosas no superen el stock inicial.
3. `stock_final + compras_exitosas == stock_inicial` siempre se cumpla.

---

## 2. Estrategia de Seguridad

### Autenticación con JWT

Se usa **JSON Web Tokens (JWT)** stateless firmados con HMAC-SHA256. Las ventajas sobre sesiones en servidor:
- **Escalabilidad**: cualquier instancia puede validar el token sin consultar una sesión centralizada.
- **Sin estado**: el servidor no mantiene sesiones, reduciendo uso de memoria.
- **Portabilidad**: funciona en apps móviles, SPAs y microservicios.

El token incluye el email del usuario y su rol, lo que permite autorizar sin consultar la base de datos en cada request.

### Contraseñas seguras

Las contraseñas se almacenan con **BCrypt** (factor de costo 10 por defecto). BCrypt incluye un salt automático por cada hash, lo que protege contra ataques de rainbow tables. Nunca se almacena la contraseña en texto plano.

### Autorización por roles (RBAC)

Se definen dos roles: `ADMIN` y `CLIENTE`. La autorización se aplica en dos niveles:

1. **A nivel de ruta** en `SecurityConfig`: reglas globales por patrón de URL y método HTTP.
2. **A nivel de método** con `@PreAuthorize("hasRole('ADMIN')")`: más granular y cercano a la lógica de negocio.

Adicionalmente, en `OrderService.getOrderById()` se verifica programáticamente que un `CLIENTE` solo pueda acceder a sus propias órdenes, incluso si la ruta es correcta. Los intentos de acceso no autorizado se registran en auditoría.

### Protección de endpoints

- Todos los endpoints excepto `/api/auth/**` y `GET /api/products/**` requieren autenticación.
- El filtro `JwtAuthenticationFilter` intercepta cada request, valida el token y puebla el `SecurityContext`.
- Los errores de autenticación retornan `401 Unauthorized`; los de autorización, `403 Forbidden`.

---

## 3. Arquitectura Elegida: Capas Clásica

### Estructura

```
Controller → Service → Repository → Entity
```

### Justificación de responsabilidades

**Controller (capa de presentación):**
- Recibe y valida los requests HTTP.
- Delega toda la lógica al Service.
- Retorna los DTOs apropiados con códigos HTTP correctos.
- No conoce JPA ni la base de datos.

**Service (capa de negocio):**
- Contiene toda la lógica de negocio (validaciones, cálculos, reglas).
- Maneja las transacciones (`@Transactional`).
- Orquesta múltiples repositorios cuando es necesario.
- Llama al servicio de auditoría.

**Repository (capa de datos):**
- Extiende `JpaRepository` para operaciones CRUD estándar.
- Define queries personalizadas con `@Query` cuando es necesario.
- Aplica bloqueos (`@Lock`) para control de concurrencia.

**Entity (dominio):**
- Representa el modelo de datos con anotaciones JPA.
- Contiene lógica de dominio pura (ej: `Product.decreaseStock()`).
- No depende de ninguna capa superior.

### ¿Por qué capas y no hexagonal?

La arquitectura hexagonal es ideal cuando se necesita intercambiar adaptadores (cambiar de PostgreSQL a MongoDB, o de REST a gRPC) sin tocar el dominio. Para este proyecto de tamaño mediano con un único adaptador de persistencia (JPA) y un único puerto de entrada (REST), la arquitectura en capas es **más sencilla, más legible y suficiente**. Añadir puertos y adaptadores habría introducido complejidad accidental sin beneficio real.

### Bajo acoplamiento

- Los DTOs (`record`) se usan exclusivamente en los Controllers, nunca en el Service.
- Las entidades JPA nunca se exponen directamente al cliente.
- El `AuditService` usa `@Async` con `Propagation.REQUIRES_NEW`, por lo que es completamente independiente del flujo principal: si falla el log de auditoría, la orden igual se crea.

---

## 4. Persistencia y Transacciones

### Soft Delete

Los productos eliminados se marcan como `active = false` en vez de eliminarse físicamente. Esto preserva el historial de órdenes que referencian esos productos, garantizando integridad referencial del historial.

### Auditoría automática

Con `@EntityListeners(AuditingEntityListener.class)` y `@EnableJpaAuditing`, los campos `createdAt` y `updatedAt` se populan automáticamente sin código manual.

### Transacciones en órdenes

La creación de una orden usa `@Transactional(isolation = Isolation.REPEATABLE_READ)`. Si cualquier parte del proceso falla (stock insuficiente, producto no encontrado, error de BD), toda la operación hace rollback: el stock no se descuenta y la orden no se persiste.
