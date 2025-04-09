package top.loryn.utils

import top.loryn.schema.Column
import top.loryn.schema.DerivedColumn
import top.loryn.schema.DerivedTable
import top.loryn.schema.Table

fun <E, T : Table<E>> T.aliased(alias: String) = DerivedTable(this, alias = alias)

operator fun <E, T : Table<E>, C : Any> T.invoke(block: T.() -> Column<E, C>) =
    DerivedColumn(block(), table = this)

const val APPEND_COLUMN_LABEL_ATTR_KEY = "APPEND_COLUMN_LABEL"
