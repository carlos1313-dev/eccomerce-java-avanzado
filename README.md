# Ecommerce API

API REST segura para gestión de órdenes e inventario, protegida con JWT y prevenida para condiciones de carrera con pesimist_lock.

## Requisitos previos

- Java 21
- Docker Desktop
- Maven 3.9+

## Instalación y ejecución

### 1. Clonar el repositorio
```bash
git clone https://github.com/tuusuario/ecommerce-api.git
cd ecommerce-api
```

### 2. Iniciar la base de datos (ya todo está configurado con docker compose)
```bash
docker-compose up -d
```

### 3. Ejecutar la aplicación (estará disponible en http://localhost:8081)
```bash
./mvnw spring-boot:run
```
### 3. Probar la API 
```bash
[./mvnw spring-boot:run](http://localhost:8081/swagger-ui.html)
```
### 4. Ejecutar el TEST (en este test se hace la prueba de concurrencia solicitada, con 20 compradores intentando comprar al mismo tiempo un producto de sólo 15 de stock) 


