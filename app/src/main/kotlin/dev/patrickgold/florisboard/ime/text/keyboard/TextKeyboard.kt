/*
 * Copyright (C) 2021-2025 The FlorisBoard Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.patrickgold.florisboard.ime.text.keyboard

import dev.patrickgold.florisboard.ime.keyboard.Key
import dev.patrickgold.florisboard.ime.keyboard.Keyboard
import dev.patrickgold.florisboard.ime.keyboard.KeyboardMode
import dev.patrickgold.florisboard.ime.popup.PopupMapping
import kotlin.math.abs

class TextKeyboard(
    val arrangement: Array<Array<TextKey>>,
    override val mode: KeyboardMode,
    val extendedPopupMapping: PopupMapping?,
    val extendedPopupMappingDefault: PopupMapping?,
) : Keyboard() {
    val rowCount: Int
        get() = arrangement.size

    val keyCount: Int
        get() = arrangement.sumOf { it.size }

    override fun getKeyForPos(pointerX: Float, pointerY: Float): TextKey? {
        for (key in keys()) {
            if (key.touchBounds.contains(pointerX, pointerY)) {
                return key
            }
        }
        return null
    }

    override fun layout(
        keyboardWidth: Float,
        keyboardHeight: Float,
        desiredKey: Key,
        extendTouchBoundariesDownwards: Boolean,
    ) {
        if (arrangement.isEmpty()) return

        val desiredTouchBounds = desiredKey.touchBounds
        val desiredVisibleBounds = desiredKey.visibleBounds
        if (desiredTouchBounds.isEmpty() || desiredVisibleBounds.isEmpty()) return
        if (keyboardWidth.isNaN() || keyboardHeight.isNaN()) return
        val rowMarginH = abs(desiredTouchBounds.width - desiredVisibleBounds.width)
        val rowMarginV = (keyboardHeight - desiredTouchBounds.height * rowCount.toFloat()) / (rowCount - 1).coerceAtLeast(1).toFloat()

        for ((r, row) in rows().withIndex()) {
            val posY = (desiredTouchBounds.height + rowMarginV) * r
            val availableWidth = (keyboardWidth - rowMarginH) / desiredTouchBounds.width
            var requestedWidth = 0.0f
            var shrinkSum = 0.0f
            var growSum = 0.0f
            for (key in row) {
                requestedWidth += key.flayWidthFactor
                shrinkSum += key.flayShrink
                growSum += key.flayGrow
            }
            if (requestedWidth <= availableWidth) {
                // Requested with is smaller or equal to the available with, so we can grow
                val additionalWidth = availableWidth - requestedWidth
                var posX = rowMarginH / 2.0f
                for ((k, key) in row.withIndex()) {
                    val keyWidth = desiredTouchBounds.width * when (growSum) {
                        0.0f -> when (k) {
                            0, row.size - 1 -> key.flayWidthFactor + additionalWidth / 2.0f
                            else -> key.flayWidthFactor
                        }
                        else -> key.flayWidthFactor + additionalWidth * (key.flayGrow / growSum)
                    }
                    key.touchBounds.apply {
                        left = posX
                        top = posY
                        right = posX + keyWidth
                        bottom = posY + desiredTouchBounds.height
                    }
                    key.visibleBounds.apply {
                        left = key.touchBounds.left + abs(desiredTouchBounds.left - desiredVisibleBounds.left) + when {
                            growSum == 0.0f && k == 0 -> ((additionalWidth / 2.0f) * desiredTouchBounds.width)
                            else -> 0.0f
                        }
                        top = key.touchBounds.top + abs(desiredTouchBounds.top - desiredVisibleBounds.top)
                        right = key.touchBounds.right - abs(desiredTouchBounds.right - desiredVisibleBounds.right) - when {
                            growSum == 0.0f && k == row.size - 1 -> ((additionalWidth / 2.0f) * desiredTouchBounds.width)
                            else -> 0.0f
                        }
                        bottom = key.touchBounds.bottom - abs(desiredTouchBounds.bottom - desiredVisibleBounds.bottom)
                    }
                    posX += keyWidth
                    // After-adjust touch bounds for the row margin
                    key.touchBounds.apply {
                        if (k == 0) {
                            left = 0.0f
                        } else if (k == row.size - 1) {
                            right = keyboardWidth
                        }
                        if (extendTouchBoundariesDownwards && r + 1 == arrangement.size) {
                            bottom += height
                        }
                    }
                }
            } else {
                // Requested size too big, must shrink.
                val clippingWidth = requestedWidth - availableWidth
                var posX = rowMarginH / 2.0f
                for ((k, key) in row.withIndex()) {
                    val keyWidth = desiredTouchBounds.width * if (key.flayShrink == 0.0f) {
                        key.flayWidthFactor
                    } else {
                        key.flayWidthFactor - clippingWidth * (key.flayShrink / shrinkSum)
                    }
                    key.touchBounds.apply {
                        left = posX
                        top = posY
                        right = posX + keyWidth
                        bottom = posY + desiredTouchBounds.height
                    }
                    key.visibleBounds.apply {
                        left = key.touchBounds.left + abs(desiredTouchBounds.left - desiredVisibleBounds.left)
                        top = key.touchBounds.top + abs(desiredTouchBounds.top - desiredVisibleBounds.top)
                        right = key.touchBounds.right - abs(desiredTouchBounds.right - desiredVisibleBounds.right)
                        bottom = key.touchBounds.bottom - abs(desiredTouchBounds.bottom - desiredVisibleBounds.bottom)
                    }
                    posX += keyWidth
                    // After-adjust touch bounds for the row margin
                    key.touchBounds.apply {
                        if (k == 0) {
                            left = 0.0f
                        } else if (k == row.size - 1) {
                            right = keyboardWidth
                        }
                        if (extendTouchBoundariesDownwards && r + 1 == arrangement.size) {
                            bottom += height
                        }
                    }
                }
            }
        }
    }

    override fun keys(): Iterator<TextKey> {
        return TextKeyboardIterator(arrangement)
    }

    fun rows(): Iterator<Array<TextKey>> {
        return arrangement.iterator()
    }

    /**
     * Lays out the keyboard in split mode: each row is divided into two halves separated by a
     * horizontal gap of [splitGapWidth] pixels. The left half occupies
     * `(keyboardWidth - splitGapWidth) / 2` pixels starting at x=0, and the right half occupies
     * the same width starting at `(keyboardWidth + splitGapWidth) / 2`.
     *
     * The split point is chosen so that the accumulated [TextKey.flayWidthFactor] of the left
     * segment is as close as possible to half the total row width factor.
     */
    fun layoutSplit(
        keyboardWidth: Float,
        keyboardHeight: Float,
        desiredKey: Key,
        extendTouchBoundariesDownwards: Boolean,
        splitGapWidth: Float,
    ) {
        if (arrangement.isEmpty()) return
        val desiredTouchBounds = desiredKey.touchBounds
        val desiredVisibleBounds = desiredKey.visibleBounds
        if (desiredTouchBounds.isEmpty() || desiredVisibleBounds.isEmpty()) return
        if (keyboardWidth.isNaN() || keyboardHeight.isNaN()) return

        val halfWidth = (keyboardWidth - splitGapWidth) / 2f
        val rightOffset = halfWidth + splitGapWidth

        val rowMarginH = abs(desiredTouchBounds.width - desiredVisibleBounds.width)
        val rowMarginV = (keyboardHeight - desiredTouchBounds.height * rowCount.toFloat()) /
            (rowCount - 1).coerceAtLeast(1).toFloat()

        for ((r, row) in rows().withIndex()) {
            val posY = (desiredTouchBounds.height + rowMarginV) * r
            val isLastRow = extendTouchBoundariesDownwards && r + 1 == arrangement.size

            // Determine split index by accumulated width factors
            val totalWidthFactor = row.sumOf { it.flayWidthFactor.toDouble() }.toFloat()
            var accumulated = 0f
            var splitIndex = (row.size + 1) / 2
            for (k in row.indices) {
                accumulated += row[k].flayWidthFactor
                if (accumulated >= totalWidthFactor / 2f) {
                    splitIndex = k + 1
                    break
                }
            }

            if (row.size <= 1) {
                // Single-key row: place the key in the left half only
                layoutRowSegment(
                    row = row, from = 0, to = row.size,
                    posY = posY, segmentWidth = halfWidth,
                    startX = 0f, endX = halfWidth,
                    rowMarginH = rowMarginH,
                    desiredTouchBounds = desiredTouchBounds,
                    desiredVisibleBounds = desiredVisibleBounds,
                    extendTouchDownwards = isLastRow,
                )
                continue
            }

            splitIndex = splitIndex.coerceIn(1, row.size - 1)

            layoutRowSegment(
                row = row, from = 0, to = splitIndex,
                posY = posY, segmentWidth = halfWidth,
                startX = 0f, endX = halfWidth,
                rowMarginH = rowMarginH,
                desiredTouchBounds = desiredTouchBounds,
                desiredVisibleBounds = desiredVisibleBounds,
                extendTouchDownwards = isLastRow,
            )
            layoutRowSegment(
                row = row, from = splitIndex, to = row.size,
                posY = posY, segmentWidth = halfWidth,
                startX = rightOffset, endX = keyboardWidth,
                rowMarginH = rowMarginH,
                desiredTouchBounds = desiredTouchBounds,
                desiredVisibleBounds = desiredVisibleBounds,
                extendTouchDownwards = isLastRow,
            )
        }
    }

    /**
     * Positions keys in [row] from index [from] (inclusive) to [to] (exclusive) within a segment
     * of [segmentWidth] pixels that starts at [startX] and ends at [endX].
     */
    private fun layoutRowSegment(
        row: Array<TextKey>,
        from: Int,
        to: Int,
        posY: Float,
        segmentWidth: Float,
        startX: Float,
        endX: Float,
        rowMarginH: Float,
        desiredTouchBounds: dev.patrickgold.florisboard.lib.FlorisRect,
        desiredVisibleBounds: dev.patrickgold.florisboard.lib.FlorisRect,
        extendTouchDownwards: Boolean,
    ) {
        if (from >= to) return
        val availableWidth = (segmentWidth - rowMarginH) / desiredTouchBounds.width
        var requestedWidth = 0.0f
        var shrinkSum = 0.0f
        var growSum = 0.0f
        for (k in from until to) {
            requestedWidth += row[k].flayWidthFactor
            shrinkSum += row[k].flayShrink
            growSum += row[k].flayGrow
        }
        val segCount = to - from
        if (requestedWidth <= availableWidth) {
            val additionalWidth = availableWidth - requestedWidth
            var posX = startX + rowMarginH / 2.0f
            for (idx in from until to) {
                val key = row[idx]
                val k = idx - from
                val isFirst = k == 0
                val isLast = k == segCount - 1
                val keyWidth = desiredTouchBounds.width * when (growSum) {
                    0.0f -> when (k) {
                        0, segCount - 1 -> key.flayWidthFactor + additionalWidth / 2.0f
                        else -> key.flayWidthFactor
                    }
                    else -> key.flayWidthFactor + additionalWidth * (key.flayGrow / growSum)
                }
                key.touchBounds.apply {
                    left = posX
                    top = posY
                    right = posX + keyWidth
                    bottom = posY + desiredTouchBounds.height
                }
                key.visibleBounds.apply {
                    left = key.touchBounds.left + abs(desiredTouchBounds.left - desiredVisibleBounds.left) + when {
                        growSum == 0.0f && isFirst -> (additionalWidth / 2.0f) * desiredTouchBounds.width
                        else -> 0.0f
                    }
                    top = key.touchBounds.top + abs(desiredTouchBounds.top - desiredVisibleBounds.top)
                    right = key.touchBounds.right - abs(desiredTouchBounds.right - desiredVisibleBounds.right) - when {
                        growSum == 0.0f && isLast -> (additionalWidth / 2.0f) * desiredTouchBounds.width
                        else -> 0.0f
                    }
                    bottom = key.touchBounds.bottom - abs(desiredTouchBounds.bottom - desiredVisibleBounds.bottom)
                }
                posX += keyWidth
                key.touchBounds.apply {
                    if (isFirst) left = startX
                    if (isLast) right = endX
                    if (extendTouchDownwards) bottom += height
                }
            }
        } else {
            val clippingWidth = requestedWidth - availableWidth
            var posX = startX + rowMarginH / 2.0f
            for (idx in from until to) {
                val key = row[idx]
                val k = idx - from
                val isFirst = k == 0
                val isLast = k == segCount - 1
                val keyWidth = desiredTouchBounds.width * if (key.flayShrink == 0.0f) {
                    key.flayWidthFactor
                } else {
                    key.flayWidthFactor - clippingWidth * (key.flayShrink / shrinkSum)
                }
                key.touchBounds.apply {
                    left = posX
                    top = posY
                    right = posX + keyWidth
                    bottom = posY + desiredTouchBounds.height
                }
                key.visibleBounds.apply {
                    left = key.touchBounds.left + abs(desiredTouchBounds.left - desiredVisibleBounds.left)
                    top = key.touchBounds.top + abs(desiredTouchBounds.top - desiredVisibleBounds.top)
                    right = key.touchBounds.right - abs(desiredTouchBounds.right - desiredVisibleBounds.right)
                    bottom = key.touchBounds.bottom - abs(desiredTouchBounds.bottom - desiredVisibleBounds.bottom)
                }
                posX += keyWidth
                key.touchBounds.apply {
                    if (isFirst) left = startX
                    if (isLast) right = endX
                    if (extendTouchDownwards) bottom += height
                }
            }
        }
    }

    class TextKeyboardIterator internal constructor(
        private val arrangement: Array<Array<TextKey>>
    ) : Iterator<TextKey> {
        private var rowIndex: Int = 0
        private var keyIndex: Int = 0

        override fun hasNext(): Boolean {
            return rowIndex < arrangement.size && keyIndex < arrangement[rowIndex].size
        }

        override fun next(): TextKey {
            val next = arrangement[rowIndex][keyIndex]
            if (keyIndex + 1 == arrangement[rowIndex].size) {
                rowIndex++
                keyIndex = 0
            } else {
                keyIndex++
            }
            return next
        }
    }
}
