<div align="center">

![Fitness Training Management Platform](../docs/assets/app-hero.png)

</div>

<div align="right">
  <img width="25" height="25" src="../docs/assets/icons/backend/java/png/java.png" alt="Java" />
  <img width="25" height="25" src="../docs/assets/icons/backend/java/png/jsf.png" alt="JavaFX" />
  <img width="25" height="25" src="../docs/assets/icons/database/png/postgres.png" alt="PostgreSQL" />
  <img width="25" height="25" src="../docs/assets/icons/devops/png/maven.png" alt="Maven" />
  <img width="25" height="25" src="../docs/assets/icons/devops/png/ci-circle.png" alt="Flyway" />
  <img width="25" height="25" src="../docs/assets/icons/devops/png/docker.png" alt="Docker" />
  <img width="25" height="25" src="../docs/assets/icons/devops/png/git.png" alt="Git" />
  &nbsp;&nbsp;
  <a href="./README.es.md" target="_blank">
    <img src="../docs/assets/translation/arg-flag.svg" width="65" height="40" alt="Español" />
  </a>
  <a href="../README.md" target="_blank">
    <img src="../docs/assets/translation/eeuu-flag.svg" width="65" height="40" alt="English" />
  </a>
</div>

<br>

<div align="center">

# Fitness Training Management Platform ![(status-completed)](../docs/assets/icons/badges/status-completed.svg)

</div>

Aplicación de escritorio para centros de entrenamiento y gimnasios: clientes, membresías, pagos, recepción, personal, entrenamiento, evaluaciones, nutrición y analytics.

Java 21, JavaFX, PostgreSQL 16, Flyway, Maven, Docker Compose y despliegue portable para Windows.

<br>

## Index 📜

<details>
  <summary>Ver detalle</summary>

<div align="right">

`Última actualización: 28/08/26`

</div>

### Sección 1) Descripción y configuración

