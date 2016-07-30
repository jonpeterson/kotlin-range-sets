# Kotlin Range Sets

Implementation of set logic on the underlying values of Kotlin's Range classes.

## Compatibility

Compiled for Java 6 and Kotlin 1.0.2.

## Getting Started with Gradle

```groovy
dependencies {
    compile 'com.github.jonpeterson:kotlin-range-sets:1.0.0'
}
```

## Getting Started with Maven

```xml
<dependency>
    <groupId>com.github.jonpeterson</groupId>
    <artifactId>kotlin-range-sets</artifactId>
    <version>1.0.0</version>
</dependency>
```

## Documentation

[Dokka](https://jonpeterson.github.io/docs/kotlin-range-sets/1.0.0/index.html)

## Examples

```kt
// create set with overlapped, out-of-order ranges
val set = IntRangeSet(5..9, 3..7, 13..16)

// set is kept in normalized form
assert(set == IntRangeSet(3..9, 13..16))


// set doesn't contain 10
assert(set.containsValue(10) == false)

// set contains 4, 5, 6, 7, 8
assert(set.contains(4..8) == true)

// set is missing 10, 11, 12
assert(set.contains(8..14) == false)


// nothing was added
assert(set.add(7..9) == false)

// 17 was added
assert(set.add(15..17) == true)
assert(set == IntRangeSet(3..9, 13..17))


// nothing was removed
assert(set.remove(10..12) == false)

// 9 was removed
assert(set.remove(9..11) == true)
assert(set == IntRangeSet(3..8, 13..17))


// everything was retained (nothing changed)
assert(set.retain(2..20) == false)

// 3, 4 were dropped
assert(set.retain(5..18) == true)
assert(set == IntRangeSet(5..8, 13..17))


// created new range set with the difference
assert(set.difference(7..19) == IntRangeSet(9..12, 18..19))

// created new range set with the gaps between the existing ranges
assert(set.gaps() == IntRangeSet(9..12))
```