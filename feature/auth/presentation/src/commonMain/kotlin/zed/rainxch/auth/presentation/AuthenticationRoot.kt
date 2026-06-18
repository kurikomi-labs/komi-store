@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class,
)

package zed.rainxch.auth.presentation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import zed.rainxch.auth.presentation.model.AuthLoginState
import zed.rainxch.auth.presentation.model.GithubDeviceStartUi
import zed.rainxch.core.presentation.components.buttons.KomiButton
import zed.rainxch.core.presentation.components.buttons.KomiButtonSize
import zed.rainxch.core.presentation.components.buttons.KomiButtonVariant
import zed.rainxch.core.presentation.components.inputs.KomiTextField
import zed.rainxch.core.presentation.components.overlays.KomiSheet
import zed.rainxch.core.presentation.components.overlays.KomiSheetPlacement
import zed.rainxch.core.presentation.personality.classicPersonality
import zed.rainxch.core.presentation.personality.utils.PersonalityPreview
import zed.rainxch.core.presentation.utils.ObserveAsEvents
import zed.rainxch.core.presentation.utils.constrainedContentWidth
import zed.rainxch.githubstore.core.presentation.res.Res
import zed.rainxch.githubstore.core.presentation.res.auth_use_device_code_instead
import zed.rainxch.githubstore.core.presentation.res.app_icon
import zed.rainxch.githubstore.core.presentation.res.auth_check_status
import zed.rainxch.githubstore.core.presentation.res.auth_error_with_message
import zed.rainxch.githubstore.core.presentation.res.auth_polling_status
import zed.rainxch.githubstore.core.presentation.res.auth_rate_limited
import zed.rainxch.githubstore.core.presentation.res.auth_hide_signin_options
import zed.rainxch.githubstore.core.presentation.res.auth_more_signin_options
import zed.rainxch.githubstore.core.presentation.res.continue_as_guest
import zed.rainxch.githubstore.core.presentation.res.copy_code
import zed.rainxch.githubstore.core.presentation.res.enter_code_on_github
import zed.rainxch.githubstore.core.presentation.res.ic_github
import zed.rainxch.githubstore.core.presentation.res.more_requests
import zed.rainxch.githubstore.core.presentation.res.more_requests_description
import zed.rainxch.githubstore.core.presentation.res.open_github
import zed.rainxch.githubstore.core.presentation.res.pat_cancel
import zed.rainxch.githubstore.core.presentation.res.pat_input_label
import zed.rainxch.githubstore.core.presentation.res.pat_input_placeholder
import zed.rainxch.githubstore.core.presentation.res.pat_open_settings
import zed.rainxch.githubstore.core.presentation.res.pat_sheet_description
import zed.rainxch.githubstore.core.presentation.res.pat_sheet_title
import zed.rainxch.githubstore.core.presentation.res.pat_submit
import zed.rainxch.githubstore.core.presentation.res.pat_use_token_instead
import zed.rainxch.githubstore.core.presentation.res.redirecting_message
import zed.rainxch.githubstore.core.presentation.res.sign_in_with_github
import zed.rainxch.githubstore.core.presentation.res.signed_in
import zed.rainxch.githubstore.core.presentation.res.try_again
import zed.rainxch.githubstore.core.presentation.res.unlock_full_experience
import zed.rainxch.githubstore.core.presentation.res.waiting_for_authorization

@Composable
fun AuthenticationRoot(
    onNavigateToHome: () -> Unit,
    viewModel: AuthenticationViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.onAction(AuthenticationAction.OnResumed)
    }

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            AuthenticationEvents.OnNavigateToMain -> onNavigateToHome()
        }
    }

    AuthenticationScreen(
        state = state,
        onAction = viewModel::onAction,
    )
}

