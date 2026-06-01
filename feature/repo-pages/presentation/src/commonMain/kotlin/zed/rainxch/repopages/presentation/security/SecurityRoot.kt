package zed.rainxch.repopages.presentation.security

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
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
    owner: String,
    repo: String,
    onNavigateBack: () -> Unit,
    viewModel: SecurityViewModel = koinViewModel { parametersOf(owner, repo) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    SecurityScreen(
        state = state,
        onBack = onNavigateBack,
        onRetry = viewModel::retry,
    )
}

@Composable
private fun SecurityScreen(
    state: SecurityUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding(),
    ) {
        RepoPagesTopBar(title = stringResource(Res.string.repo_pages_security_title), onBack = onBack)
        when {
            state.isLoading -> RepoPagesLoading()
            state.errorMessage != null -> RepoPagesError(message = state.errorMessage, onRetry = onRetry)
            state.overview != null -> SecurityContent(overview = state.overview)
        }
    }
}

@Composable
private fun SecurityContent(overview: SecurityOverview) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
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
    val color = when (severity) {
        AdvisorySeverity.CRITICAL -> Color(0xFFCF222E)
        AdvisorySeverity.HIGH -> Color(0xFFBC4C00)
        AdvisorySeverity.MEDIUM -> Color(0xFF9A6700)
        AdvisorySeverity.LOW -> Color(0xFF1A7F37)
        AdvisorySeverity.UNKNOWN -> Color(0xFF6E7781)
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
