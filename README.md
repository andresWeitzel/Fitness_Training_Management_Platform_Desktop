# Fitness Training Management Platform

Aplicación de escritorio para la operación de **gimnasios y centros de entrenamiento**: clientes, credenciales de acceso, membresías, cobros, recepción, entrenamiento y nutrición.

Producto nuevo en **Java 21 / JavaFX / PostgreSQL**. No es una traducción línea a línea de [DSOO_ClubDeportivo](https://github.com/andresWeitzel/DSOO_ClubDeportivo) (C# / WinForms / MySQL). Ese sistema se usó como **fuente de ideas y alcance**; se reimplementan las reglas de negocio útiles y se dejan afuera decisiones de diseño que no conviene portar.

| | Origen (club) | Esta plataforma |
|--|---------------|-----------------|
| Usuarios de negocio | Socio y visitante | **Cliente** (con planes/membresías cuando existan) |
| Stack | C# .NET 8, WinForms, MySQL, SPs | Java 21, JavaFX, JPA, Flyway, PostgreSQL |
| Identidad | Login con SP, claves en claro | BCrypt, roles y permisos |
| Persistencia | CRUD principalmente por stored procedures | Repositorios + migraciones Flyway |

---

## 1. Introducción

El sistema informatiza la **gestión diaria de un centro de entrenamiento**: alta de clientes, identificación (número de cliente, carnet y QR), control de acceso futuro, cobros con mora, planes de entrenamiento y seguimiento nutricional.

El personal interno (administración, recepción, entrenadores y nutricionistas) opera la aplicación de escritorio. **El cliente no ingresa** a este software de gestión.

Este README condensa el análisis de alcance (equivalente al documento de análisis del TP del club). El detalle de ejecución técnica está al final.

---

## 2. Alcance del sistema

| Módulo | Contenido | Estado |
|--------|-----------|--------|
| **Identidad y sesión** | Login, roles, menú filtrado por permiso, panel de inicio | Implementado |
| **Clientes** | Ficha (documento único, contacto), baja lógica, búsqueda | Implementado |
| **Credenciales** | Número de cliente (`CLI-xxxxxx`), carnet 12 meses, código QR | Implementado |
| **Membresías** | Planes, alta de membresía, vencimientos | Planificado |
| **Pagos y mora** | Cobro, recargo por atraso, reactivar acceso al regularizar | Planificado |
| **Recepción (check-in)** | Ingreso, capacidad diaria, bloqueo por deuda | Planificado |
| **Personal** | ABM de usuarios internos y asignación de roles | Planificado (usuarios seed hoy) |
| **Entrenamiento** | Ejercicios y rutinas **estructuradas** (no texto libre) | Planificado |
| **Evaluaciones** | Historial de evaluaciones físicas (no pisar el registro anterior) | Planificado |
| **Nutrición** | Turnos y planes; ficha de salud con historial | Planificado |
| **Analytics** | Indicadores, vencimientos y mora | Planificado |
| **Liquidación de haberes** | Sueldos de profesores (último día hábil en el origen) | **Fuera de alcance** en esta etapa |

**Dentro de alcance (ideas que se conservan del club):** mora/recargo, bloqueo de acceso por deuda, capacidad diaria, carnet, ficha de salud, turnos de nutrición, UI según rol.

**Fuera de alcance (no se porta):** capa de negocio en stored procedures, contraseñas en texto plano, borrado físico en cascada, WinForms como servicio, un solo arancel fijo, rutinas en texto libre, ficha médica 1:1 que se sobrescribe, liquidación de sueldos en el primer tramo.

---

## 3. Requerimientos funcionales

Estado: **I** implementado · **P** planificado · **N** no se implementa en esta etapa.

| ID | Descripción | Módulo | Origen club | Estado |
|----|-------------|--------|-------------|--------|
| RF-01 | Iniciar sesión con usuario/contraseña (BCrypt) y sesión por rol | Identidad | Login / `IngresoLogin` | I |
| RF-02 | Configurar y persistir conexión a PostgreSQL; crear base y migrar (Flyway) | Identidad | `FormConfiguracionConexion` | I |
| RF-03 | Ver panel con indicadores reales (clientes, carnets, QR, bajas) | Panel | Panel principal | I |
| RF-04 | Registrar y editar cliente (documento único, nombre, contacto) | Clientes | RF-01 socios | I |
| RF-05 | Buscar clientes por documento, nombre o email | Clientes | FormSocios | I |
| RF-06 | Dar de baja lógica (no elimina historial) | Clientes | — (el origen borraba) | I |
| RF-07 | Asignar número de cliente automático `CLI-xxxxxx` | Credenciales | Socio / carnet | I |
| RF-08 | Emitir y renovar carnet con vencimiento a 12 meses | Credenciales | RF-03 | I |
| RF-09 | Generar credencial QR para check-in futuro | Credenciales | — (extensión) | I |
| RF-10 | Filtrar menú y acciones según permisos (consulta vs gestión) | Identidad | `Permisos.cs` | I |
| RF-11 | Registrar visitante / pase diario | Clientes | RF-02 | P |
| RF-12 | ABM de planes y membresías con vencimiento | Membresías | RF-04 cuotas | P |
| RF-13 | Registrar pago de membresía | Pagos | RF-04 | P |
| RF-14 | Bloquear acceso si hay deuda (mora) | Pagos / Check-in | RF-05 | P |
| RF-15 | Registrar pago de mora/recargo y reactivar acceso | Pagos | RF-06 | P |
| RF-16 | Cobrar ingreso diario (equivalente visitante) | Pagos | RF-07 | P |
| RF-17 | Registrar check-in (carnet/QR) y cupo diario | Recepción | Acceso club | P |
| RF-18 | ABM de personal interno y roles | Personal | RF-08 | P |
| RF-19 | Gestionar ejercicios y rutinas estructuradas | Entrenamiento | RF-10 | P |
| RF-20 | Registrar evaluaciones físicas con historial | Evaluaciones | — | P |
| RF-21 | Turnos y planes de nutrición | Nutrición | RF-12 | P |
| RF-22 | Ficha de salud / restricciones (historial, no overwrite 1:1) | Nutrición | RF-13, RF-14 | P |
| RF-23 | Listados de vencimientos y mora | Analytics | RF-15, RF-16 | P |
| RF-24 | Reportes de operación (ingresos, ocupación) | Analytics | RF-17 (parcial) | P |
| RF-25 | Asistencia diaria de profesores | Personal | RF-09 | N |
| RF-26 | Liquidar haberes mensuales | Personal | RF-11 | N |

Foto de cliente (`photo_path`) está prevista en el esquema; la carga desde la UI todavía no.

---

## 4. Casos de uso

| CU | Nombre | Actor | Vista / módulo | Estado |
|----|--------|-------|----------------|--------|
| CU-01 | Configurar PostgreSQL | Operador | `db-setup.fxml` | I |
| CU-02 | Iniciar sesión | Todo personal interno | `login.fxml` | I |
| CU-03 | Consultar panel | Quien tenga `DASHBOARD_VIEW` | `dashboard.fxml` | I |
| CU-04 | Registrar / editar cliente | Admin, Recepción | `clients.fxml` | I |
| CU-05 | Consultar clientes (solo lectura) | Entrenador, Nutricionista | `clients.fxml` | I |
| CU-06 | Dar de baja cliente | Admin, Recepción | `clients.fxml` | I |
| CU-07 | Emitir / renovar carnet | Admin, Recepción | `clients.fxml` | I |
| CU-08 | Generar QR de acceso | Admin, Recepción | `clients.fxml` | I |
| CU-09 | Cerrar sesión | Todo personal interno | Shell | I |
| CU-10 | Gestionar membresías | Admin, Recepción | Navegación protegida | P |
| CU-11 | Registrar pagos y mora | Admin, Recepción | Navegación protegida | P |
| CU-12 | Controlar ingreso (check-in) | Admin, Recepción | Navegación protegida | P |
| CU-13 | Administrar personal | Admin | Navegación protegida | P |
| CU-14 | Armar rutina de entrenamiento | Admin, Entrenador | Navegación protegida | P |
| CU-15 | Registrar evaluación física | Admin, Entrenador, Nutricionista | Navegación protegida | P |
| CU-16 | Gestionar nutrición | Admin, Nutricionista | Navegación protegida | P |
| CU-17 | Consultar analytics | Admin | Navegación protegida | P |

Los módulos planificados ya aparecen en el menú si el rol tiene permiso; muestran una pantalla de **próximo módulo**, no datos inventados.

### Control de acceso por rol

La UI filtra el menú (`NavigationCatalog` + `AuthorizationService`). En Clientes, `CLIENTS_VIEW` permite consulta y `CLIENTS_MANAGE` habilita alta, edición, baja y credenciales.

| Módulo | Admin | Recepción | Entrenador | Nutricionista | Cliente |
|--------|:-----:|:---------:|:----------:|:-------------:|:-------:|
| Panel (CU-03) | ✓ | ✓ | ✓ | ✓ | — |
| Clientes consulta (CU-05) | ✓ | ✓ | ✓ | ✓ | — |
| Clientes gestión + carnet/QR (CU-04, 06–08) | ✓ | ✓ | — | — | — |
| Membresías (CU-10) | ✓ | ✓ | — | — | — |
| Pagos / mora (CU-11) | ✓ | ✓ | — | — | — |
| Recepción / check-in (CU-12) | ✓ | ✓ | — | — | — |
| Personal (CU-13) | ✓ | — | — | — | — |
| Entrenamiento (CU-14) | ✓ | — | ✓ | — | — |
| Evaluaciones (CU-15) | ✓ | — | ✓ | ✓ | — |
| Nutrición (CU-16) | ✓ | — | — | ✓ | — |
| Analytics (CU-17) | ✓ | — | — | — | — |
| Configuración de base | ✓ (desde login) | ✓ | ✓ | ✓ | — |

El **cliente no opera** esta aplicación. Equivale a socio/visitante del club: son sujetos de gestión, no usuarios del escritorio.

Permisos en código: `PermissionCode` y tabla `role_permissions` (Flyway `V1__init_identity.sql`).

---

## 5. Glosario

| Término | Definición |
|---------|------------|
| Cliente | Persona inscripta en el centro (reemplaza “socio” / “visitante” del origen). |
| Credencial | Identificador de acceso: número de cliente, carnet o QR. |
| Carnet | Credencial física/lógica con vigencia (hoy 12 meses). |
| Membresía | Plan contratado con vigencia (cuota del club). |
| Mora | Deuda vencida; en el origen suspendía el acceso hasta regularizar. |
| Check-in | Registro de ingreso al predio. |
| Baja lógica | El cliente deja de estar activo; no se borra el historial. |
| Rol | Perfil interno: ADMIN, RECEPTIONIST, TRAINER, NUTRITIONIST. |

---

## Stack

Java 21, JavaFX (FXML/CSS), Maven, JPA/Hibernate (schema `validate`), PostgreSQL 16, Flyway, JUnit 5, Mockito. Sin Spring Boot. Arquitectura: **FXML → Controller → Service → Repository → JPA**.

---

## Instalación en cliente (entrega)

Documentación completa: [`install/CLIENTE.md`](install/CLIENTE.md).

| Uso | Comando / archivo |
|-----|-------------------|
| **Desarrollo (siempre últimos cambios)** | Doble clic en `run.bat` → compila y ejecuta |
| Probar build empaquetado | `scripts\start-app-packaged.bat` (después de `package.bat`) |
| Empaquetar para cliente | `package.bat` → carpeta `target\client-dist\` |
| Base Docker (dev o cliente) | `scripts\start-db.bat` |
| Backup | `scripts\backup-db.bat` |

Antes de entregar: copiar `.env.example` → `.env` y **cambiar la contraseña** de PostgreSQL.

---

## Requisitos para ejecutar (desarrollo)

- JDK 21
- Maven 3.9+
- Docker Desktop (PostgreSQL 16 en contenedor)
- Windows 10 o superior (la UI es nativa JavaFX; **no** se Dockeriza la app)

## Base de datos (Docker)

La app corre en Windows. Solo Postgres va en Docker.

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
| Contraseña | la de `.env` (dev: `postgres` si no creó `.env`) |

Datos en el volumen `fitness_training_pgdata`. Borrar todo: `docker compose down -v`.

Si en el host hay un servicio PostgreSQL de Windows en el mismo puerto, dejarlo detenido para que no ocupe `5432`.

## Cómo ejecutar

```bash
copy .env.example .env
docker compose up -d
mvn -q test
mvn javafx:run
```

O en Windows: `scripts\start-db.bat` y doble clic en **`run.bat`** (recompila siempre; no usa `target\client\` viejo).

La primera vez confirma PostgreSQL. Crea la base si hace falta, aplica Flyway y carga usuarios/clientes de desarrollo.

Configuración: `%USERPROFILE%\.fitness-training\database.properties`.

## Usuarios de desarrollo

| Usuario | Contraseña | Rol |
|---------|------------|-----|
| `admin` | `1234` | ADMIN |
| `empleado1` | `emp123` | RECEPTIONIST |
| `juan_prof` | `prof123` | TRAINER |
| `maria_nutri` | `nutri123` | NUTRITIONIST |

En el login, **Mostrar cuentas de prueba** completa usuario y clave. Las contraseñas se guardan con BCrypt.

## Cómo usarlo hoy

1. `docker compose up -d` y `mvn javafx:run`.
2. Conexión: valores de la tabla PostgreSQL → **Continuar al login**.
3. Entrar como administrador (o mostrar cuentas de prueba).
4. En el **Panel**: indicadores y accesos rápidos.
5. En **Clientes**: alta, búsqueda, carnet y QR. Entrenador y nutricionista solo consultan.

## Estado actual

Implementado: autenticación, roles, shell, panel y módulo de clientes/credenciales.

Siguiente tramo natural: **membresías → pagos (mora/recargo del club) → check-in**.
