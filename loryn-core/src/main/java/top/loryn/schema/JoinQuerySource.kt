package top.loryn.schema

import top.loryn.database.SqlBuilder
import top.loryn.expression.ColumnExpression
import top.loryn.expression.SqlExpression
import top.loryn.utils.SqlParamList

class JoinQuerySource(
    val left: QuerySource,
    val right: QuerySource,
    val joinType: String?,
    val on: SqlExpression<Boolean>,
) : QuerySource {
    enum class JoinType(val keyword: String?) {
        DEFAULT(null),
        INNER("INNER"),
        LEFT("LEFT"),
        RIGHT("RIGHT"),
        FULL("FULL"),
        CROSS("CROSS"),
        ;
    }

    constructor(
        left: QuerySource,
        right: QuerySource,
        joinType: JoinType,
        on: SqlExpression<Boolean>,
    ) : this(left, right, joinType.keyword, on)

    override val columns = emptyList<ColumnExpression<*>>()

    override fun SqlBuilder.appendSql(params: SqlParamList) = also {
        append(left, params).appendAliasUsingAs(left)
        joinType?.also { append(' ').append(it) }
        append(' ').appendKeyword("JOIN").append(' ').append(right, params).appendAliasUsingAs(right)
        append(' ').appendKeyword("ON").append(' ').append(on, params)
    }
}
