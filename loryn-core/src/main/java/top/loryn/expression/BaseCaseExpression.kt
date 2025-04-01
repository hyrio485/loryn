package top.loryn.expression

import top.loryn.database.SqlBuilder

abstract class BaseCaseExpression<T : Any, R : Any>(
    val branches: List<Pair<SqlExpression<T>, SqlExpression<R>>>,
    val elseExpr: SqlExpression<R>? = null,
) : SqlExpression<R> {
    init {
        require(branches.isNotEmpty()) { "At least one branch must be provided" }
    }

    protected inline fun SqlBuilder.doAppendSqlOriginal(
        params: MutableList<SqlParam<*>>,
        appendValue: SqlBuilder.(MutableList<SqlParam<*>>) -> Unit = {},
    ) = also {
        appendKeyword("CASE").append(' ').appendValue(params)
        branches.forEach { (condition, result) ->
            append(' ').appendKeyword("WHEN").append(' ').appendExpression(condition, params)
            append(' ').appendKeyword("THEN").append(' ').appendExpression(result, params)
        }
        elseExpr?.also {
            append(' ').appendKeyword("ELSE").append(' ').appendExpression(it, params)
        }
        append(' ').appendKeyword("END")
    }
}
