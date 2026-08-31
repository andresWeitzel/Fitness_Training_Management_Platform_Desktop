# Entrega opcional con runtime embebido

**No es la forma habitual de entregar el producto.**

La entrega normal asume que el gimnasio tiene **Java 21** y **PostgreSQL 16** instalados (o Docker opcional). Ver [QUE-EJECUTAR.md](../QUE-EJECUTAR.md).

Esta carpeta contiene scripts **experimentales** para empaquetar Java y PostgreSQL dentro del zip (~200 MB), sin instalar nada en la PC del cliente.

La entrega **normal** usa `scripts\dev\build\package.bat` + `Iniciar.bat` con winget (Java/Docker) o PostgreSQL del sistema. Ver [README.md](../README.md).

| Script | Uso |
|--------|-----|
| `prepare-runtime.bat` | Descarga JRE + PostgreSQL binaries a `install/optional/runtime/` |
| `package-full.bat` | Prepara runtime + ejecuta `scripts/dev/build/package.bat` |

Detalle: [ENTREGA-PORTABLE.md](./ENTREGA-PORTABLE.md)
