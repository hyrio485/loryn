package top.loryn.schema

import top.loryn.database.SqlBuilder
import top.loryn.expression.ColumnExpression
import top.loryn.support.SqlType
import top.loryn.support.WithAlias.Companion.getAliasOrNull
import top.loryn.utils.SqlParamList

open class Column<T>(
    val table: Table,
    override val name: String,
    override val sqlType: SqlType<T>,
    val primaryKey: Boolean = false,
    notNull: Boolean = false,
) : ColumnExpression<T> {
    val notNull = primaryKey || notNull

    override fun buildSql(builder: SqlBuilder, params: SqlParamList) {
        builder.appendAlias(table) { appendRef(it).append('.') }.appendRef(name)
    }

    override fun toString() =
        "${table.getAliasOrNull()?.let { "$it." }.orEmpty()}$name${
            this@Column.getAliasOrNull()?.let { "($it)" }.orEmpty()
        }"
}
