package top.loryn.database

import top.loryn.expression.SqlExpression
import top.loryn.expression.SqlParam
import top.loryn.schema.Table

open class SqlBuilder(
    keywords: Set<String>,
    private val uppercaseKeywords: Boolean,
) {
    protected val keywordsLc: Set<String> = keywords.mapTo(mutableSetOf()) { it.lowercase() }

    private val builder = StringBuilder()

    open fun start() {}
    open fun end() {}

    fun append(str: CharSequence) = also { builder.append(str) }
    fun append(char: Char) = also { builder.append(char) }

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
        append(if (uppercaseKeywords) keyword.uppercase() else keyword)
    }

    open fun appendExpression(expression: SqlExpression<*>, params: MutableList<SqlParam<*>>) = also {
        expression.run { appendSql(params) }
    }

    fun build() = end().let { builder.toString() }
}
