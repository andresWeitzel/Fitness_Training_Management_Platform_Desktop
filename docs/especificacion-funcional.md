# Especificación funcional

Documento de **requerimientos funcionales**, **casos de uso** y **matriz de roles**. El [README](../README.md) es el índice del producto; este archivo es el detalle de análisis.

Estado: **I** implementado · **P** planificado · **N** no se implementa en esta etapa.

---

## 1. Actores

| Actor | Quién es | Opera la app |
|-------|----------|--------------|
| Administrador | Dueño / gerencia del centro | Sí |
| Recepción | Mostrador | Sí |
| Entrenador | Staff de entrenamiento | Sí |
| Nutricionista | Staff de nutrición | Sí |
| Cliente | Persona inscripta en el centro | **No**. Es sujeto de gestión, no usuario del escritorio |

---

## 2. Módulos y estado

| Módulo | Contenido | Estado |
|--------|-----------|--------|
| Identidad y sesión | Login, roles, menú por permiso, panel | **Cerrado (I)** |
| Clientes y credenciales | Ficha, listado, baja lógica, n° cliente, carnet, QR | **Cerrado (I)** |
| Membresías | Planes, alta, vencimientos | **Cerrado (I)** |
| Pagos y mora | Cobro, recargo, reactivar acceso | **Cerrado (I)** |
| Recepción (check-in) | Ingreso, histórico, bloqueo por deuda | **Cerrado (I)** |
| Personal | ABM de usuarios internos | **Cerrado (I)** |
| Entrenamiento | Ejercicios y rutinas estructuradas | **Cerrado (I)** |
| Evaluaciones | Historial de evaluaciones físicas | **Cerrado (I)** |
| Nutrición | Turnos, planes, ficha de salud con historial | **Cerrado (I)** |
| Analytics | Indicadores, vencimientos, mora, ingresos, ocupación | **Cerrado (I)** |
| Liquidación de haberes | Sueldos | **N** en esta etapa |

El alcance cubre la operación diaria del centro. Quedan **fuera de esta etapa** liquidación de haberes y asistencia de profesores.

---

## 3. Requerimientos funcionales

### Identidad, instalación y panel

| ID | Descripción | Estado |
|----|-------------|--------|
| RF-01 | Iniciar sesión con usuario/contraseña (BCrypt) y sesión por rol | I |
| RF-02 | Primera instalación: configurar PostgreSQL, crear base y migrar (Flyway). Después, solo **administrador autenticado** cambia la conexión. En login se **verifica** el estado, no se edita | I |
| RF-03 | Ver panel con indicadores reales (clientes activos, carnets, QR, bajas) | I |
| RF-10 | Filtrar menú y acciones según permisos | I |

### Clientes (módulo cerrado)

| ID | Descripción | Estado |
|----|-------------|--------|
| RF-04 | Registrar y editar cliente (documento único entre activos, nombre, contacto) | I |
| RF-05 | Filtrar el listado en vivo por documento, nombre, email, teléfono o n° de cliente | I |
| RF-05b | Filtrar el listado por alcance: activos, bajas o todos | I |
| RF-06 | Dar de baja lógica (no elimina historial). El DNI queda libre para un alta nueva. La ficha de baja se puede consultar | I |
| RF-07 | Asignar número de cliente automático `CLI-xxxxxx` al dar de alta | I |
| RF-08 | Emitir y renovar carnet con vencimiento a 12 meses | I |
| RF-09 | Generar credencial QR para check-in futuro (una vigente por cliente) | I |
| RF-09b | Consultar credenciales en ficha (tipo, código, vigencia / vencimiento) | I |

Foto de cliente (`photo_path`) está en el esquema; la carga desde la UI **no** forma parte del cierre de este módulo.

**Pase diario (RF-11)** queda planificado y se resuelve con membresías o recepción, no como extensión de la ficha de cliente.

### Extendidos / fuera de etapa

| ID | Descripción | Módulo | Estado |
|----|-------------|--------|--------|
| RF-11 | Registrar pase diario / ingreso sin membresía vigente | Membresías / Recepción | I |
| RF-12 | ABM de planes y membresías con vencimiento | Membresías | I |
| RF-13 | Registrar pago de membresía | Pagos | I |
| RF-14 | Bloquear acceso si hay deuda (mora) | Pagos / Check-in | I |
| RF-15 | Registrar pago de mora/recargo y reactivar acceso | Pagos | I |
| RF-16 | Cobrar ingreso diario | Pagos | I |
| RF-17 | Registrar check-in (carnet/QR) e histórico de ingresos | Recepción | I |
| RF-18 | ABM de personal interno y roles | Personal | I |
| RF-19 | Gestionar ejercicios y rutinas estructuradas | Entrenamiento | I |
| RF-20 | Registrar evaluaciones físicas con historial | Evaluaciones | I |
| RF-21 | Turnos y planes de nutrición | Nutrición | I |
| RF-22 | Ficha de salud / restricciones (historial, no overwrite 1:1) | Nutrición | I |
| RF-23 | Listados de vencimientos y mora | Analytics | I |
| RF-24 | Reportes de operación (ingresos, ocupación) | Analytics | I |
| RF-25 | Asistencia diaria de profesores | Personal | N |
| RF-26 | Liquidar haberes mensuales | Personal | N |

