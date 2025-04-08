package top.loryn.expression

enum class JoinType(val keyword: String?) {
    DEFAULT(null),
    INNER("INNER"),
    LEFT("LEFT"),
    RIGHT("RIGHT"),
    FULL("FULL"),
    CROSS("CROSS"),
}
