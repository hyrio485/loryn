package top.loryn.expression

import top.loryn.database.SqlBuilder

enum class JoinType(val keyword: String?) {
    DEFAULT(null),
    INNER("INNER"),
    LEFT("LEFT"),
    RIGHT("RIGHT"),
    FULL("FULL"),
    CROSS("CROSS"),
    ;
}

class JoinExpression<E : Any>(
    val left: QuerySourceExpression<*>,
    val right: QuerySourceExpression<*>,
    val joinType: String?,
    val on: SqlExpression<Boolean>,
    val createEntity: (() -> E)? = null,
) : QuerySourceExpression<E>(null) {
    constructor(
        left: QuerySourceExpression<*>,
        right: QuerySourceExpression<*>,
        joinType: JoinType,
        on: SqlExpression<Boolean>,
        createEntity: (() -> E)? = null,
    ) : this(left, right, joinType.keyword, on, createEntity)

    override val columns = emptyList<ColumnExpression<E, *>>()

    override fun createEntity(): E {
        if (createEntity != null) {
            return createEntity.invoke()
        }
        return super.createEntity()
    }

    override fun SqlBuilder.appendSql(params: MutableList<SqlParam<*>>) = also {
        appendExpression(left, params).append(' ')
        joinType?.also { append(it).append(' ') }
        appendKeyword("JOIN").append(' ')
        appendExpression(right, params).append(' ')
        appendKeyword("ON").append(' ')
        appendExpression(on, params)
    }
}
