# Entrega portable — Java y PostgreSQL sin instalar en la PC del cliente

La pieza de entrega (`scripts\dev\build\package.bat` → zip) puede incluir **todo lo necesario** para ejecutar la app sin que el gimnasio instale Java ni PostgreSQL por separado.

## Resumen

| Componente | Carpeta en el proyecto | ¿Cliente instala algo? |
|------------|------------------------|-------------------------|
| Aplicación | `app/` (siempre en el zip) | No |
| Java 21 | `runtime/jdk/` (opcional) | No, si está en el zip |
| PostgreSQL 16 | `runtime/postgresql/` (opcional) | No, si está en el zip |
| Docker | — | Solo si **no** incluyes PG portable |

**Zip “completo” recomendado:** app + `runtime/jdk` + `runtime/postgresql` → el cliente solo descomprime y ejecuta `Iniciar.bat`.

---

## Java (JRE) en el zip o en el instalador

### En el zip (hoy)

1. Descargar **Temurin JRE 21** Windows x64:
   [Adoptium Temurin 21](https://adoptium.net/temurin/releases/?version=21&os=windows&arch=x64&package=jre)

2. Extraer en:

```
runtime/
  jdk/
    bin/
      java.exe
```

3. `scripts\dev\build\package.bat` puede copiar `runtime\jdk` al zip si existe en el proyecto. Hoy `FitnessTraining.bat` usa el Java del **PATH** del sistema; el uso de JRE embebido en esa variante es **experimental**.

**Resultado:** el cliente **no necesita** Java instalado en Windows.

### En un instalador `.exe` (futuro)

Con **jpackage**, el JRE va **dentro del instalador** (misma idea: no depende del PATH). El zip portable y el `.exe` son dos formas de entregar lo mismo.

---

## PostgreSQL portable en el zip (sin Docker)

Igual que Java: binarios dentro de la entrega, datos en `db\data\pgdata`.

### Preparación (proveedor, antes de `scripts\dev\build\package.bat`)

1. Descargar **PostgreSQL 16 binaries** para Windows x64 (zip, sin instalador):
   [PostgreSQL Binaries (EDB)](https://www.enterprisedb.com/download-postgresql-binaries)

2. Extraer el zip. Suele crear una carpeta `pgsql`. Renombrar/copiar a:

```
runtime/
  postgresql/
    bin/
      pg_ctl.exe
      initdb.exe
      pg_dump.exe
      ...
    lib/
    share/
```

3. Ejecutar `scripts\dev\build\package.bat`. Se incluye `runtime\postgresql` en el zip.

### Qué hace `Iniciar.bat` (flujo estándar del zip)

Con la entrega normal (`scripts\dev\build\package.bat` sin runtime embebido):

1. Configuración primera vez (`scripts\setup\setup-first-run.bat`)
2. Verificar / instalar Java con winget (`scripts\java\check-java.bat`)
3. Verificar / instalar Docker con winget (`scripts\docker\check-docker.bat`, opcional)
4. Si hay Docker → `scripts\db\start-db-silent.bat` levanta el contenedor
5. `app\FitnessTraining.bat` abre la aplicación (Flyway al conectar)

### PostgreSQL portable (experimental)

Objetivo de esta variante: incluir `runtime\postgresql` en el zip y arrancar PG sin Docker ni instalador. La preparación de binarios está en `install\optional\prepare-runtime.bat`; la integración completa en `Iniciar.bat` es **experimental**. Para producción hoy: Docker o PostgreSQL del sistema.

### Detener / backup (zip del cliente)

| Acción | Script |
|--------|--------|
| Detener Docker | `scripts\db\stop-db.bat` |
| Backup | `scripts\db\backup-db.bat` |
| Solo levantar base (Docker) | `scripts\db\start-db.bat` |

---

## Tamaños aproximados del zip

| Contenido | Tamaño |
|-----------|--------|
| App + librerías | ~50–80 MB |
| JRE 21 | ~50–70 MB |
| PostgreSQL binaries | ~45–55 MB |
| **Zip completo** | ~150–220 MB |

Los datos de la base crecen en `db\data\` según el uso.

---

## Comparación de modos de base de datos

| Modo | Ventaja | Desventaja |
|------|---------|------------|
| **PG portable en zip** | Cero Docker, un solo zip, copiar a varias PCs con datos en carpeta | Zip más grande; varias PCs en red = una carpeta `db\data` o sync manual |
| **Docker** | Aislamiento, fácil backup de volumen | Cliente instala Docker Desktop |
| **PostgreSQL instalado** | Servidor central en red | Instalación y configuración manual |

Para **un solo mostrador** en el gimnasio, PG portable + JRE en el zip es la opción más simple.

---

## Generar la entrega

```bat
REM 1. (Opcional) Colocar runtime\jdk y runtime\postgresql
REM 2. Empaquetar
scripts\dev\build\package.bat
```

Salida:

- `target\client-dist\`
- `target\FitnessTraining.zip`

Cliente: descomprimir → editar `db\.env` (contraseña) → **`Iniciar.bat`**.
