package com.hoang.calculator

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardTab
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun KeypadSheet(
    label: String,
    initialValue: String,
    state: UiState,
    allowNegative: Boolean = false,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    onNext: (() -> Unit)? = null
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        var sheetValue by remember(initialValue) { mutableStateOf(initialValue) }
        var sheetCursor by remember(initialValue) { mutableStateOf(initialValue.length) }
        Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            val sheetFull = if (label == "Return Rate") Calculator.formatReturnRateDigits(sheetValue.filter(Char::isDigit)) else Calculator.formatCompact(Calculator.parseNumber(sheetValue), "Never", state.settings.decimalPrecision)
            val rawPrefix = sheetValue.take(sheetCursor.coerceIn(0, sheetValue.length))
            val cursorAt = if (label == "Return Rate") {
                val digitsBefore = rawPrefix.count(Char::isDigit)
                var seen = 0
                if (digitsBefore == 0) 0 else sheetFull.indexOfLast { ch ->
                    if (ch.isDigit()) { seen++; seen <= digitsBefore } else false
                }.let { if (it >= 0) it + 1 else sheetFull.length }
            } else {
                val decimalIndex = rawPrefix.indexOf('.')
                if (decimalIndex >= 0 && sheetFull.contains('.')) {
                    val decimalOffset = rawPrefix.length - decimalIndex
                    val fullDecimal = sheetFull.indexOf('.')
                    (fullDecimal + decimalOffset).coerceIn(0, sheetFull.length)
                } else {
                    val digitsBefore = rawPrefix.count(Char::isDigit)
                    var seen = 0
                    if (digitsBefore == 0) 0 else sheetFull.indexOfLast { ch ->
                        if (ch.isDigit()) { seen++; seen <= digitsBefore } else false
                    }.let { if (it >= 0) it + 1 else sheetFull.length }
                }
            }.coerceIn(0, sheetFull.length)
            val cursorPhase by rememberInfiniteTransition(label = "keypadCursor").animateFloat(
                initialValue = 0f, targetValue = 1f,
                animationSpec = infiniteRepeatable(tween((650 / state.settings.animationSpeed).toInt(), easing = LinearEasing), RepeatMode.Restart),
                label = "cursorBlink"
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(sheetFull.take(cursorAt), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("|", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = if (cursorPhase < 0.5f) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0f))
                Text(sheetFull.drop(cursorAt), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }
            IconButton(onClick = {
                onSave(sheetValue)
                if (onNext == null) onDismiss() else onNext()
            }, modifier = Modifier.size(48.dp)) { Icon(Icons.Default.ArrowForward, "Next field") }
            val viewForHaptic = LocalView.current
            fun key(action: String) {
                if (state.settings.hapticsEnabled) viewForHaptic.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                sheetValue = when (action) {
                    "C" -> { sheetCursor = 0; "" }
                    "Next" -> { sheetCursor = sheetValue.length; sheetValue }
                    "Back" -> {
                        val index = sheetCursor.coerceIn(0, sheetValue.length)
                        if (index > 0) { sheetValue = sheetValue.removeRange(index - 1, index); sheetCursor = index - 1 }
                        sheetValue
                    }
                    "+/-" -> if (allowNegative) if (sheetValue.startsWith("-")) sheetValue.drop(1) else "-$sheetValue" else sheetValue
                    "<" -> { sheetCursor = (sheetCursor - 1).coerceAtLeast(0); sheetValue }
                    ">" -> { sheetCursor = (sheetCursor + 1).coerceAtMost(sheetValue.length); sheetValue }
                    else -> {
                        val index = sheetCursor.coerceIn(0, sheetValue.length)
                        sheetValue = sheetValue.substring(0, index) + action + sheetValue.substring(index)
                        sheetCursor = index + action.length
                        sheetValue
                    }
                }.filterIndexed { index, char -> char.isDigit() || (char == '.' && !sheetValue.take(index).contains('.')) || (char == '-' && index == 0 && allowNegative) }
            }
            val keys = listOf("1","2","3","C","4","5","6","Next","7","8","9","Back","<","0",">","OK")
            if (state.settings.keypadSymbolChips) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
                    listOf(".", "00", if (allowNegative) "+/-" else "").filter { it.isNotEmpty() }.forEach { chip ->
                        FilterChip(selected = false, onClick = { key(if (chip == "00") "00" else chip) }, label = { Text(chip) })
                    }
                }
            }
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                keys.chunked(4).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        row.forEach { keyName ->
                            Box(
                                modifier = Modifier.weight(1f).height(48.dp).clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .combinedClickable(
                                        onClick = {
                                            if (keyName == "OK") {
                                                onSave(sheetValue)
                                                onDismiss()
                                            } else key(keyName)
                                        },
                                        onLongClick = if (keyName == "Back") ({ sheetValue = ""; sheetCursor = 0 }) else null
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                when (keyName) {
                                    "Back" -> Icon(Icons.Default.Backspace, "Backspace")
                                    "Next" -> Icon(Icons.Default.KeyboardTab, "Move cursor to end")
                                    "<" -> Icon(Icons.Default.KeyboardArrowLeft, "Move cursor left")
                                    ">" -> Icon(Icons.Default.KeyboardArrowRight, "Move cursor right")
                                    "OK" -> Icon(Icons.Default.Check, "OK")
                                    "C" -> Icon(Icons.Default.DeleteSweep, "Clear All")
                                    else -> Text(keyName, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
            if (state.settings.keypadQuickAdjustChips) Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("+1" to 1.0, "+10" to 10.0, "+100" to 100.0, "+1K" to 1000.0, "+10K" to 10000.0).forEach { (chipLabel, delta) ->
                        FilterChip(selected = false, onClick = {
                            sheetValue = if (label == "Return Rate") {
                                val current = Calculator.parseReturnRateDigits(sheetValue.filter(Char::isDigit))
                                val adjusted = (current + delta).coerceIn(10.0, 99.99999999)
                                BigDecimal.valueOf(adjusted).stripTrailingZeros().toPlainString().replace(".", "")
                            } else {
                                val current = Calculator.parseNumber(sheetValue) ?: 0.0
                                BigDecimal.valueOf(current + delta).stripTrailingZeros().toPlainString()
                            }
                            sheetCursor = sheetValue.length
                        }, label = { Text(chipLabel) }, modifier = Modifier.weight(1f))
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("-1" to -1.0, "-10" to -10.0, "-100" to -100.0, "-1K" to -1000.0, "-10K" to -10000.0).forEach { (chipLabel, delta) ->
                        FilterChip(selected = false, onClick = {
                            sheetValue = if (label == "Return Rate") {
                                val current = Calculator.parseReturnRateDigits(sheetValue.filter(Char::isDigit))
                                val adjusted = (current + delta).coerceIn(10.0, 99.99999999)
                                BigDecimal.valueOf(adjusted).stripTrailingZeros().toPlainString().replace(".", "")
                            } else {
                                val current = Calculator.parseNumber(sheetValue) ?: 0.0
                                BigDecimal.valueOf(current + delta).stripTrailingZeros().toPlainString()
                            }
                            sheetCursor = sheetValue.length
                        }, label = { Text(chipLabel) }, modifier = Modifier.weight(1f))
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) { Icon(Icons.Default.Close, "Cancel") }
                IconButton(onClick = {
                    onSave(sheetValue)
                    onDismiss()
                }, modifier = Modifier.size(48.dp)) { Icon(Icons.Default.Check, "OK") }
            }
        }
    }
}
