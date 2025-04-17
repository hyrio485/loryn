package top.loryn.expression

import top.loryn.database.SqlBuilder
import top.loryn.utils.SqlParamList

class NullSqlExpression<T> : SqlExpression<T> {
    override val sqlType get() = throw UnsupportedOperationException("NullSqlExpression does not have a sqlType")

    override fun buildSql(builder: SqlBuilder, params: SqlParamList) {
        builder.appendKeyword("NULL")
    }
}
