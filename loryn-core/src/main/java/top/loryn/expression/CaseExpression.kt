package top.loryn.expression

import top.loryn.database.SqlBuilder
import top.loryn.support.SqlType
import top.loryn.utils.SqlParamList

class CaseExpression<R>(
    branches: List<Pair<SqlExpression<Boolean>, SqlExpression<R>>>,
    elseExpr: SqlExpression<R>? = null,
    sqlType: SqlType<R>?,
) : BaseCaseExpression<Boolean, R>(branches, elseExpr, sqlType) {
    override fun SqlBuilder.appendSql(params: SqlParamList) = doAppendSqlOriginal(params)
}
