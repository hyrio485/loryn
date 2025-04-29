package top.loryn.support

import java.math.BigDecimal
import java.sql.*
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.*
import javax.sql.rowset.serial.SerialBlob
import kotlin.reflect.KClass
import java.sql.Date as SqlDate
import java.util.Date as JavaDate

// Boolean -> BOOLEAN

object BooleanSqlType : SqlType<Boolean>(JDBCType.BOOLEAN, Boolean::class.java) {
    override fun doSetParameter(ps: PreparedStatement, index: Int, parameter: Boolean) {
        ps.setBoolean(index, parameter)
    }

    override fun doGetResult(rs: ResultSet, index: Int): Boolean? {
        return rs.getBoolean(index)
    }
}

// Int -> INTEGER

object IntSqlType : SqlType<Int>(JDBCType.INTEGER, Int::class.java) {
    override fun doSetParameter(ps: PreparedStatement, index: Int, parameter: Int) {
        ps.setInt(index, parameter)
    }

    override fun doGetResult(rs: ResultSet, index: Int): Int? {
        return rs.getInt(index)
    }
}

// Short -> SMALLINT

object ShortSqlType : SqlType<Short>(JDBCType.SMALLINT, Short::class.java) {
    override fun doSetParameter(ps: PreparedStatement, index: Int, parameter: Short) {
        ps.setShort(index, parameter)
    }

    override fun doGetResult(rs: ResultSet, index: Int): Short? {
        return rs.getShort(index)
    }
}

// Long -> BIGINT

object LongSqlType : SqlType<Long>(JDBCType.BIGINT, Long::class.java) {
    override fun doSetParameter(ps: PreparedStatement, index: Int, parameter: Long) {
        ps.setLong(index, parameter)
    }

    override fun doGetResult(rs: ResultSet, index: Int): Long? {
        return rs.getLong(index)
    }
}

// Float -> FLOAT

object FloatSqlType : SqlType<Float>(JDBCType.FLOAT, Float::class.java) {
    override fun doSetParameter(ps: PreparedStatement, index: Int, parameter: Float) {
        ps.setFloat(index, parameter)
    }

    override fun doGetResult(rs: ResultSet, index: Int): Float? {
        return rs.getFloat(index)
    }
}

// Double -> DOUBLE

object DoubleSqlType : SqlType<Double>(JDBCType.DOUBLE, Double::class.java) {
    override fun doSetParameter(ps: PreparedStatement, index: Int, parameter: Double) {
        ps.setDouble(index, parameter)
    }

    override fun doGetResult(rs: ResultSet, index: Int): Double? {
        return rs.getDouble(index)
    }
}

// BigDecimal -> DECIMAL

object DecimalSqlType : SqlType<BigDecimal>(JDBCType.DECIMAL, BigDecimal::class.java) {
    override fun doSetParameter(ps: PreparedStatement, index: Int, parameter: BigDecimal) {
        ps.setBigDecimal(index, parameter)
    }

    override fun doGetResult(rs: ResultSet, index: Int): BigDecimal? {
        return rs.getBigDecimal(index)
    }
}

// String -> VARCHAR

object VarcharSqlType : SqlType<String>(JDBCType.VARCHAR, String::class.java) {
    override fun doSetParameter(ps: PreparedStatement, index: Int, parameter: String) {
        ps.setString(index, parameter)
    }

    override fun doGetResult(rs: ResultSet, index: Int): String? {
        return rs.getString(index)
    }
}

val StringSqlType = VarcharSqlType

// String -> LONGVARCHAR

object TextSqlType : SqlType<String>(JDBCType.LONGVARCHAR, String::class.java) {
    override fun doSetParameter(ps: PreparedStatement, index: Int, parameter: String) {
        ps.setString(index, parameter)
    }

    override fun doGetResult(rs: ResultSet, index: Int): String? {
        return rs.getString(index)
    }
}

// ByteArray -> BLOB

object BlobSqlType : SqlType<ByteArray>(JDBCType.BLOB, ByteArray::class.java) {
    override fun doSetParameter(ps: PreparedStatement, index: Int, parameter: ByteArray) {
        ps.setBlob(index, SerialBlob(parameter))
    }

    override fun doGetResult(rs: ResultSet, index: Int): ByteArray? {
        return rs.getBlob(index).let { blob ->
            try {
                blob.binaryStream.use { it.readBytes() }
            } finally {
                blob.free()
            }
        }
    }
}

// ByteArray -> BINARY

object BytesSqlType : SqlType<ByteArray>(JDBCType.BINARY, ByteArray::class.java) {
    override fun doSetParameter(ps: PreparedStatement, index: Int, parameter: ByteArray) {
        ps.setBytes(index, parameter)
    }

    override fun doGetResult(rs: ResultSet, index: Int): ByteArray? {
        return rs.getBytes(index)
    }
}

// Timestamp -> TIMESTAMP

object TimestampSqlType : SqlType<Timestamp>(JDBCType.TIMESTAMP, Timestamp::class.java) {
    override fun doSetParameter(ps: PreparedStatement, index: Int, parameter: Timestamp) {
        ps.setTimestamp(index, parameter)
    }

    override fun doGetResult(rs: ResultSet, index: Int): Timestamp? {
        return rs.getTimestamp(index)
    }
}

// Date -> DATE

object JdbcDateSqlType : SqlType<SqlDate>(JDBCType.DATE, SqlDate::class.java) {
    override fun doSetParameter(ps: PreparedStatement, index: Int, parameter: SqlDate) {
        ps.setDate(index, parameter)
    }

    override fun doGetResult(rs: ResultSet, index: Int): SqlDate? {
        return rs.getDate(index)
    }
}

// Date -> DATE

