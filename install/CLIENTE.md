# Instalación en la PC del cliente

Guía para instalar **Fitness Training Management Platform** en el gimnasio o centro de entrenamiento.

## Requisitos en la PC

| Requisito | Detalle |
|-----------|---------|
| Windows | 10 o superior (64 bits) |
| RAM | 4 GB mínimo recomendado |
| **Java 21** | JDK o JRE — `Iniciar.bat` puede instalarlo con winget, o manual: [Adoptium](https://adoptium.net/) |
| **PostgreSQL 16** | Servicio Windows **o** Docker — `Iniciar.bat` puede ofrecer instalar Docker con winget |
| Docker Desktop | **Opcional.** Si está instalado, `Iniciar.bat` lo usa para levantar la base |

El zip de entrega **no incluye** Java ni PostgreSQL. `Iniciar.bat` puede ayudar a instalar Java y Docker con winget; PostgreSQL del sistema se instala manualmente si no usas Docker.

**Guías:** [CONFIGURAR-JAVA-POSTGRES.md](./CONFIGURAR-JAVA-POSTGRES.md) · [QUE-EJECUTAR.md](./QUE-EJECUTAR.md) · [README.md](./README.md)

### ¿Funciona con Docker y con PostgreSQL del sistema?

**Sí.** La app conecta por JDBC (`host`, puerto, base, usuario, contraseña). Da igual si PostgreSQL corre en un contenedor Docker, como servicio de Windows en la misma PC, o en un servidor de la red. Solo cambian los valores de **Servidor** y las credenciales en la pantalla de conexión (o en `database.properties`).

---

## Launcher: `Iniciar.bat`

```bat
Iniciar.bat
```

**Launcher único del cliente.** El gimnasio solo debe ejecutar este script en el día a día. `Iniciar.bat` llama en orden a los scripts en `scripts\setup\`, `scripts\java\`, `scripts\docker\` y `scripts\db\`, y abre la aplicación — no hace falta ejecutar esos `.bat` por separado.

| Paso (interno) | Qué hace |
|----------------|----------|
| 1 | Primera vez: `db\.env` y conexión local |
| 2 | Verifica Java 21; ofrece winget si falta |
| 3 | Verifica Docker (opcional); ofrece winget si falta |
| 4 | Si hay Docker, levanta PostgreSQL |
| 5 | Abre la app; Flyway migra al conectar |

Solo la app, sin verificar requisitos: `app\FitnessTraining.bat`.

---

## Tres formas de conectar la base

| Modo | Dónde está PostgreSQL | Docker | Qué hace `Iniciar.bat` |
|------|----------------------|--------|-------------------------|
| **A — Docker** | Contenedor en esta PC | Sí | Levanta Docker + abre la app |
| **B — Local** | Servicio Windows en esta PC | No | Solo abre la app (PG ya debe estar activo) |
| **C — Red** | Otra PC servidor en la LAN | No | Solo abre la app; configurar IP del servidor |

### Modo A — PostgreSQL con Docker

Ver pasos completos en [CONFIGURAR-JAVA-POSTGRES.md](./CONFIGURAR-JAVA-POSTGRES.md#3-postgresql-con-docker-opcional).

1. Instalar [Docker Desktop](https://www.docker.com/products/docker-desktop/).
2. En `db\`: copiar `.env.example` → `.env` y cambiar `POSTGRES_PASSWORD`.
3. `Iniciar.bat` levanta el contenedor y abre la app.
4. Conexión: `localhost`, puerto y credenciales de `db\.env`.

### Modo B — PostgreSQL instalado en la misma PC

Ver pasos completos en [CONFIGURAR-JAVA-POSTGRES.md](./CONFIGURAR-JAVA-POSTGRES.md#2-instalar-y-configurar-postgresql-en-el-sistema-sin-docker).

1. Instalar [PostgreSQL 16 para Windows](https://www.postgresql.org/download/windows/).
2. Asegurarse de que el **servicio PostgreSQL** esté en ejecución.
3. La app puede crear la base `fitness_training` al configurar conexión (primera vez).
4. `Iniciar.bat` abre la app directamente (no usa Docker).
5. Conexión: `localhost`, puerto `5432`, usuario y contraseña del instalador de PostgreSQL.

No hace falta `scripts\db\start-db.bat` ni la carpeta `db\` (salvo que también quieras Docker como alternativa).

### Modo C — PostgreSQL en otra PC (varios mostradores)

Ver [CONFIGURAR-JAVA-POSTGRES.md](./CONFIGURAR-JAVA-POSTGRES.md#4-postgresql-en-otra-pc-red--varios-mostradores).

---

## Entrega al cliente (proveedor)

```bat
scripts\dev\build\package.bat
```

Genera:

| Artefacto | Contenido |
|-----------|-----------|
| `target\client-dist\` | Carpeta lista para copiar |
| `target\FitnessTraining.zip` | Zip para entregar |

```
client-dist/
  Iniciar.bat          → launcher (desde scripts/client/Iniciar.bat)
  LEEME.txt
  app/                 → aplicación + JavaFX
  db/                  → docker-compose + .env (solo si usa Docker)
  scripts/
    setup/             → setup-first-run.bat
    java/              → check-java.bat
    docker/            → check-docker.bat
    db/                → start-db, stop-db, backup-db, start-db-silent
  docs/
    CLIENTE.md
    CONFIGURAR-JAVA-POSTGRES.md
    QUE-EJECUTAR.md
```

**Cliente:** descomprime → `Iniciar.bat` (o instala Java/PG manualmente si prefiere).

---

## Instalación paso a paso (Docker — modo A)

### 1. Copiar archivos

1. Descomprimir el zip en **`C:\FitnessTraining\`** (ruta corta; ver `DESCOMPRIMIR.txt` si Windows dice *ruta demasiado larga*).

### 2. Configurar contraseña

En `db\`: copiar `.env.example` → `.env`, editar `POSTGRES_PASSWORD`.

### 3. Abrir la aplicación

Doble clic en **`Iniciar.bat`**.

La primera vez crea `db\.env` (si falta) y la conexión local en `%USERPROFILE%\.fitness-training\database.properties`.

| Campo | Valor |
|-------|--------|
| Servidor | `localhost` |
| Puerto | `5432` (o el de `db\.env`) |
| Base | `fitness_training` |
| Usuario / contraseña | los de `db\.env` |

Si hace falta completar manualmente: **Probar conexión** → **Continuar al login**.

La app crea tablas (Flyway) y, si la base está vacía, usuarios de demostración.

### 4. Primer login

1. Entrar como administrador (credenciales del proveedor).
2. **Cambiar contraseñas** demo antes de uso real.
3. Cargar clientes desde **Clientes**.

Configuración guardada en: `%USERPROFILE%\.fitness-training\database.properties`

---

## Uso diario

| Acción | Cómo |
|--------|------|
| Abrir sistema | **`Iniciar.bat`** o `app\FitnessTraining.bat` |
| Levantar base (solo Docker) | `scripts\db\start-db.bat` |
| Detener base (solo Docker) | `scripts\db\stop-db.bat` |
| Cambiar conexión (admin) | Menú → configuración de base de datos |

---

## Backup

- **Docker:** `scripts\db\backup-db.bat` (con contenedor activo).
- **PostgreSQL local:** mismo script si `pg_dump` está en PATH, o backup con herramientas de PostgreSQL.

Archivos en `backups\`. Recomendación: backup semanal.

---

## Solución de problemas

| Problema | Qué hacer |
|----------|-----------|
| *Connection refused* | PostgreSQL no está activo → servicio Windows, Docker, o IP incorrecta en red |
| Puerto 5432 ocupado | Otro PostgreSQL en la PC → cambiar puerto o detener el otro servicio |
| *Java no encontrado* | Instalar Java 21 y verificar `java -version` en cmd |
| Login falla | Verificar credenciales; en demo: **Mostrar cuentas de prueba** |

---

## Entrega opcional (runtime embebido)

Scripts para zip con Java y PostgreSQL incluidos (no es el flujo normal): `install\optional\`.
