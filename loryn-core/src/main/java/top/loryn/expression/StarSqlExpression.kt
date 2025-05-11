package top.loryn.expression

import top.loryn.database.SqlBuilder
import top.loryn.utils.SqlParamList

object StarSqlExpression : SqlExpression<Nothing> {
    override val sqlType get() = sqlTypeNoNeed()

    override fun buildSql(builder: SqlBuilder, params: SqlParamList, ignoreAlias: Boolean) {
        builder.append('*')
    }
}
