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

    fun <E> bind(createEntity: () -> E): BindableQuerySource<E> =
        object : BindableQuerySource<E>, WithAlias {
            private val this0 = this@QuerySource

            override val columns = emptyList<BindableColumnExpression<E, *>>()
            override val alias = this0.getAliasOrNull()

            override fun createEntity() = createEntity.invoke()

            override fun SqlBuilder.appendSql(params: SqlParamList) = with(this0) { appendSql(params) }
        }

    fun aliased(alias: String): QuerySource =
        object : QuerySource, WithAlias {
            private val this0 = this@QuerySource

            override val columns = this0.columns
            override val alias = alias

            override fun SqlBuilder.appendSql(params: SqlParamList) = with(this0) { appendSql(params) }
        }

    fun allColumns(): ColumnExpression<Nothing> =
        object : ColumnExpression<Nothing> {
            private val this0 = this@QuerySource

            override val name = null
            override val sqlType get() = throw UnsupportedOperationException("Query source's all columns does not have a SQL type")

            override fun SqlBuilder.appendSql(params: SqlParamList) =
                appendAlias(this0) { appendRef(it).append(' ') }.append('*')
        }
}
