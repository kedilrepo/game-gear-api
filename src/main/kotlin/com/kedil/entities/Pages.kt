package com.kedil.entities

import com.relops.snowflake.Snowflake
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable

object Pages : IdTable<Long>() {
    private val snowflake = Snowflake(2)
    override val id = long("page_id").clientDefault { snowflake.next() }.entityId()
    val pageName = varchar("pageName", 100)

    override val primaryKey = PrimaryKey(id)
}

class Page(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<Page>(Pages)

    var pageName by Pages.pageName
    val structures by PageStructure referrersOn PageStructures.page
    val pageID
        get() = id.value
}