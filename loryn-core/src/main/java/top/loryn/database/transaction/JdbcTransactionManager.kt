package top.loryn.database.transaction

import java.sql.Connection

class JdbcTransactionManager(
    override val defaultIsolation: TransactionIsolation? = null,
    val connector: () -> Connection,
) : TransactionManager {
    private val threadLocal = ThreadLocal<Transaction>()

    override val currentTransaction: Transaction? get() = threadLocal.get()

    override fun newTransaction(isolation: TransactionIsolation?): Transaction {
        if (currentTransaction != null) {
            error("Current thread is already in a transaction.")
        }
        return JdbcTransaction(isolation).apply { threadLocal.set(this) }
    }

    override fun newConnection() = connector.invoke()

    private inner class JdbcTransaction(
        val desiredIsolation: TransactionIsolation?,
    ) : Transaction {
        private var originIsolation = -1
        private var originAutoCommit = true

        private val connectionLazy = lazy(LazyThreadSafetyMode.NONE) {
            newConnection().apply {
                try {
                    if (desiredIsolation != null) {
                        originIsolation = transactionIsolation
                        if (originIsolation != desiredIsolation.level) {
                            transactionIsolation = desiredIsolation.level
                        }
                    }
                    originAutoCommit = autoCommit
                    if (originAutoCommit) {
                        autoCommit = false
                    }
                } catch (e: Throwable) {
                    closeSilently()
                    throw e
                }
            }
        }

        override val connection: Connection by connectionLazy

        override fun commit() {
            if (connectionLazy.isInitialized()) {
                connection.commit()
            }
        }

        override fun rollback() {
            if (connectionLazy.isInitialized()) {
                connection.rollback()
            }
        }

        override fun close() {
            try {
                if (connectionLazy.isInitialized() && !connection.isClosed) {
                    connection.closeSilently()
                }
            } finally {
                threadLocal.remove()
            }
        }

        private fun Connection.closeSilently() {
            try {
                if (desiredIsolation != null && originIsolation != desiredIsolation.level) {
                    transactionIsolation = originIsolation
                }
                if (originAutoCommit) {
                    autoCommit = true
                }
            } catch (_: Throwable) {
            } finally {
                try {
                    close()
                } catch (_: Throwable) {
                }
            }
        }
    }
}
