# Configurar Java 21 y PostgreSQL para la aplicación

La **Fitness Training Management Platform** se conecta a PostgreSQL por **red JDBC** (servidor, puerto, base, usuario y contraseña). No importa si PostgreSQL corre en Docker, como servicio de Windows o en otra PC: la app usa los mismos datos de conexión.

## Launcher del zip: `Iniciar.bat`

En la entrega al gimnasio, **`Iniciar.bat`** (raíz del zip) orquesta la instalación y el arranque:

| Paso | Acción |
|------|--------|
| 1 | `scripts\setup\setup-first-run.bat` — primera vez: `db\.env` y `database.properties` |
| 2 | `scripts\java\check-java.bat` — si falta Java 21, ofrece instalar Temurin con **winget** |
| 3 | `scripts\docker\check-docker.bat` — si no hay Docker, ofrece instalar Docker Desktop (opcional) |
| 4 | Si hay Docker → `scripts\db\start-db-silent.bat` levanta PostgreSQL |
| 5 | `app\FitnessTraining.bat` — abre la app; **Flyway** migra al conectar |

Requisitos para winget: Windows 10/11, permisos de instalación. Si winget falla, instalar manualmente (secciones 1 y 3 más abajo).

Scripts manuales en el zip: `scripts\db\start-db.bat`, `scripts\db\stop-db.bat`, `scripts\db\backup-db.bat`.

En el **repositorio** (desarrollo), los equivalentes están en `scripts\dev\` — ver [DESARROLLO.md](./DESARROLLO.md) y [scripts/README.md](../scripts/README.md).

---

## ¿La app funciona con Docker y con PostgreSQL del sistema?

**Sí.** Está pensada para ambos (y también para servidor en red):

| Origen de PostgreSQL | Servidor en la app | Notas |
|---------------------|-------------------|--------|
| **Docker** (contenedor en esta PC) | `localhost` | `Iniciar.bat` levanta el contenedor si Docker está instalado |
| **Sistema** (servicio Windows en esta PC) | `localhost` | El servicio PostgreSQL debe estar **Iniciado** |
| **Red** (otra PC servidor) | IP del servidor (ej. `192.168.1.50`) | Solo Java + app en cada mostrador |

La app:

1. Conecta con `jdbc:postgresql://host:puerto/base`
2. Crea la base `fitness_training` si no existe (desde conexión a `postgres`)
3. Aplica migraciones Flyway al iniciar
4. Guarda la configuración en `%USERPROFILE%\.fitness-training\database.properties`

---

## 1) Instalar y configurar Java 21

### Opción A — Automática (desde el zip)

Ejecutar **`Iniciar.bat`**. Si Java no está en PATH, `scripts\java\check-java.bat` ofrece:

```bat
winget install -e --id EclipseAdoptium.Temurin.21.JRE
```

Tras instalar, cerrar la ventana, abrir una nueva y volver a ejecutar `Iniciar.bat`.

### Opción B — Manual

