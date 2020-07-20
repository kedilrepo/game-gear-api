package com.kedil.entities.contenttypes

import ContentType
import com.fasterxml.jackson.annotation.JsonProperty
import com.relops.snowflake.Snowflake
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable

object HeaderTitles : IdTable<Long>() {
    private val snowflake = Snowflake(2)
    override val id = long("titles_id").clientDefault { snowflake.next() }.entityId()
    val title = varchar("title", 300)
    val lightTitle = bool("lightTitle").default(true)
    val backgroundImage = varchar("backgroundImage", 300).nullable()
    val subTitle = text("subTitle").nullable()
    override val primaryKey = PrimaryKey(id)
}

class HeaderTitle(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<HeaderTitle>(HeaderTitles)

    var title by HeaderTitles.title
    var lightTitle by HeaderTitles.lightTitle
    var backgroundImage by HeaderTitles.backgroundImage
    var subTitle by HeaderTitles.subTitle
    val titleId
        get() = id.value


    fun toSnippet() = ContentTitle(
            this.title,
            this.lightTitle,
            this.backgroundImage,
            this.subTitle
    )
}

data class ContentTitle(
        val title: String,
        @JsonProperty("light_title") val lightTitle: Boolean,
        @JsonProperty("background_image") val backgroundImage: String?,
        @JsonProperty("sub_title") val subTitle: String?
) : ContentType
