# App de préstamos a amigos — Resumen técnico y de producto

Documento de trabajo con dos sombreros: **arquitectura** (cómo construirlo bien) y **product owner** (qué construir primero y cómo simplificarlo). Decisiones ya tomadas contigo: app **móvil** (no web), y control de fuentes de dinero **con saldo por fuente** (tipo mini-cuentas).

---

## 1. Resumen del problema

Necesitas dejar de llevar los préstamos a amigos "de memoria" o en notas sueltas. La app debe permitir:

- Registrar un préstamo nuevo (amigo, monto, interés opcional, fecha tentativa de pago).
- Avisarte por push el día que un préstamo vence.
- Ver todos los préstamos en una lista.
- Al pagar, separar el pago en abono a capital vs. interés.
- Saber de qué "bolsillo" salió el dinero de cada préstamo (salario, pago de otro amigo, etc.) y llevar el saldo de cada bolsillo.
- Ser lo más simple posible: pocas pantallas, pocos campos.
- Acceso solo tuyo (login), datos en Supabase.

---

## 2. Modelo de datos propuesto (Postgres / Supabase)

Todos los nombres de tablas y columnas en inglés (igual que las entidades y propiedades que usarás en el código Kotlin):

```
friends
  id, name, phone (nullable), notes, created_at

funding_sources          -- "bolsillos" (Salario, Pago de Juan, etc.)
  id, name, current_balance, created_at

source_movements         -- historial de entradas/salidas de cada bolsillo (trazabilidad)
  id, source_id, movement_type (income/outflow), amount, notes (opcional),
  reference_loan_id (nullable), reference_payment_id (nullable), created_at
  -- reference_loan_id y reference_payment_id nulos = movimiento manual (ej. agregaste dinero al bolsillo a mano)

loans
  id, friend_id, source_id, principal_amount,
  interest_type (none / fixed), interest_value,
  loan_date, due_date,
  outstanding_principal,
  status (active / partial / paid / overdue),
  notes, created_at

payments
  id, loan_id, payment_date,
  principal_payment, interest_payment,
  principal_destination_source_id,          -- a qué bolsillo regresa el capital
  interest_destination_source_id (opcional), -- a qué bolsillo va el interés; si es nulo, va al mismo que el capital
  created_at
```

**Cómo se conecta el flujo de dinero:** al crear un préstamo, se descuenta `principal_amount` del `funding_source` elegido (se inserta un `source_movement` de tipo `outflow`) y baja su `current_balance`. Al registrar un pago, el capital (`principal_payment`) vuelve al bolsillo que elijas en `principal_destination_source_id`, y el interés (`interest_payment`) va, por defecto, al mismo bolsillo — pero puedes elegir uno distinto en `interest_destination_source_id` (ej. un bolsillo de "Ganancias") si quieres ver tus ganancias por intereses separadas del capital que recuperas. Cada uno genera su propio `source_movement` de tipo `income`, aunque terminen en el mismo bolsillo, para que el historial siempre distinga capital recuperado de interés ganado. Así el saldo de cada bolsillo siempre refleja "cuánto tengo disponible de esa plata ahora mismo", sin que tengas que calcularlo a mano.

Esto es justo el caso que describiste: usar el dinero que te devolvió un amigo para prestárselo a otro. Con saldo por fuente puedes ver, antes de prestar, si ese bolsillo tiene fondos suficientes.

**Seguridad de datos:** todas las tablas llevan `user_id` y Row Level Security (RLS) activado, con una política `auth.uid() = user_id`. Como eres el único usuario, además puedes fijar en la política un UUID específico como capa extra (aunque con RLS + login ya es suficiente).

---

## 3. Interés: cómo se calcula (basado en ejemplos reales)

Con tres ejemplos reales del usuario se confirma que el interés **no se prorratea por días transcurridos**:

