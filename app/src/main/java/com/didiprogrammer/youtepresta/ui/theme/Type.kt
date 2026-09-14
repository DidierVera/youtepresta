package com.didiprogrammer.youtepresta.ui.theme

import androidx.compose.material3.Typography

/**
 * The app's Material 3 type scale. The default role sizes/weights are already a coherent,
 * accessible scale, so this just documents which role each screen should use consistently
 * instead of picking one-off styles:
 *
 * - Título de pantalla (screen header, e.g. a funding source's name, a loan's counterpart name):
 *   [Typography.headlineSmall], bold.
 * - Título de ítem de lista (a row's primary label — a friend's name, a loan's counterpart, a
 *   funding source's name): [Typography.titleMedium], bold. The "monto" on the same row uses the
 *   same role/weight so both sides of the row read as one table.
 * - Texto secundario (phone numbers, notes, due dates): [Typography.bodyMedium] or
 *   [Typography.bodySmall] for finer meta (timestamps), always in `onSurfaceVariant`.
 * - Texto de botón: the M3 default ([Typography.labelLarge]) — not overridden.
 */
val Typography = Typography()
