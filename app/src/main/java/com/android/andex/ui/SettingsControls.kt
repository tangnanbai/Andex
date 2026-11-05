package com.android.andex.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.android.andex.BuildConfig
import com.android.andex.R
import com.android.andex.data.AndexPrivacyPolicyUrl
import com.android.andex.data.AndexWebsiteUrl
import com.android.andex.data.AgentModeAuthorizationIssue
import com.android.andex.data.AgentModeAuthorizationMethod
import com.android.andex.data.AgentModeAuthorizationState
import com.android.andex.data.AgentModeDisplayState
import com.android.andex.data.AutomaticModelPurpose
import com.android.andex.data.AppLanguage
import com.android.andex.data.AppThemeMode
import com.android.andex.data.LlmProviderConfig
import com.android.andex.data.ProviderModelOption
import com.android.andex.data.RootSetupIssue
import com.android.andex.data.RootSetupState
import com.android.andex.data.availableModelOptions
import com.android.andex.data.availableModels
import com.android.andex.data.enabledModels
import com.android.andex.data.findModelOption
import com.android.andex.data.normalizeLlmInactivityReconnectTimeoutSeconds
import com.android.andex.data.quickActionLabel
import com.android.andex.data.resolveAutomaticModelKey
import com.android.andex.termux.TermuxSetupState
import com.android.andex.ui.theme.AndexOnSurface
import com.android.andex.ui.theme.AndexOnPrimary
import com.android.andex.ui.theme.AndexOnSurfaceVariant
import com.android.andex.ui.theme.AndexPrimary
import com.android.andex.ui.theme.AndexScrim
import com.android.andex.ui.theme.AndexSettingsBackground
import com.android.andex.ui.theme.AndexSettingsIcon
import com.android.andex.ui.theme.AndexSurface
import com.android.andex.ui.theme.AndexSurfaceHigh
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SettingsCardGroup(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .andexSettingsRule(),
    ) {
        content()
    }
}

@Composable
fun CardDivider() {
    androidx.compose.material3.HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        thickness = 0.5.dp,
        color = com.android.andex.ui.theme.AndexOutlineSoft,
    )
}

@Composable
fun SettingsNavRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    showChevron: Boolean = true,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    SettingsNavRowContent(
        icon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = AndexSettingsIcon,
                modifier = Modifier.size(24.dp),
            )
        },
        title = title,
        subtitle = subtitle,
        showChevron = showChevron,
        enabled = enabled,
        onClick = onClick,
    )
}

@Composable
fun SettingsNavRow(
    iconPainter: Painter,
    title: String,
    subtitle: String,
    showChevron: Boolean = true,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    SettingsNavRowContent(
        icon = {
            Icon(
                painter = iconPainter,
                contentDescription = null,
                tint = AndexSettingsIcon,
                modifier = Modifier.size(24.dp),
            )
        },
        title = title,
        subtitle = subtitle,
        showChevron = showChevron,
        enabled = enabled,
        onClick = onClick,
    )
}

