package top.loryn.support

interface WithAlias {
    val alias: String

    companion object {
        fun Any.getAliasOrNull() = if (this is WithAlias) alias else null
    }
}
