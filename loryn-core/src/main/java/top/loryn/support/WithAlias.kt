package top.loryn.support

import top.loryn.database.SqlBuilder
import top.loryn.utils.SqlParamList

interface WithAlias : SqlAppender {
    // 设置为可空是为了后续创建匿名对象时不需要根据代理对象是否为WithAlias来判断是否实现此接口，如果为null则与没有alias的效果一样
    val alias: String?
    val original: SqlAppender

    override fun buildSql(builder: SqlBuilder, params: SqlParamList, ignoreAlias: Boolean) {
        builder.append(original, params, ignoreAlias = ignoreAlias)
    }

    companion object {
        fun Any.getAliasOrNull() = if (this is WithAlias) alias else null
    }
}
