package top.loryn.database

import top.loryn.schema.Table
import top.loryn.support.PaginationParams
import top.loryn.support.SqlAppender
import top.loryn.support.WithAlias.Companion.getAliasOrNull
import top.loryn.utils.SqlParamList

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
    fun append(int: Int) = also { builder.append(int) }

    open fun appendRef(ref: String) =
        if (ref.lowercase() in keywordsLc) {
            append('`').append(ref).append('`')
        } else {
            append(ref)
        }

    open fun appendKeyword(keyword: String) =
        append(if (uppercaseKeywords) keyword.uppercase() else keyword.lowercase())

    open fun <T> appendList(
        iterable: Iterable<T>,
        params: SqlParamList,
        separator: String = ", ",
        block: SqlBuilder.(T, SqlParamList) -> Unit,
    ) = also {
        iterable.forEachIndexed { index, item ->
            if (index > 0) append(separator)
            block(item, params)
        }
    }

    open fun appendKeywords(operators: List<String>, params: SqlParamList, separator: String = " ") =
        appendList(operators, params, separator) { operator, _ ->
            appendKeyword(operator)
        }

    open fun appendTable(table: Table) = with(table) {
        category?.also { appendRef(it).append('.') }
        schema?.also { appendRef(it).append('.') }
        appendRef(tableName)
    }

    open fun append(
        appender: SqlAppender,
        params: SqlParamList,
        addParentheses: Boolean = false,
        ignoreAlias: Boolean = false,
    ) = also {
        if (addParentheses) append('(')
        val alias = appender.getAliasOrNull()
        if (alias == null || ignoreAlias) {
            appender.buildSql(this, params, ignoreAlias = ignoreAlias)
        } else {
            appendRef(alias)
        }
        if (addParentheses) append(')')
    }

    open fun append(
        appenders: Iterable<SqlAppender>,
        params: SqlParamList,
        addParentheses: Boolean = false,
        ignoreAlias: Boolean = false,
    ) = also {
        if (addParentheses) append('(')
        appendList(appenders, params) { sqlAppender, params ->
            append(sqlAppender, params, ignoreAlias = ignoreAlias)
        }
        if (addParentheses) append(')')
    }

    open fun appendPagination(paginationParams: PaginationParams) = also {
        throw UnsupportedOperationException("SQL dialect does not support pagination")
    }

    open fun appendAndAsAlias(appender: SqlAppender, params: SqlParamList) = also {
        append(appender, params, ignoreAlias = true)
        appender.getAliasOrNull()?.also { append(' ').appendKeyword("AS").append(' ').appendRef(it) }
    }

    fun build() = end().let { builder.toString() }
}