- **Juan:** 500.000, 0% interés, plazo 1 mes. Paga 500.000 al mes. Caso simple, sin interés.
- **Daniel:** 1.000.000, 10% interés, plazo 1 mes. Paga a los 15 días (mitad del plazo) pero el interés no se reduce a la mitad — paga el 10% completo (1.100.000 en total). Es decir, pagar antes de tiempo no prorratea el interés.
- **Camilo:** 10.000.000, 3% interés, plazo 2 meses. Al mes 1 paga 5.000.000 de capital + 300.000 de interés (3% de los 10 millones **originales**). Al mes 2 paga los 5.000.000 restantes + otros 300.000 de interés (de nuevo 3% del capital original, no del saldo de 5 millones que quedaba). El interés se cobra "por cuota" sobre el capital original, no sobre saldo decreciente tipo amortización bancaria.

**Conclusión de diseño:** cada acuerdo de interés es distinto (a veces es un % único al cierre, a veces un % por cada abono), así que automatizar el cálculo con una fórmula de fecha (días × tasa) daría números que no coinciden con lo realmente pactado. En vez de eso:

- La tasa de interés que ingresas al crear el préstamo es **solo informativa** (para recordar qué acordaste), no dispara ningún cálculo automático.
- Al registrar cada pago, escribes directamente dos montos: **abono a capital** (`principal_payment`) y **pago de interés** (`interest_payment`) — la app te muestra el total sumado para validar que cuadra con lo que te pagaron.
- El sistema lleva la cuenta con: `outstanding_principal = principal_amount − suma(principal_payment)`. El préstamo pasa a `status = paid` cuando ese saldo llega a 0, sin importar cuánto interés total se haya cobrado en el camino.

Con este diseño los tres ejemplos funcionan sin fricción, sin importar si el acuerdo fue un solo pago, pagos anticipados, o varias cuotas con interés fijo repetido.

*Pendiente para v2 (no bloquea el MVP):* decidir si quieres que la app sugiera algún interés moratorio cuando un amigo se atrasa después de la fecha pactada, o si prefieres seguir manejándolo manual como los demás casos.

---

## 4. Arquitectura técnica recomendada (Android nativo)

- **Frontend:** Kotlin + **Jetpack Compose** (recomendado sobre XML Views: es el toolkit de UI que Google impulsa para proyectos nuevos, menos código, mejor soporte a futuro — XML Views está en modo mantenimiento). Arquitectura MVVM (ViewModel + StateFlow) con una capa de Repositorio que habla con Supabase, siguiendo la guía oficial de arquitectura de Android. Navegación con Compose Navigation.
- **Backend:** Supabase (solo Postgres + Auth + RLS). **No hace falta Edge Functions ni `pg_cron`** — toda la lógica de "revisar vencimientos" vive en el propio celular (ver más abajo), así que el backend se reduce a la base de datos.
- **Cliente Supabase en Android:** `supabase-kt`, el cliente oficial de Kotlin para Supabase (Auth, Postgrest).
- **Notificaciones: 100% locales, sin servicio externo (decisión confirmada):**
  1. `WorkManager` programa un `PeriodicWorkRequest` que corre una vez al día — sin hora exacta, con flexibilidad para que el sistema lo ejecute cuando le convenga a la batería en algún momento del día (esto es justo lo que pediste: notificación "durante el día", no a una hora fija).
  2. El Worker consulta directo a Supabase (vía `supabase-kt`) por préstamos con `fecha_pago_tentativa = hoy` y estado activo/parcial.
  3. Si encuentra alguno, dispara una notificación local con `NotificationManager` (agrupada si hay varios: "Tienes 2 cobros hoy").
  4. El tap abre directo la pantalla de ese préstamo para registrar el pago.
  5. Como red de seguridad extra (sin costo), la app también revisa "¿algo vence hoy?" cada vez que la abres, por si el Work del día se retrasó.
  - **No se necesita:** proyecto de Firebase, FCM, Edge Function, `pg_cron`, ni tabla de `device_tokens` — todo esto se elimina de la arquitectura.
