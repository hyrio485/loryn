package top.loryn.support.external

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
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
        return rs.getString(index).takeUnless { it.isNullOrBlank() }?.let { objectMapper.readTree(it) }
    }
}

// ObjectNode -> VARCHAR

val JsonObjectSqlType = JsonSqlType.transform(ObjectNode::class.java, { it as ObjectNode }, { it })

inline fun <reified T> getJsonObjectBasedSqlType() = JsonObjectSqlType.transform(
    T::class.java,
    { objectMapper.treeToValue(it, T::class.java) },
    { objectMapper.valueToTree(it) },
)