object JavaDateSqlType : SqlType<JavaDate>(JDBCType.TIMESTAMP, JavaDate::class.java) {
    override fun doSetParameter(ps: PreparedStatement, index: Int, parameter: JavaDate) {
        ps.setDate(index, SqlDate(parameter.time))
    }

    override fun doGetResult(rs: ResultSet, index: Int): JavaDate? {
        return rs.getTimestamp(index)?.let { JavaDate(it.time) }
    }
}

// Time -> TIME

object TimeSqlType : SqlType<Time>(JDBCType.TIME, Time::class.java) {
    override fun doSetParameter(ps: PreparedStatement, index: Int, parameter: Time) {
        ps.setTime(index, parameter)
    }

    override fun doGetResult(rs: ResultSet, index: Int): Time? {
        return rs.getTime(index)
    }
}

// Instant -> TIMESTAMP

object InstantSqlType : SqlType<Instant>(JDBCType.TIMESTAMP, Instant::class.java) {
    override fun doSetParameter(ps: PreparedStatement, index: Int, parameter: Instant) {
        ps.setTimestamp(index, Timestamp.from(parameter))
    }

    override fun doGetResult(rs: ResultSet, index: Int): Instant? {
        return rs.getTimestamp(index).toInstant()
    }
}

// LocalDateTime -> TIMESTAMP

object LocalDateTimeSqlType : SqlType<LocalDateTime>(JDBCType.TIMESTAMP, LocalDateTime::class.java) {
    override fun doSetParameter(ps: PreparedStatement, index: Int, parameter: LocalDateTime) {
        ps.setTimestamp(index, Timestamp.valueOf(parameter))
    }

    override fun doGetResult(rs: ResultSet, index: Int): LocalDateTime? {
        return rs.getTimestamp(index).toLocalDateTime()
    }
}

// LocalDate -> DATE

object LocalDateSqlType : SqlType<LocalDate>(JDBCType.DATE, LocalDate::class.java) {
    override fun doSetParameter(ps: PreparedStatement, index: Int, parameter: LocalDate) {
        ps.setDate(index, SqlDate.valueOf(parameter))
    }

    override fun doGetResult(rs: ResultSet, index: Int): LocalDate? {
        return rs.getDate(index).toLocalDate()
    }
}

// LocalTime -> TIME

object LocalTimeSqlType : SqlType<LocalTime>(JDBCType.TIME, LocalTime::class.java) {
    override fun doSetParameter(ps: PreparedStatement, index: Int, parameter: LocalTime) {
        ps.setTime(index, Time.valueOf(parameter))
    }

    override fun doGetResult(rs: ResultSet, index: Int): LocalTime? {
        return rs.getTime(index).toLocalTime()
    }
}

// MonthDay -> VARCHAR

object MonthDaySqlType : SqlType<MonthDay>(JDBCType.VARCHAR, MonthDay::class.java) {
    private val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MM-dd")

    override fun doSetParameter(ps: PreparedStatement, index: Int, parameter: MonthDay) {
        ps.setString(index, parameter.format(formatter))
    }

    override fun doGetResult(rs: ResultSet, index: Int): MonthDay? {
        return MonthDay.parse(rs.getString(index), formatter)
    }
}

// YearMonth -> VARCHAR

object YearMonthSqlType : SqlType<YearMonth>(JDBCType.VARCHAR, YearMonth::class.java) {
    private val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM")

    override fun doSetParameter(ps: PreparedStatement, index: Int, parameter: YearMonth) {
        ps.setString(index, parameter.format(formatter))
    }

    override fun doGetResult(rs: ResultSet, index: Int): YearMonth? {
        return YearMonth.parse(rs.getString(index), formatter)
    }
}

// Year -> INTEGER

object YearSqlType : SqlType<Year>(JDBCType.INTEGER, Year::class.java) {
    override fun doSetParameter(ps: PreparedStatement, index: Int, parameter: Year) {
        ps.setInt(index, parameter.value)
    }

    override fun doGetResult(rs: ResultSet, index: Int): Year? {
        return Year.of(rs.getInt(index))
    }
}

// UUID

object UuidSqlType : SqlType<UUID>(JDBCType.OTHER, UUID::class.java) {
    override fun doSetParameter(ps: PreparedStatement, index: Int, parameter: UUID) {
        ps.setObject(index, parameter)
    }

    override fun doGetResult(rs: ResultSet, index: Int): UUID? {
        return rs.getObject(index) as UUID
    }
}

// enum

class EnumSqlType<C : Enum<C>>(
    val enumClass: Class<C>,
) : SqlType<C>(JDBCType.OTHER, enumClass) {
    constructor(enumClass: KClass<C>) : this(enumClass.java)

    private companion object {
        private val pgStatementClass = try {
            Class.forName("org.postgresql.PGStatement")
        } catch (_: ClassNotFoundException) {
            null
        }

        private val PreparedStatement.isPgSql get() = pgStatementClass?.let(::isWrapperFor) == true
    }

    override fun doSetParameter(ps: PreparedStatement, index: Int, parameter: C) {
        throw UnsupportedOperationException("doSetParameter is not necessary for enum")
    }

    override fun setParameter(ps: PreparedStatement, index: Int, parameter: C?) {
        if (ps.isPgSql) {
            if (parameter == null) {
                ps.setNull(index, Types.OTHER)
            } else {
                ps.setObject(index, parameter.name, Types.OTHER)
            }
        } else {
            if (parameter == null) {
                ps.setNull(index, Types.VARCHAR)
            } else {
                ps.setString(index, parameter.name)
            }
        }
    }

    override fun doGetResult(rs: ResultSet, index: Int): C? {
        return rs.getString(index)?.let { java.lang.Enum.valueOf(enumClass, it) }
    }
}
