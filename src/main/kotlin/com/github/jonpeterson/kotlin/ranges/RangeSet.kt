/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Jon Peterson
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.github.jonpeterson.kotlin.ranges

import java.util.*

/**
 * Base class for implementing a [MutableSet] of [ClosedRange]s.
 *
 * Ranges in the set will always be in a normalized state. This means that ranges are kept in order and overlapping or
 * adjacent ranges will be joined.
 *
 * Some operations are not designed to be thread-safe. Wrap this in [Collections.synchronizedSet] if thread-safety is
 * needed.
 *
 * @param T the type of ranges in this set
 */
abstract class RangeSet<T : Comparable<T>> : MutableSet<ClosedRange<T>>, Cloneable {
    private val ranges = TreeSet<ClosedRange<T>>(Comparator { a, b ->
        when {
            (a.endInclusive < b.start) -> -1 // a strictly less than b
            (b.endInclusive < a.start) -> 1  // a strictly greater than b
            else -> 0  // the ranges overlap
        }
    })

    /**
     * Creates a set with no ranges.
     */
    constructor()

    /**
     * Creates a set with ranges added.
     *
     * @property ranges ranges to normalize and add
     */
    constructor(ranges: List<ClosedRange<T>>) {
        addAll(ranges)
    }

    /**
     * Creates a set with ranges shallow-copied from another [RangeSet].
     *
     * @property rangeSet set to copy ranges from
     */
    protected constructor(rangeSet: RangeSet<T>) {
        ranges.addAll(rangeSet)
    }

    /**
     * @return number of ranges
     */
    override val size: Int
        get() = ranges.size

    /**
     * Checks if the specified value is present in the set.
     *
     * Examples:
     * ```
     * assert(IntRangeSet(listOf(5..8, 11..16)).contains(6) == true)
     * assert(IntRangeSet(listOf(5..8, 11..16)).contains(9) == false)
     * ```
     *
     * @param value value to determine presence of
     * @return whether the value is contained within the set
     */
    fun containsValue(value: T): Boolean = ranges.any { it.contains(value) }

    /**
     * Checks if all values in the specified range are present in the set.
     *
     * Examples:
     * ```
     * assert(IntRangeSet(listOf(5..8, 11..16)).contains(6..7) == true)
     * assert(IntRangeSet(listOf(5..8, 11..16)).contains(6..12) == false)
     * ```
     *
     * @param element range of values to determine presence of
     * @return whether all values are all contained within the set
     */
    override fun contains(element: ClosedRange<T>): Boolean {
        for(range in ranges)
            if(element.start.compareTo(range.start) >= 0 && element.endInclusive.compareTo(range.endInclusive) <= 0)
                return true

        return false
    }

    /**
     * Checks if all values in the specified ranges are present in the set.
     *
     * Examples:
     * ```
     * assert(IntRangeSet(listOf(5..8, 11..16)).containsAll(listOf(6..7, 14..15)) == true)
     * assert(IntRangeSet(listOf(5..8, 11..16)).containsAll(listOf(6..12, 14..15)) == false)
     * ```
     *
     * @param elements ranges of values to determine presence of
     * @return whether all values are all contained within the set
     */
    override fun containsAll(elements: Collection<ClosedRange<T>>): Boolean = elements.all { contains(it) }

    /**
     * @return whether the set contains any values
     */
    override fun isEmpty(): Boolean = ranges.isEmpty()

    /**
     * @return a view into ranges containing the subset that overlap with the
     * specified (inclusive) range
     */
    private fun overlap(start: T, end: T): SortedSet<ClosedRange<T>> {
        val startRange = start..start
        val endRange = end..end
        return ranges.headSet(endRange, true).tailSet(startRange)
    }

    /**
     * Adds range of values to the set; addition set logic.
     *
     * Examples:
     * ```
     * assert(IntRangeSet(listOf(7..11)).apply { add(3..5) }.toList() == listOf(3..5, 7..11))
     * assert(IntRangeSet(listOf(5..11)).apply { add(7..10) }.toList() == listOf(5..11))
     * assert(IntRangeSet(listOf(5..8, 11..16)).apply { add(7..10) }.toList() == listOf(5..16))
     * ```
     *
     * @param element range of values to add
     * @return whether any values were added
     */
    override fun add(element: ClosedRange<T>): Boolean {
        // Expand the range by 1 in each direction when computing the overlap to enable coalescing with adjacent ranges.
        val subRanges = overlap(decrementValue(element.start), incrementValue(element.endInclusive))
        if (subRanges.isEmpty()) {
            ranges.add(element)
        } else {
            val firstStart = subRanges.first().start
            val newStart = firstStart.coerceAtMost(element.start)
            val lastEnd = subRanges.last().endInclusive
            val newEnd = lastEnd.coerceAtLeast(element.endInclusive)
            if (subRanges.size == 1 && newStart == firstStart && newEnd == lastEnd) {
                return false
            }
            subRanges.clear()
            ranges.add(createRange(newStart, newEnd))
        }
        return true
    }

