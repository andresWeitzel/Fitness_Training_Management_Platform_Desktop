# ¿Quién hace qué y qué ejecutar?

## Desarrollador — pruebas en el repo

```bat
copy .env.example .env
scripts\dev\db\start-db.bat
scripts\dev\app\run.bat
```

| Script | Uso |
|--------|-----|
| `scripts\dev\app\run.bat` | Compilar y ejecutar (Maven) |
| `scripts\dev\build\package.bat` | Generar zip opcional para llevar en USB |
| `Iniciar.bat` | Probar flujo de instalación en cliente |

Detalle: [DESARROLLO.md](./DESARROLLO.md)

---

## Cliente — instalación en el gimnasio

Descargar de GitHub → descomprimir en `C:\FitnessTraining` → ejecutar:

```bat
Iniciar.bat
```

Un solo script. Compila la primera vez, configura, Java/Docker, base y app.

Detalle: [CLIENTE.md](./CLIENTE.md)
