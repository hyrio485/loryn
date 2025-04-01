package top.loryn.expression

/**
 * 查询源表达式。
 *
 * @param E 绑定的查询结果的实体类型。
 */
abstract class QuerySourceExpression<E> : EntityCreator<E>, SqlExpression<Nothing> {
    abstract val columns: List<ColumnExpression<E, *>>
}
