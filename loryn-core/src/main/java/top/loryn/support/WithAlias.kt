package top.loryn.support

interface WithAlias {
    // 设置为可空是为了后续创建匿名对象时不需要根据代理对象是否为WithAlias来判断是否实现此接口，如果为null则与没有alias的效果一样
    val alias: String?

    companion object {
        fun Any.getAliasOrNull() = if (this is WithAlias) alias else null
    }
}
