package top.loryn.database

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.jdbc.support.SQLErrorCodeSQLExceptionTranslator
import top.loryn.database.transaction.*
import top.loryn.expression.SqlAndParams
import top.loryn.expression.SqlParam
import top.loryn.support.LazyLogObject
import top.loryn.support.StdOutLogger
import top.loryn.support.WrappedSqlException
import java.sql.Connection
import java.sql.DatabaseMetaData
import java.sql.DriverManager
import java.sql.SQLException
import java.util.*
import javax.sql.DataSource
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

class Database(
    val transactionManager: TransactionManager,
    val logger: Logger = defaultLogger,
    val dialect: SqlDialect = detectDialectImplementation(),
    val exceptionTranslator: ((WrappedSqlException) -> Throwable?)? = null,
    val config: Config = Config(),
    metadata: Metadata? = null,
) {
    val metadata = metadata ?: useConnection { conn ->
        Metadata(conn.metaData)
    }.also {
        logger.info(
            "Connected to {}, productName: {}, productVersion: {}",
            it.url, it.productName, it.productVersion
        )
    }

    fun copy(
        transactionManager: TransactionManager = this.transactionManager,
        logger: Logger = this.logger,
        dialect: SqlDialect = this.dialect,
        exceptionTranslator: ((WrappedSqlException) -> Throwable?)? = this.exceptionTranslator,
        config: Config = this.config,
        metadata: Metadata = this.metadata,
    ) = Database(transactionManager, logger, dialect, exceptionTranslator, config, metadata)

    fun withLogger(logger: Logger) = copy(logger = logger)
    fun withLogger(name: String) = copy(logger = LoggerFactory.getLogger(name))
    fun withLogger(clazz: Class<*>) = copy(logger = LoggerFactory.getLogger(clazz))

    data class Config(
        val uppercaseKeywords: Boolean = true,
    )

    data class Metadata(
        val url: String,
        val productName: String,
        val productVersion: String,
        val keywords: Set<String>,
        val identifierQuoteString: String,
        val extraNameCharacters: String,
        val supportsMixedCaseIdentifiers: Boolean,
        val storesMixedCaseIdentifiers: Boolean,
        val storesUpperCaseIdentifiers: Boolean,
        val storesLowerCaseIdentifiers: Boolean,
        val supportsMixedCaseQuotedIdentifiers: Boolean,
        val storesMixedCaseQuotedIdentifiers: Boolean,
        val storesUpperCaseQuotedIdentifiers: Boolean,
        val storesLowerCaseQuotedIdentifiers: Boolean,
        val maxColumnNameLength: Int,
    ) {
        private companion object {
            private inline fun <reified T> DatabaseMetaData.attr(block: DatabaseMetaData.() -> T) = try {
                block()
            } catch (_: Exception) {
                when (T::class) {
                    String::class -> ""
                    Boolean::class -> false
                    Int::class -> 0
                    else -> throw IllegalArgumentException("Unsupported type ${T::class}")
                } as T
            }
        }

        constructor(metadata: DatabaseMetaData) : this(
            metadata.attr { url },
            metadata.attr { databaseProductName },
            metadata.attr { databaseProductVersion },
            ANSI_SQL_2003_KEYWORDS + metadata.attr { sqlKeywords }.uppercase().split(','),
            metadata.attr { identifierQuoteString }.trim(),
            metadata.attr { extraNameCharacters },
            metadata.attr { supportsMixedCaseIdentifiers() },
            metadata.attr { storesMixedCaseIdentifiers() },
            metadata.attr { storesUpperCaseIdentifiers() },
            metadata.attr { storesLowerCaseIdentifiers() },
            metadata.attr { supportsMixedCaseQuotedIdentifiers() },
            metadata.attr { storesMixedCaseQuotedIdentifiers() },
            metadata.attr { storesUpperCaseQuotedIdentifiers() },
            metadata.attr { storesLowerCaseQuotedIdentifiers() },
            metadata.attr { maxColumnNameLength },
        )
    }

    @OptIn(ExperimentalContracts::class)
    inline fun <T> useConnection(block: (Connection) -> T): T {
        contract {
            callsInPlace(block, InvocationKind.EXACTLY_ONCE)
        }
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

    @OptIn(ExperimentalContracts::class)
    inline fun <T> useTransaction(
        rollbackFor: Class<out Throwable> = Throwable::class.java,
        isolation: TransactionIsolation? = null,
        block: (Transaction) -> T,
    ): T {
        contract {
            callsInPlace(block, InvocationKind.EXACTLY_ONCE)
        }
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
                try {
                    if (throwable == null || !rollbackFor.isInstance(throwable)) {
                        transaction.commit()
                    } else {
                        transaction.rollback()
                    }
                } finally {
                    transaction.close()
                }
            }
        }
    }

    // region Logging

    fun showParams(args: List<SqlParam<*>>) {
        logger.debug("Parameters: {}", LazyLogObject {
            if (args.isEmpty()) {
                "<no parameters>"
            } else {
                args.joinToString { "${it.value}(${it.sqlType.javaClassName})" }
            }
        })
    }

    fun showSql(sql: String, args: List<SqlParam<*>>? = null) {
        logger.debug("SQL: {}", sql)
        args?.also(::showParams)
    }

    fun showSql(sqlAndParams: SqlAndParams) {
        showSql(sqlAndParams.sql, sqlAndParams.params)
    }

    fun showEffects(effects: Int) {
        logger.debug("Effects: {}", effects)
    }

    // endregion

    companion object {
        private val defaultLogger by lazy {
            try {
                val logger: Logger? = LoggerFactory.getLogger(Database::class.java)
                if (logger == null) {
                    System.err.println("Loryn(W): Failed to get logger, using System.out instead")
                    StdOutLogger
                } else {
                    val slf4jNopLoggerExists = try {
                        Class.forName("org.slf4j.helpers.NOPLogger")
                        true
                    } catch (_: ClassNotFoundException) {
                        false
                    }
                    if (slf4jNopLoggerExists && logger is org.slf4j.helpers.NOPLogger) {
                        System.err.println("Loryn(W): Got org.slf4j.helpers.NOPLogger, using System.out instead")
                        StdOutLogger
                    } else logger
                }
            } catch (_: Exception) {
                System.err.println("Loryn(W): Error occurred while getting logger, using System.out instead")
                StdOutLogger
            }
        }

        private fun detectDialectImplementation(): SqlDialect {
            val dialects = ServiceLoader.load(SqlDialect::class.java).toList()
            return when (dialects.size) {
                0 -> object : SqlDialect {}
                1 -> dialects[0]
                else -> error("Detected more than one dialect implementations, please specify one manually: $dialects")
            }
        }

        fun connect(
            logger: Logger = defaultLogger,
            dialect: SqlDialect = detectDialectImplementation(),
            exceptionTranslator: ((WrappedSqlException) -> Throwable?)? = null,
            config: Config = Config(),
            connector: () -> Connection,
        ) = Database(
            transactionManager = JdbcTransactionManager(connector = connector),
            logger = logger,
            dialect = dialect,
            exceptionTranslator = exceptionTranslator,
            config = config,
        )

        fun connect(
            dataSource: DataSource,
            logger: Logger = defaultLogger,
            dialect: SqlDialect = detectDialectImplementation(),
            exceptionTranslator: ((WrappedSqlException) -> Throwable?)? = null,
            config: Config = Config(),
        ) = Database(
            transactionManager = JdbcTransactionManager { dataSource.connection },
            logger = logger,
            dialect = dialect,
            exceptionTranslator = exceptionTranslator,
            config = config,
        )

        fun connect(
            url: String, user: String? = null, password: String? = null, driver: String? = null,
            logger: Logger = defaultLogger,
            dialect: SqlDialect = detectDialectImplementation(),
            exceptionTranslator: ((WrappedSqlException) -> Throwable?)? = null,
            config: Config = Config(),
        ): Database {
            if (!driver.isNullOrBlank()) {
                Class.forName(driver)
            }
            return Database(
                transactionManager = JdbcTransactionManager { DriverManager.getConnection(url, user, password) },
                logger = logger,
                dialect = dialect,
                exceptionTranslator = exceptionTranslator,
                config = config,
            )
        }

        fun connectWithSpringSupport(
            dataSource: DataSource,
            logger: Logger = defaultLogger,
            dialect: SqlDialect = detectDialectImplementation(),
            config: Config = Config(),
        ): Database {
            val translator = SQLErrorCodeSQLExceptionTranslator(dataSource)
            return Database(
                transactionManager = SpringManagedTransactionManager(dataSource),
                dialect = dialect,
                logger = logger,
                exceptionTranslator = {
                    val (sqlException, sql) = it
                    translator.translate("[Loryn] ${it.message}", sql, sqlException)
                },
                config = config,
            )
        }

        /**
         * Keywords in SQL:2003 standard, all in uppercase.
         * See https://ronsavage.github.io/SQL/sql-2003-2.bnf.html#key%20word
         */
        private val ANSI_SQL_2003_KEYWORDS = setOf(
            "A",
            "ABS",
            "ABSOLUTE",
            "ACTION",
            "ADA",
            "ADD",
            "ADMIN",
            "AFTER",
            "ALL",
            "ALLOCATE",
            "ALTER",
            "ALWAYS",
            "AND",
            "ANY",
            "ARE",
            "ARRAY",
            "AS",
            "ASC",
            "ASENSITIVE",
            "ASSERTION",
            "ASSIGNMENT",
            "ASYMMETRIC",
            "AT",
            "ATOMIC",
            "ATTRIBUTE",
            "ATTRIBUTES",
            "AUTHORIZATION",
            "AVG",
            "BEFORE",
            "BEGIN",
            "BERNOULLI",
            "BETWEEN",
            "BIGINT",
            "BINARY",
            "BLOB",
            "BOOLEAN",
            "BOTH",
            "BREADTH",
            "BY",
            "C",
            "CALL",
            "CALLED",
            "CARDINALITY",
            "CASCADE",
            "CASCADED",
            "CASE",
            "CAST",
            "CATALOG",
            "CATALOG_NAME",
            "CEIL",
            "CEILING",
            "CHAIN",
            "CHAR",
            "CHARACTER",
            "CHARACTERISTICS",
            "CHARACTERS",
            "CHARACTER_LENGTH",
            "CHARACTER_SET_CATALOG",
            "CHARACTER_SET_NAME",
            "CHARACTER_SET_SCHEMA",
            "CHAR_LENGTH",
            "CHECK",
            "CHECKED",
            "CLASS_ORIGIN",
            "CLOB",
            "CLOSE",
            "COALESCE",
            "COBOL",
            "CODE_UNITS",
            "COLLATE",
            "COLLATION",
            "COLLATION_CATALOG",
            "COLLATION_NAME",
            "COLLATION_SCHEMA",
            "COLLECT",
            "COLUMN",
            "COLUMN_NAME",
            "COMMAND_FUNCTION",
            "COMMAND_FUNCTION_CODE",
            "COMMIT",
            "COMMITTED",
            "CONDITION",
            "CONDITION_NUMBER",
            "CONNECT",
            "CONNECTION_NAME",
            "CONSTRAINT",
            "CONSTRAINTS",
            "CONSTRAINT_CATALOG",
            "CONSTRAINT_NAME",
            "CONSTRAINT_SCHEMA",
            "CONSTRUCTORS",
            "CONTAINS",
            "CONTINUE",
            "CONVERT",
            "CORR",
            "CORRESPONDING",
            "COUNT",
            "COVAR_POP",
            "COVAR_SAMP",
            "CREATE",
            "CROSS",
            "CUBE",
            "CUME_DIST",
            "CURRENT",
            "CURRENT_COLLATION",
            "CURRENT_DATE",
            "CURRENT_DEFAULT_TRANSFORM_GROUP",
            "CURRENT_PATH",
            "CURRENT_ROLE",
            "CURRENT_TIME",
            "CURRENT_TIMESTAMP",
            "CURRENT_TRANSFORM_GROUP_FOR_TYPE",
            "CURRENT_USER",
            "CURSOR",
            "CURSOR_NAME",
            "CYCLE",
            "DATA",
            "DATE",
            "DATETIME_INTERVAL_CODE",
            "DATETIME_INTERVAL_PRECISION",
            "DAY",
            "DEALLOCATE",
            "DEC",
            "DECIMAL",
            "DECLARE",
            "DEFAULT",
            "DEFAULTS",
            "DEFERRABLE",
            "DEFERRED",
            "DEFINED",
            "DEFINER",
            "DEGREE",
            "DELETE",
            "DENSE_RANK",
            "DEPTH",
            "DEREF",
            "DERIVED",
            "DESC",
            "DESCRIBE",
            "DESCRIPTOR",
            "DETERMINISTIC",
            "DIAGNOSTICS",
            "DISCONNECT",
            "DISPATCH",
            "DISTINCT",
            "DOMAIN",
            "DOUBLE",
            "DROP",
            "DYNAMIC",
            "DYNAMIC_FUNCTION",
            "DYNAMIC_FUNCTION_CODE",
            "EACH",
            "ELEMENT",
            "ELSE",
            "END",
            "END-EXEC",
            "EQUALS",
            "ESCAPE",
            "EVERY",
            "EXCEPT",
            "EXCEPTION",
            "EXCLUDE",
            "EXCLUDING",
            "EXEC",
            "EXECUTE",
            "EXISTS",
            "EXP",
            "EXTERNAL",
            "EXTRACT",
            "FALSE",
            "FETCH",
            "FILTER",
            "FINAL",
            "FIRST",
            "FLOAT",
            "FLOOR",
            "FOLLOWING",
            "FOR",
            "FOREIGN",
            "FORTRAN",
            "FOUND",
            "FREE",
            "FROM",
            "FULL",
            "FUNCTION",
            "FUSION",
            "G",
            "GENERAL",
            "GET",
            "GLOBAL",
            "GO",
            "GOTO",
            "GRANT",
            "GRANTED",
            "GROUP",
            "GROUPING",
            "HAVING",
            "HIERARCHY",
            "HOLD",
            "HOUR",
            "IDENTITY",
            "IMMEDIATE",
            "IMPLEMENTATION",
            "IN",
            "INCLUDING",
            "INCREMENT",
            "INDICATOR",
            "INITIALLY",
            "INNER",
            "INOUT",
            "INPUT",
            "INSENSITIVE",
            "INSERT",
            "INSTANCE",
            "INSTANTIABLE",
            "INT",
            "INTEGER",
            "INTERSECT",
            "INTERSECTION",
            "INTERVAL",
            "INTO",
            "INVOKER",
            "IS",
            "ISOLATION",
            "ISOLATION",
            "JOIN",
            "K",
            "KEY",
            "KEY_MEMBER",
            "KEY_TYPE",
            "LANGUAGE",
            "LARGE",
            "LAST",
            "LATERAL",
            "LEADING",
            "LEFT",
            "LENGTH",
            "LEVEL",
            "LIKE",
            "LN",
            "LOCAL",
            "LOCALTIME",
            "LOCALTIMESTAMP",
            "LOCATOR",
            "LOWER",
            "M",
            "MAP",
            "MATCH",
            "MATCHED",
            "MAX",
            "MAXVALUE",
            "MEMBER",
            "MERGE",
            "MESSAGE_LENGTH",
            "MESSAGE_OCTET_LENGTH",
            "MESSAGE_TEXT",
            "METHOD",
            "MIN",
            "MINUTE",
            "MINVALUE",
            "MOD",
            "MODIFIES",
            "MODULE",
            "MONTH",
            "MORE",
            "MULTISET",
            "MUMPS",
            "NAME",
            "NAMES",
            "NATIONAL",
            "NATURAL",
            "NCHAR",
            "NCLOB",
            "NESTING",
            "NEW",
            "NEXT",
            "NO",
            "NONE",
            "NORMALIZE",
            "NORMALIZED",
            "NOT",
            "NULL",
            "NULLABLE",
            "NULLIF",
            "NULLS",
            "NUMBER",
            "NUMERIC",
            "OBJECT",
            "OCTETS",
            "OCTET_LENGTH",
            "OF",
            "OLD",
            "ON",
            "ONLY",
            "OPEN",
            "OPTION",
            "OPTIONS",
            "OR",
            "ORDER",
            "ORDERING",
            "ORDINALITY",
            "OTHERS",
            "OUT",
            "OUTER",
            "OUTPUT",
            "OVER",
            "OVERLAPS",
            "OVERLAY",
            "OVERRIDING",
            "PAD",
            "PARAMETER",
            "PARAMETER_MODE",
            "PARAMETER_NAME",
            "PARAMETER_ORDINAL_POSITION",
            "PARAMETER_SPECIFIC_CATALOG",
            "PARAMETER_SPECIFIC_NAME",
            "PARAMETER_SPECIFIC_SCHEMA",
            "PARTIAL",
            "PARTITION",
            "PASCAL",
            "PATH",
            "PERCENTILE_CONT",
            "PERCENTILE_DISC",
            "PERCENT_RANK",
            "PLACING",
            "PLI",
            "POSITION",
            "POWER",
            "PRECEDING",
            "PRECISION",
            "PREPARE",
            "PRESERVE",
            "PRIMARY",
            "PRIOR",
            "PRIVILEGES",
            "PROCEDURE",
            "PUBLIC",
            "RANGE",
            "RANK",
            "READ",
            "READS",
            "REAL",
            "RECURSIVE",
            "REF",
            "REFERENCES",
            "REFERENCING",
            "REGR_AVGX",
            "REGR_AVGY",
            "REGR_COUNT",
            "REGR_INTERCEPT",
            "REGR_R2",
            "REGR_SLOPE",
            "REGR_SXX",
            "REGR_SXY",
            "REGR_SYY",
            "RELATIVE",
            "RELEASE",
            "REPEATABLE",
            "RESTART",
            "RESULT",
            "RETURN",
            "RETURNED_CARDINALITY",
            "RETURNED_LENGTH",
            "RETURNED_OCTET_LENGTH",
            "RETURNED_SQLSTATE",
            "RETURNS",
            "REVOKE",
            "RIGHT",
            "ROLE",
            "ROLLBACK",
            "ROLLUP",
            "ROUTINE",
            "ROUTINE_CATALOG",
            "ROUTINE_NAME",
            "ROUTINE_SCHEMA",
            "ROW",
            "ROWS",
            "ROW_COUNT",
            "ROW_NUMBER",
            "SAVEPOINT",
            "SCALE",
            "SCHEMA",
            "SCHEMA_NAME",
            "SCOPE_CATALOG",
            "SCOPE_NAME",
            "SCOPE_SCHEMA",
            "SCROLL",
            "SEARCH",
            "SECOND",
            "SECTION",
            "SECURITY",
            "SELECT",
            "SELF",
            "SENSITIVE",
            "SEQUENCE",
            "SERIALIZABLE",
            "SERVER_NAME",
            "SESSION",
            "SESSION_USER",
            "SET",
            "SETS",
            "SIMILAR",
            "SIMPLE",
            "SIZE",
            "SMALLINT",
            "SOME",
            "SOURCE",
            "SPACE",
            "SPECIFIC",
            "SPECIFICTYPE",
            "SPECIFIC_NAME",
            "SQL",
            "SQLEXCEPTION",
            "SQLSTATE",
            "SQLWARNING",
            "SQRT",
            "START",
            "STATE",
            "STATEMENT",
            "STATIC",
            "STDDEV_POP",
            "STDDEV_SAMP",
            "STRUCTURE",
            "STYLE",
            "SUBCLASS_ORIGIN",
            "SUBMULTISET",
            "SUBSTRING",
            "SUM",
            "SYMMETRIC",
            "SYSTEM",
            "SYSTEM_USER",
            "TABLE",
            "TABLESAMPLE",
            "TABLE_NAME",
            "TEMPORARY",
            "THEN",
            "TIES",
            "TIME",
            "TIMESTAMP",
            "TIMEZONE_HOUR",
            "TIMEZONE_MINUTE",
            "TO",
            "TOP_LEVEL_COUNT",
            "TRAILING",
            "TRANSACTION",
            "TRANSACTIONS_COMMITTED",
            "TRANSACTIONS_ROLLED_BACK",
            "TRANSACTION_ACTIVE",
            "TRANSFORM",
            "TRANSFORMS",
            "TRANSLATE",
            "TRANSLATION",
            "TREAT",
            "TRIGGER",
            "TRIGGER_CATALOG",
            "TRIGGER_NAME",
            "TRIGGER_SCHEMA",
            "TRIM",
            "TRUE",
            "TYPE",
            "UESCAPE",
            "UNBOUNDED",
            "UNCOMMITTED",
            "UNDER",
            "UNION",
            "UNIQUE",
            "UNKNOWN",
            "UNNAMED",
            "UNNEST",
            "UPDATE",
            "UPPER",
            "USAGE",
            "USER",
            "USER_DEFINED_TYPE_CATALOG",
            "USER_DEFINED_TYPE_CODE",
            "USER_DEFINED_TYPE_NAME",
            "USER_DEFINED_TYPE_SCHEMA",
            "USING",
            "VALUE",
            "VALUES",
            "VARCHAR",
            "VARYING",
            "VAR_POP",
            "VAR_SAMP",
            "VIEW",
            "WHEN",
            "WHENEVER",
            "WHERE",
            "WIDTH_BUCKET",
            "WINDOW",
            "WITH",
            "WITHIN",
            "WITHOUT",
            "WORK",
            "WRITE",
            "YEAR",
            "ZONE"
        )
    }
}
