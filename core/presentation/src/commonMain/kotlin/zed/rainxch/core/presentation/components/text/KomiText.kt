package zed.rainxch.core.presentation.components.text

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import zed.rainxch.core.presentation.locals.LocalPersonality
import zed.rainxch.core.presentation.personality.classicPersonality
import zed.rainxch.core.presentation.personality.manga.MangaAccent
import zed.rainxch.core.presentation.personality.manga.MangaPaper
import zed.rainxch.core.presentation.personality.utils.PersonalityPreview

private val KomiTextRole.isHeading: Boolean
    get() =
        this == KomiTextRole.Display ||
            this == KomiTextRole.Title ||
            this == KomiTextRole.Stamp ||
            this == KomiTextRole.Label

@Composable
fun KomiText(
    text: String,
    modifier: Modifier = Modifier,
    role: KomiTextRole = KomiTextRole.Body,
    color: Color = Color.Unspecified,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    textAlign: TextAlign? = null,
    uppercase: Boolean? = null,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontWeight: FontWeight? = null,
) {
    val personality = LocalPersonality.current
    val roleStyle =
        when (role) {
            KomiTextRole.Display -> personality.type.display
            KomiTextRole.Title -> personality.type.title
            KomiTextRole.Stamp -> personality.type.stamp
            KomiTextRole.Body -> personality.type.body
            KomiTextRole.Label -> personality.type.label
            KomiTextRole.Mono -> personality.type.mono
        }
    val sizedStyle = if (fontSize.isSpecified) roleStyle.copy(fontSize = fontSize) else roleStyle
    val style = if (fontWeight != null) sizedStyle.copy(fontWeight = fontWeight) else sizedStyle
    val resolvedUppercase = uppercase ?: (role.isHeading && personality.type.uppercaseHeadings)
    Text(
        text = if (resolvedUppercase) text.uppercase() else text,
        modifier = modifier,
        color = if (color != Color.Unspecified) color else personality.colors.onSurface,
        maxLines = maxLines,
        overflow = overflow,
        textAlign = textAlign,
        style = style,
    )
}

@Preview
@Composable
private fun KomiTextRolesPreview() {
    PersonalityPreview {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            KomiText("Discover", role = KomiTextRole.Display)
            KomiText("Trending Now", role = KomiTextRole.Title)
            KomiText("No. 01", role = KomiTextRole.Stamp)
            KomiText(
                "Get up and running with large language models locally, in a single command.",
                role = KomiTextRole.Body,
            )
            KomiText("New Release", role = KomiTextRole.Label, color = LocalPersonality.current.colors.primary)
            KomiText("v0.5.4 · 174M", role = KomiTextRole.Mono)
        }
    }
}

@Preview
@Composable
private fun KomiTextNightPreview() {
    PersonalityPreview(paper = MangaPaper.NIGHT, accent = MangaAccent.SUN) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            KomiText("Discover", role = KomiTextRole.Display)
            KomiText("Trending Now", role = KomiTextRole.Title)
            KomiText("Get up and running with large language models locally.", role = KomiTextRole.Body)
            KomiText("v0.5.4", role = KomiTextRole.Mono)
        }
    }
}

@Preview
@Composable
private fun KomiTextNordPreview() {
    PersonalityPreview(paper = MangaPaper.NORD, accent = MangaAccent.FROST) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            KomiText("Discover", role = KomiTextRole.Display)
            KomiText("Trending Now", role = KomiTextRole.Title)
            KomiText("Get up and running with large language models locally.", role = KomiTextRole.Body)
        }
    }
}

@Preview
@Composable
private fun KomiTextEllipsisPreview() {
    PersonalityPreview {
        KomiText(
            "OllamaSuperLongRepositoryNameThatOverflowsTheLine",
            role = KomiTextRole.Title,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Preview
@Composable
private fun KomiTextClassicPreview() {
    PersonalityPreview(classicPersonality()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            KomiText("Discover", role = KomiTextRole.Display)
            KomiText("Trending Now", role = KomiTextRole.Title)
            KomiText(
                "Get up and running with large language models locally, in a single command.",
                role = KomiTextRole.Body,
            )
            KomiText("v0.5.4 · 174M", role = KomiTextRole.Mono)
        }
    }
}

@Preview
@Composable
private fun KomiTextClassicDarkPreview() {
    PersonalityPreview(classicPersonality(dark = true)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            KomiText("Discover", role = KomiTextRole.Display)
            KomiText("Trending Now", role = KomiTextRole.Title)
            KomiText("Get up and running with large language models locally.", role = KomiTextRole.Body)
        }
    }
}
