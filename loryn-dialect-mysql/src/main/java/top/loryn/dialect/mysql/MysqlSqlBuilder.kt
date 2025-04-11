package top.loryn.dialect.mysql

import top.loryn.database.SqlBuilder
import top.loryn.expression.SelectExpression
import top.loryn.expression.SqlParam
import top.loryn.support.PaginationParams
import top.loryn.support.SqlAppender

open class MysqlSqlBuilder(
    keywords: Set<String>,
    uppercaseKeywords: Boolean,
) : SqlBuilder(keywords, uppercaseKeywords) {
    override fun append(sqlAppender: SqlAppender, params: MutableList<SqlParam<*>>) = also {
        when (sqlAppender) {
            is SelectExpression -> super.append(sqlAppender, params)
            else -> super.append(sqlAppender, params)
        }
    }

    override fun append(paginationParams: PaginationParams) = also {
        val (currentPage, pageSize) = paginationParams
        appendKeyword("LIMIT").append(' ').append((currentPage - 1) * pageSize).append(',').append(' ').append(pageSize)
    }

    override fun end() = also { append(';') }
}
