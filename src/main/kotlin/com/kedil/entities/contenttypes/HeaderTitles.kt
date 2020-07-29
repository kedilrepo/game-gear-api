package com.kedil.entities.contenttypes

import ContentEntity
import ContentType
import com.fasterxml.jackson.annotation.JsonProperty
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

object HeaderTitles : IdTable<Long>() {
    private val snowflake = Snowflake(2)
    override val id = long("titles_id").clientDefault { snowflake.next() }.entityId()
    val title = varchar("title", 300)
    val lightTitle = bool("lightTitle").default(true)
    val backgroundImage = varchar("backgroundImage", 300).nullable()
    val subTitle = text("subTitle").nullable()
    override val primaryKey = PrimaryKey(id)
}

class HeaderTitle(id: EntityID<Long>) : LongEntity(id), ContentEntity {
    companion object : LongEntityClass<HeaderTitle>(HeaderTitles)

    var title by HeaderTitles.title
    var lightTitle by HeaderTitles.lightTitle
    var backgroundImage by HeaderTitles.backgroundImage
    var subTitle by HeaderTitles.subTitle
    val titleId
        get() = id.value


    override fun toSnippet() = ContentTitle(
            this.title,
            this.lightTitle,
            this.backgroundImage,
            this.subTitle
    )

    override fun deleteEntity() {
        val it = this
        transaction {
            it.delete()
        }
    }
}

class ContentTitle(
        val title: String,
        @JsonProperty("light_title") val lightTitle: Boolean,
        @JsonProperty("background_image") val backgroundImage: String?,
        @JsonProperty("sub_title") val subTitle: String?
) : ContentType {

    override fun createNew(newPosition: Long, newPage: Page): AdminContent {
        println(this.title)
        return transaction {
            println(title)
            val it = this
            val title = HeaderTitle.new {
                this.title = this@ContentTitle.title
                this.lightTitle = this@ContentTitle.lightTitle
                this.backgroundImage = this@ContentTitle.backgroundImage
                this.subTitle = this@ContentTitle.subTitle
            }
            val str = PageStructure.new {
                contentType = ContentTypes.TITLE
                contentId = title.titleId
                position = newPosition
                page = newPage
            }
            AdminContent(
                    str.pageStructureId.toString(),
                    title.toSnippet()
            )
        }
    }

    override fun edit(entity: ContentEntity): Boolean {
        if(entity !is HeaderTitle) {
            return false
        }

        transaction {
            entity.title = title
            entity.lightTitle = lightTitle
            entity.backgroundImage = backgroundImage
            entity.subTitle = subTitle
        }

        return true
    }

}