    /**
     * Adds ranges of values to the set; addition set logic.
     *
     * Examples:
     * ```
     * assert(IntRangeSet(listOf(5..21)).apply { addAll(listOf(7..10, 12..15)) }.toList() == listOf(5..21))
     * assert(IntRangeSet(listOf(5..8, 11..16)).apply { addAll(listOf(7..10, 14..20)) }.toList() == listOf(5..20))
     * assert(IntRangeSet(listOf(11..16)).apply { addAll(listOf(7..10, 18..21)) }.toList() == listOf(7..16, 18..21))
     * ```
     *
     * @param elements ranges of values to add
     * @return whether any values were added
     */
    override fun addAll(elements: Collection<ClosedRange<T>>): Boolean {
        return elements.map { add(it) }.any()
    }

    /**
     * Removes all values from the set.
     */
    override fun clear() {
        ranges.clear()
    }

    /**
     * Removes a range of values from the set; subtraction set logic.
     *
     * Examples:
     * ```
     * assert(IntRangeSet(listOf(5..21)).apply { remove(7..15) }.toList() == listOf(5..6, 16..21))
     * assert(IntRangeSet(listOf(5..9, 13..21)).apply { remove(7..15) }.toList() == listOf(5..6, 16..21))
     * assert(IntRangeSet(listOf(5..9, 13..21)).apply { remove(3..28) }.isEmpty())
     * ```
     *
     * @param element range of values to remove
     * @return whether any values were removed
     */
    override fun remove(element: ClosedRange<T>): Boolean {
        val subRange = overlap(element.start, element.endInclusive)
        if (subRange.isEmpty()) {
            return false
        }

        val firstStart = subRange.first().start
        val lastEnd = subRange.last().endInclusive
        subRange.clear()
        if (firstStart < element.start) ranges.add(createRange(firstStart, decrementValue(element.start)))
        if (element.endInclusive < lastEnd) ranges.add(createRange(incrementValue(element.endInclusive), lastEnd))

        return true
    }

    /**
     * Removes ranges of values from the set; subtraction set logic.
     *
     * Examples:
     * ```
     * assert(IntRangeSet(listOf(5..21)).apply { removeAll(listOf(7..15, 12..17, 20..20)) }.toList() == listOf(5..6, 18..19, 21..21))
     * assert(IntRangeSet(listOf(5..9, 13..21)).apply { removeAll(listOf(7..15, 19..23)) }.toList() == listOf(5..6, 16..18))
     * ```
     *
     * @param elements ranges of values to remove
     * @return whether any values were removed
     */
    override fun removeAll(elements: Collection<ClosedRange<T>>): Boolean {
        return elements.map { remove(it) }.any { it }
    }

    /**
     * Removes all values from set except those contained in the specified range; intersection set logic.
     *
     * Examples:
     * ```
     * assert(IntRangeSet(listOf(5..21)).apply { retain(7..15) }.toList() == listOf(7..15))
     * assert(IntRangeSet(listOf(5..9, 13..21)).apply { retain(7..15) }.toList() == listOf(7..9, 13..15))
     * assert(IntRangeSet(listOf(5..9, 13..21)).apply { retain(2..25) }.toList() == listOf(5..9, 13..21))
     * ```
     *
     * @param element range of values to keep
     * @return whether any values were removed
     */
    fun retain(element: ClosedRange<T>): Boolean {
        return retainAll(empty().apply { add(element) })
    }

    /**
     * Removes all values from set except those contained in the specified ranges; intersection set logic.
     *
     * Examples:
     * ```
     * assert(IntRangeSet(listOf(5..21)).apply { retainAll(listOf(7..10, 12..15)) }.toList() == listOf(7..10, 12..15))
     * assert(IntRangeSet(listOf(5..9, 13..21)).apply { retainAll(listOf(7..15, 18..25)) }.toList() == listOf(7..9, 13..15, 18..21))
     * ```
     *
     * @param elements ranges of values to keep
     * @return whether any values were removed
     */
    override fun retainAll(elements: Collection<ClosedRange<T>>): Boolean {
        if (ranges.isEmpty()) return false

        if (elements.isEmpty()) {
            ranges.clear()
            return true
        }

        // Both ranges and elements are non-empty by this point.

        // Coalesce and sort incoming ranges.
        val thoseRanges: RangeSet<T> = if (elements is RangeSet) {
            elements
        } else {
            empty().apply { addAll(elements) }
        }
        return retainAll(thoseRanges)
    }

