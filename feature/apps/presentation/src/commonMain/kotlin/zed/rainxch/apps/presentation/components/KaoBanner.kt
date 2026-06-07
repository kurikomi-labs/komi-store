package zed.rainxch.apps.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import zed.rainxch.core.presentation.components.buttons.GhsButton
import zed.rainxch.core.presentation.components.buttons.GhsButtonSize
import zed.rainxch.core.presentation.components.buttons.GhsButtonVariant
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import zed.rainxch.githubstore.core.presentation.res.Res
import zed.rainxch.githubstore.core.presentation.res.kao_banner_body
import zed.rainxch.githubstore.core.presentation.res.kao_banner_cta
import zed.rainxch.githubstore.core.presentation.res.kao_banner_dismiss_cd
import zed.rainxch.githubstore.core.presentation.res.kao_banner_title

@Composable
fun KaoBanner(
    onLearnMore: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer,
    ) {
        Column(
            modifier = Modifier.padding(start = 12.dp, top = 4.dp, end = 4.dp, bottom = 8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Shield,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.size(24.dp).padding(top = 10.dp),
                )

                Spacer(Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f).padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = stringResource(Res.string.kao_banner_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                    )

                    Text(
                        text = stringResource(Res.string.kao_banner_body),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(Res.string.kao_banner_dismiss_cd),
                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(end = 8.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                GhsButton(
                    onClick = onLearnMore,
                    label = stringResource(Res.string.kao_banner_cta),
                    variant = GhsButtonVariant.Text,
                    size = GhsButtonSize.Sm,
                )
            }
        }
    }
}
