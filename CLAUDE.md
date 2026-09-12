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

- Fase en curso: **Fase 0 — Setup**.
- Última fase completada: ninguna todavía.
