package com.kedil.entities

import ContentType
import com.fasterxml.jackson.annotation.JsonProperty
import com.relops.snowflake.Snowflake
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ReferenceOption

object PageStructures : IdTable<Long>() {
    private val snowflake = Snowflake(0)
    override val id = long("page_structure_id").clientDefault { snowflake.next() }.entityId()
    val page = reference("pageId", Pages, onDelete = ReferenceOption.CASCADE, onUpdate = ReferenceOption.CASCADE)
    val contentType = varchar("contentType", length = 100)
    val contentId = long("contentId")
    val position = long("position")
    override val primaryKey = PrimaryKey(id)
}

class PageStructure(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<PageStructure>(PageStructures)

    var contentType by PageStructures.contentType
    var contentId by PageStructures.contentId
    var page by Page referencedOn PageStructures.page
    var position by PageStructures.position
    val pageStructureId
        get() = id.value
}


data class ManagerSnippet(
    val page: String,
    val addOn: Boolean,
    val content: List<ContentType>
)

data class InsertSnippet(
    val page: String,
    val content: ContentType,
    val position: Long
)

data class DeleteSnippet(
    @JsonProperty("structure_id")
    val structureId: Long
)

data class ChangePosition(
    @JsonProperty("new_position")
    val newPosition: Long,
    @JsonProperty("structure_id")
    val structureId: Long
)