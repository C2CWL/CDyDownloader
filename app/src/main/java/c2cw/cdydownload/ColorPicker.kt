package c2cw.cdydownload

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

/** Preset swatches plus hue / saturation / brightness bars. Changes apply live. */
@OptIn(ExperimentalLayoutApi::class)
@Composable internal fun CustomColorPicker(color: Int, onChange: (Int) -> Unit) {
    val initial = remember { rgbToHsv(color) }
    var hue by remember { mutableFloatStateOf(initial.first) }
    var sat by remember { mutableFloatStateOf(initial.second) }
    var value by remember { mutableFloatStateOf(initial.third.coerceAtLeast(0.25f)) }
    fun commit() = onChange(hsvToRgb(hue, sat, value))
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            PRESET_ACCENTS.forEach { preset ->
                val chosen = (preset and 0xFFFFFF) == (color and 0xFFFFFF)
                Box(Modifier.size(34.dp).clip(CircleShape).background(Color(preset))
                    .border(if(chosen) 3.dp else 0.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                    .pressable(scale = 0.9f) {
                        val (h, s, v) = rgbToHsv(preset); hue = h; sat = s; value = v; onChange(preset)
                    }.testTag("accentSwatch-${hexOf(preset)}"), contentAlignment = Alignment.Center) {
                    if(chosen) Glyph("check", modifier = Modifier.size(18.dp), tint = Color.White)
                }
            }
        }
        LabeledBar("色相", hue / 360f, Brush.horizontalGradient((0..6).map { Color(hsvToRgb(it * 60f, 1f, 1f)) }), "accentHue") {
            hue = it * 360f; commit()
        }
        LabeledBar("饱和度", sat, Brush.horizontalGradient(listOf(Color(hsvToRgb(hue, 0f, value)), Color(hsvToRgb(hue, 1f, value)))), "accentSat") {
            sat = it; commit()
        }
        LabeledBar("亮度", (value - 0.25f) / 0.75f, Brush.horizontalGradient(listOf(Color(hsvToRgb(hue, sat, 0.25f)), Color(hsvToRgb(hue, sat, 1f)))), "accentValue") {
            value = 0.25f + it * 0.75f; commit()
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(22.dp).clip(RoundedCornerShape(6.dp)).background(Color(color)))
            Text(hexOf(color), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("深色和浅色模式会自动调整明暗，保证文字清晰", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable private fun LabeledBar(label: String, fraction: Float, brush: Brush, tag: String, onChange: (Float) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        GradientBar(fraction, brush, Modifier.testTag(tag), onChange)
    }
}

@Composable private fun GradientBar(fraction: Float, brush: Brush, modifier: Modifier, onChange: (Float) -> Unit) {
    val update by rememberUpdatedState(onChange)
    Canvas(modifier.fillMaxWidth().height(28.dp)
        .pointerInput(Unit) { detectTapGestures { update((it.x / size.width).coerceIn(0f, 1f)) } }
        .pointerInput(Unit) { detectHorizontalDragGestures { change, _ ->
            change.consume(); update((change.position.x / size.width).coerceIn(0f, 1f))
        } }) {
        val track = size.height * 0.5f
        val top = (size.height - track) / 2
        drawRoundRect(brush, topLeft = Offset(0f, top), size = androidx.compose.ui.geometry.Size(size.width, track),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(track / 2))
        val x = fraction.coerceIn(0f, 1f) * size.width
        val r = size.height / 2 - 1f
        drawCircle(Color.Black.copy(alpha = 0.25f), r + 1.5f, Offset(x.coerceIn(r, size.width - r), size.height / 2))
        drawCircle(Color.White, r, Offset(x.coerceIn(r, size.width - r), size.height / 2))
    }
}
