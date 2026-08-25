# Fitness Training Management Platform

Plataforma de escritorio para **operar un gimnasio o centro de entrenamiento de punta a punta**: recepción, administración, entrenamiento y nutrición en un solo sistema.

Hoy un centro chico o mediano suele tener el alta de clientes en un lado, los cobros en otro y el ingreso en la puerta sin un criterio único. **Fitness Training** unifica esa operación: el personal trabaja sobre la misma base, con roles claros, y el producto se va ampliando por módulos sin rehacer lo anterior.

La app la usa el **equipo interno** (administración, recepción, entrenadores, nutricionistas). Quien entrena en el centro es un **cliente gestionado** por el sistema; no inicia sesión en este software.

---

## Qué resuelve

- **Una ficha por persona.** Documento, contacto, número de cliente, carnet y QR. Alta, edición, consulta y baja lógica sin perder historial.
- **Operación según el rol.** Cada perfil ve lo que le corresponde. Recepción gestiona clientes; el entrenador consulta; el administrador configura la base y ve el tablero.
- **Base de datos propia del centro.** PostgreSQL local o en un servidor del gimnasio. Primera instalación guiada; después la conexión la cambia solo un administrador.
- **Listo para crecer.** La arquitectura está pensada para sumar membresías, cobros, control de ingreso, rutinas, evaluaciones y nutrición sobre los mismos clientes.

---

## Visión de producto

El camino de crecimiento es el de un centro real:

1. **Identificar** a cada cliente (hoy).
2. **Vender** un plan (membresías).
3. **Cobrar** y manejar mora (pagos).
4. **Controlar el ingreso** con carnet o QR y cupo diario (recepción).
5. **Entrenar y hacer seguimiento** (rutinas, evaluaciones, nutrición).
6. **Medir** ocupación, vencimientos e ingresos (analytics).

Cada módulo se apoya en el anterior. No hace falta un ecosistema de apps sueltas ni una planilla paralela.

---

## Módulos

| Módulo | Qué aporta | Estado |
|--------|------------|--------|
| Identidad y panel | Login seguro, menú por permiso, indicadores reales | Disponible |
| **Clientes y credenciales** | Ficha, listado en vivo, bajas, n° de cliente, carnet 12 meses, QR | **Disponible** |
| Membresías | Planes, altas y vencimientos | **Disponible** |
| Pagos y mora | Cobro, recargo por atraso, reactivar acceso | **Disponible** |
| **Recepción** | Check-in con carnet/QR, histórico y bloqueo por deuda | **Disponible** |
| Personal | Usuarios internos y roles | **Disponible** |
| Entrenamiento | Ejercicios y rutinas estructuradas | **Disponible** |
| Evaluaciones | Historial de evaluaciones físicas | En roadmap |
| Nutrición | Turnos, planes y ficha de salud con historial | En roadmap |
| Analytics | Vencimientos, mora, ocupación e ingresos | En roadmap |

**Siguiente entrega de producto:** evaluaciones físicas y nutrición.

---

## Cómo se escala

| Escenario | Cómo entra el producto |
|-----------|------------------------|
| Un mostrador, una PC | App + PostgreSQL en la misma máquina |
| Varias PCs de recepción / admin | Misma base en un servidor del centro; cada puesto corre la app |
| Más sedes o más usuarios | Misma plataforma modular: se activan membresías, cobros y check-in sin cambiar de sistema |
| Crecimiento de datos | PostgreSQL y migraciones de esquema versionadas (Flyway) |

Tecnología: Java 21, JavaFX, JPA/Hibernate, PostgreSQL 16, Flyway. Escritorio nativo en Windows, rápido de operar en mostrador.

---

## Documentación

| Documento | Contenido |
|-----------|-----------|
| **Este README** | Producto, visión y cómo ejecutarlo |
| [Especificación funcional](docs/especificacion-funcional.md) | Requerimientos, casos de uso y roles |
| [Instalación](install/CLIENTE.md) | Entrega e instalación en la PC del centro |

---

## Cómo ejecutar (desarrollo)

JDK 21 · Maven 3.9+ · Docker Desktop · Windows 10+. Solo PostgreSQL va en contenedor.

```bash
copy .env.example .env
docker compose up -d
mvn -q test
```

En Windows: `scripts\start-db.bat` y doble clic en **`run.bat`**.

| Campo | Desarrollo |
|-------|------------|
| Host / puerto | `localhost` · `5432` |
| Base | `fitness_training` |
| Usuario | `postgres` |
| Contraseña | la de `.env` (`postgres` si no hay `.env`) |

Primera vez: se crea la base si hace falta y se aplican las migraciones. Después, un administrador cambia la conexión desde **Sistema → Base de datos**. Configuración local: `%USERPROFILE%\.fitness-training\database.properties`.

### Cuentas de desarrollo

| Usuario | Contraseña | Rol |
|---------|------------|-----|
| `admin` | `1234` | Administrador |
| `empleado1` | `emp123` | Recepción |
| `juan_prof` | `prof123` | Entrenador |
| `maria_nutri` | `nutri123` | Nutricionista |

En el login, **Cuentas de prueba** completa usuario y clave.

### Recorrido rápido

1. Abrir la app con `run.bat`.
2. Verificar PostgreSQL e ingresar.
3. Panel con indicadores.
4. Clientes: alta, filtro, bajas, carnet y QR.

---

## Empaquetado

| Uso | Comando |
|-----|---------|
| Desarrollo | `run.bat` |
| Carpeta de entrega | `package.bat` → `target\client-dist\` |
| Probar empaquetado | `scripts\start-app-packaged.bat` |
| Backup | `scripts\backup-db.bat` |

Antes de entregar: `.env.example` → `.env` y **cambiar la contraseña** de PostgreSQL. Guía: [`install/CLIENTE.md`](install/CLIENTE.md).