- **Autenticación:** Supabase Auth con email/password. Como eres el único usuario, creas la cuenta una sola vez desde el panel de Supabase; no necesitas pantalla de registro, solo login.
- **Persistencia local:** no hace falta Room ni caché offline para la v1 — se consulta Supabase directamente cada vez, con estados de carga simples. Se puede agregar Room más adelante si algún día necesitas que funcione sin internet.

Esta combinación (Android nativo con Compose + Supabase + notificación local vía WorkManager) es la más simple posible: nada de infraestructura de push externa, solo tu app y la base de datos.

---

## 5. Recomendaciones de producto (simplicidad ante todo)

- **Formulario de préstamo nuevo:** solo 3 campos obligatorios — amigo, monto, fecha tentativa. Interés y bolsillo de origen con valores por defecto ("ninguno" y tu bolsillo más usado) y expandibles si quieres cambiarlos, para no mostrar 8 campos de una vez.
- **Amigo nuevo al vuelo:** si el amigo no existe, se crea desde el mismo formulario (un campo tipo "buscar o crear"), sin salir a otra pantalla.
- **Una sola lista principal** con estados visuales por color: al día (verde), vence hoy (amarillo), atrasado (rojo), pagado (gris/archivado). Evita tener listas separadas por estado.
- **Pantalla de detalle del préstamo:** resumen + historial de pagos + un botón "Registrar pago" que abre una hoja inferior (bottom sheet), no una pantalla nueva, con el saldo de capital pendiente ya calculado, dos campos simples para escribir cuánto es abono a capital y cuánto es interés (ver sección 3), un bolsillo destino (por defecto el mismo de donde salió el préstamo), y un "mostrar más" opcional para elegir un bolsillo distinto solo para el interés si quieres separar tus ganancias.
- **Notificación → acción directa:** al tocar la notificación, ve directo a "Registrar pago" de ese préstamo, no a la lista general.
- **Bolsillos (fuentes):** una pantalla simple tipo "billeteras" que muestra cada fuente con su saldo actual; crear una nueva fuente es un solo campo (nombre + saldo inicial opcional). Al entrar al detalle de un bolsillo, ves su historial completo de movimientos (préstamos que salieron, pagos que entraron, y ajustes manuales) — eso es lo que te da trazabilidad. Un botón "Agregar dinero" en ese detalle te deja registrar un ingreso manual (monto + nota opcional) cuando metes plata al bolsillo que no viene de un pago de préstamo.

### MVP (primera versión) vs. v2

**MVP:** amigos, préstamos (crear/editar/eliminar), pagos con desglose capital/interés, push el día del vencimiento, login único, lista + detalle, bolsillos con saldo.

**v2 (después, si lo sigues usando):** reportes (total prestado por amigo/por bolsillo/por mes), recordatorios configurables (ej. avisar 2 días antes), interés por mora automático, exportar a CSV/Excel, notas o adjuntos (foto del acuerdo), respaldo/exportación de toda la base de datos.

---

## 6. Decisiones ya tomadas

- Plataforma: **Android nativo** (Kotlin + Jetpack Compose recomendado) — no multiplataforma, solo Android.
- Fuentes de dinero: **con saldo por bolsillo** (no solo etiqueta), incluyendo el historial de movimientos.
- Interés: **informativo al crear el préstamo, desglose manual (capital/interés) al registrar cada pago** — sin fórmula automática por fecha, porque cada acuerdo real varía (ver sección 3).
- Notificaciones: **100% locales con WorkManager**, sin hora exacta (en algún momento del día), sin Firebase/FCM ni Edge Functions.
- Convención de nombres: **todo el código en inglés** — nombres de tablas/columnas en Supabase, clases/propiedades Kotlin, y nombres de carpetas y archivos del proyecto. Este documento se mantiene en español (para discutir contigo), pero cualquier identificador técnico se define en inglés desde el día uno (ya reflejado en las secciones 2 y 7).

---

## 7. Supabase paso a paso (cuenta, proyecto y base de datos)

