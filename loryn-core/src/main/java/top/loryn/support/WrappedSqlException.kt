package top.loryn.support

import java.sql.SQLException

data class WrappedSqlException(
    val e: SQLException,
    val sql: String? = null,
) : RuntimeException("Error querying database. Cause: $e", e) {
    override fun toString() = "$e; sql: $sql"
}
