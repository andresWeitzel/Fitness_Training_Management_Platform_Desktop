# Runtime y entrega

## Entrega normal (recomendada)

El zip generado con `scripts\dev\build\package.bat` incluye **solo la aplicación**. El gimnasio necesita:

- **Java 21** en el sistema (o instalación asistida vía winget en `Iniciar.bat`)
- **PostgreSQL 16** como servicio Windows, **o** Docker Desktop (winget opcional en `Iniciar.bat`)

Guías: [CLIENTE.md](./CLIENTE.md) · [CONFIGURAR-JAVA-POSTGRES.md](./CONFIGURAR-JAVA-POSTGRES.md)

## Scripts en el repositorio

| Carpeta | Para quién | Contenido |
|---------|------------|-----------|
| `scripts/dev/` | Desarrollador | `run`, `package`, `start-db`, `stop-db` |
| `scripts/client/` | Cliente (en el zip) | `check-java`, `check-docker`, `setup-first-run`, DB |

Mapa: [scripts/README.md](../scripts/README.md)

## Entrega opcional (runtime embebido)

Incluir JRE y binarios de PostgreSQL en el zip para no instalar nada en la PC del cliente. **Experimental** — ver [optional/README.md](./optional/README.md).

| Script | Uso |
|--------|-----|
| `install/optional/prepare-runtime.bat` | Descarga JRE + PG a `install/optional/runtime/` |
| `install/optional/package-full.bat` | Runtime + `scripts/dev/build/package.bat` |

La integración completa en `Iniciar.bat` (PG portable sin Docker) está documentada en [optional/ENTREGA-PORTABLE.md](./optional/ENTREGA-PORTABLE.md) como objetivo de esa variante; el flujo estándar usa winget + Docker o PostgreSQL del sistema.
