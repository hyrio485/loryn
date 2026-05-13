package top.loryn.database.impl

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import top.loryn.database.Database
import top.loryn.database.SqlDialect
import top.loryn.database.transaction.Transaction
import top.loryn.database.transaction.TransactionIsolation
import top.loryn.database.transaction.TransactionManager
import top.loryn.support.WrappedSqlException
import java.sql.Connection
import java.sql.SQLException

class DefaultDatabaseImpl(
    val transactionManager: TransactionManager,
    override val logger: Logger = Database.defaultLogger,
    override val dialect: SqlDialect = Database.detectDialectImplementation(),
    val exceptionTranslator: ((WrappedSqlException) -> Throwable?)? = null,
    override val config: Database.Config = Database.Config(),
    metadata: Database.Metadata? = null,
) : Database {
    override val metadata = metadata ?: useConnection { conn ->
        Database.Metadata(conn.metaData)
    }.also {
        logger.info(
            "Connected to {}, productName: {}, productVersion: {}",
            it.url, it.productName, it.productVersion,
        )
    }

    fun copy(
        transactionManager: TransactionManager = this.transactionManager,
        logger: Logger = this.logger,
        dialect: SqlDialect = this.dialect,
        exceptionTranslator: ((WrappedSqlException) -> Throwable?)? = this.exceptionTranslator,
        config: Database.Config = this.config,
        metadata: Database.Metadata = this.metadata,
    ) = DefaultDatabaseImpl(transactionManager, logger, dialect, exceptionTranslator, config, metadata)

    override fun withLogger(logger: Logger) = copy(logger = logger)
    override fun withLogger(name: String) = copy(logger = LoggerFactory.getLogger(name))
    override fun withLogger(clazz: Class<*>) = copy(logger = LoggerFactory.getLogger(clazz))

    override fun <T> useConnection(block: (Connection) -> T): T {
        try {
            val transaction = transactionManager.currentTransaction
            val connection = transaction?.connection ?: transactionManager.newConnection()
            try {
                return block(connection)
            } finally {
                if (transaction == null) connection.close()
            }
        } catch (e: WrappedSqlException) {
            throw exceptionTranslator?.invoke(e) ?: e
        } catch (e: SQLException) {
            throw exceptionTranslator?.invoke(WrappedSqlException(e)) ?: e
        }
    }

    override fun <T> useTransaction(
        rollbackFor: Class<out Throwable>,
        isolation: TransactionIsolation?,
        block: (Transaction) -> T,
    ): T {
        val current = transactionManager.currentTransaction
        val isOuter = current == null
        val transaction = current ?: transactionManager.newTransaction(isolation)
        var throwable: Throwable? = null

        try {
            return block(transaction)
        } catch (e: WrappedSqlException) {
            throwable = exceptionTranslator?.invoke(e) ?: e
            throw throwable
        } catch (e: SQLException) {
            throwable = exceptionTranslator?.invoke(WrappedSqlException(e)) ?: e
            throw throwable
        } catch (e: Throwable) {
            throwable = e
            throw throwable
        } finally {
            if (isOuter) {
                transaction.use { transaction ->
                    if (throwable == null || !rollbackFor.isInstance(throwable)) {
                        transaction.commit()
                    } else {
                        transaction.rollback()
                    }
                }
            }
        }
    }
}
