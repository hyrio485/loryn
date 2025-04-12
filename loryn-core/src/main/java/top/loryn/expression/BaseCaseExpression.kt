package top.loryn.expression

import top.loryn.database.SqlBuilder
import top.loryn.support.SqlType
import top.loryn.utils.SqlParamList

abstract class BaseCaseExpression<T, R>(
    val branches: List<Pair<SqlExpression<T>, SqlExpression<R>>>,
    val elseExpr: SqlExpression<R>? = null,
    protected val sqlTypeNullable: SqlType<R>?,
) : SqlExpression<R> {
    init {
        require(branches.isNotEmpty()) { "At least one branch must be provided" }
    }

    override val sqlType get() = sqlTypeNullable ?: throw UnsupportedOperationException("sqlType not specified")

    protected inline fun SqlBuilder.doAppendSqlOriginal(
        params: SqlParamList,
        appendValue: SqlBuilder.(SqlParamList) -> Unit = {},
    ) = also {
        appendKeyword("CASE").append(' ').appendValue(params)
        branches.forEach { (condition, result) ->
            append(' ').appendKeyword("WHEN").append(' ').append(condition, params)
            append(' ').appendKeyword("THEN").append(' ').append(result, params)
        }
        elseExpr?.also {
            append(' ').appendKeyword("ELSE").append(' ').append(it, params)
        }
        append(' ').appendKeyword("END")
    }
}