    /**
     * Removes all values from set except those contained in the specified ranges; intersection set logic.
     *
     * Examples:
     * ```
     * assert(IntRangeSet(listOf(5..21)).apply { retainAll(listOf(7..10, 12..15)) }.toList() == listOf(7..10, 12..15))
     * assert(IntRangeSet(listOf(5..9, 13..21)).apply { retainAll(listOf(7..15, 18..25)) }.toList() == listOf(7..9, 13..15, 18..21))
     * ```
     *
     * @param elements ranges of values to keep
     * @return whether any values were removed
     */
    fun retainAll(elements: RangeSet<T>): Boolean {
        val toAdd = mutableListOf<ClosedRange<T>>()
        val thisIt = ranges.iterator()
        val thatIt = elements.iterator()
        var thatRange: ClosedRange<T>? = thatIt.next()
        var result = false
        while (thisIt.hasNext()) {
            val thisRange = thisIt.next()

            while (thatRange != null && thatRange.endInclusive < thisRange.start)
                thatRange = if (thatIt.hasNext()) thatIt.next() else null

            if (thatRange == null || thisRange.start < thatRange.start || thisRange.endInclusive > thatRange.endInclusive) {
                result = true
                thisIt.remove()
                while (thatRange != null && thatRange.start <= thisRange.endInclusive) {
                    toAdd.add(createRange(
                            thatRange.start.coerceAtLeast(thisRange.start),
                            thatRange.endInclusive.coerceAtMost(thisRange.endInclusive)))
                    if (thatRange.endInclusive > thisRange.endInclusive) break
                    thatRange = if (thatIt.hasNext()) thatIt.next() else null
                }
            }
        }
        ranges.addAll(toAdd)
        return result
    }

    /**
     * Creates a new [RangeSet] containing values not present in this set within the specified range.
     *
     * Examples:
     * ```
     * assert(IntRangeSet(listOf(3..7, 12..16, 22..27, 29..32)).difference(5..20).toList() == listOf(8..11, 17..20)
     * assert(IntRangeSet(listOf(3..7, 12..16, 22..27, 29..32)).difference(1..35).toList() == listOf(1..2, 8..11, 17..21, 28..28, 33..35)
     * ```
     *
     * @return new set containing values not present in this set within the specified range
     */
    fun difference(element: ClosedRange<T>): RangeSet<T> {
        return differenceAll(listOf(element))
    }

    /**
     * Creates a new [RangeSet] containing values not present in this set within the specified ranges.
     *
     * Examples:
     * ```
     * assert(IntRangeSet(listOf(3..7, 12..16, 22..27, 29..32)).differenceAll(listOf()).isEmpty()
     * assert(IntRangeSet(listOf(3..7, 12..16, 22..27, 29..32)).differenceAll(listOf(5..14, 26..30)).toList() == listOf(8..11, 28..28)
     * assert(IntRangeSet(listOf(3..7, 12..16, 22..27, 29..32)).differenceAll(listOf(1..35)).toList() == listOf(1..2, 8..11, 17..21, 28..28, 33..35)
     * ```
     *
     * @return new set containing values not present in this set within the specified range
     */
    fun differenceAll(elements: Collection<ClosedRange<T>>): RangeSet<T> {
        val difference = clone()
        difference.clear()
        difference.ranges.addAll(elements)
        difference.removeAll(ranges)
        return difference
    }

    /**
     * Creates a new [RangeSet] containing missing values between the ranges of this set.
     *
     * Examples:
     * ```
     * assert(IntRangeSet(listOf(5..21)).gaps().isEmpty())
     * assert(IntRangeSet(listOf(3..7, 12..16, 22..27, 29..32)).gaps() == listOf(8..11, 17..21, 28..28))
     * ```
     *
     * @return new set containing missing values between the ranges of this set
     */
    fun gaps(): RangeSet<T> {
        return if(ranges.isEmpty()) clone().apply { clear() } else difference(ranges.first.endInclusive..ranges.last.start)
    }

    override fun iterator(): MutableIterator<ClosedRange<T>> = ranges.iterator()

    override fun hashCode(): Int {
        return ranges.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return this === other || (other is RangeSet<*> && ranges == other.ranges)
    }

    protected abstract fun createRange(start: T, endInclusive: T): ClosedRange<T>

    protected abstract fun incrementValue(value: T): T

    protected abstract fun decrementValue(value: T): T

    protected abstract fun empty(): RangeSet<T>

    override abstract fun clone(): RangeSet<T>
}
