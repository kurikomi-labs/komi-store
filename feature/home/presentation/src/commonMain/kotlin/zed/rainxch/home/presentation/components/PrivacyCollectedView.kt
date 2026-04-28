package zed.rainxch.home.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import zed.rainxch.githubstore.core.presentation.res.Res
import zed.rainxch.githubstore.core.presentation.res.privacy_collected_app_launched_desc
import zed.rainxch.githubstore.core.presentation.res.privacy_collected_back
import zed.rainxch.githubstore.core.presentation.res.privacy_collected_cache_hit_desc
import zed.rainxch.githubstore.core.presentation.res.privacy_collected_cache_miss_desc
import zed.rainxch.githubstore.core.presentation.res.privacy_collected_cold_start_ms_desc
import zed.rainxch.githubstore.core.presentation.res.privacy_collected_crash_desc
import zed.rainxch.githubstore.core.presentation.res.privacy_collected_details_viewed_desc
import zed.rainxch.githubstore.core.presentation.res.privacy_collected_first_paint_ms_desc
import zed.rainxch.githubstore.core.presentation.res.privacy_collected_import_auto_linked_desc
import zed.rainxch.githubstore.core.presentation.res.privacy_collected_import_manually_linked_desc
import zed.rainxch.githubstore.core.presentation.res.privacy_collected_import_match_attempted_desc
import zed.rainxch.githubstore.core.presentation.res.privacy_collected_import_scan_completed_desc
import zed.rainxch.githubstore.core.presentation.res.privacy_collected_import_scan_started_desc
import zed.rainxch.githubstore.core.presentation.res.privacy_collected_import_skipped_desc
import zed.rainxch.githubstore.core.presentation.res.privacy_collected_intro
import zed.rainxch.githubstore.core.presentation.res.privacy_collected_never_account
import zed.rainxch.githubstore.core.presentation.res.privacy_collected_never_ip
import zed.rainxch.githubstore.core.presentation.res.privacy_collected_never_paths
import zed.rainxch.githubstore.core.presentation.res.privacy_collected_never_personal
import zed.rainxch.githubstore.core.presentation.res.privacy_collected_never_raw_queries
import zed.rainxch.githubstore.core.presentation.res.privacy_collected_never_repo_identifiers
import zed.rainxch.githubstore.core.presentation.res.privacy_collected_never_title
import zed.rainxch.githubstore.core.presentation.res.privacy_collected_never_tokens
import zed.rainxch.githubstore.core.presentation.res.privacy_collected_operation_failed_desc
import zed.rainxch.githubstore.core.presentation.res.privacy_collected_proxy_configured_desc
import zed.rainxch.githubstore.core.presentation.res.privacy_collected_proxy_used_desc
import zed.rainxch.githubstore.core.presentation.res.privacy_collected_search_executed_desc
import zed.rainxch.githubstore.core.presentation.res.privacy_collected_section_discovery
import zed.rainxch.githubstore.core.presentation.res.privacy_collected_section_library
import zed.rainxch.githubstore.core.presentation.res.privacy_collected_section_network
import zed.rainxch.githubstore.core.presentation.res.privacy_collected_section_performance
import zed.rainxch.githubstore.core.presentation.res.privacy_collected_section_reliability
import zed.rainxch.githubstore.core.presentation.res.privacy_collected_section_server_side
import zed.rainxch.githubstore.core.presentation.res.privacy_collected_section_session
import zed.rainxch.githubstore.core.presentation.res.privacy_collected_section_updates
import zed.rainxch.githubstore.core.presentation.res.privacy_collected_server_side_intro
import zed.rainxch.githubstore.core.presentation.res.privacy_collected_server_side_logs
import zed.rainxch.githubstore.core.presentation.res.privacy_collected_server_side_search_misses
import zed.rainxch.githubstore.core.presentation.res.privacy_collected_session_duration_desc
import zed.rainxch.githubstore.core.presentation.res.privacy_collected_title
import zed.rainxch.githubstore.core.presentation.res.privacy_collected_update_installed_desc
import zed.rainxch.githubstore.core.presentation.res.privacy_collected_view_source

private data class CollectedEvent(
    val name: String,
    val description: StringResource,
)

private data class CollectedSection(
    val title: StringResource,
    val events: List<CollectedEvent>,
)

