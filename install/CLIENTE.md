# Instalación en la PC del cliente

Descargar de GitHub, descomprimir en `C:\FitnessTraining` y ejecutar:

```bat
Iniciar.bat
```

Un solo script: **comprueba** lo instalado en la PC, **avisa en CMD** si falta algo (con enlaces para descargar), y cuando los requisitos críticos están listos compila (si hace falta), configura, levanta la base y abre la app.

## Launcher: `Iniciar.bat`

```bat
Iniciar.bat
```

| Paso (automático) | Qué hace |
|-------------------|----------|
| 1 | Comprueba Java 21, Maven (solo primera compilación) y Docker/PostgreSQL |
| 2 | Si falta algo crítico → muestra `[FALTA]` + URL y **no continúa** |
| 3 | Compila la primera vez si descargó el código desde GitHub |
| 4 | Configuración local |
| 5 | Levanta PostgreSQL en Docker (si Docker está activo) |
| 6 | Abre la app |

Solo la app: `app\FitnessTraining.bat`.

**Importante:** ejecutar `Iniciar.bat` en la **raíz** de la carpeta descomprimida (no `scripts\client\Iniciar.bat`).

## Requisitos en la PC del cliente

| Requisito | Detalle |
|-----------|---------|
| Windows | 10/11 (64 bits) |
| Java 21 | Obligatorio — [Adoptium](https://adoptium.net/) |
| Maven 3.9+ | Solo si compila desde GitHub (sin zip precompilado) — [Maven](https://maven.apache.org/download.cgi) |
| PostgreSQL o Docker | Uno de los dos para la base — [Docker Desktop](https://www.docker.com/products/docker-desktop/) o [PostgreSQL](https://www.postgresql.org/download/windows/) |

`Iniciar.bat` **no instala** software automáticamente. Muestra en pantalla qué falta y dónde descargarlo; después de instalar manualmente, volver a ejecutar `Iniciar.bat`.

Con Docker: editar `db\.env` (`POSTGRES_PASSWORD`) antes del primer `Iniciar.bat`.

Guías: [CONFIGURAR-JAVA-POSTGRES.md](./CONFIGURAR-JAVA-POSTGRES.md) · [QUE-EJECUTAR.md](./QUE-EJECUTAR.md)

---

## Tres formas de conectar la base

| Modo | Dónde está PostgreSQL | Qué hace `Iniciar.bat` |
|------|----------------------|-------------------------|
| **A — Docker** | Contenedor en esta PC | Levanta Docker + abre la app |
| **B — Local** | Servicio Windows en esta PC | Abre la app (PG ya activo) |
| **C — Red** | Otra PC en la LAN | Abre la app; configurar IP del servidor |

---

## Uso diario

| Acción | Cómo |
|--------|------|
| Abrir sistema | `Iniciar.bat` |
| Levantar base (solo Docker) | `scripts\db\start-db.bat` |
| Detener base (solo Docker) | `scripts\db\stop-db.bat` |
| Backup | `scripts\db\backup-db.bat` |

---

## Entrega opcional (zip portable)

Si preferís llevar un zip en vez del repo: `scripts\dev\build\package.bat` genera `target\FitnessTraining.zip`. El cliente **no necesita Maven** si el zip ya incluye la app compilada. Ver [optional/README.md](./optional/README.md).
