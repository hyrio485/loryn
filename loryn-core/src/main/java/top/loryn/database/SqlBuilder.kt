package top.loryn.database

import top.loryn.expression.SqlExpression
import top.loryn.expression.SqlParam
import top.loryn.schema.Table
import top.loryn.support.PaginationParams

open class SqlBuilder(
    keywords: Set<String>,
    private val uppercaseKeywords: Boolean,
) {
    protected val keywordsLc: Set<String> = keywords.mapTo(mutableSetOf()) { it.lowercase() }

    private val builder = StringBuilder()

    open fun start() = this
    open fun end() = this

    fun append(str: CharSequence) = also { builder.append(str) }
    fun append(char: Char) = also { builder.append(char) }
    fun append(any: Any) = also { builder.append(any) }

    open fun appendRef(ref: String) = also {
        if (ref.lowercase() in keywordsLc) {
            builder.append('`').append(ref).append('`')
        } else {
            builder.append(ref)
        }
    }

    open fun appendTable(table: Table<*>) = also {
        with(table) {
            category?.also { appendRef(it).append('.') }
            schema?.also { appendRef(it).append('.') }
            appendRef(tableName)
        }
    }

    open fun appendKeyword(keyword: String) = also {
        append(if (uppercaseKeywords) keyword.uppercase() else keyword.lowercase())
    }

    open fun appendExpression(expression: SqlExpression<*>, params: MutableList<SqlParam<*>>) = also {
        expression.run { appendSql(params) }
    }

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

    open fun appendExpressions(expressions: List<SqlExpression<*>>, params: MutableList<SqlParam<*>>) =
        appendList(expressions, params) { expression, params ->
            appendExpression(expression, params)
        }

    open fun appendPagination(paginationParams: PaginationParams) = also {
        throw UnsupportedOperationException("SQL dialect does not support pagination")
    }

    fun build() = end().let { builder.toString() }
}
