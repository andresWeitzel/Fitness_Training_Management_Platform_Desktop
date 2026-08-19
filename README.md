# Fitness Training Management Platform

Aplicación de escritorio para la operación de gimnasios y centros de entrenamiento.

Stack: Java 21, JavaFX, Maven, JPA/Hibernate, PostgreSQL, Flyway, JUnit 5.

## Requisitos

- JDK 21
- Maven 3.9+
- Docker Desktop (PostgreSQL 16 en contenedor)

## Base de datos (Docker)

La app JavaFX corre en tu Windows. Solo Postgres va en Docker.

```bash
docker compose up -d
```

Eso se hace **una vez** (o cuando el contenedor no esté levantado). Un cambio en Java/FXML **no** pide rebuild de Docker: alcanza con `mvn javafx:run`.

| Campo | Valor |
|-------|--------|
| Host | `localhost` |
| Puerto | `5432` |
| Base | `fitness_training` |
| Usuario | `postgres` |
| Contraseña | `postgres` |

Datos persistentes en el volumen `fitness_training_pgdata`. Para borrar todo: `docker compose down -v`.

No empaquetamos JavaFX en Docker: necesita ventana nativa de Windows. Meter la UI en un contenedor implicaría rebuild de imagen en cada cambio y un display (VNC/X11) que no aporta en esta etapa.

## Cómo ejecutar

```bash
docker compose up -d
mvn -q test
mvn javafx:run
```

La primera vez confirma los datos de PostgreSQL (los de la tabla de arriba). Crea la base si hace falta, aplica Flyway y genera usuarios de desarrollo.

La configuración queda en `%USERPROFILE%\.fitness-training\database.properties`.

## Usuarios de desarrollo

| Usuario | Contraseña | Rol |
|---------|------------|-----|
| `admin` | `1234` | ADMIN |
| `empleado1` | `emp123` | RECEPTIONIST |
| `juan_prof` | `prof123` | TRAINER |
| `maria_nutri` | `nutri123` | NUTRITIONIST |

Las contraseñas se guardan con BCrypt. El menú se filtra por permisos.

## Clientes y credenciales

Alta con documento único, baja lógica (no borra historial) y credenciales:

- `CLI-000001` número de cliente, generado al dar de alta
- carnet con vencimiento a 12 meses (emitir / renovar)
- código QR para el futuro check-in

TRAINER y NUTRITIONIST ven el listado en solo lectura. ADMIN y RECEPTIONIST pueden gestionar.

## Estado actual

Fase actual: autenticación, roles, shell y módulo de clientes/credenciales. Membresías, check-in, entrenamiento y nutrición siguen como navegación protegida.