* [1.0) Descripción del proyecto](#10-descripción-del-proyecto-)
* [1.1) Desarrollo y configuración](#11-desarrollo-y-configuración-)
* [1.2) Tecnologías y módulos](#12-tecnologías-y-módulos-)

### Sección 2) Pruebas

* [2.0) Pruebas](#20-pruebas-)

### Sección 3) Capturas

* [3.0) Galería de pantallas](#30-galería-de-pantallas-)

### Sección 4) Referencias y entrega

* [4.0) Cuentas demo](#40-cuentas-demo-)
* [4.1) Estructura del repositorio](#41-estructura-del-repositorio-)
* [4.2) Documentación](#42-documentación-)
* [4.3) Entrega al gimnasio](#43-entrega-al-gimnasio-)
* [4.4) Pruebas funcionales (YouTube)](#44-pruebas-funcionales-youtube-)
* [4.5) Licencia](#45-licencia-)

<br>

</details>

<br>

## Sección 1) Descripción y configuración

### 1.0) Descripción del proyecto [🔝](#index-)

<details>
  <summary>Ver detalle</summary>
  <br>

Fitness Training Management Platform es una aplicación de escritorio (Windows) para la operación diaria de un centro de entrenamiento: clientes y credenciales (carnet, QR), membresías, pagos, recepción, personal, entrenamiento, evaluaciones, nutrición y analytics (reportes + gráficos).

Características: multi-rol (admin, recepción, entrenador, nutricionista), check-in con bloqueo por mora, exportación CSV para Excel, conexión a PostgreSQL (Docker, sistema o red) y migraciones Flyway.

Fuera de alcance en esta etapa: liquidación de haberes y asistencia de profesores ([especificación funcional](../docs/especificacion-funcional.md)).

Arquitectura por capas: JavaFX (FXML) → servicios de dominio → JDBC → PostgreSQL 16.

<br>

</details>

### 1.1) Desarrollo y configuración [🔝](#index-)

<details>
  <summary>Ver detalle</summary>
  <br>

Requisitos: JDK 21, Maven 3.9+, Windows 10/11. Base de datos: Docker Desktop o PostgreSQL 16 instalado en el sistema.

Probar en tu PC (desarrollo):

```bat
copy .env.example .env
docker compose up -d
run.bat
```

`run.bat` compila y ejecuta la app desde el código fuente. No genera el zip del cliente.

Variables en `.env`: `POSTGRES_PORT`, `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`. Flyway corre al iniciar la app. En desarrollo se cargan datos demo (seeders).

<br>

</details>

### 1.2) Tecnologías y módulos [🔝](#index-)

<details>
  <summary>Ver detalle</summary>
  <br>

| Área | Tecnología |
|------|------------|
| Runtime / UI | Java 21, JavaFX 21, FXML |
| Build | Maven |
| Base de datos | PostgreSQL 16, Flyway |
| Seguridad | BCrypt |
| Contenedor DB | Docker Compose (opcional) |
| Pruebas | JUnit 5, Mockito |

| Módulo | Rol típico |
|--------|------------|
| Panel, Clientes, Membresías, Pagos | Admin, Recepción |
| Recepción (check-in) | Recepción |
| Personal | Admin |
| Entrenamiento, Evaluaciones | Admin, Entrenador |
| Nutrición | Admin, Nutricionista |
| Analytics (reportes y gráficos) | Admin |

<br>

</details>

<br>

## Sección 2) Pruebas

### 2.0) Pruebas [🔝](#index-)

Pruebas en `src/test/java` (lógica de negocio, CSV, analytics).

```bat
mvn test
mvn test -Dtest=CsvExporterTest
mvn compile
```

<br>

## Sección 3) Capturas

### 3.0) Galería de pantallas [🔝](#index-)

<details>
  <summary>Ver detalle</summary>
  <br>

Capturas: 27/08/2026 — [docs/README.md](../docs/README.md)

| Panel | Login | Recepción |
|:---:|:---:|:---:|
| ![Panel](../docs/img/panel-inicio.png) | ![Login](../docs/img/login-iniciar-sesion.png) | ![Recepción](../docs/img/recepcion-control-ingreso.png) |

| Pagos | Nutrición | Analytics — Reportes |
|:---:|:---:|:---:|
| ![Pagos](../docs/img/pagos-listado-cobro.png) | ![Nutrición](../docs/img/nutricion-turno-detalle.png) | ![Reportes](../docs/img/analytics-reportes-mora.png) |

| Analytics — Gráficos |
|:---:|
| ![Gráficos](../docs/img/analytics-graficos.png) |

<br>

</details>

<br>

## Sección 4) Referencias y entrega

### 4.0) Cuentas demo [🔝](#index-)

Con seeders de desarrollo activos (no usar en producción):

| Usuario | Contraseña | Rol |
|---------|------------|-----|
| `admin` | `1234` | Administrador |
| `empleado1` | `emp123` | Recepción |
| `juan_prof` | `prof123` | Entrenador |
| `maria_nutri` | `nutri123` | Nutricionista |

### 4.1) Estructura del repositorio [🔝](#index-)

```
├── docs/           # Especificación, capturas, assets
├── install/        # Guías de instalación
├── src/            # Código, FXML, migraciones Flyway
├── run.bat         # Desarrollo: compilar y ejecutar
├── package.bat     # Generar zip para el gimnasio
└── docker-compose.yml
```

### 4.2) Documentación [🔝](#index-)

* [¿Qué ejecutar?](../install/QUE-EJECUTAR.md)
* [Capturas y assets](../docs/README.md)
* [Instalación cliente](../install/CLIENTE.md)
* [Configurar Java y PostgreSQL](../install/CONFIGURAR-JAVA-POSTGRES.md)
* [Desarrollo](../install/DESARROLLO.md)
* [Especificación funcional](../docs/especificacion-funcional.md)
* [README de referencia (estructura)](https://github.com/andresWeitzel/ApiRest_Electronic_Devices_ExpressJS)

### 4.3) Entrega al gimnasio [🔝](#index-)

| Script | Quién | Qué hace |
|--------|-------|----------|
| `run.bat` | Desarrollador | Prueba la app en tu PC (Maven + código fuente). No genera zip. |
| `package.bat` | Desarrollador | Compila y crea `target\FitnessTraining-client-win64.zip` (solo la app). |
| `Iniciar.bat` | Cliente | Dentro del zip: abre la app (+ Docker si está instalado). |

El zip no incluye Java ni PostgreSQL. El gimnasio los instala una vez en el sistema (o usa Docker). Guía: [CONFIGURAR-JAVA-POSTGRES.md](../install/CONFIGURAR-JAVA-POSTGRES.md).

Conexión a la base: Docker (opcional), PostgreSQL local o servidor en red — [CLIENTE.md](../install/CLIENTE.md).

### 4.4) Pruebas funcionales (YouTube) [🔝](#index-)

Playlist de pruebas funcionales (próximamente).

<!-- Agregar enlace cuando esté publicado:
* [Playlist YouTube](https://www.youtube.com/...) <img src="../docs/assets/icons/social-networks/yt.svg" width="25" />
-->

### 4.5) Licencia [🔝](#index-)

GPL-3.0 — ver [LICENSE](../LICENSE).

<br>
