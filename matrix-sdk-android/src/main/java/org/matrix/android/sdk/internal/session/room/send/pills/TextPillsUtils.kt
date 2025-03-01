/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.matrix.android.sdk.internal.session.room.send.pills

import android.text.SpannableString
import org.matrix.android.sdk.api.extensions.orTrue
import org.matrix.android.sdk.api.session.permalinks.PermalinkService
import org.matrix.android.sdk.api.session.room.model.RoomEmoteContent.Companion.USAGE_STICKER
import org.matrix.android.sdk.api.session.room.send.MatrixItemSpan
import org.matrix.android.sdk.api.util.MatrixItem
import java.util.Collections
import javax.inject.Inject

/**
 * Utility class to detect special span in CharSequence and turn them into
 * formatted text to send them as a Matrix messages.
 */
internal class TextPillsUtils @Inject constructor(
        private val mentionLinkSpecComparator: MentionLinkSpecComparator,
        private val permalinkService: PermalinkService
) {

    /**
     * Detects if transformable spans are present in the text.
     * @return the transformed String or null if no Span found
     */
    fun processSpecialSpansToHtml(text: CharSequence): String? {
        return transformPills(text, permalinkService.createMentionSpanTemplate(PermalinkService.SpanTemplateType.HTML))
    }

    /**
     * Detects if transformable spans are present in the text.
     * @return the transformed String or null if no Span found
     */
    fun processSpecialSpansToMarkdown(text: CharSequence): String? {
        return transformPills(text, permalinkService.createMentionSpanTemplate(PermalinkService.SpanTemplateType.MARKDOWN))
    }

    private fun transformPills(text: CharSequence, template: String): String? {
        val spannableString = SpannableString.valueOf(text)
        val pills = spannableString
                ?.getSpans(0, text.length, MatrixItemSpan::class.java)
                ?.map { MentionLinkSpec(it, spannableString.getSpanStart(it), spannableString.getSpanEnd(it)) }
                // we use the raw text for @room notification instead of a link
                ?.filterNot { it.span.matrixItem is MatrixItem.EveryoneInRoomItem }
                ?.toMutableList()
                ?.takeIf { it.isNotEmpty() }
                ?: return null

        // we need to prune overlaps!
        pruneOverlaps(pills)

        return buildString {
            var currIndex = 0
            pills.forEachIndexed { _, (urlSpan, start, end) ->
                // We want to replace with the pill with a html link
                // append text before pill
                append(text, currIndex, start)
                // append the pill
                // Different handling necessary for custom emotes
                if (urlSpan.matrixItem is MatrixItem.EmoteItem) {
                    // Note we use the same template for both HTML and MARKDOWN conversion. We do this since markdown inline images are not mighty enough
                    // for custom emotes (i.e., that would drop the data-mx-emoticon tag, which we want to keep). But we can use inline html in markdown.
                    val imgHtml = "<img data-mx-emoticon height=\"32\" src=\"${urlSpan.matrixItem.avatarUrl}\" title=\":${urlSpan.matrixItem.displayName}:\" alt=\":${urlSpan.matrixItem.displayName}:\">"
                    append(imgHtml)
                } else {
                    append(String.format(template, urlSpan.matrixItem.id, urlSpan.matrixItem.id))
                }
                currIndex = end
            }
            // append text after the last pill
            append(text, currIndex, text.length)
        }
    }

    private fun pruneOverlaps(links: MutableList<MentionLinkSpec>) {
        Collections.sort(links, mentionLinkSpecComparator)
        var len = links.size
        var i = 0
        while (i < len - 1) {
            val a = links[i]
            val b = links[i + 1]
            var remove = -1

            // test if there is an overlap
            if (b.start in a.start until a.end) {
                when {
                    b.end <= a.end ->
                        // b is inside a -> b should be removed
                        remove = i + 1
                    a.end - a.start > b.end - b.start ->
                        // overlap and a is bigger -> b should be removed
                        remove = i + 1
                    a.end - a.start < b.end - b.start ->
                        // overlap and a is smaller -> a should be removed
                        remove = i
                }

                if (remove != -1) {
                    links.removeAt(remove)
                    len--
                    continue
                }
            }
            i++
        }
    }
}

fun CharSequence.requiresFormattedMessage(): Boolean {
    val spannableString = SpannableString.valueOf(this)
    val pills = spannableString
            ?.getSpans(0, length, MatrixItemSpan::class.java)
            ?.map { MentionLinkSpec(it, spannableString.getSpanStart(it), spannableString.getSpanEnd(it)) }
            // We cannot send emotes without markdown/formatted messages
            ?.filter { it.span.matrixItem is MatrixItem.EmoteItem }
            ?: return false
    return pills.isNotEmpty()
}

fun CharSequence.asSticker(): MatrixItem.EmoteItem? {
    val spannableString = SpannableString.valueOf(this)
    val emotes = spannableString
            ?.getSpans(0, length, MatrixItemSpan::class.java)
            ?.map { MentionLinkSpec(it, spannableString.getSpanStart(it), spannableString.getSpanEnd(it)) }
            ?.filter { it.span.matrixItem is MatrixItem.EmoteItem }
    if (emotes?.size == 1) {
        val emote = emotes[0]
        if (emote.start != 0 || emote.end != length) {
            return null
        }
        val emoteItem = emote.span.matrixItem as MatrixItem.EmoteItem
        val emoteImage = emoteItem.emoteImage
        if (emoteImage.usage?.contains(USAGE_STICKER).orTrue()) {
            return emoteItem
        }
    }
    return null
}
