package top.loryn.dialect.mysql

import top.loryn.database.SqlBuilder
import top.loryn.support.PaginationParams

open class MysqlSqlBuilder(
    keywords: Set<String>,
    uppercaseKeywords: Boolean,
) : SqlBuilder(keywords, uppercaseKeywords) {
    override fun appendPagination(paginationParams: PaginationParams) = also {
        val (currentPage, pageSize) = paginationParams
        appendKeyword("LIMIT").append(' ').append((currentPage - 1) * pageSize).append(',').append(' ').append(pageSize)
    }

    override fun end() = append(';')
}
