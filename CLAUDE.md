# Loans Tracker — guía para Claude Code

App Android nativa (Kotlin + Jetpack Compose) para administrar y monitorear préstamos informales a amigos: registrar préstamos, cobrar con desglose de capital/interés, saber de qué "bolsillo" salió cada préstamo, y avisar por notificación local el día que algo vence. Uso personal, un solo usuario.

Documentación completa (modelo de datos, esquema SQL, ejemplos de interés, roadmap por fases, prompts ya usados): **`docs/spec.md`** en este mismo repo. Este archivo es solo el resumen que debe respetarse siempre; para el detalle de una fase específica, lee `docs/spec.md`.

## Stack técnico

- Kotlin + Jetpack Compose (no XML Views), arquitectura MVVM (ViewModel + StateFlow) + capa de Repositorio.
- Supabase como backend: Postgres + Auth + RLS. Cliente `supabase-kt` (módulos `postgrest-kt`, `auth-kt`).
- Notificaciones: **100% locales con WorkManager** (`PeriodicWorkRequest`, sin hora exacta). No usar Firebase, FCM, Edge Functions ni `pg_cron` — quedó descartado a propósito por simplicidad.
- Sin Room ni caché offline en la v1: se consulta Supabase directo.
- Sin librería de inyección de dependencias (Hilt/Koin): usar un provider/singleton simple para el cliente de Supabase.

## Convenciones de código (no negociables)

- **Todo en inglés**: nombres de paquetes, clases, propiedades, archivos y carpetas. Los mensajes que se muestran al usuario en la UI sí pueden ir en español si el usuario lo pide explícitamente para una pantalla, pero el código en sí siempre en inglés.
- Las tablas de Supabase están en `snake_case` (inglés); las data classes de Kotlin en `camelCase`, mapeadas con `@Serializable` + `@SerialName`.
- Estructura de paquetes: `data/model`, `data/remote`, `data/repository`, `ui/<feature>` (`auth`, `sources`, `friends`, `loans`, `payments`).

## Reglas de negocio que no se deben "optimizar" ni reinterpretar

- **El interés nunca se calcula automáticamente por fecha.** Al crear un préstamo, la tasa es solo informativa. Al registrar un pago, el usuario escribe manualmente `principal_payment` e `interest_payment` — no hay fórmula de prorrateo por días. (Motivo: los acuerdos reales con amigos no siguen amortización bancaria — ver ejemplos en `docs/spec.md` sección 3.)
- El estado del préstamo (`status`) se deriva de `outstanding_principal` (`principal_amount − suma de principal_payment`), nunca del interés.
- Capital e interés de un mismo pago pueden ir a **bolsillos (`funding_sources`) distintos**: `principal_destination_source_id` (obligatorio) e `interest_destination_source_id` (opcional, si es nulo usa el mismo que el capital).
- Cada movimiento de dinero en un bolsillo (préstamo, pago, o ingreso/retiro manual) debe quedar registrado en `source_movements` — es la fuente de trazabilidad, no es opcional.
- Un solo usuario, sin pantalla de registro: RLS filtra siempre por `auth.uid() = user_id`.

## Estado actual del proyecto

> Actualiza esta sección manualmente (o pídele a Claude Code que la actualice) cada vez que se cierre una fase, para que la próxima sesión sepa dónde vamos sin tener que preguntarte.

