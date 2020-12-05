package com.kedil.entities.blog

import ContentEntity
import ContentType
import com.fasterxml.jackson.annotation.JsonProperty
import com.kedil.config.ContentTypes
import com.kedil.entities.Page
import com.kedil.entities.PageStructures.clientDefault
import com.kedil.entities.PageStructures.entityId
import com.kedil.entities.Pages
import com.kedil.entities.contenttypes.Ad
import com.kedil.entities.contenttypes.ComparisonTable
import com.kedil.entities.contenttypes.HeaderTitle
import com.kedil.entities.contenttypes.ImageBox
import com.relops.snowflake.Snowflake
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.transactions.transaction

object BlogStructures : IdTable<Long>() {
    private val snowflake = Snowflake(1)
    override val id = long("page_structure_id").clientDefault { snowflake.next() }.entityId()
    val blog = reference(
        "blogId",
        Blogs,
        onDelete = ReferenceOption.CASCADE,
        onUpdate = ReferenceOption.CASCADE
    )
    val contentType = varchar("contentType", length = 100)
    val contentId = long("contentId")
    val position = long("position")
    override val primaryKey = PrimaryKey(id)
}

class BlogStructure(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<BlogStructure>(BlogStructures)

    var contentType by BlogStructures.contentType
    var contentId by BlogStructures.contentId
    var blog by Blog referencedOn BlogStructures.blog
    var position by BlogStructures.position
    val blogStructureId
        get() = id.value
    fun content() : ContentEntity?  {
        when(contentType) {
            ContentTypes.TITLE -> return transaction { HeaderTitle.findById(contentId) }
            ContentTypes.TEXT_NO_PICTURE -> return transaction { TextNoPicture.findById(contentId) }
            ContentTypes.TEXT_WITH_LEFT_PICTURE -> return transaction { TextLeftPicture.findById(contentId) }
            ContentTypes.TEXT_WITH_RIGHT_PICTURE -> return transaction { TextRightPicture.findById(contentId) }
            ContentTypes.COMPARISON_TABLE -> return transaction { ComparisonTable.findById(contentId) }
            ContentTypes.INFO_BOX -> return transaction { Infobox.findById(contentId) }
            ContentTypes.AD -> return Ad()
            ContentTypes.IMAGE_BOX -> return transaction { ImageBox.findById(contentId) }
            else -> return null
        }
    }
}

data class BlogInsertSnippet(
    val blog: String,
    val content: ContentType,
    val position: Long
)