---

## 4. Casos de uso

### Implementados

| CU | Nombre | Actor | Vista | Estado |
|----|--------|-------|-------|--------|
| CU-01 | Configurar PostgreSQL (primera instalación) | Operador en instalación | `db-setup.fxml` | I |
| CU-01b | Cambiar conexión PostgreSQL | Administrador | Sistema → Base de datos | I |
| CU-01c | Verificar estado de conexión | Cualquier usuario en login | `login.fxml` | I |
| CU-02 | Iniciar sesión | Personal interno | `login.fxml` | I |
| CU-03 | Consultar panel | Quien tenga `DASHBOARD_VIEW` | `dashboard.fxml` | I |
| CU-04 | Registrar / editar cliente | Admin, Recepción | `clients.fxml` | I |
| CU-05 | Consultar clientes (incluye bajas, solo lectura) | Entrenador, Nutricionista | `clients.fxml` | I |
| CU-06 | Dar de baja cliente | Admin, Recepción | `clients.fxml` | I |
| CU-07 | Emitir / renovar carnet | Admin, Recepción | `clients.fxml` | I |
| CU-08 | Generar QR de acceso | Admin, Recepción | `clients.fxml` | I |
| CU-09 | Cerrar sesión | Personal interno | Shell | I |

### Extendidos (cerrados salvo N)

| CU | Nombre | Actor | Estado |
|----|--------|-------|--------|
| CU-10 | Gestionar membresías | Admin, Recepción | I |
| CU-11 | Registrar pagos y mora | Admin, Recepción | I |
| CU-12 | Controlar ingreso (check-in) | Admin, Recepción | I |
| CU-13 | Administrar personal | Admin | I |
| CU-14 | Armar rutina de entrenamiento | Admin, Entrenador | I |
| CU-15 | Registrar evaluación física | Admin, Entrenador, Nutricionista | I |
| CU-16 | Gestionar nutrición | Admin, Nutricionista | I |
| CU-17 | Consultar analytics | Admin | I |

---

## 5. Matriz de acceso por rol

La UI filtra el menú (`NavigationCatalog` + `AuthorizationService`). En Clientes, `CLIENTS_VIEW` consulta y `CLIENTS_MANAGE` habilita alta, edición, baja y credenciales.

| Módulo | Admin | Recepción | Entrenador | Nutricionista | Cliente |
|--------|:-----:|:---------:|:----------:|:-------------:|:-------:|
| Panel (CU-03) | ✓ | ✓ | ✓ | ✓ | — |
| Clientes consulta (CU-05) | ✓ | ✓ | ✓ | ✓ | — |
| Clientes gestión + carnet/QR (CU-04, 06–08) | ✓ | ✓ | — | — | — |
| Membresías (CU-10) | ✓ | ✓ | — | — | — |
| Pagos / mora (CU-11) | ✓ | ✓ | — | — | — |
| Recepción / check-in (CU-12) | ✓ | ✓ | — | — | — |
| Personal (CU-13) | ✓ | — | — | — | — |
| Entrenamiento (CU-14) | ✓ | — | ✓ | — | — |
| Evaluaciones (CU-15) | ✓ | — | ✓ | ✓ | — |
| Nutrición (CU-16) | ✓ | — | — | ✓ | — |
| Analytics (CU-17) | ✓ | — | — | — | — |
| Configuración de base (CU-01b) | ✓ | — | — | — | — |

Permisos en código: `PermissionCode` y tabla `role_permissions` (Flyway `V1__init_identity.sql`).

---

## 6. Cierre del módulo Clientes

Queda **cerrado para operación diaria** (admin y recepción) con este contrato:

1. Alta con documento único entre clientes activos.
2. Edición de contacto; el documento no se modifica.
3. Listado con filtro en vivo y alcance Activos / Bajas / Todos.
4. Baja lógica: historial se conserva, DNI disponible para un alta nueva, ficha consultable.
5. Credenciales: n° de cliente al alta, carnet 12 meses, QR de acceso.
6. Entrenador y nutricionista solo consultan.

**Fuera de este cierre (a propósito):** foto, pase diario, membresía, pagos y control de ingreso.

Siguiente módulo natural: **Membresías** (RF-12, CU-10).

---

## 6b. Cierre del módulo Pagos

Queda **cerrado para operación diaria** (admin y recepción) con este contrato:

1. Registrar cobro de membresía vinculado a una membresía del cliente.
2. Registrar mora/recargo (pendiente o cobrado) y cancelar pendientes.
3. Cobrar ingreso diario sin membresía.
4. Listar y filtrar: todos, cobrados, pendientes, en mora, cancelados.
5. API `hasBlockingDebt(clientId)` / `hasOpenDebt(clientId)`: bloquea solo mora (PENDING vencido o `LATE_FEE` pendiente), no cualquier pendiente futuro.
6. Congruencia: asignar/renovar/cambiar plan en Membresías genera cobro `MEMBERSHIP` (Pendiente / Cobrado / Cortesía).

