package top.loryn.dialect.mysql

import top.loryn.database.SqlBuilder
import top.loryn.expression.SelectExpression
import top.loryn.expression.SqlExpression
import top.loryn.expression.SqlParam

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

    override fun end() = also { append(';') }
}
