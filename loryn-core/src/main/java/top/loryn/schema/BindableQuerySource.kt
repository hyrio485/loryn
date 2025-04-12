package top.loryn.schema

import top.loryn.expression.BindableColumnExpression

interface BindableQuerySource<E> : QuerySource {
    override val columns: List<BindableColumnExpression<E, *>>

    fun createEntity(): E
}
