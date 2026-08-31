# Scripts

Batch en `scripts/` y launcher **`Iniciar.bat`** en la raíz del repo (delega al flujo cliente).

## Desarrollo (`scripts/dev/`)

| Carpeta | Script | Uso |
|---------|--------|-----|
| `app/` | `run.bat` | Compilar y ejecutar (Maven) |
| `app/` | `start-app-packaged.bat` | Solo app en `target\client-dist\app\` |
| `build/` | `package.bat` | Generar zip de entrega |
| `db/` | `start-db.bat` | Levantar PostgreSQL (Docker) |
| `db/` | `stop-db.bat` | Detener contenedores |
| `client/` | `start-client-dist.bat` | Probar `Iniciar.bat` en `target\client-dist\` |

```bat
scripts\dev\db\start-db.bat
scripts\dev\app\run.bat
scripts\dev\build\package.bat
scripts\dev\client\start-client-dist.bat
```

## Cliente (`scripts/client/`)

| Carpeta | Script | En el zip |
|---------|--------|-----------|
| *(raíz)* | `Iniciar.bat` | Raíz del zip |
| `setup/` | `setup-first-run.bat` | `scripts\setup\` |
| `setup/` | `validate-package.bat` | Pre-checks; busca `app\` + `scripts\` automaticamente |
| `java/` | `check-java.bat` | `scripts\java\` |
| `docker/` | `check-docker.bat` | `scripts\docker\` |
| `db/` | `start-db.bat` | `scripts\db\` |
| `db/` | `start-db-silent.bat` | `scripts\db\` |
| `db/` | `stop-db.bat` | `scripts\db\` |
| `db/` | `backup-db.bat` | `scripts\db\` |

`Iniciar.bat` incluye un **mapeador automatico**: sube carpetas hasta encontrar `app\` + `scripts\`, o `target\client-dist\`, sin importar si se ejecuta desde la raiz del zip, `scripts\client\` del repo, etc.

## Opcional

`install/optional/` — empaquetado con Java/PostgreSQL embebido.
