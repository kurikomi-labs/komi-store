package zed.rainxch.details.presentation.components.sections

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import zed.rainxch.details.presentation.DetailsState
import zed.rainxch.details.presentation.model.LogResult
import zed.rainxch.details.presentation.utils.asText
import zed.rainxch.githubstore.core.presentation.res.*

fun LazyListScope.logs(state: DetailsState) {
    item {
        HorizontalDivider()

        Text(
            text = stringResource(Res.string.install_logs),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(vertical = 8.dp),
            fontWeight = FontWeight.Bold,
        )
    }

    itemsIndexed(
        items = state.installLogs,
        key = { index, log -> "$index|${log.timeIso}|${log.assetName}" },
        contentType = { _, _ -> "install_log" },
    ) { _, log ->
        Text(
            text = "> ${log.result.asText()}: ${log.assetName}",
            style =
                MaterialTheme.typography.labelSmall.copy(
                    fontStyle = FontStyle.Italic,
                ),
            color =
                if (log.result is LogResult.Error) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.outline
                },
        )
    }
}
