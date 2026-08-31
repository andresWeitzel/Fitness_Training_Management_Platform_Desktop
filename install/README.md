# Instalación y scripts

Índice de guías y scripts del proyecto. Los `.bat` de desarrollo están en `scripts/dev/`; los del zip del gimnasio en `scripts/client/` (copiados a `client-dist/scripts/` al empaquetar).

## ¿Qué ejecutar?

| Persona | Acción | Documento |
|---------|--------|-----------|
| Desarrollador | Probar / empaquetar | [QUE-EJECUTAR.md](./QUE-EJECUTAR.md) |
| Cliente (gimnasio) | Instalar y usar el zip | [CLIENTE.md](./CLIENTE.md) |
| Cualquiera | Java, PostgreSQL, Docker, red | [CONFIGURAR-JAVA-POSTGRES.md](./CONFIGURAR-JAVA-POSTGRES.md) |

## Desarrollo (repositorio)

```bat
copy .env.example .env
scripts\dev\db\start-db.bat
scripts\dev\app\run.bat
```

Launcher en la raíz: **`Iniciar.bat`** — prueba el flujo del cliente (usa `target\client-dist\` si existe).

| Script | Uso |
|--------|-----|
| `scripts/dev/app/run.bat` | Compilar y ejecutar (Maven) |
| `scripts/dev/build/package.bat` | Generar zip de entrega |
| `Iniciar.bat` | Probar launcher del cliente |
| `scripts/dev/db/start-db.bat` | Levantar PostgreSQL (Docker) |
| `scripts/dev/db/stop-db.bat` | Detener contenedores |
| `scripts/dev/app/start-app-packaged.bat` | Probar build empaquetado |

Detalle: [DESARROLLO.md](./DESARROLLO.md) · Mapa completo: [scripts/README.md](../scripts/README.md)

## Cliente — instalación en el gimnasio

Instalación en la **PC del cliente** (proveedor/desarrollador). El gimnasio no empaqueta; solo usa `Iniciar.bat` a diario.

| Paso | Acción |
|------|--------|
| 1 | Llevar `FitnessTraining.zip` a la PC del cliente |
| 2 | Descomprimir en `C:\FitnessTraining` |
| 3 | `Iniciar.bat` (configurar `db\.env` si usa Docker) |

Un solo launcher en el zip: **`Iniciar.bat`** (fuente: `scripts/client/Iniciar.bat`).

| Paso | Script interno | Qué hace |
|------|----------------|----------|
| 1 | `scripts\setup\setup-first-run.bat` | Primera vez: `db\.env` y conexión local |
| 2 | `scripts\java\check-java.bat` | Verifica Java 21; ofrece winget |
| 3 | `scripts\docker\check-docker.bat` | Si no hay Docker, ofrece winget (opcional) |
| 4 | `scripts\db\start-db-silent.bat` | Levanta PG en Docker (si hay Docker) |
| 5 | `app\FitnessTraining.bat` | Abre la app (Flyway al conectar) |

Scripts manuales en el zip (`client-dist/scripts/`):

| Script | Uso |
|--------|-----|
| `start-db.bat` | Solo base (Docker, con mensajes) |
| `stop-db.bat` | Detener Docker |
| `backup-db.bat` | Respaldo `.sql` |

Plantillas en el repo: `scripts/client/Iniciar.bat`, `install/client-dist/LEEME.txt`.

## Entrega opcional (runtime embebido)

Zip con Java y PostgreSQL dentro (~200 MB). No es el flujo normal.

| Recurso | Ubicación |
|---------|-----------|
| Scripts | [optional/](./optional/) |
| Guía | [optional/ENTREGA-PORTABLE.md](./optional/ENTREGA-PORTABLE.md) |
| Java portable (concepto) | [RUNTIME.md](./RUNTIME.md) |

## Estructura de `install/`

```
install/
  README.md                 ← este índice
  QUE-EJECUTAR.md           ← roles y comandos rápidos
  CLIENTE.md                ← instalación en el gimnasio
  CONFIGURAR-JAVA-POSTGRES.md
  DESARROLLO.md
  RUNTIME.md
  FitnessTraining.bat       ← plantilla Maven (app empaquetada)
  client-dist/
    Iniciar.bat             ← plantilla launcher del zip
    LEEME.txt
  optional/                 ← empaquetado con runtime embebido
```
