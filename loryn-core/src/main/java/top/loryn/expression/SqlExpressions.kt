package top.loryn.expression

import top.loryn.database.SqlBuilder
import top.loryn.schema.Column
import top.loryn.support.BooleanSqlType
import top.loryn.support.PaginationParams
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
            override val sqlType: SqlType<T>
                get() = sqlTypeNullable
                    ?: throw UnsupportedOperationException("This column expression does not have a SQL type.")

            override fun SqlBuilder.appendSqlOriginal(params: MutableList<SqlParam<*>>) =
                appendExpression(expression, params)
        }
    }

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

data class NullSqlExpression<T : Any>(
    val label: String? = null,
) : SqlExpression<T> {
    override fun SqlBuilder.appendSql(params: MutableList<SqlParam<*>>) = also {
        appendKeyword("NULL")
    }
}

class ParameterExpression<T : Any>(
    val value: T?,
    override val sqlType: SqlType<T>,
) : SqlExpression<T> {
    override fun SqlBuilder.appendSql(params: MutableList<SqlParam<*>>) = also {
        append("?")
        params += SqlParam(value, sqlType)
    }
}

class UnaryExpression<T : Any, R : Any>(
    val operator: String,
    val expr: SqlExpression<T>,
    override val sqlType: SqlType<R>,
    val addParentheses: Boolean = true,
) : SqlExpression<R> {
    override fun SqlBuilder.appendSql(params: MutableList<SqlParam<*>>) = also {
        appendKeyword(operator).append(' ')
        if (addParentheses) append('(')
        appendExpression(expr, params)
        if (addParentheses) append(')')
    }
}

class BinaryExpression<T1 : Any, T2 : Any, R : Any>(
    val operators: List<String>,
    val expr1: SqlExpression<T1>,
    val expr2: SqlExpression<T2>,
    override val sqlType: SqlType<R>,
    val addParentheses: Boolean = false,
) : SqlExpression<R> {
    init {
        require(operators.isNotEmpty()) { "At least one operator must be provided" }
    }

    override fun SqlBuilder.appendSql(params: MutableList<SqlParam<*>>) = also {
        if (addParentheses) append('(')
        appendExpression(expr1, params)
        if (addParentheses) append(')')
        append(' ').appendList(operators, params, " ") { operator, _ ->
            appendKeyword(operator)
        }.append(' ')
        if (addParentheses) append('(')
        appendExpression(expr2, params)
        if (addParentheses) append(')')
    }
}

class InExpression(
    val expr: SqlExpression<*>,
    val list: List<SqlExpression<*>>,
    val not: Boolean = false,
) : SqlExpression<Boolean> {
    override val sqlType = BooleanSqlType

    override fun SqlBuilder.appendSql(params: MutableList<SqlParam<*>>) = also {
        appendExpression(expr, params).append(' ')
        if (not) appendKeyword("NOT").append(' ')
        appendKeyword("IN").append(" (").appendExpressions(list, params).append(')')
    }
}

abstract class BaseCaseExpression<T : Any, R : Any>(
    val branches: List<Pair<SqlExpression<T>, SqlExpression<R>>>,
    val elseExpr: SqlExpression<R>? = null,
) : SqlExpression<R> {
    init {
        require(branches.isNotEmpty()) { "At least one branch must be provided" }
    }

    protected inline fun SqlBuilder.doAppendSqlOriginal(
        params: MutableList<SqlParam<*>>,
        appendValue: SqlBuilder.(MutableList<SqlParam<*>>) -> Unit = {},
    ) = also {
        appendKeyword("CASE").append(' ').appendValue(params)
        branches.forEach { (condition, result) ->
            append(' ').appendKeyword("WHEN").append(' ').appendExpression(condition, params)
            append(' ').appendKeyword("THEN").append(' ').appendExpression(result, params)
        }
        elseExpr?.also {
            append(' ').appendKeyword("ELSE").append(' ').appendExpression(it, params)
        }
        append(' ').appendKeyword("END")
    }
}

class CaseExpression<R : Any>(
    branches: List<Pair<SqlExpression<Boolean>, SqlExpression<R>>>,
    elseExpr: SqlExpression<R>? = null,
    override val sqlType: SqlType<R>,
) : BaseCaseExpression<Boolean, R>(branches, elseExpr) {
    override fun SqlBuilder.appendSql(params: MutableList<SqlParam<*>>) = doAppendSqlOriginal(params)
}

class CaseValueExpression<T : Any, R : Any>(
    val value: SqlExpression<T>,
    branches: List<Pair<SqlExpression<T>, SqlExpression<R>>>,
    elseExpr: SqlExpression<R>? = null,
) : BaseCaseExpression<T, R>(branches, elseExpr) {
    override fun SqlBuilder.appendSql(params: MutableList<SqlParam<*>>) =
        doAppendSqlOriginal(params) { appendExpression(value, params) }
}

data class AssignmentExpression<E, C : Any>(
    val column: ColumnExpression<E, C>,
    val value: SqlExpression<C>,
) : SqlExpression<Nothing> {
    init {
        if (column is Column<*, *> && column.notNull && value is ParameterExpression<*> && value.value == null) {
            throw IllegalArgumentException("The column $column cannot be null.")
        }
    }

    override fun SqlBuilder.appendSql(params: MutableList<SqlParam<*>>) = also {
        appendExpression(column, params).append(' ').appendKeyword("=").append(' ').appendExpression(value, params)
    }
}

interface EntityCreator<E> {
    fun createEntity(): E = throw UnsupportedOperationException()
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
    val paginationParams: PaginationParams?,
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

    private fun SqlBuilder.appendMain(params: MutableList<SqlParam<*>>) = also {
        if (from != null && where != null) {
            append(' ')
        }
        from?.also { append(' ').appendKeyword("FROM").append(' ').appendExpression(it, params) }
        where?.also { append(' ').appendKeyword("WHERE").append(' ').appendExpression(it, params) }
    }

    override fun SqlBuilder.appendSql(params: MutableList<SqlParam<*>>) = also {
        appendKeyword("SELECT").append(' ')
        if (columns.isNotEmpty()) {
            appendExpressions(columns, params)
        } else {
            append('*')
        }
        appendMain(params)
        // append order by
        paginationParams?.also { append(' ').appendPagination(it) }
    }

    fun SqlBuilder.appendSqlCount(params: MutableList<SqlParam<*>>) = also {
        appendKeyword("SELECT").append(' ').appendKeyword("COUNT").append("(1)").appendMain(params)
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
