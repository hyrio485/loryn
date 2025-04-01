package top.loryn.support.external

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import top.loryn.schema.Column
import top.loryn.schema.Table
import top.loryn.support.SqlType
import top.loryn.support.external.LorynObjectMapperHolder.objectMapper
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

// ObjectNode -> VARCHAR

val JsonObjectSqlType =
    JsonSqlType.transform(ObjectNode::class.java, { it as ObjectNode }, { it })

fun <E> Table<E>.jsonObject(name: String): Column<E, ObjectNode> {
    return registerColumn(name, JsonObjectSqlType)
}
