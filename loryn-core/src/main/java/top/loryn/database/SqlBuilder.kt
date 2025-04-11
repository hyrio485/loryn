package top.loryn.database

import top.loryn.expression.SqlExpression
import top.loryn.expression.SqlParam
import top.loryn.schema.Table
import top.loryn.support.PaginationParams
import top.loryn.support.SqlAppender
import top.loryn.support.WithAlias

open class SqlBuilder(
    keywords: Set<String>,
    private val uppercaseKeywords: Boolean,
) {
    protected val keywordsLc: Set<String> = keywords.mapTo(mutableSetOf()) { it.lowercase() }

    val attributes = mutableMapOf<String, Any?>()

    private val builder = StringBuilder()

    open fun start() = this
    open fun end() = this

    fun append(str: CharSequence) = also { builder.append(str) }
    fun append(char: Char) = also { builder.append(char) }
    fun append(any: Any) = also { builder.append(any) }

    open fun appendRef(ref: String) =
        if (ref.lowercase() in keywordsLc) {
            append('`').append(ref).append('`')
        } else {
            append(ref)
        }

    open fun appendKeyword(keyword: String) =
        append(if (uppercaseKeywords) keyword.uppercase() else keyword.lowercase())

    open fun <T> appendList(
        list: List<T>,
        params: MutableList<SqlParam<*>>,
        separator: String = ", ",
        block: SqlBuilder.(T, MutableList<SqlParam<*>>) -> Unit,
    ) = also {
        list.forEachIndexed { index, item ->
            if (index > 0) append(separator)
            block(item, params)
        }
    }

    open fun append(table: Table) = with(table) {
        category?.also { appendRef(it).append('.') }
        schema?.also { appendRef(it).append('.') }
        appendRef(tableName)
    }

    open fun append(sqlAppender: SqlAppender, params: MutableList<SqlParam<*>>) =
        sqlAppender.run { appendSql(params) }

    open fun append(expressions: List<SqlExpression<*>>, params: MutableList<SqlParam<*>>) =
        appendList(expressions, params) { expression, params ->
            append(expression, params)
        }

    open fun append(paginationParams: PaginationParams) = also {
        throw UnsupportedOperationException("SQL dialect does not support pagination")
    }

    /** 如果对象有别名，则调用 [block]，其参数为别名（非空）。 */
    open fun appendAlias(any: Any, block: SqlBuilder.(String) -> Unit) = also {
        if (any is WithAlias) {
            any.alias?.also { block(it) }
        }
    }

    fun build() = end().let { builder.toString() }
}
