package top.loryn.dialect.mysql

import top.loryn.database.SqlBuilder
import top.loryn.expression.SelectExpression
import top.loryn.expression.SqlExpression
import top.loryn.expression.SqlParam
import top.loryn.support.PaginationParams

open class MysqlSqlBuilder(
    keywords: Set<String>,
    uppercaseKeywords: Boolean,
) : SqlBuilder(keywords, uppercaseKeywords) {
    override fun appendExpression(expression: SqlExpression<*>, params: MutableList<SqlParam<*>>) = also {
        when (expression) {
            is SelectExpression<*> -> super.appendExpression(expression, params)
            else -> super.appendExpression(expression, params)
        }
    }

    override fun appendPagination(paginationParams: PaginationParams) = also {
        val (currentPage, pageSize) = paginationParams
        appendKeyword("LIMIT").append(' ').append((currentPage - 1) * pageSize).append(',').append(' ').append(pageSize)
    }

    override fun end() = also { append(';') }
}