Como lo has usado poco, aquí va el paso a paso completo, desde crear el proyecto hasta tener las credenciales listas para Android:

1. **Crear el proyecto:** entra a supabase.com, inicia sesión (o crea cuenta), clic en "New project". Elige tu organización, dale un nombre (ej. `loans-tracker`), define una **contraseña de base de datos** (guárdala en un gestor de contraseñas, la necesitarás poco pero es la del usuario `postgres`), y elige la región más cercana (para Colombia, la más cercana suele ser South America - São Paulo). Clic en "Create new project" y espera 1-2 minutos mientras se aprovisiona.
2. **Crear las tablas:** en el menú lateral entra a **SQL Editor** → "New query", pega completo el script de la sección 7.1 de abajo, y ejecútalo (botón "Run" o Ctrl/Cmd+Enter). Verifica en **Table Editor** que aparecen las 5 tablas.
3. **Confirmar el proveedor de email:** en **Authentication → Providers**, confirma que "Email" está habilitado (viene así por defecto).
4. **Simplificar la confirmación de correo (opcional pero recomendado):** en **Authentication → Settings**, puedes desactivar "Confirm email" ya que vas a crear tu único usuario manualmente y no necesitas el flujo de confirmación por correo.
5. **Crear tu único usuario:** en **Authentication → Users**, clic en "Add user" → "Create new user". Ingresa tu correo y una contraseña segura; si aparece la opción "Auto Confirm User", actívala para no depender de un correo de confirmación. Este es el único login que existirá en la app.
6. **Obtener las credenciales para Android:** en **Project Settings** (ícono de engranaje) → **API**, copia el **Project URL** y la clave **`anon` `public`**. Estas dos son las que usará `supabase-kt` desde la app. **Nunca uses la `service_role` key desde la app** — esa es solo para uso de servidor y da acceso total sin pasar por RLS.
7. **Guardar las credenciales sin subirlas a GitHub:** en tu proyecto Android, agrega al archivo `local.properties` (que Android Studio ya excluye de git por defecto):
   ```properties
   SUPABASE_URL=https://tu-proyecto.supabase.co
   SUPABASE_ANON_KEY=tu-anon-key-aqui
   ```
   Y en `app/build.gradle.kts`, expón esos valores como `BuildConfig` para leerlos desde Kotlin sin hardcodearlos:
   ```kotlin
   android {
       defaultConfig {
           val localProperties = org.jetbrains.kotlin.konan.properties.Properties()
           localProperties.load(rootProject.file("local.properties").inputStream())
           buildConfigField("String", "SUPABASE_URL", "\"${localProperties["SUPABASE_URL"]}\"")
           buildConfigField("String", "SUPABASE_ANON_KEY", "\"${localProperties["SUPABASE_ANON_KEY"]}\"")
       }
       buildFeatures { buildConfig = true }
   }
   ```
   Confirma que `local.properties` está en tu `.gitignore` (por defecto lo está en cualquier proyecto nuevo de Android Studio) antes de tu primer commit.

### 7.1 Script SQL (tablas + RLS)

Todo en inglés, listo para pegar en el SQL Editor:

