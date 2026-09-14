package com.didiprogrammer.youtepresta.ui.theme

import androidx.compose.ui.graphics.Color

// Material 3 tonal palette derived from seed #0061A4 (a confident, trustworthy blue — the kind
// used by banking/finance apps), following the standard M3 tone-role mapping (primary=tone 40
// light / tone 80 dark, onPrimary=100/20, primaryContainer=90/30, etc.) rather than a
// hand-picked one-off color per role, so light and dark stay visually related.

// Light scheme
val md_theme_light_primary = Color(0xFF0061A4)
val md_theme_light_onPrimary = Color(0xFFFFFFFF)
val md_theme_light_primaryContainer = Color(0xFFD1E4FF)
val md_theme_light_onPrimaryContainer = Color(0xFF001D36)
val md_theme_light_secondary = Color(0xFF535F70)
val md_theme_light_onSecondary = Color(0xFFFFFFFF)
val md_theme_light_secondaryContainer = Color(0xFFD7E3F7)
val md_theme_light_onSecondaryContainer = Color(0xFF101C2B)
val md_theme_light_tertiary = Color(0xFF6B5778)
val md_theme_light_onTertiary = Color(0xFFFFFFFF)
val md_theme_light_tertiaryContainer = Color(0xFFF2DAFF)
val md_theme_light_onTertiaryContainer = Color(0xFF251431)
val md_theme_light_error = Color(0xFFBA1A1A)
val md_theme_light_onError = Color(0xFFFFFFFF)
val md_theme_light_errorContainer = Color(0xFFFFDAD6)
val md_theme_light_onErrorContainer = Color(0xFF410002)
val md_theme_light_background = Color(0xFFFDFCFF)
val md_theme_light_onBackground = Color(0xFF1A1C1E)
val md_theme_light_surface = Color(0xFFFDFCFF)
val md_theme_light_onSurface = Color(0xFF1A1C1E)
val md_theme_light_surfaceVariant = Color(0xFFDFE2EB)
val md_theme_light_onSurfaceVariant = Color(0xFF43474E)
val md_theme_light_outline = Color(0xFF73777F)
val md_theme_light_outlineVariant = Color(0xFFC3C7CF)
val md_theme_light_scrim = Color(0xFF000000)
val md_theme_light_inverseSurface = Color(0xFF2F3033)
val md_theme_light_inverseOnSurface = Color(0xFFF1F0F4)
val md_theme_light_inversePrimary = Color(0xFF9ECAFF)

// Dark scheme
val md_theme_dark_primary = Color(0xFF9ECAFF)
val md_theme_dark_onPrimary = Color(0xFF003258)
val md_theme_dark_primaryContainer = Color(0xFF00497D)
val md_theme_dark_onPrimaryContainer = Color(0xFFD1E4FF)
val md_theme_dark_secondary = Color(0xFFBBC7DB)
val md_theme_dark_onSecondary = Color(0xFF253140)
val md_theme_dark_secondaryContainer = Color(0xFF3B4858)
val md_theme_dark_onSecondaryContainer = Color(0xFFD7E3F7)
val md_theme_dark_tertiary = Color(0xFFD6BEE4)
val md_theme_dark_onTertiary = Color(0xFF3B2948)
val md_theme_dark_tertiaryContainer = Color(0xFF523F5F)
val md_theme_dark_onTertiaryContainer = Color(0xFFF2DAFF)
val md_theme_dark_error = Color(0xFFFFB4AB)
val md_theme_dark_onError = Color(0xFF690005)
val md_theme_dark_errorContainer = Color(0xFF93000A)
val md_theme_dark_onErrorContainer = Color(0xFFFFDAD6)
val md_theme_dark_background = Color(0xFF1A1C1E)
val md_theme_dark_onBackground = Color(0xFFE2E2E6)
val md_theme_dark_surface = Color(0xFF1A1C1E)
val md_theme_dark_onSurface = Color(0xFFE2E2E6)
val md_theme_dark_surfaceVariant = Color(0xFF43474E)
val md_theme_dark_onSurfaceVariant = Color(0xFFC3C7CF)
val md_theme_dark_outline = Color(0xFF8D9199)
val md_theme_dark_outlineVariant = Color(0xFF43474E)
val md_theme_dark_scrim = Color(0xFF000000)
val md_theme_dark_inverseSurface = Color(0xFFE2E2E6)
val md_theme_dark_inverseOnSurface = Color(0xFF2F3033)
val md_theme_dark_inversePrimary = Color(0xFF0061A4)

// Semantic status colors — Material 3 has no built-in "success"/"warning" role, only
// primary/secondary/tertiary/error, so these two are custom. "Atrasado" (overdue) reuses the
// scheme's own error/errorContainer instead of a bespoke color, and "Pagado" (paid) reuses
// onSurfaceVariant/surfaceVariant, since both are already neutral/danger roles that fit.
val md_theme_light_success = Color(0xFF1E7B34)
val md_theme_light_onSuccess = Color(0xFFFFFFFF)
val md_theme_light_successContainer = Color(0xFFD7F2DC)
val md_theme_light_onSuccessContainer = Color(0xFF0A3915)

val md_theme_dark_success = Color(0xFF8FDD9B)
val md_theme_dark_onSuccess = Color(0xFF00390D)
val md_theme_dark_successContainer = Color(0xFF1F4A29)
val md_theme_dark_onSuccessContainer = Color(0xFFD7F2DC)

val md_theme_light_warning = Color(0xFF825500)
val md_theme_light_onWarning = Color(0xFFFFFFFF)
val md_theme_light_warningContainer = Color(0xFFFFE9B3)
val md_theme_light_onWarningContainer = Color(0xFF2A1800)

val md_theme_dark_warning = Color(0xFFFFCB66)
val md_theme_dark_onWarning = Color(0xFF452B00)
val md_theme_dark_warningContainer = Color(0xFF4A3300)
val md_theme_dark_onWarningContainer = Color(0xFFFFE9B3)
