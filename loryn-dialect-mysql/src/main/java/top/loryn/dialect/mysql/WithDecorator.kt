//package top.loryn.dialect.mysql
//
//import top.loryn.database.SqlBuilder
//import top.loryn.schema.Table
//import top.loryn.statement.DmlStatement
//import top.loryn.statement.DqlStatement
//
//private fun SqlBuilder.appendWithClause(
//    selects: List<Pair<Table<*>, SelectExpression<*>>>,
//    params: ParamList,
//) = also {
//    appendKeyword("WITH").append(' ').appendList(selects, params) { (table, select), params ->
//        appendRef(table.tableName).append(' ').appendKeyword("AS").append(' ')
//        append('(').append(select, params).append(')')
//    }.append(' ')
//}
//
//class WithDmlDecorator(
//    val selects: List<Pair<Table<*>, SelectExpression<*>>>,
//    val statement: DmlStatement,
//) : DmlStatement(statement.database) {
//    constructor(
//        vararg selects: Pair<Table<*>, SelectExpression<*>>,
//        statement: DmlStatement,
//    ) : this(selects.toList(), statement)
//
//    override fun SqlBuilder.doGenerateSql(params: ParamList) = also {
//        appendWithClause(selects, params)
//        with(statement) { doGenerateSql(params) }
//    }
//}
//
//class WithDqlDecorator<E>(
//    val selects: List<Pair<Table<*>, SelectExpression<*>>>,
//    val statement: DqlStatement<E>,
//) : DqlStatement<E>(statement.database) {
//    override val createEntity = statement.createEntity
//    override val columns = statement.columns
//    override val usingIndex = statement.usingIndex
//
//    constructor(
//        vararg selects: Pair<Table<*>, SelectExpression<*>>,
//        statement: DqlStatement<E>,
//    ) : this(selects.toList(), statement)
//
//    override fun SqlBuilder.doGenerateSql(params: ParamList) = also {
//        appendWithClause(selects, params)
//        with(statement) { doGenerateSql(params) }
//    }
//
//    override fun SqlBuilder.doGenerateCountSql(
//        column: ColumnExpression<*>?, params: ParamList,
//    ) = also {
//        appendWithClause(selects, params)
//        with(statement) { doGenerateCountSql(column, params) }
//    }
//}
