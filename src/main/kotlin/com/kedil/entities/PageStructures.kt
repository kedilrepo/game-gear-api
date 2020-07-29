package com.kedil.entities

import ContentEntity
import ContentType
import com.fasterxml.jackson.annotation.JsonProperty
import com.kedil.config.ContentTypes
import com.kedil.entities.contenttypes.Ad
import com.kedil.entities.contenttypes.ComparisonTable
import com.kedil.entities.contenttypes.HeaderTitle
import com.relops.snowflake.Snowflake
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.transactions.transaction

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
    fun content() : ContentEntity?  {
        return when(contentType) {
            ContentTypes.TITLE -> transaction { HeaderTitle.findById(contentId) }
            ContentTypes.TEXT_NO_PICTURE -> transaction { TextNoPicture.findById(contentId) }
            ContentTypes.TEXT_WITH_LEFT_PICTURE -> transaction { TextLeftPicture.findById(contentId) }
            ContentTypes.TEXT_WITH_RIGHT_PICTURE -> transaction { TextRightPicture.findById(contentId) }
            ContentTypes.COMPARISON_TABLE -> transaction { ComparisonTable.findById(contentId) }
            ContentTypes.INFO_BOX -> transaction { Infobox.findById(contentId) }
            ContentTypes.AD -> Ad()
            else -> null
        }
    }
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
    val structureId: String
)

data class ChangePosition(
    @JsonProperty("new_position")
    val newPosition: Long,
    @JsonProperty("structure_id")
    val structureId: String
)

data class EditSnippet(
    val structureId: String,
    val content: ContentType
)
