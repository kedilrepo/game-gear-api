package com.kedil.entities.contenttypes

import ContentType
import com.fasterxml.jackson.annotation.JsonProperty
import com.relops.snowflake.Snowflake
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable

object Titles : IdTable<Long>() {
    private val snowflake = Snowflake(2)
    override val id = long("titles_id").clientDefault { snowflake.next() }.entityId()
    val title = varchar("title", 300)
    val backgroundImage = varchar("backgroundImage", 300).nullable()
    val subTitle = text("subTitle").nullable()
    override val primaryKey = PrimaryKey(id)
}

class Title(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<Title>(Titles)

    var title by Titles.title
    var backgroundImage by Titles.backgroundImage
    var subTitle by Titles.subTitle
    val titleId
        get() = id.value
}

data class ContentTitle(val title: String, @JsonProperty("background_image") val backgroundImage: String, @JsonProperty("sub_title") val subTitle: String) : ContentType
