package top.loryn.database.transaction

import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy
import javax.sql.DataSource

class SpringManagedTransactionManager(dataSource: DataSource) : TransactionManager {
    private val proxy =
        dataSource as? TransactionAwareDataSourceProxy ?: TransactionAwareDataSourceProxy(dataSource)

    override val defaultIsolation: TransactionIsolation? = null

    override val currentTransaction: Transaction? = null

    override fun newTransaction(isolation: TransactionIsolation?): Nothing {
        throw UnsupportedOperationException(
            "Transaction is managed by Spring, please use Spring's @Transactional annotation instead."
        )
    }

    override fun newConnection() = proxy.connection
}
