package zed.rainxch.githubstore.app.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.DownloadForOffline
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.koin.compose.viewmodel.koinViewModel
import zed.rainxch.core.domain.model.AppTheme
import zed.rainxch.core.domain.model.ThemeMode
import zed.rainxch.core.presentation.components.buttons.OutlineButton
import zed.rainxch.core.presentation.components.buttons.PrimaryButton
import zed.rainxch.core.presentation.components.buttons.TintedButton
import zed.rainxch.core.presentation.theme.fraunces
import zed.rainxch.core.presentation.theme.tokens.Radii
import zed.rainxch.core.presentation.utils.ObserveAsEvents
import zed.rainxch.core.presentation.utils.primaryColor
import zed.rainxch.core.presentation.vocabulary.CookieShape
import zed.rainxch.core.presentation.vocabulary.Squiggle

@Composable
fun OnboardingRoot(
    onNavigateToSignIn: () -> Unit,
    onNavigateToHome: () -> Unit,
    viewModel: OnboardingViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            OnboardingEvent.NavigateToSignIn -> onNavigateToSignIn()
            OnboardingEvent.NavigateToHome -> onNavigateToHome()
        }
    }
    OnboardingScreen(state = state, onAction = viewModel::onAction)
}

@Composable
fun OnboardingScreen(
    state: OnboardingState,
    onAction: (OnboardingAction) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .systemBarsPadding()
                .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        StepIndicator(total = state.steps.size, currentIndex = state.currentIndex)
        Spacer(Modifier.height(32.dp))

        val permissionsController = rememberOnboardingPermissionsController()

        AnimatedContent(
            targetState = state.currentStep,
            transitionSpec = {
                (
                    slideInHorizontally { it } + fadeIn() togetherWith
                        slideOutHorizontally { -it } + fadeOut()
                )
            },
            label = "onboarding-step",
            modifier = Modifier.weight(1f).fillMaxWidth(),
        ) { step ->
            when (step) {
                OnboardingStep.PALETTE -> StepPalette(state, onAction)
                OnboardingStep.SIGN_IN -> StepSignIn(onAction)
                OnboardingStep.PERMISSIONS -> StepPermissions(permissionsController)
            }
        }

        ActionRow(state, onAction)
    }
}

@Composable
private fun StepIndicator(
    total: Int,
    currentIndex: Int,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(total) { i ->
            val active = i <= currentIndex
            Box(
                modifier =
                    Modifier
                        .size(width = if (i == currentIndex) 24.dp else 8.dp, height = 8.dp)
                        .clip(CircleShape)
                        .background(
                            if (active) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outlineVariant
                            },
                        ),
            )
        }
    }
}

@Composable
private fun StepPalette(
    state: OnboardingState,
    onAction: (OnboardingAction) -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text(
            text = "Pick your palette",
            style =
                MaterialTheme.typography.displaySmall.copy(
                    fontFamily = fraunces,
                    fontWeight = FontWeight.SemiBold,
                ),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Squiggle()
        Text(
            text = "Four palettes × Light, Dark, AMOLED, System.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            AppTheme.entries.forEach { palette ->
                PaletteSwatch(
                    palette = palette,
                    isSelected = state.selectedPalette == palette,
                    onClick = { onAction(OnboardingAction.OnPaletteSelected(palette)) },
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        ModeRow(
            selected = state.selectedMode,
            onSelect = { onAction(OnboardingAction.OnModeSelected(it)) },
        )
    }
}

@Composable
private fun PaletteSwatch(
    palette: AppTheme,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Box(
            modifier =
                Modifier
                    .size(64.dp)
                    .clip(CookieShape)
                    .background(palette.primaryColor)
                    .border(
                        width = if (isSelected) 3.dp else 0.dp,
                        color = MaterialTheme.colorScheme.onSurface,
                        shape = CookieShape,
                    ),
        )
        Text(
            text = palette.name.lowercase().replaceFirstChar { it.uppercaseChar() },
            color =
                if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            style =
                MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                ),
        )
    }
}

@Composable
private fun ModeRow(
    selected: ThemeMode,
    onSelect: (ThemeMode) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .clip(Radii.chip)
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        ThemeMode.entries.forEach { mode ->
            val isActive = mode == selected
            Box(
                modifier =
                    Modifier
                        .clip(Radii.chip)
                        .background(if (isActive) MaterialTheme.colorScheme.primary else Color.Transparent)
                        .clickable { onSelect(mode) }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Text(
                    text = mode.name.lowercase().replaceFirstChar { it.uppercaseChar() },
                    color =
                        if (isActive) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}

@Composable
private fun StepSignIn(onAction: (OnboardingAction) -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .size(96.dp)
                    .clip(CookieShape)
                    .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "G",
                color = MaterialTheme.colorScheme.onPrimary,
                fontFamily = fraunces,
                fontWeight = FontWeight.Bold,
                fontSize = 48.sp,
            )
        }
        Text(
            text = "Sign in with GitHub",
            style =
                MaterialTheme.typography.headlineSmall.copy(
                    fontFamily = fraunces,
                    fontWeight = FontWeight.SemiBold,
                ),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Squiggle()
        Text(
            text = "Stars, profile, and rate-limit headroom. Optional — skip to browse anonymously.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        PrimaryButton(onClick = { onAction(OnboardingAction.OnSignInClick) }) {
            Text("Sign in")
        }
    }
}

@Composable
private fun StepPermissions(controller: OnboardingPermissionsController) {
    val notificationsGranted by controller.notificationsGranted
    val installSourcesGranted by controller.installSourcesGranted
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Two quick prompts",
            style =
                MaterialTheme.typography.headlineSmall.copy(
                    fontFamily = fraunces,
                    fontWeight = FontWeight.SemiBold,
                ),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Squiggle()
        Text(
            text = "Both optional. You can flip these later in Tweaks.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        PermissionRow(
            icon = Icons.Outlined.Notifications,
            label = "Notifications",
            description = "Update alerts and install progress",
            isGranted = notificationsGranted,
            onAllowClick = { controller.requestNotifications() },
        )
        PermissionRow(
            icon = Icons.Outlined.DownloadForOffline,
            label = "Install unknown apps",
            description = "Required to install APKs outside Play",
            isGranted = installSourcesGranted,
            onAllowClick = { controller.requestInstallSources() },
        )
    }
}

@Composable
private fun PermissionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    description: String,
    isGranted: Boolean,
    onAllowClick: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(Radii.card)
                .background(cs.surfaceContainer)
                .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (isGranted) cs.tertiaryContainer else cs.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isGranted) cs.tertiary else cs.primary,
                modifier = Modifier.size(22.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                color = cs.onSurface,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            )
            Text(
                text = description,
                color = cs.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        if (isGranted) {
            Box(
                modifier =
                    Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(cs.tertiary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Granted",
                    tint = cs.onPrimary,
                    modifier = Modifier.size(20.dp),
                )
            }
        } else {
            TintedButton(onClick = onAllowClick) { Text("Allow") }
        }
    }
}

@Composable
private fun ActionRow(
    state: OnboardingState,
    onAction: (OnboardingAction) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (state.currentStep == OnboardingStep.SIGN_IN || state.currentStep == OnboardingStep.PERMISSIONS) {
            OutlineButton(onClick = { onAction(OnboardingAction.OnSkipStepClick) }) {
                Text("Skip")
            }
        } else {
            Spacer(Modifier.size(80.dp))
        }
        PrimaryButton(onClick = { onAction(OnboardingAction.OnNextClick) }) {
            Text(if (state.isLast) "Get started" else "Next")
        }
    }
}
