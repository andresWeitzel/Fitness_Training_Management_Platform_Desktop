# Documentación — capturas de pantalla

Galería de la aplicación **Fitness Training Management Platform** (versión actual).

**Última actualización de capturas:** 27/08/2026

## Imagen principal (README)

| Archivo | Uso |
|---------|-----|
| [`img/recepcion-control-ingreso.png`](./img/recepcion-control-ingreso.png) | Banner principal del README |
| [`assets/app-hero.png`](./assets/app-hero.png) | Copia sincronizada del banner (opcional) |

## Íconos del README (stack tecnológico)

Ruta: [`assets/icons/`](./assets/icons/) — **18×18 px** en el README (debajo de la captura principal, alineados a la derecha).

Banderas de idioma: bloque separado (**65×40 px**, PNG), en [`assets/translation/`](./assets/translation/).

| Ícono | Archivo |
|-------|---------|
| Java | `icons/backend/java/png/java.png` |
| JavaFX | `icons/backend/java/png/jsf.png` |
| PostgreSQL | `icons/database/png/postgres.png` |
| Maven | `icons/devops/png/maven.png` |
| Docker | `icons/devops/png/docker.png` |

Banderas: `assets/translation/arg-flag.svg`, `eeuu-flag.svg`.

Para cambiar la imagen del README, actualiza `docs/img/recepcion-control-ingreso.png` (y opcionalmente `docs/assets/app-hero.png`).

## Galería por módulo

| Archivo | Módulo / pantalla |
|---------|-------------------|
| [`panel-inicio.png`](./img/panel-inicio.png) | Panel — dashboard administrador |
| [`login-iniciar-sesion.png`](./img/login-iniciar-sesion.png) | Login — iniciar sesión |
| [`recepcion-control-ingreso.png`](./img/recepcion-control-ingreso.png) | Recepción — control de ingreso |
| [`pagos-listado-cobro.png`](./img/pagos-listado-cobro.png) | Pagos — listado y nuevo cobro |
| [`nutricion-turno-detalle.png`](./img/nutricion-turno-detalle.png) | Nutrición — detalle de turno |
| [`analytics-reportes-mora.png`](./img/analytics-reportes-mora.png) | Analytics — reportes (pestaña Mora) |
| [`analytics-graficos.png`](./img/analytics-graficos.png) | Analytics — gráficos |

## Cómo agregar nuevas fotos

1. Guarda la captura en `docs/img/` con nombre descriptivo en minúsculas y guiones (ej. `clientes-listado.png`).
2. Si es la imagen principal del README, actualiza `recepcion-control-ingreso.png` (banner por defecto).
3. Actualiza la tabla de esta página y la sección **Capturas** del [README](../README.md).

## Otros documentos

| Documento | Contenido |
|-----------|-----------|
| [Especificación funcional](./especificacion-funcional.md) | RF, casos de uso, matriz de roles |
| [Índice instalación](../install/README.md) | Guías y scripts (dev vs cliente) |
| [¿Qué ejecutar?](../install/QUE-EJECUTAR.md) | Roles: desarrollador vs gimnasio |
| [Mapa de scripts](../scripts/README.md) | `scripts/dev/` y `scripts/client/` |
| [Instalación cliente](../install/CLIENTE.md) | Despliegue en PC del gimnasio |
| [Java y PostgreSQL](../install/CONFIGURAR-JAVA-POSTGRES.md) | Instalación manual y winget |
| [Desarrollo](../install/DESARROLLO.md) | Setup local (`scripts/dev/app/run.bat`, Docker, Maven) |
