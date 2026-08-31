# Scripts

Todo el batch del proyecto está en `scripts/`. No hay `.bat` en la raíz del repositorio.

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
| `setup/` | `validate-package.bat` | `scripts\setup\` (pre-checks al iniciar) |
| `java/` | `check-java.bat` | `scripts\java\` |
| `docker/` | `check-docker.bat` | `scripts\docker\` |
| `db/` | `start-db.bat` | `scripts\db\` |
| `db/` | `start-db-silent.bat` | `scripts\db\` |
| `db/` | `stop-db.bat` | `scripts\db\` |
| `db/` | `backup-db.bat` | `scripts\db\` |

`Iniciar.bat` no se ejecuta desde `scripts\client\` en el repo: va en la raíz del zip (o en `target\client-dist\` tras empaquetar).

## Opcional

`install/optional/` — empaquetado con Java/PostgreSQL embebido.