```sql
-- Funding sources ("bolsillos")
create table funding_sources (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null default auth.uid() references auth.users(id),
  name text not null,
  current_balance numeric not null default 0,
  created_at timestamptz not null default now()
);

-- Friends
create table friends (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null default auth.uid() references auth.users(id),
  name text not null,
  phone text,
  notes text,
  created_at timestamptz not null default now()
);

-- Loans
create table loans (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null default auth.uid() references auth.users(id),
  friend_id uuid not null references friends(id),
  source_id uuid not null references funding_sources(id),
  principal_amount numeric not null,
  interest_type text not null default 'none', -- none | fixed
  interest_value numeric default 0,
  loan_date date not null default current_date,
  due_date date not null,
  outstanding_principal numeric not null,
  status text not null default 'active', -- active | partial | paid | overdue
  notes text,
  created_at timestamptz not null default now()
);

-- Payments
create table payments (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null default auth.uid() references auth.users(id),
  loan_id uuid not null references loans(id),
  payment_date date not null default current_date,
  principal_payment numeric not null default 0,
  interest_payment numeric not null default 0,
  principal_destination_source_id uuid not null references funding_sources(id),
  interest_destination_source_id uuid references funding_sources(id), -- null = mismo que principal_destination_source_id
  created_at timestamptz not null default now()
);

-- Movements per funding source (drives the balance)
create table source_movements (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null default auth.uid() references auth.users(id),
  source_id uuid not null references funding_sources(id),
  movement_type text not null, -- income | outflow
  amount numeric not null,
  notes text, -- ej. "Bono de fin de año" en un movimiento manual
  reference_loan_id uuid references loans(id),   -- null = no viene de un préstamo
  reference_payment_id uuid references payments(id), -- null = no viene de un pago
  created_at timestamptz not null default now()
);

-- RLS: enable and restrict everything to your own user
alter table funding_sources enable row level security;
alter table friends enable row level security;
alter table loans enable row level security;
alter table payments enable row level security;
alter table source_movements enable row level security;

create policy "owner_only" on funding_sources for all using (auth.uid() = user_id) with check (auth.uid() = user_id);
create policy "owner_only" on friends for all using (auth.uid() = user_id) with check (auth.uid() = user_id);
create policy "owner_only" on loans for all using (auth.uid() = user_id) with check (auth.uid() = user_id);
create policy "owner_only" on payments for all using (auth.uid() = user_id) with check (auth.uid() = user_id);
create policy "owner_only" on source_movements for all using (auth.uid() = user_id) with check (auth.uid() = user_id);
```

Nota: `outstanding_principal` en `loans` se inicializa igual a `principal_amount` al crear el préstamo, y se recalcula (`− suma de principal_payment`) cada vez que registras un pago — puedes hacerlo desde la app (más simple de depurar al inicio) o más adelante con un trigger de Postgres si quieres que sea automático a nivel de base de datos.

---

## 8. Roadmap de desarrollo

Pensado para ir haciendo commits/PRs por fase en tu repo de GitHub. Cada fase termina en un estado usable ("milestone").

**Cómo dividir el trabajo entre este chat y Claude Code en Android Studio:** este chat es donde tomamos decisiones de arquitectura/producto, mantenemos este documento y generamos artefactos como el script SQL. Todo lo que implica tocar el proyecto Android real —crearlo, escribir el código de cada fase, compilar, correr en el emulador/dispositivo, y corregir errores de compilación o de ejecución— lo haces directamente con Claude Code desde el terminal integrado en Android Studio, porque ahí sí tiene acceso al SDK de Android, al emulador y al proyecto real. Te recomiendo llevar este documento dentro del repo (ej. `docs/spec.md`) para que puedas pasarle a Claude Code el contexto de cada fase al pedirle el código. Cada fase abajo indica qué le pedirías a Claude Code.

**Fase 0 — Setup**
- *Aquí (Supabase, fuera del código):* crear el proyecto en Supabase, correr el script SQL de la sección 7.1, y crear tu único usuario desde el panel (sección 7, pasos 1-6).
- *Con Claude Code en Android Studio:* pídele crear el proyecto Android nuevo (Empty Activity, Compose, Kotlin), agregar las dependencias (Compose BOM, Navigation Compose, `lifecycle-viewmodel-compose`, Coroutines, `kotlinx-serialization`, `io.github.jan-tennert.supabase:postgrest-kt` y `:auth-kt`, `ktor-client-okhttp`, `androidx.work:work-runtime-ktx`), configurar `local.properties` + `BuildConfig` para las credenciales de Supabase (sección 7, paso 7), y crear la estructura de paquetes en inglés: `data/` (models + repositories), `domain/` (opcional), `ui/auth/`, `ui/sources/`, `ui/friends/`, `ui/loans/`, `ui/payments/`.
- *Milestone:* el proyecto compila y corre en el emulador, conectado a Supabase, con las 5 tablas ya creadas.