@Composable
private fun SettingsNavRowContent(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String,
    showChevron: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val contentAlpha = if (enabled) 1f else 0.38f
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.alpha(contentAlpha)) { icon() }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = AndexOnSurface.copy(alpha = contentAlpha),
            )
            // An empty Text still takes a full line, which pushes a title-only row's title above the
            // vertically centered icon and chevron.
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = AndexOnSurfaceVariant.copy(alpha = contentAlpha),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (showChevron) {
            Spacer(Modifier.width(8.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos,
                contentDescription = null,
                tint = AndexOnSurfaceVariant.copy(alpha = if (enabled) 0.5f else 0.2f),
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

// -----------------------------------------------------------------------------
// Page enum - drives the local in-composable navigation
// -----------------------------------------------------------------------------

internal data class SelectionOption(
    val key: String,
    val title: String,
    val subtitle: String,
    val selected: Boolean,
    val onClick: () -> Unit,
)
@OptIn(ExperimentalFoundationApi::class)
internal fun Modifier.settingsBringIntoViewOnFocus(): Modifier = composed {
    val requester = remember { BringIntoViewRequester() }
    val scope = rememberCoroutineScope()

    bringIntoViewRequester(requester)
        .onFocusChanged { focusState ->
            if (focusState.isFocused) {
                scope.launch {
                    delay(250)
                    requester.bringIntoView()
                }
            }
        }
}

// ChatGPT-style inline text field (inside a card)

@Composable
internal fun ChatGptTextField(
    label: String,
    value: TextFieldValue,
    minLines: Int = 1,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    isSecret: Boolean = false,
    placeholder: String = label,
    supportingText: String = "",
    onValueChange: (TextFieldValue) -> Unit,
) {
    var passwordVisible by rememberSaveable(label) { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = AndexOnSurfaceVariant,
        )
        Spacer(Modifier.height(6.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .settingsBringIntoViewOnFocus(),
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = AndexOnSurface),
            cursorBrush = SolidColor(AndexPrimary),
            minLines = minLines,
            keyboardOptions = keyboardOptions,
            visualTransformation = if (isSecret && !passwordVisible) {
                PasswordVisualTransformation()
            } else {
                VisualTransformation.None
            },
            decorationBox = { innerTextField ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        if (value.text.isEmpty()) {
                            Text(
                                text = placeholder,
                                style = MaterialTheme.typography.bodyLarge,
                                color = AndexOnSurfaceVariant.copy(alpha = 0.5f),
                            )
                        }
                        innerTextField()
                    }
                    if (isSecret) {
                        IconButton(
                            onClick = { passwordVisible = !passwordVisible },
                            modifier = Modifier.size(40.dp),
                        ) {
                            Icon(
                                imageVector = if (passwordVisible) {
                                    Icons.Rounded.VisibilityOff
                                } else {
                                    Icons.Rounded.Visibility
                                },
                                contentDescription = stringResource(
                                    if (passwordVisible) {
                                        R.string.common_hide_password
                                    } else {
                                        R.string.common_show_password
                                    }
                                ),
                                tint = AndexOnSurfaceVariant,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
            },
        )
        if (supportingText.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = supportingText,
                style = MaterialTheme.typography.bodySmall,
                color = AndexOnSurfaceVariant,
            )
        }
    }
}

// Action button

@Composable
internal fun SelectionDropdownField(
    label: String,
    supportingText: String,
    selectedLabel: String,
    options: List<SelectionOption>,
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = AndexOnSurface,
        )
        if (supportingText.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = supportingText,
                style = MaterialTheme.typography.bodySmall,
                color = AndexOnSurfaceVariant,
            )
        }
        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(AndexSettingsBackground)
                .clickable { expanded = true }
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = selectedLabel,
                style = MaterialTheme.typography.bodyLarge,
                color = AndexOnSurface,
            )
            Icon(
                imageVector = Icons.Rounded.ArrowDropDown,
                contentDescription = label,
                tint = AndexOnSurfaceVariant,
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.andexGlass(RoundedCornerShape(24.dp), popup = true),
            containerColor = Color.Transparent,
        ) {
            Column {
                options.forEach { option ->
                    AndexDropdownMenuItem(
                        selected = option.selected,
                        onClick = {
                            expanded = false
                            option.onClick()
                        },
                    ) {
                        Column {
                            Text(option.title, color = AndexOnSurface)
                            if (option.subtitle.isNotBlank()) {
                                Text(
                                    text = option.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AndexOnSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AndexDropdownMenuItem(
    selected: Boolean,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AndexSurface)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.weight(1f)) {
            content()
        }
        if (selected) {
            Spacer(Modifier.width(12.dp))
            Icon(Icons.Rounded.Check, contentDescription = null, tint = com.android.andex.ui.theme.AndexOnSurface)
        }
    }
}

@Composable
internal fun ThemeModeToggle(
    isDark: Boolean,
    onToggle: () -> Unit,
) {
    val trackColor = if (isDark) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        AndexSettingsBackground
    }
    val thumbColor = if (isDark) {
        MaterialTheme.colorScheme.primary
    } else {
        AndexSurface
    }
    val icon = if (isDark) Icons.Rounded.DarkMode else Icons.Rounded.WbSunny
    val iconTint = if (isDark) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        AndexOnSurfaceVariant
    }

    Box(
        modifier = Modifier
            .size(width = 68.dp, height = 38.dp)
            .clip(CircleShape)
            .background(trackColor)
            .clickable(onClick = onToggle)
            .padding(4.dp),
    ) {
        Box(
            modifier = Modifier
                .align(if (isDark) Alignment.CenterEnd else Alignment.CenterStart)
                .size(30.dp)
                .clip(CircleShape)
                .background(thumbColor),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
internal fun SettingsActionButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    icon: ImageVector? = null,
) {
    Button(
        onClick = onClick,
        enabled = enabled && !isLoading,
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = AndexPrimary,
            contentColor = AndexOnPrimary,
        ),
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = AndexOnPrimary,
            )
            Spacer(Modifier.width(8.dp))
        }
        if (icon != null) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text = label, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
internal fun SettingsSubtleActionButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = AndexSurface,
            contentColor = AndexOnSurface,
            disabledContainerColor = AndexSurface.copy(alpha = 0.55f),
            disabledContentColor = AndexOnSurfaceVariant,
        ),
    ) {
        if (icon != null) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// Small chip button (skill / server actions)

@Composable
internal fun SmallChipButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isDestructive: Boolean = false,
    enabled: Boolean = true,
) {
    val textColor = if (isDestructive) MaterialTheme.colorScheme.error else AndexOnSurface
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (isDestructive) {
                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.75f)
                } else {
                    AndexSurfaceHigh
                }
            )
            .clickable(enabled = enabled, onClick = onClick)
            .alpha(if (enabled) 1f else 0.45f)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = textColor)
    }
}

@Composable
internal fun ActionPreviewPill(
    label: String,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(AndexSettingsBackground)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            imageVector = Icons.Rounded.AutoAwesome,
            contentDescription = null,
            tint = com.android.andex.ui.theme.AndexOnSurface,
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = AndexOnSurface,
        )
    }
}

@Composable
internal fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val showText = title.isNotBlank() || subtitle.isNotBlank()
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Bottom,
    ) {
        if (showText) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = AndexOnSurface,
                )
                if (subtitle.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = AndexOnSurfaceVariant,
                    )
                }
            }
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}

@Composable
internal fun SettingsChoiceRow(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val selectedBackground = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.78f)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) selectedBackground else AndexSettingsBackground)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = AndexOnSurface,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = AndexOnSurfaceVariant,
            )
        }
        if (selected) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
internal fun DetailLine(
    label: String,
    value: String,
) {
    if (value.isBlank()) return
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = AndexOnSurfaceVariant,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = AndexOnSurface,
        )
        Spacer(Modifier.height(10.dp))
    }
}
