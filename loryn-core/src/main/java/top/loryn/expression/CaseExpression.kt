package top.loryn.expression

import top.loryn.database.SqlBuilder
import top.loryn.support.SqlType

class CaseExpression<R : Any>(
    branches: List<Pair<SqlExpression<Boolean>, SqlExpression<R>>>,
    elseExpr: SqlExpression<R>? = null,
    override val sqlType: SqlType<R>,
) : BaseCaseExpression<Boolean, R>(branches, elseExpr) {
    override fun SqlBuilder.appendSql(params: MutableList<SqlParam<*>>) = doAppendSqlOriginal(params)
}
