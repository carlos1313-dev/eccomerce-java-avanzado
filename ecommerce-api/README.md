# E-Commerce API — Java Backend Avanzado

API REST segura para gestión de órdenes e inventario, construida con **Spring Boot 3**, **Spring Security + JWT**, **JPA/Hibernate** y **PostgreSQL**.

---

## Tecnologías utilizadas

| Categoría | Tecnología |
|---|---|
| Lenguaje | Java 21 |
| Framework | Spring Boot 3.2 |
| Seguridad | Spring Security + JWT (jjwt 0.12) |
| Persistencia | Spring Data JPA + Hibernate |
| Base de datos | PostgreSQL 15+ |
| Documentación | SpringDoc OpenAPI 3 (Swagger UI) |
| Build | Maven |
| Testing | JUnit 5 + H2 (in-memory) |

---

## Requisitos previos

- Java 21+
- Maven 3.9+
- PostgreSQL 15+ corriendo localmente

---

## Instrucciones de ejecución

### 1. Crear la base de datos

```sql
CREATE DATABASE ecommerce_db;
```

### 2. Configurar credenciales (si es necesario)

Edita `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/ecommerce_db
spring.datasource.username=postgres
spring.datasource.password=postgres
```

### 3. Ejecutar la aplicación

```bash
mvn spring-boot:run
```

La aplicación inicia en: `http://localhost:8080`

### 4. Acceder a Swagger UI

```
http://localhost:8080/swagger-ui.html
```

### 5. Ejecutar pruebas (incluye prueba de concurrencia)

```bash
mvn test
```

---

## Credenciales de prueba

Al iniciar por primera vez, registra los usuarios vía `POST /api/auth/register`:

**Admin:**
```json
{
  "name": "Administrador",
  "email": "admin@ecommerce.com",
  "password": "Admin1234",
  "role": "ADMIN"
}
```

**Cliente:**
```json
{
  "name": "Cliente Ejemplo",
  "email": "cliente@ecommerce.com",
  "password": "Cliente1234",
  "role": "CLIENTE"
}
```

Luego haz login en `POST /api/auth/login` y usa el token JWT en el header:
```
Authorization: Bearer <token>
```

---

## Endpoints principales

### Autenticación
| Método | Endpoint | Acceso | Descripción |
|---|---|---|---|
| POST | `/api/auth/register` | Público | Registrar usuario |
| POST | `/api/auth/login` | Público | Login → obtener JWT |

### Productos
| Método | Endpoint | Acceso | Descripción |
|---|---|---|---|
| GET | `/api/products` | Público | Listar productos activos |
| GET | `/api/products/{id}` | Público | Ver un producto |
| POST | `/api/products` | ADMIN | Crear producto |
| PUT | `/api/products/{id}` | ADMIN | Actualizar producto |
| DELETE | `/api/products/{id}` | ADMIN | Soft delete |

### Órdenes
| Método | Endpoint | Acceso | Descripción |
|---|---|---|---|
| POST | `/api/orders` | Autenticado | Crear orden |
| GET | `/api/orders/my` | Autenticado | Mis órdenes |
| GET | `/api/orders/{id}` | Autenticado* | Ver orden por ID |
| GET | `/api/orders` | ADMIN | Todas las órdenes |

*Un CLIENTE solo puede ver sus propias órdenes.

### Auditoría
| Método | Endpoint | Acceso | Descripción |
|---|---|---|---|
| GET | `/api/audit` | ADMIN | Todos los logs |
| GET | `/api/audit/user/{email}` | ADMIN | Logs por usuario |
| GET | `/api/audit/entity/{type}` | ADMIN | Logs por entidad |

---

## Decisiones técnicas

### Java moderno
- **Records** para todos los DTOs: inmutables, compactos, con equals/hashCode automáticos.
- **Streams** en el servicio de órdenes para calcular totales y mapear ítems.
- **Optional** en repositorios para manejar búsquedas sin lanzar null.
- **Text blocks** y **String.formatted()** para mensajes.

### Concurrencia
Ver `JUSTIFICACION_TECNICA.md` para detalle completo.

**Estrategia elegida: Bloqueo pesimista (`SELECT FOR UPDATE`).**
Los productos se bloquean al inicio de cada transacción de compra. Los IDs se ordenan antes de bloquear para evitar deadlocks.

### Arquitectura
Capas clásicas con responsabilidad clara:
- **Controller** → HTTP (entrada/salida)
- **Service** → lógica de negocio y transacciones
- **Repository** → acceso a datos
- **Entity** → modelo de dominio

### Seguridad
- JWT stateless con `BCrypt` para contraseñas
- `@PreAuthorize` para control de acceso a nivel de método
- Auditoría asíncrona para no penalizar el rendimiento

---

## Estructura del proyecto

```
src/
├── main/java/com/ecommerce/
│   ├── EcommerceApiApplication.java
│   ├── audit/          # AuditService (registro asíncrono)
│   ├── config/         # SecurityConfig, OpenApiConfig, AsyncConfig
│   ├── controller/     # AuthController, ProductController, OrderController, AuditController
│   ├── dto/            # Dtos.java (records Java moderno)
│   ├── entity/         # User, Product, Order, OrderItem, AuditLog
│   ├── exception/      # GlobalExceptionHandler y excepciones custom
│   ├── repository/     # Repositorios JPA
│   ├── security/       # JwtUtils, JwtAuthenticationFilter, UserDetailsServiceImpl
│   └── service/        # AuthService, ProductService, OrderService
└── test/java/com/ecommerce/
    └── ConcurrencyTest.java  # Prueba de concurrencia con 20 hilos
```
