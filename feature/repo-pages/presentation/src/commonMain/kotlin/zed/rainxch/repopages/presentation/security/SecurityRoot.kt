package zed.rainxch.repopages.presentation.security

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import zed.rainxch.core.presentation.locals.LocalStatusColors
import zed.rainxch.githubstore.core.presentation.res.Res
import zed.rainxch.githubstore.core.presentation.res.repo_pages_security_advisories_header
import zed.rainxch.githubstore.core.presentation.res.repo_pages_security_no_advisories
import zed.rainxch.githubstore.core.presentation.res.repo_pages_security_no_policy
import zed.rainxch.githubstore.core.presentation.res.repo_pages_security_policy_header
import zed.rainxch.githubstore.core.presentation.res.repo_pages_security_title
import zed.rainxch.githubstore.core.presentation.res.repo_pages_severity_critical
import zed.rainxch.githubstore.core.presentation.res.repo_pages_severity_high
import zed.rainxch.githubstore.core.presentation.res.repo_pages_severity_low
import zed.rainxch.githubstore.core.presentation.res.repo_pages_severity_medium
import zed.rainxch.githubstore.core.presentation.res.repo_pages_severity_unknown
import zed.rainxch.repopages.domain.model.AdvisorySeverity
import zed.rainxch.repopages.domain.model.SecurityAdvisory
import zed.rainxch.repopages.domain.model.SecurityOverview
import zed.rainxch.repopages.presentation.components.RepoMarkdown
import zed.rainxch.repopages.presentation.components.RepoPagesError
import zed.rainxch.repopages.presentation.components.RepoPagesLoading
import zed.rainxch.repopages.presentation.components.RepoPagesTopBar

@Composable
fun SecurityRoot(
    onNavigateBack: () -> Unit,
    viewModel: SecurityViewModel,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    SecurityScreen(
        state = state,
        onAction = { action ->
            when (action) {
                SecurityAction.OnBackClick -> onNavigateBack()
                else -> viewModel.onAction(action)
            }
        },
    )
}

@Composable
private fun SecurityScreen(
    state: SecurityUiState,
    onAction: (SecurityAction) -> Unit,
) {
    Scaffold(
        topBar = {
            RepoPagesTopBar(
                title = stringResource(Res.string.repo_pages_security_title),
                onBack = { onAction(SecurityAction.OnBackClick) },
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        val contentModifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
        when {
            state.isLoading -> RepoPagesLoading(modifier = contentModifier)
            state.errorMessage != null -> RepoPagesError(
                message = state.errorMessage,
                onRetry = { onAction(SecurityAction.OnRetry) },
                modifier = contentModifier,
            )
            state.overview != null -> SecurityContent(
                overview = state.overview,
                modifier = contentModifier,
            )
        }
    }
}

@Composable
private fun SecurityContent(
    overview: SecurityOverview,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item(key = "advisories_header") {
            SectionHeader(stringResource(Res.string.repo_pages_security_advisories_header))
        }
        if (overview.advisories.isEmpty()) {
            item(key = "advisories_empty") {
                Text(
                    text = stringResource(Res.string.repo_pages_security_no_advisories),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            items(overview.advisories, key = { it.ghsaId }) { advisory ->
                AdvisoryCard(advisory)
            }
        }

        item(key = "policy_header") {
            SectionHeader(stringResource(Res.string.repo_pages_security_policy_header))
        }
        item(key = "policy_body") {
            val policy = overview.securityPolicyMarkdown
            if (policy.isNullOrBlank()) {
                Text(
                    text = stringResource(Res.string.repo_pages_security_no_policy),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    RepoMarkdown(content = policy, modifier = Modifier.fillMaxWidth().padding(14.dp))
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(top = 4.dp),
    )
}

@Composable
private fun AdvisoryCard(advisory: SecurityAdvisory) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = advisory.summary,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SeverityBadge(advisory.severity)
                val meta = buildString {
                    advisory.cveId?.let { append(it) }
                    advisory.publishedAt?.let {
                        if (isNotEmpty()) append(" · ")
                        append(it.take(10))
                    }
                }
                if (meta.isNotEmpty()) {
                    Text(
                        text = meta,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            advisory.description?.takeIf { it.isNotBlank() }?.let { description ->
                RepoMarkdown(content = description, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun SeverityBadge(severity: AdvisorySeverity) {
    val statusColors = LocalStatusColors.current
    val color = when (severity) {
        AdvisorySeverity.CRITICAL -> statusColors.severityCritical
        AdvisorySeverity.HIGH -> statusColors.severityHigh
        AdvisorySeverity.MEDIUM -> statusColors.severityMedium
        AdvisorySeverity.LOW -> statusColors.severityLow
        AdvisorySeverity.UNKNOWN -> statusColors.severityUnknown
    }
    val label = when (severity) {
        AdvisorySeverity.CRITICAL -> stringResource(Res.string.repo_pages_severity_critical)
        AdvisorySeverity.HIGH -> stringResource(Res.string.repo_pages_severity_high)
        AdvisorySeverity.MEDIUM -> stringResource(Res.string.repo_pages_severity_medium)
        AdvisorySeverity.LOW -> stringResource(Res.string.repo_pages_severity_low)
        AdvisorySeverity.UNKNOWN -> stringResource(Res.string.repo_pages_severity_unknown)
    }
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.16f),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
        )
    }
}
