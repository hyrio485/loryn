package top.loryn.expression

import top.loryn.database.SqlBuilder
import top.loryn.schema.QuerySource
import top.loryn.support.WithAlias
import top.loryn.utils.SqlParamList

class UnionExpression(
    val select1: SelectExpression,
    val select2: SelectExpression,
    val addParentheses: Boolean = true,
) : SqlExpression<Nothing> {
    override val sqlType get() = sqlTypeNoNeed()

    init {
        require(select1.columns.size == select2.columns.size) {
            "The number of columns in both SELECT statements must be the same"
        }
    }

    override fun buildSql(builder: SqlBuilder, params: SqlParamList, ignoreAlias: Boolean) {
        builder
            .append(select1, params, addParentheses = addParentheses, ignoreAlias = ignoreAlias)
            .append(' ')
            .appendKeyword("UNION")
            .append(' ')
            .append(select2, params, addParentheses = addParentheses, ignoreAlias = ignoreAlias)
    }

    fun asQuerySource(alias: String? = null): QuerySource =
        object : QuerySource, WithAlias {
            private val this0 = this@UnionExpression

            override val columns = select1.columns

            override val alias = alias
            override val original = this0

            // 因为这里是将子查询包装成了 QuerySource ，要在构建的SQL前后加括号（与其他情况的默认行为不同），因此要重写此方法。
            override fun buildSql(builder: SqlBuilder, params: SqlParamList, ignoreAlias: Boolean) {
                builder.append(original, params, addParentheses = true, ignoreAlias = ignoreAlias)
            }
        }
}
