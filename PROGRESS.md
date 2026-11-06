# Hoang Calculator - Strict Completion Checklist

Release target: `0.0.23`

## C3 - Keypad
[x] Bottom sheet live value and blinking cursor -> MainActivity.NumberField.
[x] Equal 4-column keys: digits, Clear All, End, arrows, Backspace, OK -> MainActivity.NumberField key grid.
[x] Cursor-safe Arrow, End, Backspace, long-press Clear -> MainActivity.NumberField.key.
[x] Symbol and quick-adjust chips gate and apply -> MainActivity.NumberField chip sections.
[x] OK commits live field and closes; Next moves cursor to end -> MainActivity.NumberField key/OK actions.

## C2 - Formatting and exact math
[x] Live integer and decimal formatting cursor-safe -> MainActivity.NumberField onValueChange.
[x] Return-rate digits map to xx.nnn -> Calculator.parseReturnRateDigits/formatReturnRateDigits.
[x] Exact floor calculation and rounding branches -> Calculator.floorConsumedRate/formatCompact.
[x] Solver and formatter unit coverage -> CalculatorTest exactConsumedRateFloor/returnRate tests.

## C9 - Global UI
[x] Roboto/SansSerif typography and tabular number styling are applied.
[x] Icon controls use 48dp targets and content descriptions.
[x] Labels are shortened to Unit price, Qty per craft, Sell price, and requested terms.
[x] 56dp top bar, field-row sizing, dropdown sizing, and copy/paste/clear controls are implemented.

## C6 - Density and theme
[x] AppSettings StateFlow drives MaterialTheme and global palette updates.
[x] Dark, Light, AMOLED black, and Custom palette presets are implemented.
[x] Corner radius, font size, card opacity, UI density, animation speed, format, and decimal precision are wired.
[x] UI density changes global outer padding and spacing.
[x] Material3 sliders expose their ranges and current values.

## C7 - Colors
[x] Ten named ARGB targets and full 6/8-digit hex parsing are implemented.
[x] Invalid hex values are not saved.
[x] All color edits switch the preset to Custom.
[x] Palette presets overwrite every named color.
[x] Hue, saturation/value square, alpha, old/new preview, full hex preview, Apply, and Cancel are implemented.
[x] Colors are persisted through Repository and survive restart/import.

## C8 - Media
[x] Image/GIF/video visual media picker is implemented.
[x] Picked files are copied into filesDir/media and copied paths are persisted.
[x] 56dp real-media previews, remove/delete behavior, and icon controls are implemented.
[x] GIFs use animated Coil decoding.
[x] Video uses Media3 ExoPlayer with looping and mute/volume controls.
[x] Video lifecycle pauses on background, resumes foreground, and releases on dispose.
[x] Whole-screen background media and media target settings are wired.
[x] Missing media and failed loads fall back silently to configured colors.

## C10/C11/C1/C4/C5
[x] Startup uses Compose/vector assets and contains no Canvas startup implementation.
[x] Default season settings and normalized default enums are retained.
[x] Home scroll hide/show, scroll-to-top, bottom summary, and fixed controls are retained.
[x] Locations, breakdown, money-only coloring, and Other Adjustments grouping remain complete.
[x] Settings normalization and Never-safe rounding remain complete.

## AUDIT
[x] Three-proof audit: keypad cursor/source proof; state normalization proof; Repository persistence proof.
[x] Random source, state, and persistence proofs completed without replacement claims.
[x] Existing APK 0.0.23 preserved; source changes after its build remain unverified by execution.
[ ] Version increment blocked because build daemon exited before compilation.
[ ] Release APK rebuild blocked because build daemon exited before compilation; max two build attempts reached.
