package zed.rainxch.tweaks.presentation.feedback.util

import io.ktor.http.URLBuilder
import io.ktor.http.encodeURLParameter
import zed.rainxch.tweaks.presentation.feedback.FeedbackState
import zed.rainxch.tweaks.presentation.feedback.model.FeedbackCategory
import zed.rainxch.tweaks.presentation.feedback.model.FeedbackChannel

object FeedbackComposer {
    const val FEEDBACK_EMAIL = "hello@github-store.org"
    const val FEEDBACK_REPO = "OpenHub-Store/GitHub-Store"
    const val BODY_MAX_CHARS = 7_500

    fun composeUrl(state: FeedbackState, channel: FeedbackChannel): String {
        val title = state.title.trim()
        val body = composeBody(state, channel)
        return when (channel) {
            FeedbackChannel.EMAIL -> buildMailto(title, body)
            FeedbackChannel.GITHUB -> buildGithubIssueUrl(title, body, state)
        }
    }

    fun composeBody(state: FeedbackState, channel: FeedbackChannel): String {
        val builder = StringBuilder()

        builder.appendSection("Description", state.description)

        when (state.category) {
            FeedbackCategory.BUG -> {
                builder.appendSection("Steps to reproduce", state.stepsToReproduce)
                builder.appendSection("Expected vs actual", state.expectedActual)
            }
            FeedbackCategory.FEATURE_REQUEST -> {
                builder.appendSection("Use case", state.useCase)
                builder.appendSection("Proposed solution", state.proposedSolution)
            }
            FeedbackCategory.CHANGE_REQUEST -> {
                builder.appendSection("Current behaviour", state.currentBehaviour)
                builder.appendSection("Desired behaviour", state.desiredBehaviour)
            }
            FeedbackCategory.OTHER -> {   }
        }

        if (state.attachDiagnostics) {
            state.diagnostics?.let { d ->
                builder.append("\n\n---\n**Diagnostics**\n")
                builder.append("- App: GitHub Store v").append(d.appVersion).append('\n')
                builder.append("- Platform: ").append(d.platform).append(' ').append(d.osVersion).append('\n')
                builder.append("- Locale: ").append(d.locale).append('\n')
                builder.append("- Theme: ").append(d.themePalette).append(" / ").append(d.themeMode).append('\n')
                d.installerType?.let { builder.append("- Installer: ").append(it).append('\n') }
                if (channel == FeedbackChannel.GITHUB) {
                    d.githubUsername?.let { builder.append("- GitHub user: @").append(it).append('\n') }
                }
            }
        }

        return builder.toString().truncateToCap()
    }

    private fun StringBuilder.appendSection(title: String, content: String) {
        val trimmed = content.trim()
        if (trimmed.isEmpty()) return
        if (isNotEmpty()) append("\n\n")
        append("## ").append(title).append('\n').append(trimmed)
    }

    private fun String.truncateToCap(): String {
        if (length <= BODY_MAX_CHARS) return this
        val suffix = "\n\n…[truncated]"
        return substring(0, BODY_MAX_CHARS - suffix.length) + suffix
    }

    private fun buildMailto(title: String, body: String): String {
        val subject = title.encodeURLParameter()
        val encodedBody = body.encodeURLParameter()
        return "mailto:$FEEDBACK_EMAIL?subject=$subject&body=$encodedBody"
    }

    private fun buildGithubIssueUrl(title: String, body: String, state: FeedbackState): String {
        val labels = listOf(state.category.githubLabel, state.topic.githubLabel).joinToString(",")
        return URLBuilder("https://github.com/$FEEDBACK_REPO/issues/new").apply {
            parameters.append("title", title)
            parameters.append("body", body)
            parameters.append("labels", labels)
        }.buildString()
    }
}
