package top.loryn.expression

import top.loryn.database.SqlBuilder
import top.loryn.utils.SqlParamList

class CaseValueExpression<T, R>(
    val value: SqlExpression<T>,
    branches: List<Pair<SqlExpression<T>, SqlExpression<R>>>,
    elseExpr: SqlExpression<R>? = null,
) : BaseCaseExpression<T, R>(branches, elseExpr) {
    override fun SqlBuilder.appendSql(params: SqlParamList) =
        doAppendSqlOriginal(params) { append(value, params) }
}
