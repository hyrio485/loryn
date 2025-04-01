package top.loryn.expression

import top.loryn.database.SqlBuilder

class CaseValueExpression<T : Any, R : Any>(
    val value: SqlExpression<T>,
    branches: List<Pair<SqlExpression<T>, SqlExpression<R>>>,
    elseExpr: SqlExpression<R>? = null,
) : BaseCaseExpression<T, R>(branches, elseExpr) {
    override fun SqlBuilder.appendSql(params: MutableList<SqlParam<*>>) =
        doAppendSqlOriginal(params) { appendExpression(value, params) }
}
