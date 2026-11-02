package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.WeatherFormatter
import com.example.data.model.WidgetPalette
import com.example.data.model.WidgetStyle
import kotlin.math.abs

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WidgetOpacityAndDateCard(
    backgroundAlpha: Float,
    showBorder: Boolean,
    showDate: Boolean,
    useEnglishDate: Boolean,
    showCityOnWidget: Boolean,
    onChangeOpacity: (Float) -> Unit,
    onSelectPreset: (Float, Boolean) -> Unit,
    onToggleShowBorder: (Boolean) -> Unit,
    onToggleShowDate: (Boolean) -> Unit,
    onToggleEnglishDate: (Boolean) -> Unit,
    onToggleShowCityOnWidget: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val clamped = backgroundAlpha.coerceIn(0f, 1f)
    val isSemiPreset = abs(clamped - 0.68f) < 0.03f && !showBorder
    val isTransparentBorderPreset = clamped <= 0.01f && showBorder
    val isTransparentBorderlessPreset = clamped <= 0.01f && !showBorder
    val isSolidPreset = clamped >= 0.99f && !showBorder

    Card(
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.50f)
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.widget_opacity_section),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = WeatherFormatter.formatOpacityShortTag(clamped, showBorder),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            // 4 Quick Opacity Presets matching the screenshot + borderless option
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = isSemiPreset,
                    onClick = { onSelectPreset(0.68f, false) },
                    label = { Text("半透明 (68%)") },
                    leadingIcon = if (isSemiPreset) {
                        { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    } else null,
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("opacity_preset_semi")
                )
                FilterChip(
                    selected = isTransparentBorderPreset,
                    onClick = { onSelectPreset(0.0f, true) },
                    label = { Text("透明描边 (0%)") },
                    leadingIcon = if (isTransparentBorderPreset) {
                        { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    } else null,
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("opacity_preset_transparent_border")
                )
                FilterChip(
                    selected = isTransparentBorderlessPreset,
                    onClick = { onSelectPreset(0.0f, false) },
                    label = { Text("全透无框 (0%)") },
                    leadingIcon = if (isTransparentBorderlessPreset) {
                        { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    } else null,
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("opacity_preset_transparent_borderless")
                )
                FilterChip(
                    selected = isSolidPreset,
                    onClick = { onSelectPreset(1.0f, false) },
                    label = { Text("纯色实心 (100%)") },
                    leadingIcon = if (isSolidPreset) {
                        { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    } else null,
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("opacity_preset_solid")
                )
            }

            // Fine-grained 0%..100% Opacity Slider
            Column {
                Slider(
                    value = clamped,
                    onValueChange = onChangeOpacity,
                    valueRange = 0f..1f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("widget_opacity_slider")
                )
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "0% 全透明",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "自由拖动微调透明度",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "100% 纯色实心",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            SettingToggleRow(
                label = stringResource(R.string.widget_show_border_label),
                checked = showBorder,
                onCheckedChange = onToggleShowBorder,
                switchTag = "widget_border_switch"
            )

            SettingToggleRow(
                label = stringResource(R.string.widget_show_date_label),
                checked = showDate,
                onCheckedChange = onToggleShowDate,
                switchTag = "widget_show_date_switch"
            )

            if (showDate) {
                SettingToggleRow(
                    label = stringResource(R.string.widget_english_date_label),
                    checked = useEnglishDate,
                    onCheckedChange = onToggleEnglishDate,
                    switchTag = "widget_english_date_switch"
                )
            }

            SettingToggleRow(
                label = stringResource(R.string.widget_show_city_label),
                checked = showCityOnWidget,
                onCheckedChange = onToggleShowCityOnWidget,
                switchTag = "widget_show_city_switch"
            )
        }
    }
}

@Composable
private fun SettingToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    switchTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(switchTag)
        )
    }
}

@Composable
fun WidgetStyleAndPaletteCard(
    selectedStyle: WidgetStyle,
    selectedPalette: WidgetPalette,
    previewTallForecast: Boolean = false,
    showFahrenheitToggle: Boolean = false,
    useFahrenheit: Boolean = false,
    onSelectStyle: (WidgetStyle) -> Unit,
    onSelectPalette: (WidgetPalette) -> Unit,
    onToggleTallPreview: (() -> Unit)? = null,
    onToggleFahrenheit: ((Boolean) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.50f)
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = stringResource(R.string.widget_style_section),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            // 2×2 Grid of At a Glance Widget Layouts
            WidgetStyle.entries.chunked(2).forEach { rowStyles ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowStyles.forEach { style ->
                        val selected = selectedStyle == style
                        Surface(
                            onClick = { onSelectStyle(style) },
                            shape = RoundedCornerShape(18.dp),
                            color = if (selected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surface
                            },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 52.dp)
                                .testTag("style_option_${style.key.lowercase()}")
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp)
                            ) {
                                Text(
                                    text = style.label,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (selected) {
                                        MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    },
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            if (selectedStyle == WidgetStyle.FORECAST_CARD && onToggleTallPreview != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.UnfoldMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "预览高尺寸展开态 (4×3 多日预报)",
                            style = MaterialTheme.typography.labelLarge
                        )
                        Text(
                            text = "在桌面拉高微件尺寸时将自动展开未来多日趋势",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = previewTallForecast,
                        onCheckedChange = { onToggleTallPreview() }
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            Text(
                text = stringResource(R.string.widget_palette_section),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                WidgetPalette.entries.forEach { palette ->
                    val selected = selectedPalette == palette
                    val swatch = resolveComposeWidgetColors(palette)
                    Surface(
                        onClick = { onSelectPalette(palette) },
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 68.dp)
                            .border(
                                width = if (selected) 2.5.dp else 1.dp,
                                color = if (selected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.outlineVariant
                                },
                                shape = RoundedCornerShape(18.dp)
                            )
                            .testTag("palette_option_${palette.key.lowercase()}")
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(swatch.bg)
                                    .border(1.5.dp, swatch.badgeBg, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                if (selected) {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = null,
                                        tint = swatch.textPrimary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = palette.label,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }

            if (showFahrenheitToggle && onToggleFahrenheit != null) {
                SettingToggleRow(
                    label = "使用华氏度 (°F)",
                    checked = useFahrenheit,
                    onCheckedChange = onToggleFahrenheit,
                    switchTag = "widget_fahrenheit_switch"
                )
            }
        }
    }
}
