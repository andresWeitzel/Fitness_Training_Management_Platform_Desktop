# Instalación en la PC del cliente

Guía para instalar **Fitness Training Management Platform** en el gimnasio o centro de entrenamiento.

## Requisitos en la PC

| Requisito | Detalle |
|-----------|---------|
| Windows | 10 o superior (64 bits) |
| RAM | 4 GB mínimo recomendado |
| **Java 21** | JDK o JRE instalado (`java` en PATH) — [Adoptium](https://adoptium.net/) |
| **PostgreSQL 16** | Instalado como servicio Windows **o** vía Docker (opcional) |
| Docker Desktop | **Opcional.** Si está instalado, `Iniciar.bat` lo usa para levantar la base |

El zip de entrega **no incluye** Java ni PostgreSQL. El cliente los instala una vez en el sistema (o usa Docker).

---

## Tres formas de conectar la base

| Modo | Dónde está PostgreSQL | Docker | Qué hace `Iniciar.bat` |
|------|----------------------|--------|-------------------------|
| **A — Docker** | Contenedor en esta PC | Sí | Levanta Docker + abre la app |
| **B — Local** | Servicio Windows en esta PC | No | Solo abre la app (PG ya debe estar activo) |
| **C — Red** | Otra PC servidor en la LAN | No | Solo abre la app; configurar IP del servidor |

### Modo A — PostgreSQL con Docker

1. Instalar [Docker Desktop](https://www.docker.com/products/docker-desktop/).
2. En `db\`: copiar `.env.example` → `.env` y cambiar `POSTGRES_PASSWORD`.
3. `Iniciar.bat` levanta el contenedor y abre la app.
4. Conexión: `localhost`, puerto y credenciales de `db\.env`.

### Modo B — PostgreSQL instalado en la misma PC

1. Instalar [PostgreSQL 16 para Windows](https://www.postgresql.org/download/windows/).
2. Asegurarse de que el **servicio PostgreSQL** esté en ejecución.
3. La app puede crear la base `fitness_training` al configurar conexión (primera vez).
4. `Iniciar.bat` abre la app directamente (no usa Docker).
5. Conexión: `localhost`, puerto `5432`, usuario y contraseña del instalador de PostgreSQL.

No hace falta `scripts\start-db.bat` ni la carpeta `db\` (salvo que también quieras Docker como alternativa).

### Modo C — PostgreSQL en otra PC (varios mostradores)

1. PostgreSQL en **una PC fija** (servidor), por ejemplo IP `192.168.1.50`.
2. En cada mostrador: copiar el zip, instalar **Java 21**, ejecutar `Iniciar.bat` o `app\FitnessTraining.bat`.
3. En la app: **Configurar conexión** (primera vez o desde ajustes de admin):
   - **Servidor:** IP del servidor (no `localhost`)
   - **Puerto / base / usuario / contraseña:** los del servidor
4. Abrir puerto **5432** en el firewall solo dentro de la red local.

En red, cada puesto solo necesita la carpeta `app\` si la conexión ya está guardada en `%USERPROFILE%\.fitness-training\database.properties`.

---

## Entrega al cliente (proveedor)

```bat
package.bat
```

Genera:

| Artefacto | Contenido |
|-----------|-----------|
| `target\client-dist\` | Carpeta lista para copiar |
| `target\FitnessTraining-client-win64.zip` | Zip para entregar |

```
client-dist/
  Iniciar.bat
  configurar-primera-vez.bat
  LEEME.txt
  app/                 → aplicación + JavaFX
  db/                  → docker-compose + .env (solo si usa Docker)
  scripts/             → start-db, stop-db, backup (Docker)
  docs/CLIENTE.md
```

**Cliente:** descomprime → instala Java y PostgreSQL (o Docker) → `Iniciar.bat`.

---

## Instalación paso a paso (Docker — modo A)

### 1. Copiar archivos

Descomprimir en `C:\FitnessTraining\` (o carpeta fija).

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
| Levantar base (solo Docker) | `scripts\start-db.bat` |
| Detener base (solo Docker) | `scripts\stop-db.bat` |
| Cambiar conexión (admin) | Menú → configuración de base de datos |

---

## Backup

- **Docker:** `scripts\backup-db.bat` (con contenedor activo).
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