1. Ir a [Adoptium Temurin 21](https://adoptium.net/temurin/releases/?version=21&os=windows&arch=x64&package=jre)
2. Descargar **JRE 21** (o JDK 21) para Windows x64
3. Ejecutar el instalador

### Durante la instalación

- Marcar **“Set JAVA_HOME variable”** (si aparece)
- Marcar **“Add to PATH”** / agregar Java al PATH
- Completar la instalación

### Verificar

Abrir **cmd** y ejecutar:

```bat
java -version
```

Debe mostrar algo como `openjdk version "21.x.x"`.

Si falla:

1. Reiniciar la PC (o cerrar y abrir cmd)
2. O agregar manualmente al PATH la carpeta `bin` del Java instalado, por ejemplo:
   `C:\Program Files\Eclipse Adoptium\jre-21.x.x-hotspot\bin`

### Probar la aplicación

```bat
cd C:\FitnessTraining\app
FitnessTraining.bat
```

Si Java está bien, abre la ventana de la app (puede pedir configurar conexión a la base).

---

## 2) Instalar PostgreSQL en el sistema (sin Docker)

### Descarga

1. [PostgreSQL para Windows](https://www.postgresql.org/download/windows/)
2. Usar el instalador de **PostgreSQL 16** (EDB / Stack Builder)

### Durante la instalación

| Paso | Valor recomendado |
|------|-------------------|
| Puerto | `5432` (por defecto) |
| Usuario superusuario | `postgres` |
| Contraseña | **Anotarla** — la usarás en la app |
| Locale | Por defecto |

Dejar marcado que PostgreSQL se instala como **servicio de Windows** (se inicia con el sistema).

### Verificar que el servicio está activo

1. `Win + R` → `services.msc`
2. Buscar **postgresql-x64-16** (o similar)
3. Estado: **En ejecución**

O en cmd (si `pg_isready` está en PATH):

```bat
pg_isready -h localhost -p 5432
```

Respuesta esperada: `accepting connections`

### Configurar la app (primera vez)

1. Ejecutar `Iniciar.bat` o `app\FitnessTraining.bat`
2. En **Configurar conexión**:

| Campo | Valor |
|-------|--------|
| Servidor | `localhost` |
| Puerto | `5432` |
| Base de datos | `fitness_training` |
| Usuario | `postgres` (o el que creaste) |
| Contraseña | La del instalador |

3. **Probar conexión** → **Continuar al login**

La app crea la base `fitness_training` si no existe y aplica las tablas (Flyway).

### No hace falta Docker

Con PostgreSQL del sistema:

- No uses `scripts\db\start-db.bat` (es solo para Docker)
- `Iniciar.bat` abre directamente la aplicación

---

## 3) PostgreSQL con Docker (opcional)

Usar este modo si **no** quieres instalar PostgreSQL en Windows pero sí tienes Docker Desktop.

### Instalar Docker Desktop

**Opción A — Automática:** `Iniciar.bat` llama a `scripts\docker\check-docker.bat` si Docker no está instalado (winget).

**Opción B — Manual:**

1. [Docker Desktop para Windows](https://www.docker.com/products/docker-desktop/)
2. Instalar y reiniciar si pide
3. Abrir Docker Desktop y esperar que diga que está **running**

### Configurar contraseña de la base

En la carpeta del zip descomprimido:

```bat
cd C:\FitnessTraining\db
copy .env.example .env
notepad .env
```

Editar al menos:

```env
POSTGRES_PASSWORD=una_contraseña_segura
```

Opcional: `POSTGRES_PORT`, `POSTGRES_DB`, `POSTGRES_USER` (por defecto `5432`, `fitness_training`, `postgres`).

### Levantar PostgreSQL

```bat
cd C:\FitnessTraining
Iniciar.bat
```

O solo la base:

```bat
scripts\db\start-db.bat
```

Verificar:

```bat
cd db
docker compose ps
```

Contenedor `fitness-training-postgres` en estado **running**.

### Conexión en la app

| Campo | Valor |
|-------|--------|
| Servidor | `localhost` |
| Puerto | El de `db\.env` (normalmente `5432`) |
| Base | `fitness_training` |
| Usuario | `postgres` |
| Contraseña | La de `POSTGRES_PASSWORD` en `.env` |

`scripts\setup\setup-first-run.bat` (al usar `Iniciar.bat`) puede crear esta conexión automáticamente leyendo `db\.env`.

---

## 4) PostgreSQL en otra PC (red / varios mostradores)

### En la PC servidor

1. Instalar PostgreSQL 16 (modo sistema, no Docker en cada puesto)
2. Anotar IP fija, por ejemplo `192.168.1.50`
3. Crear usuario y base (o dejar que la app cree `fitness_training` desde un primer puesto admin)
4. En `postgresql.conf`: `listen_addresses = '*'` o la IP local
5. En `pg_hba.conf`: permitir la subred del gimnasio, por ejemplo:
   `host all all 192.168.1.0/24 scram-sha-256`
6. Reiniciar servicio PostgreSQL
7. Firewall Windows: permitir entrada TCP **5432** solo en red local

### En cada mostrador (recepción, etc.)

1. Instalar **solo Java 21**
2. Copiar carpeta `app\` del zip (o el zip completo)
3. Ejecutar `FitnessTraining.bat`
4. Configurar conexión:

| Campo | Valor |
|-------|--------|
| Servidor | `192.168.1.50` (IP del servidor) |
| Puerto | `5432` |
| Base | `fitness_training` |
| Usuario / contraseña | Los del servidor |

---

## 5) Resumen rápido por escenario

| Escenario | Instalar | Ejecutar | Servidor en la app |
|-----------|----------|----------|-------------------|
| Todo en una PC, sin Docker | Java + PostgreSQL | `Iniciar.bat` | `localhost` |
| Todo en una PC, con Docker | Java + Docker | `Iniciar.bat` | `localhost` |
| Servidor + varios PCs | Java en cada PC; PG en servidor | `FitnessTraining.bat` | IP del servidor |

---

## 6) Archivo de configuración guardado

Tras una conexión exitosa, la app guarda:

`%USERPROFILE%\.fitness-training\database.properties`

Ejemplo:

```properties
db.host=localhost
db.port=5432
db.name=fitness_training
db.user=postgres
db.password=tu_contraseña
```

El administrador puede cambiar la conexión desde la app (menú de configuración de base de datos).

---

## Solución de problemas

| Problema | Causa habitual | Solución |
|----------|----------------|----------|
| `Java no encontrado` | Java no en PATH | Reinstalar Java con “Add to PATH” o agregar `bin` manualmente |
| `Connection refused` | PostgreSQL no corre | Servicio Windows, Docker, o IP incorrecta |
| `password authentication failed` | Contraseña incorrecta | Revisar contraseña del instalador PG o `db\.env` |
| Puerto 5432 ocupado | Dos PostgreSQL (Docker + sistema) | Usar solo uno, o cambiar `POSTGRES_PORT` en `.env` |
| Red: no conecta | Firewall / `pg_hba.conf` | Abrir 5432 y permitir subred local |

Más contexto: [CLIENTE.md](./CLIENTE.md)
