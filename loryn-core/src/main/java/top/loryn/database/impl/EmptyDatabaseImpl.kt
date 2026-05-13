package top.loryn.database.impl

import org.slf4j.Logger
import top.loryn.database.Database
import top.loryn.database.transaction.Transaction
import top.loryn.database.transaction.TransactionIsolation
import java.sql.Connection

object EmptyDatabaseImpl : Database {
    private fun notSupported(): Nothing =
        throw UnsupportedOperationException("This database implementation does not support any operations.")

    override val logger get() = notSupported()
    override val dialect get() = notSupported()
    override val config get() = notSupported()
    override val metadata get() = notSupported()

    override fun withLogger(logger: Logger) = notSupported()
    override fun withLogger(name: String) = notSupported()
    override fun withLogger(clazz: Class<*>) = notSupported()

    override fun <T> useConnection(block: (Connection) -> T) = notSupported()
    override fun <T> useTransaction(
        rollbackFor: Class<out Throwable>,
        isolation: TransactionIsolation?,
        block: (Transaction) -> T,
    ) = notSupported()
}