@Composable
fun AuthenticationScreen(
    state: AuthenticationState,
    onAction: (AuthenticationAction) -> Unit,
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .constrainedContentWidth()
                    .fillMaxHeight()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(56.dp))

                val iconScale by animateFloatAsState(
                    targetValue = when (state.loginState) {
                        is AuthLoginState.LoggedIn -> 0.92f
                        is AuthLoginState.Error -> 0.96f
                        else -> 1f
                    },
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow,
                    ),
                    label = "icon_scale",
                )

                Image(
                    painter = painterResource(Res.drawable.app_icon),
                    contentDescription = null,
                    modifier = Modifier
                        .size(96.dp)
                        .graphicsLayer {
                            scaleX = iconScale
                            scaleY = iconScale
                        }
                        .clip(RoundedCornerShape(24.dp)),
                    contentScale = ContentScale.Crop,
                )

                Spacer(Modifier.height(24.dp))

                AnimatedContent(
                    targetState = state.loginState,
                    transitionSpec = {
                        val enter = fadeIn(tween(350)) + slideInVertically(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessMediumLow,
                            ),
                            initialOffsetY = { it / 5 },
                        )
                        val exit = fadeOut(tween(200))
                        enter togetherWith exit
                    },
                    contentKey = { it::class },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    label = "auth_state",
                ) { authState ->
                    when (authState) {
                        is AuthLoginState.LoggedOut -> StateLoggedOut(onAction = onAction)
                        is AuthLoginState.DevicePrompt -> StateDevicePrompt(
                            state = state,
                            authState = authState,
                            onAction = onAction,
                        )
                        is AuthLoginState.Pending -> StatePending()
                        is AuthLoginState.LoggedIn -> StateLoggedIn()
                        is AuthLoginState.Error -> StateError(authState = authState, onAction = onAction)
                    }
                }
            }

            if (state.isPatSheetVisible) {
                PatSignInSheet(
                    input = state.patInput,
                    error = state.patError,
                    isSubmitting = state.isPatSubmitting,
                    onAction = onAction,
                )
            }
        }
    }
}

