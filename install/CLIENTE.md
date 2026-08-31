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
| 1 | Instala Java 21, Maven y Docker con winget si faltan |
| 2 | Compila la primera vez desde GitHub |
| 3 | Configuración local |
| 4 | Levanta PostgreSQL en Docker |
| 5 | Abre la app |

Solo la app: `app\FitnessTraining.bat`.

## Requisitos en la PC del cliente

| Requisito | Detalle |
|-----------|---------|
| Windows | 10/11 (64 bits) |
| Internet | Para winget (Java, Maven, Docker) |
| Permisos | Instalación con winget (admin recomendado) |

`Iniciar.bat` instala Java 21, Maven y Docker automáticamente si no están.

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
