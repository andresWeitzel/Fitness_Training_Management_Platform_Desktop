# Instalación en la PC del cliente

Descargar de GitHub, descomprimir en `C:\FitnessTraining` y ejecutar:

```bat
Iniciar.bat
```

Un solo script: compila la primera vez si hace falta, configura, verifica Java/Docker, levanta la base y abre la app.

## Launcher: `Iniciar.bat`

```bat
Iniciar.bat
```

| Paso (automático) | Qué hace |
|-------------------|----------|
| 1 | Compila si es la primera vez desde GitHub |
| 2 | Configuración local (primera vez) |
| 3 | Verifica Java 21; ofrece winget si falta |
| 4 | Verifica Docker (opcional) |
| 5 | Levanta PostgreSQL si hay Docker |
| 6 | Abre la app |

Solo la app: `app\FitnessTraining.bat`.

## Requisitos en la PC del cliente

| Requisito | Detalle |
|-----------|---------|
| Windows | 10 o superior (64 bits) |
| Java 21 | Primera vez desde GitHub; `Iniciar.bat` puede instalarlo con winget |
| Maven | Solo la primera compilación desde GitHub |
| PostgreSQL 16 | Servicio Windows o Docker |

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

Si preferís llevar un zip en vez del repo: `scripts\dev\build\package.bat` genera `target\FitnessTraining.zip`. Ver [optional/README.md](./optional/README.md).
