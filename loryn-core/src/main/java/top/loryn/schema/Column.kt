package top.loryn.schema

import top.loryn.database.SqlBuilder
import top.loryn.expression.ColumnExpression
import top.loryn.expression.SqlParam
import top.loryn.support.SqlType

open class Column<T>(
    val table: Table,
    override val name: String,
    override val sqlType: SqlType<T>,
    val primaryKey: Boolean = false,
    val notNull: Boolean = false,
) : ColumnExpression<T>(name) {
    // region 数据库表描述方法

    private fun copy(
        table: Table = this.table,
        name: String = this.name,
        sqlType: SqlType<T> = this.sqlType,
        primaryKey: Boolean = this.primaryKey,
        notNull: Boolean = this.notNull,
    ) = Column<T>(table, name, sqlType, primaryKey, notNull).also { table.registerColumn(it) }

    fun primaryKey(primaryKey: Boolean = true): Column<T> =
        copy(primaryKey = primaryKey, notNull = true)

    fun notNull(notNull: Boolean = true): Column<T> = copy(notNull = notNull)

    // endregion

    override fun SqlBuilder.appendSql(params: MutableList<SqlParam<*>>) =
        appendAlias(table) { appendRef(it).append('.') }.appendRef(name)
}
