package top.loryn.support

import java.sql.SQLException

data class WrappedSqlException(val sqlException: SQLException, val sql: String? = null) :
    RuntimeException("Error querying database. Cause: $sqlException", sqlException)
