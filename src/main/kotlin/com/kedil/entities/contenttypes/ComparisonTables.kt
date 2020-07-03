package com.kedil.entities.contenttypes

import ContentType
import com.relops.snowflake.Snowflake
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable

object ComparisonTables : IdTable<Long>() {
    private val snowflake = Snowflake(9)
    override val id = long("table_id").clientDefault { snowflake.next() }.entityId()
    val json = text("json_table")

    override val primaryKey = PrimaryKey(id)
}

class ComparisonTable(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<ComparisonTable>(ComparisonTables)

    var json by ComparisonTables.json

    val tableId
        get() = id.value

    fun toSnippet() = ContentComparisonTable(this.json)
}


data class ContentComparisonTable(
        val json: String
) : ContentType