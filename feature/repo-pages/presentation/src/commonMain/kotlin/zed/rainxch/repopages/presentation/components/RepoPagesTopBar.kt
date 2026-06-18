package zed.rainxch.repopages.presentation.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.stringResource
import zed.rainxch.githubstore.core.presentation.res.Res
import zed.rainxch.githubstore.core.presentation.res.cd_back

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepoPagesTopBar(
    title: String,
    onBack: () -> Unit,
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp,
                ),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 4.dp),
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(Res.string.cd_back),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    )
}