**Fase 1 — Login**
- *Con Claude Code:* pantalla de login (email + password) en Compose (`ui/auth/LoginScreen.kt`), `AuthViewModel` + `AuthRepository` que envuelve `auth-kt` (la sesión la persiste la propia librería), y una pantalla `HomeScreen` vacía protegida tras iniciar sesión.
- *Milestone:* inicias sesión con tu único usuario y ves una pantalla protegida.

**Fase 2 — Funding sources ("bolsillos")**
- *Con Claude Code:* listar fuentes con su saldo (`LazyColumn`), crear una nueva (nombre + saldo inicial → genera su primer `source_movement` de tipo `income`). Pantalla de detalle por fuente con su historial de movimientos (trazabilidad) y un botón "Agregar dinero" para ingresos manuales (monto + nota opcional, sin `reference_loan_id`/`reference_payment_id`). Archivos en `ui/sources/`.
- *Milestone:* creas "Presupuesto para préstamos" con saldo inicial, le agregas dinero manualmente después, y ves ambos movimientos en su historial.

**Fase 3 — Friends (amigos)**
- *Con Claude Code:* listar y crear amigos (nombre + teléfono opcional) en `ui/friends/`. Componente de "buscar o crear amigo" reutilizable (lo usarás en el formulario de préstamo).
- *Milestone:* CRUD de amigos funcionando.

**Fase 4 — Loans (préstamos)**
- *Con Claude Code:* formulario de préstamo nuevo en `ui/loans/`: amigo (buscar/crear), monto, fuente origen, fecha tentativa; interés opcional detrás de un "mostrar más". Al guardar: crear el `loan`, registrar el `source_movement` tipo `outflow` y descontar `current_balance`. Lista principal con badges de color por `status`. Pantalla de detalle con el resumen.
- *Milestone:* creas un préstamo y ves el saldo de la fuente reflejando el descuento.

**Fase 5 — Payments (pagos)**
- *Con Claude Code:* bottom sheet "Registrar pago" en `ui/payments/`: campos `principal_payment` e `interest_payment`, fuente destino para el capital (`principal_destination_source_id`) con un "mostrar más" opcional para elegir una fuente distinta para el interés (`interest_destination_source_id`, si es nulo se usa la misma del capital). Al guardar: crear el `payment` y registrar **dos** `source_movement` tipo `income` (uno por capital, otro por interés, aunque vayan al mismo bolsillo), recalcular `outstanding_principal`, actualizar `status` (`paid` si llega a 0, `partial` si queda saldo). Historial de pagos visible en el detalle del préstamo.
- *Milestone:* replicar el ejemplo de Camilo (2 pagos parciales) y que el préstamo quede en `paid` al final.

**Fase 6 — Notificación diaria local**
- *Con Claude Code:* `PeriodicWorkRequest` (24h, sin hora exacta) que consulta préstamos con `due_date = hoy`. Notificación local (agrupada si hay varios) con deep link a la pantalla del préstamo. Chequeo adicional al abrir la app, como respaldo.
- *Milestone:* recibes la notificación un día que programaste un préstamo para hoy.

**Fase 7 — Pulido**
- *Con Claude Code:* estados de carga/error, confirmaciones antes de eliminar, validaciones de formulario (montos positivos, fechas válidas), ícono y nombre de la app.
- *Milestone: versión 1.0, la app cubre el caso de uso completo descrito al inicio de este documento.*

**v2 (después de usarla un tiempo):** reportes por amigo/fuente/mes, interés moratorio configurable, exportar a CSV, recordatorios configurables, adjuntos/notas con foto, deudas propias (ver sección 9).

---

## 9. Extensión futura: deudas propias (ej. un préstamo bancario)

Caso: pides un préstamo al banco y usas ese dinero para prestárselo a tus amigos. Esto **no es un bolsillo** (un bolsillo es dinero que tienes; una deuda bancaria es dinero que debes) — se modela aparte y se conecta a un bolsillo cuando el banco te desembolsa.

