package top.loryn.database

interface SqlDialect {
    fun newSqlBuilder(keywords: Set<String>, uppercaseKeywords: Boolean): SqlBuilder {
        return SqlBuilder(keywords, uppercaseKeywords)
    }
}
