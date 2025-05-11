package top.loryn.schema

import top.loryn.database.SqlBuilder
import top.loryn.expression.BindableColumnExpression
import top.loryn.expression.ColumnExpression
import top.loryn.expression.SqlExpression
import top.loryn.expression.StarSqlExpression
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
            override val sqlType get() = sqlTypeNoNeed("Query source's all columns")

            override fun buildSql(builder: SqlBuilder, params: SqlParamList, ignoreAlias: Boolean) {
                this0.getAliasOrNull()?.also { builder.appendRef(it).append(' ') }
                builder.append(StarSqlExpression, params, ignoreAlias = ignoreAlias)
            }
        }

    operator fun <T> get(columnExpression: ColumnExpression<T>) =
        object : ColumnExpression<T> {
            private val this0 = this@QuerySource

            override val name = columnExpression.name
            override val sqlType = columnExpression.sqlType

            override fun buildSql(builder: SqlBuilder, params: SqlParamList, ignoreAlias: Boolean) {
                val alias = this0.getAliasOrNull()
                    ?: throw IllegalStateException("Query source $this0 does not have an alias")
                val columnAlias = columnExpression.getAliasOrNull()
                    ?: columnExpression.name
                    ?: throw IllegalStateException("Column $columnExpression does not have an alias or name")
                builder.appendRef(alias).append('.').appendRef(columnAlias)
            }
        }
}