private val sections =
    listOf(
        CollectedSection(
            title = Res.string.privacy_collected_section_session,
            events =
                listOf(
                    CollectedEvent("app_launched", Res.string.privacy_collected_app_launched_desc),
                    CollectedEvent("session_duration", Res.string.privacy_collected_session_duration_desc),
                ),
        ),
        CollectedSection(
            title = Res.string.privacy_collected_section_discovery,
            events =
                listOf(
                    CollectedEvent("search_executed", Res.string.privacy_collected_search_executed_desc),
                    CollectedEvent("details_viewed", Res.string.privacy_collected_details_viewed_desc),
                ),
        ),
        CollectedSection(
            title = Res.string.privacy_collected_section_library,
            events =
                listOf(
                    CollectedEvent("import_scan_started", Res.string.privacy_collected_import_scan_started_desc),
                    CollectedEvent("import_scan_completed", Res.string.privacy_collected_import_scan_completed_desc),
                    CollectedEvent("import_match_attempted", Res.string.privacy_collected_import_match_attempted_desc),
                    CollectedEvent("import_auto_linked", Res.string.privacy_collected_import_auto_linked_desc),
                    CollectedEvent("import_manually_linked", Res.string.privacy_collected_import_manually_linked_desc),
                    CollectedEvent("import_skipped", Res.string.privacy_collected_import_skipped_desc),
                ),
        ),
        CollectedSection(
            title = Res.string.privacy_collected_section_performance,
            events =
                listOf(
                    CollectedEvent("cold_start_ms", Res.string.privacy_collected_cold_start_ms_desc),
                    CollectedEvent("first_paint_ms", Res.string.privacy_collected_first_paint_ms_desc),
                    CollectedEvent("cache_hit", Res.string.privacy_collected_cache_hit_desc),
                    CollectedEvent("cache_miss", Res.string.privacy_collected_cache_miss_desc),
                ),
        ),
        CollectedSection(
            title = Res.string.privacy_collected_section_reliability,
            events =
                listOf(
                    CollectedEvent("crash", Res.string.privacy_collected_crash_desc),
                    CollectedEvent("operation_failed", Res.string.privacy_collected_operation_failed_desc),
                ),
        ),
        CollectedSection(
            title = Res.string.privacy_collected_section_network,
            events =
                listOf(
                    CollectedEvent("proxy_configured", Res.string.privacy_collected_proxy_configured_desc),
                    CollectedEvent("proxy_used", Res.string.privacy_collected_proxy_used_desc),
                ),
        ),
        CollectedSection(
            title = Res.string.privacy_collected_section_updates,
            events =
                listOf(
                    CollectedEvent("update_installed", Res.string.privacy_collected_update_installed_desc),
                ),
        ),
    )

private val neverCollected =
    listOf(
        Res.string.privacy_collected_never_raw_queries,
        Res.string.privacy_collected_never_repo_identifiers,
        Res.string.privacy_collected_never_paths,
        Res.string.privacy_collected_never_account,
        Res.string.privacy_collected_never_tokens,
        Res.string.privacy_collected_never_ip,
        Res.string.privacy_collected_never_personal,
    )

private val serverSideItems =
    listOf(
        Res.string.privacy_collected_server_side_search_misses,
        Res.string.privacy_collected_server_side_logs,
    )

@Composable
fun PrivacyCollectedView(
    onBack: () -> Unit,
    onViewSource: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 8.dp, end = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(Res.string.privacy_collected_back),
                )
            }
            Spacer(Modifier.size(8.dp))
            Text(
                text = stringResource(Res.string.privacy_collected_title),
                style = MaterialTheme.typography.headlineSmall,
            )
        }
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 24.dp,
                end = 24.dp,
                top = 8.dp,
                bottom = 24.dp,
            ),
        ) {
            item {
                Text(
                    text = stringResource(Res.string.privacy_collected_intro),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
            }
            sections.forEach { section ->
                item { SectionHeader(stringResource(section.title)) }
                items(section.events) { event ->
                    EventRow(event.name, stringResource(event.description))
                }
            }
            item {
                Spacer(Modifier.height(8.dp))
                SectionHeader(stringResource(Res.string.privacy_collected_section_server_side))
                Text(
                    text = stringResource(Res.string.privacy_collected_server_side_intro),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
            }
            items(serverSideItems) { res ->
                BulletRow(stringResource(res))
            }
            item {
                Spacer(Modifier.height(8.dp))
                SectionHeader(stringResource(Res.string.privacy_collected_never_title))
            }
            items(neverCollected) { res ->
                BulletRow(stringResource(res))
            }
            item {
                Spacer(Modifier.height(16.dp))
                TextButton(
                    onClick = onViewSource,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(Res.string.privacy_collected_view_source))
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Spacer(Modifier.height(20.dp))
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
    )
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun EventRow(name: String, description: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontFamily = FontFamily.Monospace,
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun BulletRow(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = "•",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(end = 8.dp),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
