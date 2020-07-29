package com.kedil.entities.contenttypes

import ContentEntity
import ContentType
import com.kedil.config.ContentTypes
import com.kedil.entities.Page
import com.kedil.entities.PageStructure
import com.kedil.entities.admin.AdminContent
import com.relops.snowflake.Snowflake
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.transactions.transaction

object ComparisonTables : IdTable<Long>() {
    private val snowflake = Snowflake(9)
    override val id = long("table_id").clientDefault { snowflake.next() }.entityId()
    val json = text("json_table")

    override val primaryKey = PrimaryKey(id)
}

class ComparisonTable(id: EntityID<Long>) : LongEntity(id), ContentEntity {
    companion object : LongEntityClass<ComparisonTable>(ComparisonTables)

    var json by ComparisonTables.json

    val tableId
        get() = id.value

    override fun toSnippet() = ContentComparisonTable(this.json)

    override fun deleteEntity() {
        val it = this
        transaction {
            it.delete()
        }
    }
}


class ContentComparisonTable(
        val json: String
) : ContentType {
    override fun createNew(newPosition: Long, newPage: Page): AdminContent {
       return transaction {
           val contentTable = ComparisonTable.new {
               this.json = this@ContentComparisonTable.json
           }
           val str = PageStructure.new {
               contentType = ContentTypes.COMPARISON_TABLE
               contentId = contentTable.tableId
               position = newPosition
               page = newPage
           }
           AdminContent(
                   str.pageStructureId.toString(),
                   contentTable.toSnippet()
           )
       }
    }

    override fun edit(entity: ContentEntity): Boolean {
        if(entity !is ComparisonTable) {
            return false;
        }
        transaction {
            entity.json = json
        }
        return true;
    }
}