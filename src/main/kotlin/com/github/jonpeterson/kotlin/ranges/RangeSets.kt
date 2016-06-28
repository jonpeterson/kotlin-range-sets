package com.github.jonpeterson.kotlin.ranges

class IntRangeSet : RangeSet<Int> {

    constructor() : super()

    constructor(ranges: List<IntRange>) : super(ranges)

    protected constructor(rangeSet: IntRangeSet) : super(rangeSet)

    override fun createRange(start: Int, endInclusive: Int): IntRange = IntRange(start, endInclusive)

    override fun incrementValue(value: Int): Int = value + 1

    override fun decrementValue(value: Int): Int = value - 1

    override fun clone(): RangeSet<Int> = IntRangeSet(this)
}

class LongRangeSet : RangeSet<Long> {

    constructor() : super()

    constructor(ranges: List<LongRange>) : super(ranges)

    protected constructor(rangeSet: LongRangeSet) : super(rangeSet)

    override fun createRange(start: Long, endInclusive: Long): LongRange = LongRange(start, endInclusive)

    override fun incrementValue(value: Long): Long = value + 1

    override fun decrementValue(value: Long): Long = value - 1

    override fun clone(): RangeSet<Long> = LongRangeSet(this)
}

class CharRangeSet : RangeSet<Char> {

    constructor() : super()

    constructor(ranges: List<CharRange>) : super(ranges)

    protected constructor(rangeSet: CharRangeSet) : super(rangeSet)

    override fun createRange(start: Char, endInclusive: Char): CharRange = CharRange(start, endInclusive)

    override fun incrementValue(value: Char): Char = value + 1

    override fun decrementValue(value: Char): Char = value - 1

    override fun clone(): RangeSet<Char> = CharRangeSet(this)
}