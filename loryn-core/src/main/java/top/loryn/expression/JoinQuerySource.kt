package top.loryn.expression

import top.loryn.database.SqlBuilder
import top.loryn.schema.QuerySource

class JoinQuerySource(
    val left: QuerySource,
    val right: QuerySource,
    val joinType: String?,
    val on: SqlExpression<Boolean>,
    //    val createEntity: (() -> E)? = null,
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
        //        createEntity: (() -> E)? = null,
    ) : this(left, right, joinType.keyword, on)

    override val columns = emptyList<ColumnExpression<*>>()

    override fun SqlBuilder.appendSql(params: MutableList<SqlParam<*>>) = also {
        append(left, params)
        joinType?.also { append(' ').append(it) }
        append(' ').appendKeyword("JOIN").append(' ').append(right, params)
        append(' ').appendKeyword("ON").append(' ').append(on, params)
    }
}
