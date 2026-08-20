# Instalación en la PC del cliente

Guía para instalar **Fitness Training Management Platform** en el gimnasio o centro de entrenamiento. Se actualiza junto con el producto.

## Qué se instala

| Componente | Qué hace |
|------------|----------|
| **PostgreSQL** | Guarda clientes, usuarios y datos del negocio |
| **Aplicación JavaFX** | Pantallas de recepción, admin, entrenadores, etc. |

Son **dos piezas**: primero la base, después la app.

---

## Requisitos en la PC

| Requisito | Detalle |
|-----------|---------|
| Windows | 10 o superior (64 bits) |
| RAM | 4 GB mínimo recomendado |
| Disco | ~500 MB app + espacio para la base |
| **Opción A – Docker** | Docker Desktop instalado y en ejecución |
| **Opción B – Sin Docker** | PostgreSQL 16 instalado en Windows |
| **Java** | Solo si usa la carpeta portable `app/` (JDK 21). El instalador `.exe` futuro incluirá Java |

---

## Entrega al cliente (desde el proveedor)

En desarrollo, generar la carpeta de entrega:

```bat
package.bat
```

Eso crea `target\client-dist\` con:

```
client-dist/
  app/           → FitnessTraining.bat + librerías
  db/            → docker-compose.yml + .env.example
  scripts/       → start-db, stop-db, backup-db
  docs/          → esta guía
```

Comprimir `client-dist` en un `.zip` y copiarlo a la PC del cliente.

---

## Instalación paso a paso (Docker)

### 1. Copiar archivos

Descomprimir el zip en una carpeta fija, por ejemplo:

`C:\FitnessTraining\`

### 2. Configurar contraseña de la base

En `db\`:

1. Copiar `.env.example` → `.env`
2. Editar `.env` y cambiar `POSTGRES_PASSWORD` (no dejar `postgres` en producción)

### 3. Instalar Docker Desktop

Si no está instalado: [Docker Desktop para Windows](https://www.docker.com/products/docker-desktop/).

### 4. Levantar PostgreSQL

Doble clic en `scripts\start-db.bat` (o desde `db\`: `docker compose up -d`).

Verificar que el contenedor `fitness-training-postgres` esté **running**.

### 5. Abrir la aplicación

Doble clic en `app\FitnessTraining.bat`.

**Primera vez:** pantalla **Configurar conexión**

| Campo | Valor |
|-------|--------|
| Servidor | `localhost` |
| Puerto | `5432` (o el de `.env`) |
| Base | `fitness_training` |
| Usuario | el de `.env` |
| Contraseña | la de `.env` |

Pulsar **Probar conexión** → **Continuar al login**.

La app crea tablas (Flyway) y, si la base está vacía, usuarios de demostración.

### 6. Primer login y seguridad

1. Entrar como administrador (credenciales entregadas por el proveedor).
2. **Cambiar contraseñas** de usuarios demo antes de uso real.
3. Cargar clientes desde **Clientes**.

La configuración queda en:

`%USERPROFILE%\.fitness-training\database.properties`

---

## Instalación sin Docker (PostgreSQL nativo)

1. Instalar [PostgreSQL 16 para Windows](https://www.postgresql.org/download/windows/).
2. Crear base `fitness_training` y un usuario con permiso DDL/DML.
3. Abrir `app\FitnessTraining.bat`.
4. En configurar conexión, usar host `localhost` y los datos del paso 2.

No hace falta `scripts\start-db.bat`; PostgreSQL debe estar como **servicio de Windows** iniciado.

---

## Varias PCs en la misma red

- PostgreSQL en **una PC fija** (servidor), por ejemplo IP `192.168.1.50`.
- En cada puesto: solo copiar carpeta `app\` e instalar Java 21 si es portable.
- En configurar conexión: **Servidor** = IP del servidor (no `localhost`).

Abrir puerto **5432** solo dentro de la red local (firewall).

---

## Uso diario

| Acción | Cómo |
|--------|------|
| Iniciar base (Docker) | `scripts\start-db.bat` |
| Abrir sistema | `app\FitnessTraining.bat` |
| Cerrar app | Botón × en la ventana |
| Apagar PC | Opcional: `scripts\stop-db.bat` (Docker conserva datos en volumen) |

---

## Backup

Ejecutar `scripts\backup-db.bat` (con Docker levantado).

Genera un archivo `.sql` en la carpeta `backups\`.

**Recomendación:** backup semanal o antes de actualizaciones.

---

## Actualizar versión

1. Backup (`backup-db.bat`).
2. Reemplazar carpeta `app\` con la nueva entrega.
3. Si hay cambios de base, la app aplica migraciones Flyway al iniciar.

---

## Solución de problemas

| Problema | Qué hacer |
|----------|-----------|
| *Connection refused* | Base no levantada → `start-db.bat` o servicio PostgreSQL |
| Puerto 5432 ocupado | Otro PostgreSQL en Windows → detenerlo o cambiar `POSTGRES_PORT` en `.env` |
| *Java no encontrado* | Instalar JDK 21 o usar instalador con JRE incluido |
| Login falla | Verificar usuario/contraseña; en demo usar **Mostrar cuentas de prueba** |

---

## Pendiente / próximas mejoras de instalación

- [ ] Instalador `.msi` / `.exe` con JRE embebido (jpackage)
- [ ] Forzar cambio de contraseña admin en primer login
- [ ] Ocultar cuentas demo en builds de producción
- [ ] Script de restauración desde backup

Estos ítems se irán cerrando en el mismo repositorio.
