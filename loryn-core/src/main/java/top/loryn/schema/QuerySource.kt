package top.loryn.schema

import top.loryn.database.SqlBuilder
import top.loryn.expression.BindableColumnExpression
import top.loryn.expression.ColumnExpression
import top.loryn.expression.SqlExpression
import top.loryn.schema.JoinQuerySource.JoinType
import top.loryn.support.SqlAppender
import top.loryn.support.WithAlias
import top.loryn.support.WithAlias.Companion.getAliasOrNull
import top.loryn.utils.SqlParamList

interface QuerySource : SqlAppender {
    val columns: List<ColumnExpression<*>>

    fun join(right: QuerySource, joinType: JoinType, on: SqlExpression<Boolean>) =
        JoinQuerySource(this, right, joinType, on)

    fun leftJoin(right: QuerySource, on: SqlExpression<Boolean>) =
        JoinQuerySource(this, right, JoinType.LEFT, on)

    fun innerJoin(right: QuerySource, on: SqlExpression<Boolean>) =
        JoinQuerySource(this, right, JoinType.INNER, on)

    fun rightJoin(right: QuerySource, on: SqlExpression<Boolean>) =
        JoinQuerySource(this, right, JoinType.RIGHT, on)

    fun <E> bind(createEntity: () -> E): BindableQuerySource<E> =
        object : BindableQuerySource<E>, WithAlias {
            private val this0 = this@QuerySource

            override val columns = emptyList<BindableColumnExpression<E, *>>()

            override val alias = this0.getAliasOrNull()
            override val original = this0

            override fun createEntity() = createEntity.invoke()
        }

    fun aliased(alias: String): QuerySource =
        object : QuerySource, WithAlias {
            private val this0 = this@QuerySource

            override val columns = this0.columns

            override val alias = alias
            override val original = this0
        }

    fun allColumns(): ColumnExpression<Nothing> =
        object : ColumnExpression<Nothing> {
            private val this0 = this@QuerySource

            override val name = null
            override val sqlType get() = throw UnsupportedOperationException("Query source's all columns does not have a SQL type")

            override fun buildSql(builder: SqlBuilder, params: SqlParamList) {
                builder.appendAlias(this0) { appendRef(it).append(' ') }.append('*')
            }
        }
}
