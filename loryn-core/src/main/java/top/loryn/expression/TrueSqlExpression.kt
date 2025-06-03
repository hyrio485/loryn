package top.loryn.expression

import top.loryn.database.SqlBuilder
import top.loryn.support.BooleanSqlType
import top.loryn.utils.SqlParamList

object TrueSqlExpression : SqlExpression<Boolean> {
    override val sqlType get() = BooleanSqlType

    override fun buildSql(builder: SqlBuilder, params: SqlParamList, ignoreAlias: Boolean) {
        builder.append('1').append(' ').appendKeyword("=").append(' ').append('1')
    }
}
