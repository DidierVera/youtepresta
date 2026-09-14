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

- Fase en curso: **Fase 4** (préstamos — ver sección 8 de `docs/spec.md`: formulario nuevo préstamo con `FriendPicker` embebido, descuento del bolsillo origen, lista con badges de estado).
- Última fase completada: **Fase 3 — Friends (amigos)**. `FriendRepository` (singleton) con `getFriends` (ordenado por `name`) y `createFriend(name, phone, notes)`. `FriendsViewModel`/`FriendsScreen` en `ui/friends`: lista (`LazyColumn`) + `AlertDialog` de creación (nombre obligatorio, teléfono y notas opcionales). Además, `FriendPicker` — composable reutilizable e independiente (con su propio `FriendPickerViewModel`, sin acoplarse a `FriendsScreen`/`FriendsViewModel`) que carga los amigos una sola vez, filtra en memoria mientras se escribe, y si no hay coincidencias ofrece "Crear amigo '<texto>'" que crea y auto-selecciona; expone solo `onFriendSelected: (Friend) -> Unit`. Listo para insertarse en el formulario de préstamo de la Fase 4. Ruta `friends` agregada al `NavHost` y botón temporal "Ver amigos" en `HomeScreen`. También quedó `FriendPickerTestScreen` (ruta `friends/picker-test`, botón temporal "Probar selector de amigo" en `HomeScreen`) solo para probar `FriendPicker` antes de que exista el formulario de préstamo — **eliminar esta pantalla y su botón/ruta cuando `FriendPicker` se inserte en la Fase 4**. Flujo completo (crear amigos, validación de nombre vacío, búsqueda con filtrado local, selección y creación en línea desde el picker) probado en dispositivo físico contra Supabase real.
- Nota técnica: el BOM de Compose del proyecto resuelve a **Material3 1.4.0**, donde `ExposedDropdownMenu` y `Modifier.menuAnchor(...)` ya no son funciones top-level sino miembros de `ExposedDropdownMenuBoxScope` (se llaman sin import, dentro del lambda de `ExposedDropdownMenuBox`), y `MenuAnchorType` quedó deprecado a favor de `ExposedDropdownMenuAnchorType`. Tenerlo en cuenta para cualquier otro selector tipo dropdown que se agregue más adelante.
- Nota técnica: los `ModalBottomSheet` con varios campos necesitan `.verticalScroll(rememberScrollState()).imePadding()` en su `Column` interno — sin eso, con el teclado abierto los campos de más abajo (Nota, botón Guardar) quedaban físicamente detrás del teclado e inalcanzables. Ya corregido en `FundingSourcesScreen` y `FundingSourceDetailScreen`; replicar en bottom sheets futuros.
- Queda en la base de datos de Supabase un bolsillo de prueba ("Presupuesto para prestamos", con movimientos "Saldo inicial", "Dinero 50000" y "Retiro de prueba") creado durante las pruebas de la Fase 2, y tres amigos de prueba ("Camilo", "Daniel", "Pedroniel") creados durante las pruebas de la Fase 3 — el usuario decidió no borrarlos por ahora.
