package top.loryn.database.transaction

import java.io.Closeable
import java.sql.Connection

enum class TransactionIsolation(val level: Int) {
    NONE(Connection.TRANSACTION_NONE),
    READ_COMMITTED(Connection.TRANSACTION_READ_COMMITTED),
    READ_UNCOMMITTED(Connection.TRANSACTION_READ_UNCOMMITTED),
    REPEATABLE_READ(Connection.TRANSACTION_REPEATABLE_READ),
    SERIALIZABLE(Connection.TRANSACTION_SERIALIZABLE),
    ;
}

interface Transaction : Closeable {
    val connection: Connection
    fun commit()
    fun rollback()
    override fun close()
}

interface TransactionManager {
    val defaultIsolation: TransactionIsolation?
    val currentTransaction: Transaction?
    fun newTransaction(isolation: TransactionIsolation? = defaultIsolation): Transaction
    fun newConnection(): Connection
}
