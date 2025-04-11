package top.loryn.utils

import top.loryn.schema.DerivedTable
import top.loryn.schema.Table

fun <E, T : Table<E>> T.aliased(alias: String) = DerivedTable(this, alias = alias)
