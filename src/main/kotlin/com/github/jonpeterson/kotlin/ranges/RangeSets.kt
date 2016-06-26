package com.github.jonpeterson.kotlin.ranges

// TODO document
class IntRangeSet : RangeSet<Int> {

    constructor() : super()

    constructor(ranges: List<IntRange>) : super(ranges)

    protected constructor(rangeSet: IntRangeSet) : super(rangeSet)

    override fun createRange(start: Int, endInclusive: Int): IntRange = IntRange(start, endInclusive)

    override fun incrementValue(value: Int): Int = value + 1

    override fun decrementValue(value: Int): Int = value - 1

    override fun clone(): RangeSet<Int> = IntRangeSet(this)
}