A diferencia del interés informal con tus amigos (sección 3, manual y sin fórmula), un préstamo bancario sí usa un sistema estándar y predecible: **cuota fija (amortización francesa)**, donde la cuota no cambia pero la proporción capital/interés sí, mes a mes:

```
cuota = principal_amount × monthly_interest_rate / (1 − (1 + monthly_interest_rate)^-term_months)
```

Para evitar conversiones de tasa efectiva anual ↔ mensual (que varían según el banco), el usuario ingresa directamente la **tasa mensual** que ya viene en su pagaré/certificado del crédito.

**Tablas propuestas (no incluidas todavía en el script de la sección 7.1 — para cuando se implemente):**

```sql
create table debts (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null default auth.uid() references auth.users(id),
  name text not null,                       -- ej. "Préstamo banco X"
  principal_amount numeric not null,
  monthly_interest_rate numeric not null,   -- ej. 0.015 = 1.5% mensual
  term_months int not null,
  start_date date not null,
  funding_source_id uuid references funding_sources(id), -- a qué bolsillo entró el desembolso
  status text not null default 'active',    -- active | paid
  created_at timestamptz not null default now()
);

create table debt_installments (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null default auth.uid() references auth.users(id),
  debt_id uuid not null references debts(id),
  installment_number int not null,
  due_date date not null,
  payment_amount numeric not null,
  principal_component numeric not null,
  interest_component numeric not null,
  remaining_balance numeric not null,
  is_paid boolean not null default false,
  paid_date date,
  paid_from_source_id uuid references funding_sources(id), -- opcional: de qué bolsillo pagaste esta cuota
  created_at timestamptz not null default now()
);
```

Al crear una `debt`, se genera de una vez toda la tabla de amortización en `debt_installments` (uso de la fórmula de cuota fija), y el desembolso se registra como un `source_movement` manual de tipo `income` en el bolsillo elegido. La misma revisión diaria de WorkManager (Fase 6) podría extenderse para avisar también de cuotas de `debt_installments` que vencen hoy, junto con los cobros a tus amigos.

---

## 10. Prompts para Claude Code por fase

**Nota:** este documento debe vivir en el repo como `docs/spec.md`. En la raíz del repo hay además un `CLAUDE.md` (resumen corto que Claude Code carga automáticamente en cada sesión, con las convenciones y reglas de negocio que no deben cambiar) que apunta aquí para el detalle — mantén su sección "Estado actual del proyecto" actualizada a medida que cierras fases.

Prompts autocontenidos (Claude Code no tiene memoria de este chat) para pegar en su terminal dentro de Android Studio. Se agregan aquí a medida que avanzamos de fase.

### Fase 0 — Setup (dependencias, estructura de paquetes, cliente de Supabase)

Requisitos previos ya cumplidos antes de usar este prompt: proyecto Supabase creado, script SQL de la sección 7.1 ejecutado, `local.properties` con `SUPABASE_URL`/`SUPABASE_ANON_KEY`, proyecto Android en blanco (Compose) creado.

