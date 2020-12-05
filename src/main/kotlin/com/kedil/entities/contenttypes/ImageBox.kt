package com.kedil.entities.contenttypes

import ContentEntity
import ContentType
import com.fasterxml.jackson.annotation.JsonProperty
import com.kedil.config.ContentTypes
import com.kedil.entities.Page
import com.kedil.entities.PageStructure
import com.kedil.entities.admin.AdminContent
import com.kedil.entities.blog.Blog
import com.kedil.entities.blog.BlogStructure
import com.relops.snowflake.Snowflake
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.transactions.transaction

object ImageBoxs : IdTable<Long>() {
    private val snowflake = Snowflake(8)
    override val id = long("imagebox_id").clientDefault { snowflake.next() }.entityId()
    val imageUrl = text("image_url")

    override val primaryKey = PrimaryKey(id)
}

class ImageBox(id: EntityID<Long>) : LongEntity(id), ContentEntity {
    companion object : LongEntityClass<ImageBox>(ImageBoxs)

    var imageUrl by ImageBoxs.imageUrl

    val imageBoxId
        get() = id.value

    override fun toSnippet() = ContentImageBox(
        this.imageUrl
    )

    override fun deleteEntity() {
        val it = this
        transaction {
            it.delete()
        }
    }
}


class ContentImageBox(@JsonProperty("image_url") val imageUrl: String) : ContentType {

    override fun createNew(newPosition: Long, newPage: Page): AdminContent {
        return transaction {
            val contentImage = ImageBox.new {
                this.imageUrl = this@ContentImageBox.imageUrl
            }
            val str = PageStructure.new {
                contentType = ContentTypes.IMAGE_BOX
                contentId = contentImage.imageBoxId
                position = newPosition
                page = newPage
            }
            AdminContent(
                str.pageStructureId.toString(),
                contentImage.toSnippet()
            )
        }
    }

    override fun createNewBlog(newPosition: Long, newBlog: Blog): AdminContent {
        return transaction {
            val contentImage = ImageBox.new {
                this.imageUrl = this@ContentImageBox.imageUrl
            }
            val str = BlogStructure.new {
                contentType = ContentTypes.IMAGE_BOX
                contentId = contentImage.imageBoxId
                position = newPosition
                blog = newBlog
            }
            AdminContent(
                str.blogStructureId.toString(),
                contentImage.toSnippet()
            )
        }
    }

    override fun edit(entity: ContentEntity): Boolean {
        if(entity !is ImageBox) {
            return false
        }

        transaction {
            entity.imageUrl = imageUrl
        }

        return true
    }

}
