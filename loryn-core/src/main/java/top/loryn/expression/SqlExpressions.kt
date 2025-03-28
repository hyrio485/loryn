package top.loryn.expression

import top.loryn.database.SqlBuilder
import top.loryn.schema.Column
import java.sql.ResultSet

abstract class ColumnExpression<E, C : Any>(
    val sqlTypeNullable: SqlType<C>?,
    val alias: String?,
    val setter: (E.(C?) -> Unit)? = null,
) : SqlExpression<C> {
    override val sqlType: SqlType<C>
        get() = sqlTypeNullable
            ?: throw UnsupportedOperationException("This column expression does not have a SQL type.")

    fun applyValue(entity: E, index: Int, resultSet: ResultSet) {
        if (setter != null) {
            entity.setter(sqlType.getResult(resultSet, index + 1))
        }
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

data class NullSqlExpression<E, C : Any>(
    val label: String? = null,
) : ColumnExpression<E, C>(null, label, null) {
    override fun SqlBuilder.appendSqlOriginal(params: MutableList<SqlParam<*>>) = also {
        appendKeyword("NULL")
    }
}

class ParameterExpression<E, C : Any>(
    val value: C?,
    sqlType: SqlType<C>,
    label: String? = null,
    setter: (E.(C?) -> Unit)? = null,
) : ColumnExpression<E, C>(sqlType, label, setter) {
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
    setter: (E.(R?) -> Unit)? = null,
) : ColumnExpression<E, R>(sqlType, label, setter) {
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
    setter: (E.(R?) -> Unit)? = null,
) : ColumnExpression<E, R>(sqlType, label, setter) {
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

class InExpression<E>(
    val expr: SqlExpression<*>,
    val list: List<SqlExpression<*>>,
    val not: Boolean = false,
    label: String? = null,
    setter: (E.(Boolean?) -> Unit)? = null,
) : ColumnExpression<E, Boolean>(BooleanSqlType, label, setter) {
    override fun SqlBuilder.appendSqlOriginal(params: MutableList<SqlParam<*>>) = also {
        appendExpression(expr, params).append(' ')
        if (not) appendKeyword("NOT").append(' ')
        appendKeyword("IN").append(" (").appendExpressions(list, params).append(')')
    }
}

abstract class BaseCaseExpression<E, T : Any, R : Any>(
    val branches: List<Pair<SqlExpression<T>, SqlExpression<R>>>,
    val elseExpr: SqlExpression<R>? = null,
    label: String? = null,
    setter: (E.(R?) -> Unit)? = null,
) : ColumnExpression<E, R>(null, label, setter) {
    init {
        require(branches.isNotEmpty()) { "At least one branch must be provided" }
    }

    protected inline fun SqlBuilder.doAppendSqlOriginal(
        params: MutableList<SqlParam<*>>,
        appendValue: SqlBuilder.(MutableList<SqlParam<*>>) -> Unit = {},
    ) = also {
        appendKeyword("CASE").append(' ').appendValue(params)
        branches.forEach { (condition, result) ->
            append(' ').appendKeyword("WHEN").appendExpression(condition, params).append(' ')
            appendKeyword("THEN").append(' ').appendExpression(result, params)
        }
        elseExpr?.also {
            append(' ').appendKeyword("ELSE").append(' ').appendExpression(it, params)
        }
        append(' ').appendKeyword("END")
    }
}

class CaseExpression<E, R : Any>(
    branches: List<Pair<SqlExpression<Boolean>, SqlExpression<R>>>,
    elseExpr: SqlExpression<R>? = null,
    label: String? = null,
    setter: (E.(R?) -> Unit)? = null,
) : BaseCaseExpression<E, Boolean, R>(branches, elseExpr, label, setter) {
    override fun SqlBuilder.appendSqlOriginal(params: MutableList<SqlParam<*>>) =
        doAppendSqlOriginal(params)
}

class CaseValueExpression<E, T : Any, R : Any>(
    val value: SqlExpression<T>,
    branches: List<Pair<SqlExpression<T>, SqlExpression<R>>>,
    elseExpr: SqlExpression<R>? = null,
    label: String? = null,
    setter: (E.(R?) -> Unit)? = null,
) : BaseCaseExpression<E, T, R>(branches, elseExpr, label, setter) {
    override fun SqlBuilder.appendSqlOriginal(params: MutableList<SqlParam<*>>) =
        doAppendSqlOriginal(params) { appendExpression(value, params) }
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
            appendExpressions(columns, params)
        } else {
            append('*')
        }
        if (from != null) {
            append(' ').appendKeyword("FROM").append(' ').appendExpression(from, params)
        }
        where?.also { append(' ').appendKeyword("WHERE").append(' ').appendExpression(it, params) }
    }

    inline fun <reified T : Any> asExpression(): ColumnExpression<E, T> {
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
