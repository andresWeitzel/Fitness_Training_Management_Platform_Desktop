# ¿Quién hace qué y qué ejecutar?

Guía corta. Si solo querés **generar el zip para el gimnasio**, salta a la sección **2**.

---

## 1) Roles

| Persona | Quién es | Qué hace |
|---------|----------|----------|
| **Vos (desarrollador / proveedor)** | Andrés, equipo técnico | Compilás, empaquetás el zip y lo entregás al gimnasio |
| **Cliente (gimnasio)** | Recepción, dueño | Descomprime el zip y hace doble clic en `Iniciar.bat` |

El cliente **no** usa Maven, **no** usa `run.bat`, **no** compila nada.

---

## 2) Lo que VOS ejecutás (una sola vez por entrega)

Abrir **cmd** o PowerShell en la carpeta del proyecto (donde está `package-full.bat`).

### Opción A — Todo automático (recomendada)

```bat
package-full.bat
```

Eso hace en orden:

1. Descarga Java 21 (JRE) y PostgreSQL 16 portable **si no están** en `runtime\`
2. Compila la aplicación con Maven
3. Arma `target\client-dist\`
4. Crea el zip `target\FitnessTraining-client-win64.zip`

**Ese zip es lo que le das al gimnasio.**

La primera vez tarda más (descarga ~100 MB). Las siguientes veces reutiliza `runtime\` y es más rápido.

### Opción B — Solo app (sin Java ni base en el zip)

```bat
package.bat
```

El gimnasio tendría que instalar Java y Docker/PostgreSQL por su cuenta. Solo para pruebas o si ya tienen todo instalado.

### Desarrollo día a día (tu PC, no el cliente)

```bat
run.bat
```

Solo para programar y probar cambios en el código. **No** es la entrega al cliente.

---

## 3) Lo que hace el CLIENTE (gimnasio)

1. Recibe `FitnessTraining-client-win64.zip`
2. Descomprime en `C:\FitnessTraining\` (o cualquier carpeta fija)
3. Abre `db\.env` y cambia `POSTGRES_PASSWORD` (primera vez)
4. Doble clic en **`Iniciar.bat`**

Listo. No instala Java ni PostgreSQL si el zip fue generado con `package-full.bat`.

---

## 4) Archivos que importan

| Archivo | Quién lo usa |
|---------|----------------|
| `package-full.bat` | Vos → zip completo para entregar |
| `package.bat` | Vos → zip solo app |
| `prepare-runtime.bat` | Vos → solo descarga Java + PostgreSQL a `runtime\` |
| `run.bat` | Vos → desarrollo |
| `Iniciar.bat` | Cliente → dentro del zip descomprimido |

---

## 5) Si algo falla al empaquetar

| Error | Solución |
|-------|----------|
| `Maven no encontrado` | Instalar Maven y JDK 21 en tu PC de desarrollo |
| Fallo la descarga | Revisar internet; volver a ejecutar `prepare-runtime.bat` |
| Zip muy grande (~200 MB) | Normal: incluye Java + PostgreSQL + app |

Más detalle técnico: [ENTREGA-PORTABLE.md](./ENTREGA-PORTABLE.md)