- Fase en curso: **Fase 5** (pagos — ver sección 8 de `docs/spec.md`: bottom sheet "Registrar pago" con `principal_payment`/`interest_payment`, bolsillo destino de capital obligatorio y de interés opcional, dos `source_movements` tipo `income` por pago, recalcular `outstanding_principal` y `status`).
- Última fase completada: **Fase 4 — Loans (préstamos)**. `LoanRepository` (singleton) con `getLoans`, `getLoan(id)` y `createLoan(friendId, sourceId, principalAmount, interestType, interestValue, dueDate)`: inserta el `loan` (`status = "active"`, `outstanding_principal = principalAmount`, `loan_date` sin enviar — lo resuelve el default `current_date` de Postgres) y descuenta el bolsillo de origen llamando a `FundingSourceRepository.recordSourceMovement(..., referenceLoanId = created.id)`. Se refactorizó `FundingSourceRepository.addManualMovement` extrayendo `recordSourceMovement` (visibilidad `internal`, con `referenceLoanId`/`referencePaymentId` opcionales) para reutilizarla también en la Fase 5 desde el repositorio de pagos. `LoanVisualStatus` (`ui/loans/LoanVisualStatus.kt`) calcula el badge (Pagado/Atrasado/Vence hoy/Al día) puramente en pantalla a partir de `status` + `due_date`, sin escribir nunca "overdue" en la base de datos. `LoansViewModel`/`LoansScreen`: lista con badge de color y FAB a `NewLoanScreen`. `NewLoanViewModel`/`NewLoanScreen` (pantalla completa, no bottom sheet): `FriendPicker` embebido para elegir/crear amigo, monto, dropdown de bolsillo de origen (con advertencia no bloqueante si el monto supera el saldo), `DatePickerDialog` de Material3 para la fecha tentativa, y sección "Mostrar más" colapsada con tipo de interés (ninguno/fijo) y valor informativo. `LoanDetailViewModel`/`LoanDetailScreen`: resumen completo del préstamo + sección "Historial de pagos" con estado vacío (sin botón de registrar pago todavía). Rutas `loans`, `loans/new` y `loan/{loanId}` (detalle en **singular** "loan", a propósito, para que nunca sea ambigua con la ruta estática `loans/new`) agregadas al `NavHost`; botón "Ver préstamos" en `HomeScreen`.
- Cómo se traen amigo/bolsillo junto a cada préstamo: **join en memoria**, no embedding de foreign keys de supabase-kt. `LoansViewModel` pide `LoanRepository.getLoans()` + `FriendRepository.getFriends()` y arma un mapa `friendId -> name`; `LoanDetailViewModel` pide el `Loan`, busca el `Friend` en `FriendRepository.getFriends()` y el `FundingSource` con `FundingSourceRepository.getFundingSource(id)`. Se eligió así por ser lo más simple de implementar bien; con el volumen de datos de un solo usuario el costo de las consultas extra es irrelevante.
- Pendiente de limpieza (no bloquea nada): `FriendPickerTestScreen` (ruta `friends/picker-test`, botón "Probar selector de amigo" en `HomeScreen`) ya quedó redundante — `FriendPicker` ya está embebido de verdad en `NewLoanScreen` — pero todavía no se eliminó. Bórrala junto con su ruta y botón en la próxima sesión que toque `ui/friends` o `HomeScreen`.
- Nota técnica: el BOM de Compose del proyecto resuelve a **Material3 1.4.0**, donde `ExposedDropdownMenu` y `Modifier.menuAnchor(...)` ya no son funciones top-level sino miembros de `ExposedDropdownMenuBoxScope` (se llaman sin import, dentro del lambda de `ExposedDropdownMenuBox`), y `MenuAnchorType` quedó deprecado a favor de `ExposedDropdownMenuAnchorType`. Tenerlo en cuenta para cualquier otro selector tipo dropdown que se agregue más adelante.
- Nota técnica: los `ModalBottomSheet` con varios campos necesitan `.verticalScroll(rememberScrollState()).imePadding()` en su `Column` interno — sin eso, con el teclado abierto los campos de más abajo (Nota, botón Guardar) quedaban físicamente detrás del teclado e inalcanzables. Ya corregido en `FundingSourcesScreen` y `FundingSourceDetailScreen`; replicar en bottom sheets futuros.
- Queda en la base de datos de Supabase un bolsillo de prueba ("Presupuesto para prestamos", con movimientos "Saldo inicial", "Dinero 50000" y "Retiro de prueba") creado durante las pruebas de la Fase 2, tres amigos de prueba ("Camilo", "Daniel", "Pedroniel") creados durante las pruebas de la Fase 3, y un préstamo de prueba (Daniel, $1.000.000, bolsillo "Presupuesto para prestamos", vencimiento 14 oct. 2026, interés fijo informativo guardado como "109" por un error de tecleo durante la prueba, sin relevancia funcional) creado durante las pruebas de la Fase 4 — el usuario decidió no borrar estos datos por ahora.