**Fuera de este cierre:** facturación electrónica / pasarela externa.

---

## 6c. Cierre del módulo Recepción

Queda **cerrado para operación diaria** (admin y recepción) con este contrato:

1. Verificar ingreso por documento, n° de cliente, carnet o QR.
2. Bloquear solo si hay mora (`hasBlockingDebt`: PENDING vencido o LATE_FEE).
3. Permitir acceso con membresía activa o pase diario cobrado hoy.
4. Ver detalle del ingreso (cliente, contacto, credenciales con copiar, modo de acceso).
5. Consultar ingresos de hoy e histórico por fecha.
6. Deep-link: denegación por mora → **Ir a Pagos** precarga el cliente.

**Fuera de este cierre:** cupo por actividad/turno (no aplica a recepción libre del predio).

---

## 6d. Cierre del módulo Personal

Queda **cerrado para administración** (solo Admin) con este contrato:

1. Listar usuarios internos (activos / bajas / todos) con búsqueda.
2. Alta con usuario, nombre, email opcional, rol y contraseña (BCrypt).
3. Edición completa desde la ficha (usuario, nombre, email, rol y contraseña opcional).
4. Baja lógica (`active=false`) sin borrar historial; reactivación posible.
5. Protecciones: no auto-baja; no dejar el sistema sin un Admin activo; usuario único.

**Fuera de este cierre:** ABM del catálogo de roles/permisos, asistencia de profesores y liquidación (RF-25/26 = N).

---

## 6e. Cierre del módulo Entrenamiento

Queda **cerrado** para Admin y Entrenador (`TRAINING_MANAGE`) con este contrato:

1. Catálogo de ejercicios (nombre, grupo, equipo, nivel, músculos secundarios, descripción, notas técnicas; alta/edición; desactivar/reactivar).
2. Rutinas por cliente con ítems estructurados (ejercicio, series, reps, descanso, carga) y detalle técnico por ejercicio.
3. Listado de rutinas por estado: activas / borrador / programadas / archivadas / todas, con búsqueda.
4. Enfoque, fecha de inicio (programadas) y registro del entrenador en sesión al crear/editar.

**Fuera de este cierre:** seguimiento de cumplimiento sesión a sesión y plantillas globales reutilizables.

---

## 6f. Cierre del módulo Evaluaciones

Queda **cerrado** para Admin, Entrenador y Nutricionista (`ASSESSMENTS_MANAGE`) con este contrato:

1. Registrar evaluación física (peso, talla, % grasa, perímetros, notas) con fecha e historial.
2. Listar/filtrar historial (todos / 30 días / 90 días) por cliente y búsqueda.
3. Consultar detalle en ventana aparte; alta desde botón + modal (sin pestaña duplicada).
4. Resumen de la última evaluación visible en la ficha del cliente.

**Fuera de este cierre:** gráficos de evolución y comparación automática entre mediciones.

---

## 6g. Cierre del módulo Nutrición

Queda **cerrado** para Admin y Nutricionista (`NUTRITION_MANAGE`) con este contrato:

1. Agendar, reprogramar, completar, cancelar y marcar ausente turnos programados.
2. Planes nutricionales (borrador / activo / archivado) con objetivos y guía alimentaria.
3. Ficha de salud append-only (alergias, restricciones, condiciones, medicación); no sobrescribe entradas previas.
4. Al dar de baja un cliente: cancelar turnos programados y archivar planes abiertos.
5. Resumen del plan nutricional activo en la ficha del cliente.

**Fuera de este cierre:** agenda compartida con cupos, plantillas de menú y sync con wearables.

---

## 6h. Cierre del módulo Analytics

Queda **cerrado** para Admin (`ANALYTICS_VIEW`) con este contrato:

1. Listado de membresías por vencer (filtro 1–90 días) con resumen.
2. Listado de mora / deuda que bloquea acceso (PENDING vencido o LATE_FEE pendiente), con atajo a Pagos.
3. Reporte de ingresos cobrados por rango de fechas, con total.
4. Reporte de ocupación diaria (ingresos y clientes únicos) por rango.
5. Exportación CSV (`;`, UTF-8) de cada reporte.

**Fuera de este cierre:** gráficos, Excel `.xlsx`, asistencia de profesores y liquidación de haberes (RF-25/26 = N).

---

## 7. Glosario

| Término | Definición |
|---------|------------|
| Cliente | Persona inscripta en el centro de entrenamiento. |
| Credencial | Identificador de acceso: número de cliente, carnet o QR. |
| Carnet | Credencial con vigencia (hoy 12 meses). |
| Membresía | Plan contratado con vigencia. |
| Mora | Deuda vencida; puede bloquear el ingreso hasta regularizar. |
| Check-in | Registro de ingreso al predio. |
| Baja lógica | El cliente deja de estar activo; no se borra el historial. |
| Rol | Perfil interno: ADMIN, RECEPTIONIST, TRAINER, NUTRITIONIST. |
