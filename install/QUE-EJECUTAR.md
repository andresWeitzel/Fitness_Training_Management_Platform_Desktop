# ¿Quién hace qué y qué ejecutar?

## Roles

| Persona | Qué hace |
|---------|----------|
| **Vos (desarrollador)** | `package.bat` → entregás el zip al gimnasio |
| **Gimnasio (cliente)** | Instala Java + PostgreSQL (o Docker), descomprime zip, `Iniciar.bat` |

---

## Desarrollador — generar entrega

```bat
package.bat
```

Crea `target\FitnessTraining-client-win64.zip` con la **aplicación** (sin Java ni PostgreSQL dentro).

Desarrollo día a día:

```bat
run.bat
```

---

## Cliente — antes de usar el zip

Instalar en la PC (una vez):

1. **Java 21** — [Adoptium](https://adoptium.net/)
2. **PostgreSQL 16** — [postgresql.org](https://www.postgresql.org/download/windows/) **o** Docker Desktop (opcional)

---

## Cliente — usar el zip

1. Descomprimir en `C:\FitnessTraining\`
2. Si usa **Docker**: editar `db\.env` (contraseña)
3. Doble clic en **`Iniciar.bat`**

### Cómo se conecta la base

| Situación | Qué pasa |
|-----------|----------|
| Tiene **Docker** | `Iniciar.bat` levanta PostgreSQL en Docker |
| **PostgreSQL en esta PC** (sin Docker) | `Iniciar.bat` abre la app; el servicio PG debe estar activo |
| **PostgreSQL en otra PC** (red) | Abre la app y configura la IP del servidor en pantalla de conexión |

Detalle: [CLIENTE.md](./CLIENTE.md)

---

## Opcional (no usar por defecto)

Entrega con Java y PostgreSQL embebidos en el zip: `install\optional\`.
