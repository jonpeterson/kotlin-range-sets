package com.github.jonpeterson.kotlin.ranges

import spock.lang.Specification
import spock.lang.Unroll

@Unroll
class RangeSetTest extends Specification {

    private static kotlin.ranges.IntRange groovyToKotlinRange(IntRange range) {
        return new kotlin.ranges.IntRange(range.from, range.to)
    }

    private static List<kotlin.ranges.IntRange> groovyToKotlinRanges(List<IntRange> ranges) {
        return ranges.collect { range -> groovyToKotlinRange(range) }
    }

    def 'add: #ranges'() {
        expect:
        new IntRangeSet(groovyToKotlinRanges(ranges)) as List == groovyToKotlinRanges(expected)

        where:
        ranges                              | expected
        [3..5, 3..5]                        | [3..5]
        [3..5, 1..5]                        | [1..5]
        [3..5, 3..7]                        | [3..7]
        [3..5, 1..4]                        | [1..5]
        [3..5, 4..7]                        | [3..7]
        [3..5, 2..7]                        | [2..7]
        [2..7, 3..5]                        | [2..7]
        [3..5, 7..9, 4..8]                  | [3..9]
        [3..5, 8..9, 13..16, 10..11, 6..6]  | [3..6, 8..11, 13..16]
        [3..5, 7..9, 13..16, 20..25, 6..21] | [3..25]
        [2..4, 0..1, 1..1, 0..0, 2..3]      | [0..4]
        [3..5, 1..2]                        | [1..5]
    }

    def 'remove: #remove'() {
        expect:
        def set = new IntRangeSet(groovyToKotlinRanges([3..5, 7..9, 13..16]))
        def emptySet = new IntRangeSet()
        def range = groovyToKotlinRange(remove)
        set.remove(range) == removed
        emptySet.remove(range) == false
        set as List == groovyToKotlinRanges(expected)
        emptySet as List == []

        where:
        remove | removed | expected
        0..2   | false   | [3..5, 7..9, 13..16]
        6..6   | false   | [3..5, 7..9, 13..16]
        10..12 | false   | [3..5, 7..9, 13..16]
        1..3   | true    | [4..5, 7..9, 13..16]
        1..4   | true    | [5..5, 7..9, 13..16]
        1..5   | true    | [7..9, 13..16]
        1..6   | true    | [7..9, 13..16]
        1..7   | true    | [8..9, 13..16]
        5..7   | true    | [3..4, 8..9, 13..16]
        4..7   | true    | [3..3, 8..9, 13..16]
        3..7   | true    | [8..9, 13..16]
        14..15 | true    | [3..5, 7..9, 13..13, 16..16]
    }

    def 'retainAll: #retain'() {
        expect:
        def set = new IntRangeSet(groovyToKotlinRanges([3..7, 12..16, 22..27]))
        def emptySet = new IntRangeSet()
        def ranges = groovyToKotlinRanges(retain)
        set.retainAll(ranges) == changed
        emptySet.retainAll(ranges) == false
        set as List == groovyToKotlinRanges(expected)
        emptySet as List == []

        where:
        retain                  | changed | expected
        [3..7]                  | true    | [3..7]
        [2..7]                  | true    | [3..7]
        [3..8]                  | true    | [3..7]
        [2..8]                  | true    | [3..7]
        [3..27]                 | false   | [3..7, 12..16, 22..27]
        [2..28]                 | false   | [3..7, 12..16, 22..27]
        [4..26]                 | true    | [4..7, 12..16, 22..26]
        [14..23]                | true    | [14..16, 22..23]

        [3..7, 12..16, 22..27]  | false   | [3..7, 12..16, 22..27]
        [4..13, 15..24]         | true    | [4..7, 12..13, 15..16, 22..24]
        [2..15, 13..26, 22..28] | false   | [3..7, 12..16, 22..27]
        [4..15, 13..26, 22..26] | true    | [4..7, 12..16, 22..26]
    }
}