@Composable
private fun StateLoggedOut(onAction: (AuthenticationAction) -> Unit) {
    var showMoreOptions by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(Res.string.unlock_full_experience),
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.SemiBold,
            ),
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(20.dp))

        BenefitsCard()

        Spacer(Modifier.weight(1f))

        PrimaryPillButton(
            text = stringResource(Res.string.sign_in_with_github),
            leadingIcon = {
                Icon(
                    painter = painterResource(Res.drawable.ic_github),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onPrimary,
                )
            },
            onClick = { onAction(AuthenticationAction.StartWebAuth) },
        )

        Spacer(Modifier.height(10.dp))

        KomiButton(
            onClick = { onAction(AuthenticationAction.SkipLogin) },
            label = stringResource(Res.string.continue_as_guest),
            variant = KomiButtonVariant.Text,
            size = KomiButtonSize.Sm,
        )

        KomiButton(
            onClick = { showMoreOptions = !showMoreOptions },
            label = if (showMoreOptions) {
                stringResource(Res.string.auth_hide_signin_options)
            } else {
                stringResource(Res.string.auth_more_signin_options)
            },
            variant = KomiButtonVariant.Text,
            size = KomiButtonSize.Sm,
            trailingIcon = Icons.Default.ExpandMore,
        )

        AnimatedVisibility(visible = showMoreOptions) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.height(4.dp))
                KomiButton(
                    onClick = { onAction(AuthenticationAction.OpenPatSheet) },
                    label = stringResource(Res.string.pat_use_token_instead),
                    variant = KomiButtonVariant.Text,
                    size = KomiButtonSize.Sm,
                )
                KomiButton(
                    onClick = { onAction(AuthenticationAction.StartLogin) },
                    label = stringResource(Res.string.auth_use_device_code_instead),
                    variant = KomiButtonVariant.Text,
                    size = KomiButtonSize.Sm,
                )
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun BenefitsCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_github),
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = stringResource(Res.string.more_requests),
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(Res.string.more_requests_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun StateDevicePrompt(
    state: AuthenticationState,
    authState: AuthLoginState.DevicePrompt,
    onAction: (AuthenticationAction) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(Res.string.enter_code_on_github),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(14.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = authState.start.userCode,
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        letterSpacing = 3.sp,
                    )
                    Spacer(Modifier.width(12.dp))
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f))
                            .clickable {
                                onAction(AuthenticationAction.CopyCode(authState.start))
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        AnimatedContent(
                            targetState = state.copied,
                            transitionSpec = {
                                (scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn()) togetherWith
                                    (scaleOut() + fadeOut())
                            },
                            label = "copy_icon",
                        ) { isCopied ->
                            Icon(
                                imageVector = if (isCopied) Icons.Default.DoneAll else Icons.Default.ContentCopy,
                                contentDescription = stringResource(Res.string.copy_code),
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
                state.info?.let { info ->
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = info,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                    )
                }
                if (authState.remainingSeconds > 0) {
                    Spacer(Modifier.height(16.dp))
                    val progress = authState.remainingSeconds.toFloat() /
                        authState.start.expiresInSec.toFloat()
                    val animatedProgress by animateFloatAsState(
                        targetValue = progress,
                        animationSpec = tween(900),
                        label = "countdown_progress",
                    )
                    val isUrgent = authState.remainingSeconds < 60
                    val progressColor by animateColorAsState(
                        targetValue = if (isUrgent) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        animationSpec = tween(500),
                        label = "progress_color",
                    )
                    val timerColor by animateColorAsState(
                        targetValue = if (isUrgent) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                        animationSpec = tween(500),
                        label = "timer_color",
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        LinearProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(4.dp)),
                            color = progressColor,
                            trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        )
                        Spacer(Modifier.width(12.dp))
                        val minutes = authState.remainingSeconds / 60
                        val seconds = authState.remainingSeconds % 60
                        val formatted = remember(minutes, seconds) {
                            "%02d:%02d".format(minutes, seconds)
                        }
                        Text(
                            text = formatted,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Medium,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            ),
                            color = timerColor,
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(18.dp))

        PrimaryPillButton(
            text = stringResource(Res.string.open_github),
            leadingIcon = {
                Icon(
                    painter = painterResource(Res.drawable.ic_github),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onPrimary,
                )
            },
            onClick = { onAction(AuthenticationAction.OpenGitHub(authState.start)) },
        )

        Spacer(Modifier.height(10.dp))

        OutlinedPillButton(
            text = if (state.isPolling) {
                stringResource(Res.string.auth_polling_status)
            } else {
                stringResource(Res.string.auth_check_status)
            },
            leadingIcon = if (state.isPolling) {
                {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            } else {
                {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
            },
            enabled = !state.isPolling,
            onClick = { onAction(AuthenticationAction.PollNow) },
        )

        if (state.pollIntervalSec > 0) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(Res.string.auth_rate_limited, state.pollIntervalSec),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.weight(2f))
    }
}

