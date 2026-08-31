# ¿Quién hace qué y qué ejecutar?

Índice general: [README.md](./README.md) · Mapa de scripts: [scripts/README.md](../scripts/README.md)

## Roles

| Persona | Qué hace |
|---------|----------|
| **Vos (desarrollador)** | `scripts\dev\build\package.bat` → entregás el zip al gimnasio |
| **Gimnasio (cliente)** | Descomprime zip → **`Iniciar.bat`** (verifica/instala Java y Docker con winget si hace falta) |

---

## Desarrollador — scripts (`scripts/dev/`)

```bat
copy .env.example .env
scripts\dev\db\start-db.bat
scripts\dev\app\run.bat
```

| Script | Uso |
|--------|-----|
| `scripts\dev\app\run.bat` | Compilar y ejecutar (Maven) |
| `scripts\dev\build\package.bat` | Generar zip de entrega |
| `scripts\dev\client\start-client-dist.bat` | Probar `Iniciar.bat` en `target\client-dist\` |
| `scripts\dev\db\start-db.bat` | Levantar PostgreSQL (Docker) |
| `scripts\dev\db\stop-db.bat` | Detener contenedores |
| `scripts\dev\app\start-app-packaged.bat` | Probar build empaquetado |

Generar entrega:

```bat
scripts\dev\build\package.bat
```

Crea `target\client-dist\` y `target\FitnessTraining-client-win64.zip` (solo la app, sin Java ni PostgreSQL dentro).

Probar como el gimnasio (launcher completo):

```bat
scripts\dev\client\start-client-dist.bat
```

`scripts\dev\app\run.bat` compila desde el código fuente y abre la app. No genera el zip.

Más detalle: [DESARROLLO.md](./DESARROLLO.md)

---

## Cliente — `Iniciar.bat`

```bat
Iniciar.bat
```

**Launcher único del cliente.** Ejecuta en orden `scripts\setup\`, `scripts\java\`, `scripts\docker\` y `scripts\db\`, y abre la app. No ejecutar esos `.bat` por separado.

1. Descomprimir el zip
2. Si usa Docker: editar `db\.env` (contraseña)
3. Ejecutar `Iniciar.bat`

| Paso (interno) | Acción |
|----------------|--------|
| 1 | Primera vez: configuración local |
| 2 | Verificar / instalar Java (winget) |
| 3 | Verificar / instalar Docker (opcional, winget) |
| 4 | Levantar base en Docker si hay Docker |
| 5 | Abrir app + migraciones Flyway |

### Cómo se conecta la base

| Situación | Qué pasa |
|-----------|----------|
| Tiene **Docker** | `Iniciar.bat` levanta PostgreSQL en Docker |
| **PostgreSQL en esta PC** (sin Docker) | `Iniciar.bat` abre la app; el servicio PG debe estar activo |
| **PostgreSQL en otra PC** (red) | Abre la app y configura la IP del servidor |

Scripts manuales (solo casos puntuales): `scripts\db\start-db.bat`, `scripts\db\stop-db.bat`, `scripts\db\backup-db.bat`.

Detalle: [CLIENTE.md](./CLIENTE.md)

---

## Opcional (no usar por defecto)

Entrega con Java y PostgreSQL embebidos en el zip: [optional/README.md](./optional/README.md)
