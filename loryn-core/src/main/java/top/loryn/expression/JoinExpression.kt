package top.loryn.expression

import top.loryn.database.SqlBuilder

class JoinExpression<E : Any>(
    val left: QuerySourceExpression<*>,
    val right: QuerySourceExpression<*>,
    val joinType: JoinType = JoinType.DEFAULT,
    val on: SqlExpression<Boolean>,
    val createEntity: (() -> E)? = null,
) : QuerySourceExpression<E>(null) {
    override val columns = emptyList<ColumnExpression<E, *>>()

    override fun createEntity(): E {
        if (createEntity != null) {
            return createEntity.invoke()
        }
        return super.createEntity()
    }

    override fun SqlBuilder.appendSql(params: MutableList<SqlParam<*>>) = also {
        appendExpression(left, params).append(' ')
        joinType.keyword?.also { append(it).append(' ') }
        appendKeyword("JOIN").append(' ')
        appendExpression(right, params).append(' ')
        appendKeyword("ON").append(' ')
        appendExpression(on, params)
    }
}