@Composable
private fun StatePending() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularWavyProgressIndicator(modifier = Modifier.size(56.dp))
        Spacer(Modifier.height(20.dp))
        Text(
            text = stringResource(Res.string.waiting_for_authorization),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun StateLoggedIn() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        var visible by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { visible = true }
        AnimatedVisibility(
            visible = visible,
            enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(),
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
        Spacer(Modifier.height(18.dp))
        Text(
            text = stringResource(Res.string.signed_in),
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(Res.string.redirecting_message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun StateError(
    authState: AuthLoginState.Error,
    onAction: (AuthenticationAction) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.errorContainer,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    imageVector = Icons.Outlined.WarningAmber,
                    contentDescription = null,
                    modifier = Modifier.size(34.dp),
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(Res.string.auth_error_with_message, authState.message),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    textAlign = TextAlign.Center,
                )
                authState.recoveryHint?.let { hint ->
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = hint,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        PrimaryPillButton(
            text = stringResource(Res.string.try_again),
            onClick = { onAction(AuthenticationAction.StartLogin) },
        )
        Spacer(Modifier.height(6.dp))
        KomiButton(
            onClick = { onAction(AuthenticationAction.SkipLogin) },
            label = stringResource(Res.string.continue_as_guest),
            variant = KomiButtonVariant.Text,
            size = KomiButtonSize.Sm,
        )
        Spacer(Modifier.weight(2f))
    }
}

@Composable
private fun PrimaryPillButton(
    text: String,
    onClick: () -> Unit,
    leadingIcon: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
) {
    KomiButton(
        onClick = onClick,
        label = text,
        enabled = enabled,
        variant = KomiButtonVariant.Primary,
        size = KomiButtonSize.Lg,
        fullWidth = true,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
    )
}

@Composable
private fun OutlinedPillButton(
    text: String,
    onClick: () -> Unit,
    leadingIcon: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        onClick = onClick,
        enabled = enabled,
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            leadingIcon?.invoke()
            if (leadingIcon != null) Spacer(Modifier.width(8.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                ),
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun PatSignInSheet(
    input: String,
    error: String?,
    isSubmitting: Boolean,
    onAction: (AuthenticationAction) -> Unit,
) {
    KomiSheet(
        onDismiss = { if (!isSubmitting) onAction(AuthenticationAction.DismissPatSheet) },
        placement = KomiSheetPlacement.Bottom,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = stringResource(Res.string.pat_sheet_title),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                ),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(Res.string.pat_sheet_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            OutlinedPillButton(
                text = stringResource(Res.string.pat_open_settings),
                onClick = { onAction(AuthenticationAction.OpenPatSettingsPage) },
            )

            KomiTextField(
                value = input,
                onValueChange = { onAction(AuthenticationAction.OnPatInputChanged(it)) },
                label = stringResource(Res.string.pat_input_label),
                placeholder = stringResource(Res.string.pat_input_placeholder),
                error = error,
                password = true,
                keyboardType = KeyboardType.Password,
                enabled = !isSubmitting,
                modifier = Modifier.fillMaxWidth(),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(50),
                    color = Color.Transparent,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
                    onClick = { onAction(AuthenticationAction.DismissPatSheet) },
                    enabled = !isSubmitting,
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(Res.string.pat_cancel),
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.SemiBold,
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }

                KomiButton(
                    onClick = { onAction(AuthenticationAction.SubmitPat) },
                    label = stringResource(Res.string.pat_submit),
                    enabled = !isSubmitting && input.isNotBlank(),
                    loading = isSubmitting,
                    variant = KomiButtonVariant.Primary,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                )
            }
        }
    }
}

@Preview
@Composable
private fun PreviewError() {
    PersonalityPreview {
        AuthenticationScreen(
            state = AuthenticationState(
                loginState = AuthLoginState.Error(
                    message = "Network timeout",
                    recoveryHint = "Check your internet connection",
                ),
            ),
            onAction = {},
        )
    }
}

@Preview
@Composable
private fun PreviewLoggedOut() {
    PersonalityPreview(personality = classicPersonality()) {
        AuthenticationScreen(
            state = AuthenticationState(loginState = AuthLoginState.LoggedOut),
            onAction = {},
        )
    }
}

@Preview
@Composable
private fun PreviewDevicePrompt() {
    PersonalityPreview {
        AuthenticationScreen(
            state = AuthenticationState(
                loginState = AuthLoginState.DevicePrompt(
                    GithubDeviceStartUi(
                        deviceCode = "",
                        userCode = "2102-UHHUF",
                        verificationUri = "",
                        expiresInSec = 900,
                    ),
                    remainingSeconds = 847,
                ),
                copied = true,
            ),
            onAction = {},
        )
    }
}

@Preview
@Composable
private fun PreviewLoggedIn() {
    PersonalityPreview {
        AuthenticationScreen(
            state = AuthenticationState(loginState = AuthLoginState.LoggedIn),
            onAction = {},
        )
    }
}
