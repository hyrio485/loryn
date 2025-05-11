package top.loryn.expression

import top.loryn.database.SqlBuilder
import top.loryn.support.SqlType
import top.loryn.utils.SqlParamList

class CaseValueExpression<T, R>(
    val value: SqlExpression<T>,
    branches: List<Pair<SqlExpression<T>, SqlExpression<R>>>,
    elseExpr: SqlExpression<R>? = null,
    sqlType: SqlType<R>?,
) : BaseCaseExpression<T, R>(branches, elseExpr, sqlType) {
    override fun buildSql(builder: SqlBuilder, params: SqlParamList, ignoreAlias: Boolean) {
        doBuildSql(builder, params, ignoreAlias) { append(value, params) }
    }
}
