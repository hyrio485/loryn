package top.loryn.expression

import top.loryn.database.SqlBuilder
import top.loryn.support.SqlType
import java.sql.ResultSet

/**
 * 可作为列元素的表达式基类。在追加SQL的时候有两种情况：
 * 1. 在选择列的列表中追加，需要追加列本体的内容及别名；
 * 2. 在条件子句中追加，如果有别名则只使用别名，否则追加列本体内容。
 */
abstract class ColumnExpression<E, C : Any>(
    val alias: String?,
    val sqlTypeNullable: SqlType<C>? = null,
    val setter: (E.(C?) -> Unit)? = null,
) : SqlExpression<C> {
    companion object {
        fun <E, T : Any> wrap(expression: SqlExpression<T>) = object : ColumnExpression<E, T>(null) {
            override fun SqlBuilder.appendSqlOriginal(params: MutableList<SqlParam<*>>) =
                appendExpression(expression, params)
        }
    }

    override val sqlType: SqlType<C>
        get() = sqlTypeNullable
            ?: throw UnsupportedOperationException("This column expression does not have a SQL type.")

    fun applyValue(entity: E, index: Int, resultSet: ResultSet) {
        if (setter != null) {
            entity.setter(sqlType.getResult(resultSet, index + 1))
        }
    }

    abstract fun SqlBuilder.appendSqlOriginal(params: MutableList<SqlParam<*>>): SqlBuilder

    open fun SqlBuilder.appendSqlInSelectClause(params: MutableList<SqlParam<*>>) = also {
        appendSqlOriginal(params)
        if (alias != null) {
            append(' ').appendKeyword("AS").append(' ').appendRef(alias)
        }
    }

    override fun SqlBuilder.appendSql(params: MutableList<SqlParam<*>>) = also {
        if (alias != null) {
            appendRef(alias)
        } else {
            appendSqlOriginal(params)
        }
    }
}
