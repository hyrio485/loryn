package top.loryn.dialect.mysql

import top.loryn.database.SqlBuilder
import top.loryn.database.SqlDialect

class MysqlSqlDialect : SqlDialect {
    override fun newSqlBuilder(keywords: Set<String>, uppercaseKeywords: Boolean): SqlBuilder {
        return MysqlSqlBuilder(keywords, uppercaseKeywords)
    }
}
