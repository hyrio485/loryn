package top.loryn.expression

import top.loryn.database.SqlBuilder
import top.loryn.schema.Column

abstract class ColumnExpression<E, C : Any>(
    val sqlTypeNullable: SqlType<C>?,
    val alias: String?,
    val setValue: ((E, C?) -> Unit)? = null,
) : SqlExpression<C> {
    override val sqlType: SqlType<C>
        get() = sqlTypeNullable
            ?: throw UnsupportedOperationException("This column expression does not have a SQL type.")

    inline fun applyValue(entity: E, getValue: (SqlType<C>) -> Any?) {
        @Suppress("UNCHECKED_CAST") // 这里可以直接转换，因为再构建statement的时候已经检查过类型了
        setValue?.invoke(entity, getValue(sqlType) as C?)
    }

    open fun SqlBuilder.appendSqlInSelectClause(params: MutableList<SqlParam<*>>) = also {
        appendSql(params)
        if (alias != null) {
            append(' ').appendKeyword("AS").append(' ').appendRef(alias)
        }
    }

    abstract fun SqlBuilder.appendSqlOriginal(params: MutableList<SqlParam<*>>): SqlBuilder

    override fun SqlBuilder.appendSql(params: MutableList<SqlParam<*>>) = also {
        if (alias != null) {
            appendRef(alias)
        } else {
            appendSqlOriginal(params)
        }
    }
}

class ParameterExpression<E, C : Any>(
    val value: C?,
    sqlType: SqlType<C>,
    label: String? = null,
    setValue: ((E, C?) -> Unit)? = null,
) : ColumnExpression<E, C>(sqlType, label, setValue) {
    override fun SqlBuilder.appendSqlOriginal(params: MutableList<SqlParam<*>>) = also {
        append("?")
        params += SqlParam(value, sqlType)
    }
}

class UnaryExpression<E, T : Any, R : Any>(
    val operator: String,
    val expr: SqlExpression<T>,
    sqlType: SqlType<R>,
    val addParentheses: Boolean = true,
    label: String? = null,
    setValue: ((E, R?) -> Unit)? = null,
) : ColumnExpression<E, R>(sqlType, label, setValue) {
    override fun SqlBuilder.appendSqlOriginal(params: MutableList<SqlParam<*>>) = also {
        appendKeyword(operator).append(' ')
        if (addParentheses) append('(')
        appendExpression(expr, params)
        if (addParentheses) append(')')
    }
}

class BinaryExpression<E, T1 : Any, T2 : Any, R : Any>(
    val operator: String,
    val expr1: SqlExpression<T1>,
    val expr2: SqlExpression<T2>,
    sqlType: SqlType<R>,
    val addParentheses: Boolean = true,
    label: String? = null,
    setValue: ((E, R?) -> Unit)? = null,
) : ColumnExpression<E, R>(sqlType, label, setValue) {
    override fun SqlBuilder.appendSqlOriginal(params: MutableList<SqlParam<*>>) = also {
        if (addParentheses) append('(')
        appendExpression(expr1, params)
        if (addParentheses) append(')')
        append(' ').appendKeyword(operator).append(' ')
        if (addParentheses) append('(')
        appendExpression(expr2, params)
        if (addParentheses) append(')')
    }
}

data class AssignmentExpression<E, C : Any>(
    val column: ColumnExpression<E, C>,
    val value: SqlExpression<C>,
) : SqlExpression<Nothing> {
    init {
        if (column is Column<*, *> && column.notNull && value is ParameterExpression<*, *> && value.value == null) {
            throw IllegalArgumentException("The column $column cannot be null.")
        }
    }

    override fun SqlBuilder.appendSql(params: MutableList<SqlParam<*>>) = also {
        appendExpression(column, params).append(' ').appendKeyword("=").append(' ').appendExpression(value, params)
    }
}

interface EntityCreator<E> {
    fun createEntity(): E {
        throw UnsupportedOperationException()
    }
}

/**
 * 查询源表达式。
 *
 * @param E 绑定的查询结果的实体类型。
 */
abstract class QuerySourceExpression<E> : EntityCreator<E>, SqlExpression<Nothing> {
    abstract val columns: List<ColumnExpression<E, *>>
}

/**
 * SELECT表达式。
 *
 * @param E 绑定的查询结果的实体类型。
 */
class SelectExpression<E>(
    val columns: List<ColumnExpression<E, *>>,
    val from: QuerySourceExpression<E>?,
    val where: SqlExpression<Boolean>?,
    private val doCreateEntity: (() -> E)? = null,
) : EntityCreator<E>, SqlExpression<Nothing> {
    override fun createEntity() =
        if (doCreateEntity != null) {
            doCreateEntity()
        } else if (from != null) {
            from.createEntity()
        } else {
            super.createEntity()
        }

    override fun SqlBuilder.appendSql(params: MutableList<SqlParam<*>>) = also {
        appendKeyword("SELECT").append(' ')
        if (columns.isNotEmpty()) {
            columns.forEachIndexed { index, column ->
                if (index > 0) append(", ")
                column.run { appendSqlInSelectClause(params) }
            }
        } else {
            append('*')
        }
        if (from != null) {
            append(' ').appendKeyword("FROM").append(' ').appendExpression(from, params)
        }
        where?.also {
            append(' ').appendKeyword("WHERE").append(' ').appendExpression(it, params)
        }
    }

    inline fun <reified T : Any> asColumn(): ColumnExpression<E, T> {
        require(columns.size == 1) { "This select expression has ${if (columns.isEmpty()) "dynamic" else "more then one"} columns" }
        val column = columns[0]
        if (column.sqlType.clazz != T::class.java) {
            throw IllegalArgumentException("The column type is not ${T::class.java}")
        }
        @Suppress("UNCHECKED_CAST")
        return column as ColumnExpression<E, T>
    }

    fun asQuerySource(alias: String?): QuerySourceExpression<E> {
        return object : QuerySourceExpression<E>() {
            override val columns = this@SelectExpression.columns

            override fun SqlBuilder.appendSql(params: MutableList<SqlParam<*>>) = also {
                append('(').appendExpression(this@SelectExpression, params).append(')')
                alias?.also { append(' ').appendRef(it) }
            }
        }
    }
}
