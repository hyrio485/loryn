package top.loryn.support.external

import com.fasterxml.jackson.databind.JsonNode
import top.loryn.schema.Column
import top.loryn.schema.Table
import top.loryn.support.SqlType
import top.loryn.utils.LorynObjectMapperHolder.objectMapper
import java.sql.JDBCType
import java.sql.PreparedStatement
import java.sql.ResultSet

// JsonNode -> VARCHAR

object JsonSqlType : SqlType<JsonNode>(JDBCType.VARCHAR, JsonNode::class.java) {
    override fun doSetParameter(ps: PreparedStatement, index: Int, parameter: JsonNode) {
        ps.setString(index, parameter.toString())
    }

    override fun doGetResult(rs: ResultSet, index: Int): JsonNode? {
        return rs.getString(index).takeIf { it.isNotBlank() }?.let { objectMapper.readTree(it) }
    }
}

fun <E> Table<E>.json(name: String): Column<E, JsonNode> {
    return registerColumn(name, JsonSqlType)
}
