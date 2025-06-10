package top.loryn.expression

import top.loryn.database.SqlBuilder
import top.loryn.schema.QuerySource
import top.loryn.support.WithAlias
import top.loryn.utils.SqlParamList

class UnionExpression(
    val expr1: SqlExpression<*>,
    val expr2: SqlExpression<*>,
    val addParentheses: Boolean = true,
) : SqlExpression<Nothing> {
    override val sqlType get() = sqlTypeNoNeed()

    override fun buildSql(builder: SqlBuilder, params: SqlParamList, ignoreAlias: Boolean) {
        builder
            .append(expr1, params, addParentheses = addParentheses, ignoreAlias = ignoreAlias)
            .append(' ')
            .appendKeyword("UNION")
            .append(' ')
            .append(expr2, params, addParentheses = addParentheses, ignoreAlias = ignoreAlias)
    }

    fun asQuerySource(alias: String? = null): QuerySource =
        object : QuerySource, WithAlias {
            private val this0 = this@UnionExpression

            override val columns = emptyList<ColumnExpression<*>>()

            override val alias = alias
            override val original = this0

            // 因为这里是将子查询包装成了 QuerySource ，要在构建的SQL前后加括号（与其他情况的默认行为不同），因此要重写此方法。
            override fun buildSql(builder: SqlBuilder, params: SqlParamList, ignoreAlias: Boolean) {
                builder.append(original, params, addParentheses = true, ignoreAlias = ignoreAlias)
            }
        }
}
