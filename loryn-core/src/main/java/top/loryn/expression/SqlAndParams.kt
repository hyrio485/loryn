package top.loryn.expression

data class SqlAndParams(val sql: String, val params: List<SqlParam<*>>) {
    constructor(sql: String, vararg params: SqlParam<*>) : this(sql, params.toList())
}
