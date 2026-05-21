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
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
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
import zed.rainxch.core.presentation.components.GithubStoreButton
import zed.rainxch.core.presentation.theme.GithubStoreTheme
import zed.rainxch.core.presentation.utils.ObserveAsEvents
import zed.rainxch.githubstore.core.presentation.res.Res
import zed.rainxch.githubstore.core.presentation.res.auth_use_device_code_instead
import zed.rainxch.githubstore.core.presentation.res.pat_cancel
import zed.rainxch.githubstore.core.presentation.res.pat_hide
import zed.rainxch.githubstore.core.presentation.res.pat_input_label
import zed.rainxch.githubstore.core.presentation.res.pat_input_placeholder
import zed.rainxch.githubstore.core.presentation.res.pat_open_settings
import zed.rainxch.githubstore.core.presentation.res.pat_sheet_description
import zed.rainxch.githubstore.core.presentation.res.pat_sheet_title
import zed.rainxch.githubstore.core.presentation.res.pat_show
import zed.rainxch.githubstore.core.presentation.res.pat_submit
import zed.rainxch.githubstore.core.presentation.res.pat_use_token_instead
import zed.rainxch.githubstore.core.presentation.res.app_icon
import zed.rainxch.githubstore.core.presentation.res.auth_check_status
import zed.rainxch.githubstore.core.presentation.res.auth_code_expires_in
import zed.rainxch.githubstore.core.presentation.res.auth_error_with_message
import zed.rainxch.githubstore.core.presentation.res.auth_polling_status
import zed.rainxch.githubstore.core.presentation.res.auth_rate_limited
import zed.rainxch.githubstore.core.presentation.res.continue_as_guest
import zed.rainxch.githubstore.core.presentation.res.copy_code
import zed.rainxch.githubstore.core.presentation.res.enter_code_on_github
import zed.rainxch.githubstore.core.presentation.res.ic_github
import zed.rainxch.githubstore.core.presentation.res.more_requests
import zed.rainxch.githubstore.core.presentation.res.more_requests_description
import zed.rainxch.githubstore.core.presentation.res.open_github
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
            AuthenticationEvents.OnNavigateToMain -> {
                onNavigateToHome()
            }
        }
    }

    AuthenticationScreen(
        state = state,
        onAction = viewModel::onAction,
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AuthenticationScreen(
    state: AuthenticationState,
    onAction: (AuthenticationAction) -> Unit,
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(48.dp))

            val iconScale by animateFloatAsState(
                targetValue =
                    when (state.loginState) {
                        is AuthLoginState.LoggedIn -> 0.9f
                        is AuthLoginState.Error -> 0.95f
                        else -> 1f
                    },
                animationSpec =
                    spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow,
                    ),
                label = "icon_scale",
            )

            Image(
                painter = painterResource(Res.drawable.app_icon),
                contentDescription = null,
                modifier =
                    Modifier
                        .size(120.dp)
                        .graphicsLayer {
                            scaleX = iconScale
                            scaleY = iconScale
                        }.clip(RoundedCornerShape(28.dp)),
                contentScale = ContentScale.Crop,
            )

            Spacer(Modifier.height(24.dp))

            AnimatedContent(
                targetState = state.loginState,
                transitionSpec = {
                    val enter =
                        fadeIn(tween(350)) +
                            slideInVertically(
                                animationSpec =
                                    spring(
                                        dampingRatio = Spring.DampingRatioLowBouncy,
                                        stiffness = Spring.StiffnessMediumLow,
                                    ),
                                initialOffsetY = { it / 5 },
                            )
                    val exit = fadeOut(tween(200))
                    enter togetherWith exit
                },
                contentKey = { it::class },
                modifier = Modifier.fillMaxWidth().weight(1f),
                label = "auth_state",
            ) { authState ->
                when (authState) {
                    is AuthLoginState.LoggedOut -> {
                        StateLoggedOut(onAction = onAction)
                    }

                    is AuthLoginState.DevicePrompt -> {
                        StateDevicePrompt(
                            state = state,
                            authState = authState,
                            onAction = onAction,
                        )
                    }

                    is AuthLoginState.Pending -> {
                        StatePending()
                    }

                    is AuthLoginState.LoggedIn -> {
                        StateLoggedIn()
                    }

                    is AuthLoginState.Error -> {
                        StateError(
                            authState = authState,
                            onAction = onAction,
                        )
                    }
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

@Composable
private fun StateLoggedOut(onAction: (AuthenticationAction) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(Res.string.unlock_full_experience),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(24.dp))

        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(32.dp),
            colors =
                CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.OpenWith,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = stringResource(Res.string.more_requests),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )

                    Text(
                        text = stringResource(Res.string.more_requests_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Spacer(Modifier.weight(1f))

        GithubStoreButton(
            text = stringResource(Res.string.sign_in_with_github),
            onClick = { onAction(AuthenticationAction.StartWebAuth) },
            icon = {
                Icon(
                    painter = painterResource(Res.drawable.ic_github),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                )
            },
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(8.dp))

        TextButton(onClick = { onAction(AuthenticationAction.OpenPatSheet) }) {
            Text(
                text = stringResource(Res.string.pat_use_token_instead),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        TextButton(onClick = { onAction(AuthenticationAction.StartLogin) }) {
            Text(
                text = stringResource(Res.string.auth_use_device_code_instead),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }

        TextButton(onClick = { onAction(AuthenticationAction.SkipLogin) }) {
            Text(
                text = stringResource(Res.string.continue_as_guest),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline,
            )
        }

        Spacer(Modifier.height(16.dp))
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
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

        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(32.dp),
            colors =
                CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(Res.string.enter_code_on_github),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(Modifier.height(16.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = authState.start.userCode,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        letterSpacing = 2.sp,
                    )

                    Spacer(Modifier.width(12.dp))

                    IconButton(
                        shapes = IconButtonDefaults.shapes(),
                        onClick = {
                            onAction(AuthenticationAction.CopyCode(authState.start))
                        },
                        colors =
                            IconButtonDefaults.iconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            ),
                    ) {
                        AnimatedContent(
                            targetState = state.copied,
                            transitionSpec = {
                                (scaleIn(
                                    spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                                ) + fadeIn()) togetherWith (scaleOut() + fadeOut())
                            },
                            label = "copy_icon",
                        ) { isCopied ->
                            Icon(
                                imageVector =
                                    if (isCopied) {
                                        Icons.Default.DoneAll
                                    } else {
                                        Icons.Default.ContentCopy
                                    },
                                contentDescription = stringResource(Res.string.copy_code),
                            )
                        }
                    }
                }

                state.info?.let { info ->
                    Spacer(Modifier.height(12.dp))

                    Text(
                        text = info,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                    )
                }

                if (authState.remainingSeconds > 0) {
                    Spacer(Modifier.height(20.dp))

                    val progress =
                        authState.remainingSeconds.toFloat() /
                            authState.start.expiresInSec.toFloat()

                    val animatedProgress by animateFloatAsState(
                        targetValue = progress,
                        animationSpec = tween(900),
                        label = "countdown_progress",
                    )

                    val isUrgent = authState.remainingSeconds < 60

                    val progressColor by animateColorAsState(
                        targetValue =
                            if (isUrgent) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.primary
                            },
                        animationSpec = tween(500),
                        label = "progress_color",
                    )

                    val timerColor by animateColorAsState(
                        targetValue =
                            if (isUrgent) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.outline
                            },
                        animationSpec = tween(500),
                        label = "timer_color",
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        LinearProgressIndicator(
                            progress = { animatedProgress },
                            modifier =
                                Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(4.dp)),
                            color = progressColor,
                            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        )

                        Spacer(Modifier.width(12.dp))

                        val minutes = authState.remainingSeconds / 60
                        val seconds = authState.remainingSeconds % 60
                        val formatted =
                            remember(minutes, seconds) {
                                "%02d:%02d".format(minutes, seconds)
                            }

                        Text(
                            text = formatted,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                            color = timerColor,
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        GithubStoreButton(
            text = stringResource(Res.string.open_github),
            onClick = {
                onAction(AuthenticationAction.OpenGitHub(authState.start))
            },
            icon = {
                Icon(
                    painter = painterResource(Res.drawable.ic_github),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                )
            },
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(12.dp))

        FilledTonalButton(
            onClick = { onAction(AuthenticationAction.PollNow) },
            enabled = !state.isPolling,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (state.isPolling) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
            }

            Spacer(Modifier.width(8.dp))

            Text(
                text =
                    if (state.isPolling) {
                        stringResource(Res.string.auth_polling_status)
                    } else {
                        stringResource(Res.string.auth_check_status)
                    },
                style = MaterialTheme.typography.labelLarge,
            )
        }

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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun StatePending() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularWavyProgressIndicator(
            modifier = Modifier.size(64.dp),
        )

        Spacer(Modifier.height(24.dp))

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
            enter =
                scaleIn(
                    spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                ) + fadeIn(),
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
        }

        Spacer(Modifier.height(20.dp))

        Text(
            text = stringResource(Res.string.signed_in),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = stringResource(Res.string.redirecting_message),
            style = MaterialTheme.typography.bodyLarge,
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

        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(32.dp),
            colors =
                CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                ),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                )

                Spacer(Modifier.height(16.dp))

                Text(
                    text =
                        stringResource(
                            Res.string.auth_error_with_message,
                            authState.message,
                        ),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    textAlign = TextAlign.Center,
                )

                authState.recoveryHint?.let { hint ->
                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = hint,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        GithubStoreButton(
            text = stringResource(Res.string.try_again),
            onClick = { onAction(AuthenticationAction.StartLogin) },
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(8.dp))

        TextButton(onClick = { onAction(AuthenticationAction.SkipLogin) }) {
            Text(
                text = stringResource(Res.string.continue_as_guest),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline,
            )
        }

        Spacer(Modifier.weight(2f))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PatSignInSheet(
    input: String,
    error: String?,
    isSubmitting: Boolean,
    onAction: (AuthenticationAction) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,

        confirmValueChange = { newValue ->
            !(isSubmitting && newValue == SheetValue.Hidden)
        },
    )
    var isMasked by remember { mutableStateOf(true) }

    ModalBottomSheet(
        onDismissRequest = { if (!isSubmitting) onAction(AuthenticationAction.DismissPatSheet) },
        sheetState = sheetState,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(Res.string.pat_sheet_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Text(
                text = stringResource(Res.string.pat_sheet_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            FilledTonalButton(
                onClick = { onAction(AuthenticationAction.OpenPatSettingsPage) },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = stringResource(Res.string.pat_open_settings),
                    style = MaterialTheme.typography.labelLarge,
                )
            }

            OutlinedTextField(
                value = input,
                onValueChange = { onAction(AuthenticationAction.OnPatInputChanged(it)) },
                label = { Text(stringResource(Res.string.pat_input_label)) },
                placeholder = { Text(stringResource(Res.string.pat_input_placeholder)) },
                singleLine = true,
                visualTransformation =
                    if (isMasked) PasswordVisualTransformation() else VisualTransformation.None,
                keyboardOptions =
                    KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        autoCorrectEnabled = false,
                        capitalization = KeyboardCapitalization.None,
                    ),
                isError = error != null,
                enabled = !isSubmitting,
                trailingIcon = {
                    IconButton(onClick = { isMasked = !isMasked }) {
                        Icon(
                            imageVector =
                                if (isMasked) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription =
                                stringResource(
                                    if (isMasked) Res.string.pat_show else Res.string.pat_hide,
                                ),
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )

            if (error != null) {
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Spacer(Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TextButton(
                    onClick = { onAction(AuthenticationAction.DismissPatSheet) },
                    enabled = !isSubmitting,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(Res.string.pat_cancel))
                }

                GithubStoreButton(
                    text = stringResource(Res.string.pat_submit),
                    onClick = { onAction(AuthenticationAction.SubmitPat) },
                    modifier = Modifier.weight(1f),
                    icon =
                        if (isSubmitting) {
                            {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                )
                            }
                        } else {
                            null
                        },
                )
            }
        }
    }
}

@Preview
@Composable
private fun PreviewError() {
    GithubStoreTheme {
        AuthenticationScreen(
            state =
                AuthenticationState(
                    loginState =
                        AuthLoginState.Error(
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
    GithubStoreTheme {
        AuthenticationScreen(
            state =
                AuthenticationState(
                    loginState = AuthLoginState.LoggedOut,
                ),
            onAction = {},
        )
    }
}

@Preview
@Composable
private fun PreviewDevicePrompt() {
    GithubStoreTheme {
        AuthenticationScreen(
            state =
                AuthenticationState(
                    loginState =
                        AuthLoginState.DevicePrompt(
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
    GithubStoreTheme {
        AuthenticationScreen(
            state =
                AuthenticationState(
                    loginState = AuthLoginState.LoggedIn,
                ),
            onAction = {},
        )
    }
}
