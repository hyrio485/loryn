package top.loryn.dialect.mysql

import top.loryn.database.SqlBuilder
import top.loryn.expression.SelectExpression
import top.loryn.expression.SqlParam
import top.loryn.statement.Statement

class WithDecorator(
    val selects: List<Pair<String, SelectExpression<*>>>,
    val statement: Statement,
) : Statement(statement.database) {
    constructor(
        vararg selects: Pair<String, SelectExpression<*>>,
        statement: Statement,
    ) : this(selects.toList(), statement)

    override fun SqlBuilder.doGenerateSql(params: MutableList<SqlParam<*>>) {
        appendKeyword("WITH").append(' ').appendList(selects, params) { (label, select), params ->
            appendRef(label).append(' ').appendKeyword("AS").append(' ')
            append('(').appendExpression(select, params).append(')')
        }
        with(statement) { doGenerateSql(params) }
    }
}