```
Contexto: estoy construyendo una app Android nativa (Kotlin + Jetpack Compose) para administrar
préstamos informales a mis amigos, con Supabase como backend (Postgres + Auth + RLS). Todo el
código (nombres de clases, propiedades, carpetas y archivos) debe estar en inglés.

Ya tengo:
- Un proyecto Android en blanco creado con la plantilla de Compose.
- Un proyecto de Supabase con este esquema ya ejecutado (5 tablas, RLS activado, política
  "solo mi propio usuario" en todas):
    friends: id, user_id, name, phone, notes, created_at
    funding_sources: id, user_id, name, current_balance, created_at
    source_movements: id, user_id, source_id, movement_type (income/outflow), amount, notes,
      reference_loan_id, reference_payment_id, created_at
    loans: id, user_id, friend_id, source_id, principal_amount, interest_type (none/fixed),
      interest_value, loan_date, due_date, outstanding_principal,
      status (active/partial/paid/overdue), notes, created_at
    payments: id, user_id, loan_id, payment_date, principal_payment, interest_payment,
      principal_destination_source_id, interest_destination_source_id, created_at
- El archivo local.properties ya tiene SUPABASE_URL y SUPABASE_ANON_KEY.

Necesito que termines la Fase 0 (setup) de este proyecto ya existente. Por favor:

1. Revisa primero la configuración actual del proyecto (build.gradle.kts, local.properties,
   .gitignore) antes de modificar nada.
2. Confirma que local.properties está en .gitignore (nunca debe subirse a git). En
   app/build.gradle.kts, agrega buildFeatures { buildConfig = true } y expón SUPABASE_URL y
   SUPABASE_ANON_KEY de local.properties como campos de BuildConfig, si no está ya hecho.
3. Agrega estas dependencias (usa las versiones estables más recientes que sean compatibles
   entre sí y con la versión de Compose/Kotlin del proyecto — verifica compatibilidad antes de
   fijarlas, no asumas versiones de memoria):
   - Navigation Compose
   - Lifecycle ViewModel Compose
   - Kotlinx Coroutines (core + android)
   - Kotlinx Serialization (plugin + librería, necesaria para los modelos de supabase-kt)
   - Supabase Kotlin SDK (io.github.jan-tennert.supabase): módulos postgrest-kt y auth-kt
   - Un engine de Ktor para el cliente HTTP de Supabase (ej. ktor-client-okhttp)
   - androidx.work:work-runtime-ktx (WorkManager; se usará en una fase posterior)
4. Crea esta estructura de paquetes (todo en inglés):
   com.<tupaquete>.loanstracker
   ├── data
   │   ├── model        -> data classes de las tablas de Supabase
   │   ├── remote        -> SupabaseClientProvider.kt (cliente único de Supabase)
   │   └── repository    -> vacío por ahora, se llena en fases siguientes
   ├── ui
   │   ├── auth
   │   ├── sources
   │   ├── friends
   │   ├── loans
   │   └── payments
   └── MainActivity.kt
5. En data/model, crea data classes en Kotlin (camelCase) que representen exactamente las 5
   tablas de arriba, usando @Serializable y @SerialName para mapear cada columna snake_case de
   Postgres a su propiedad camelCase en Kotlin.
6. En data/remote/SupabaseClientProvider.kt, crea un singleton (object) que construya un único
   SupabaseClient usando BuildConfig.SUPABASE_URL y BuildConfig.SUPABASE_ANON_KEY, instalando los
   plugins Postgrest y Auth.
7. Para comprobar que quedó bien conectado (sin construir aún pantallas de negocio): modifica
   temporalmente el Compose de MainActivity para que, al abrir la app, haga una consulta simple
   (ej. contar filas de friends) usando el SupabaseClientProvider dentro de una coroutine
   (LaunchedEffect, sin bloquear el hilo principal), y muestre en pantalla "Conectado a Supabase"
   o el error si falla.
8. Compila y corre la app en el emulador o dispositivo, y corrige cualquier error que aparezca.
9. No implementes todavía login ni pantallas de negocio (amigos, préstamos, pagos, bolsillos) —
   eso es de la Fase 1 en adelante. Esta tarea es solo dejar el proyecto compilando, con la
   estructura de paquetes y el cliente de Supabase funcionando.

Al terminar, dime qué versiones de cada dependencia usaste y si ajustaste algo por
incompatibilidades.
```

*Cómo saber que la Fase 0 quedó lista:* la app compila y corre en el emulador/dispositivo, y la pantalla muestra "Conectado a Supabase" (no un error) al abrir.

Sugerencia de flujo en GitHub: una rama por fase (`feature/login`, `feature/bolsillos`, etc.), un issue o milestone por fase de esta lista, y un tag `v1.0` al cerrar la Fase 7.
