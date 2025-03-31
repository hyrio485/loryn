package top.loryn.database.transaction

import java.sql.Connection

interface TransactionManager {
    val defaultIsolation: TransactionIsolation?
    val currentTransaction: Transaction?
    fun newTransaction(isolation: TransactionIsolation? = defaultIsolation): Transaction
    fun newConnection(): Connection
}
