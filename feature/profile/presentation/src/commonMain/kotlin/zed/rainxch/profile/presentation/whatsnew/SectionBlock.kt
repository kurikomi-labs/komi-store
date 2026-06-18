package zed.rainxch.profile.presentation.whatsnew

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import zed.rainxch.core.domain.model.announcement.WhatsNewSection
import zed.rainxch.core.domain.model.announcement.WhatsNewSectionType
import zed.rainxch.githubstore.core.presentation.res.Res
import zed.rainxch.githubstore.core.presentation.res.whats_new_section_fixed
import zed.rainxch.githubstore.core.presentation.res.whats_new_section_heads_up
import zed.rainxch.githubstore.core.presentation.res.whats_new_section_improved
import zed.rainxch.githubstore.core.presentation.res.whats_new_section_new

@Composable
fun SectionBlock(section: WhatsNewSection) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel(type = section.type)

        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                section.bullets.forEach { bullet ->
                    BulletRow(text = bullet)
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(type: WhatsNewSectionType) {
    val (label, color) =
        when (type) {
            WhatsNewSectionType.NEW -> {
                stringResource(Res.string.whats_new_section_new) to
                        MaterialTheme.colorScheme.primary
            }

            WhatsNewSectionType.IMPROVED -> {
                stringResource(Res.string.whats_new_section_improved) to
                        MaterialTheme.colorScheme.tertiary
            }

            WhatsNewSectionType.FIXED -> {
                stringResource(Res.string.whats_new_section_fixed) to
                        MaterialTheme.colorScheme.secondary
            }

            WhatsNewSectionType.HEADS_UP -> {
                stringResource(Res.string.whats_new_section_heads_up) to
                        MaterialTheme.colorScheme.error
            }
        }

    Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = color,
    )
}

@Composable
private fun BulletRow(text: String) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = "•",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
