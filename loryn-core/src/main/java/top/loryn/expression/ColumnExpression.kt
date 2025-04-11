package top.loryn.expression

import top.loryn.database.SqlBuilder
import top.loryn.schema.Column
import top.loryn.schema.DerivedColumn
import top.loryn.support.SqlType
import java.sql.ResultSet

/**
 * 可作为列元素的表达式基类。在追加SQL的时候有两种情况：
 * 1. 在选择列的列表中追加，需要追加列本体的内容及别名；
 * 2. 在条件子句中追加，如果有别名则只使用别名，否则追加列本体内容。
 */
abstract class ColumnExpression<E, C : Any>(
    val alias: String? = null,
    val label: String? = null,
    val sqlTypeNullable: SqlType<C>? = null,
    val setter: (E.(C?) -> Unit)? = null,
) : SqlExpression<C> {
    companion object {
        fun <E, T : Any> wrap(expression: SqlExpression<T>) =
            object : ColumnExpression<E, T>(sqlTypeNullable = expression.sqlType) {
                override fun SqlBuilder.appendSqlOriginal(params: MutableList<SqlParam<*>>) =
                    appendExpression(expression, params)
            }
    }

    override val sqlType: SqlType<C>
        get() = sqlTypeNullable
            ?: throw UnsupportedOperationException("This column expression does not have a SQL type.")

    private inline fun doApplyValue(entity: E, getValue: () -> C?) {
        if (setter != null) {
            entity.setter(getValue())
        }
    }

    fun getValue(resultSet: ResultSet): C? {
        require(label != null || alias != null) { "ColumnExpression $this does not have a label or alias." }
        return sqlType.getResult(resultSet, (label ?: alias)!!)
    }

    fun applyValue(entity: E, index: Int, resultSet: ResultSet) {
        doApplyValue(entity) { sqlType.getResult(resultSet, index + 1) }
    }

    fun applyValue(entity: E, resultSet: ResultSet) {
        doApplyValue(entity) { getValue(resultSet) }
    }

    open val tableColumn: Column<E, C>? = null

    fun aliased(alias: String) = DerivedColumn(this, alias = alias)

    fun <E1 : Any> withSetter(setter: (E1.(C?) -> Unit)) =
        object : ColumnExpression<E1, C>(alias, label, sqlTypeNullable, setter) {
            override fun SqlBuilder.appendSqlOriginal(params: MutableList<SqlParam<*>>) =
                appendExpression(this@ColumnExpression, params)
        }

    fun distinct() = UnaryExpression("DISTINCT", this, sqlType, false).asColumn<E, C>()

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
