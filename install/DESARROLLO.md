# Instalación para desarrollo

Guía para levantar **Fitness Training Management Platform** en tu PC de desarrollo.

## Requisitos

| Requisito | Detalle |
|-----------|---------|
| Windows | 10/11 (64 bits) |
| JDK | 21 |
| Maven | 3.9+ |
| Docker Desktop | Para PostgreSQL local (recomendado) |
| Git | Para clonar el repositorio |

## Inicio rápido

```bat
# 1. Variables de entorno
copy .env.example .env
scripts\dev\db\start-db.bat
# o: docker compose up -d

# 3. Compilar y ejecutar (limpia + compile + javafx:run)
scripts\dev\app\run.bat
```

## Scripts útiles

| Script | Uso |
|--------|-----|
| `scripts/dev/app/run.bat` | Compilar y ejecutar |
| `scripts/dev/build/package.bat` | Generar zip de entrega |
| `scripts/dev/db/start-db.bat` | Levanta PostgreSQL con Docker |
| `scripts/dev/db/stop-db.bat` | Detiene contenedores |
| `scripts/dev/client/start-client-dist.bat` | Probar `Iniciar.bat` en `target\client-dist\` |
| `scripts/dev/app/start-app-packaged.bat` | Solo app en `target\client-dist\app\` |

Índice de instalación: [README.md](./README.md) · Mapa de scripts: [scripts/README.md](../scripts/README.md)

## Maven manual

```bat
mvn compile
mvn javafx:run
mvn test
mvn test -Dtest=CsvExporterTest
```

## Primera ejecución

1. Si no hay conexión válida, la app abre **Configurar conexión**.
2. Usa los valores de `.env` (puerto, base, usuario, contraseña).
3. Flyway aplica migraciones al conectar.
4. En modo desarrollo se cargan usuarios y datos demo (ver cuentas en [README](../README.md#40-cuentas-demo-)).

## Documentación relacionada

- [README principal](../README.md)
- [Capturas y docs](./README.md) — `docs/README.md`
- [Especificación funcional](../docs/especificacion-funcional.md)
- [Instalación cliente](./CLIENTE.md)
