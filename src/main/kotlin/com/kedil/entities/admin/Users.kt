package com.kedil.entities.admin

import com.fasterxml.jackson.annotation.JsonProperty
import com.relops.snowflake.Snowflake
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.Column

object Users : IdTable<Long>() {
    private val snowflake = Snowflake(5)
    override val id = long("user_id").clientDefault { snowflake.next()  }.entityId()
    val uid = varchar("uid", 300)
    

    override val primaryKey = PrimaryKey(id)
}

class User(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<User>(Users)

    val uid by Users.uid
    val dbId get() = id.value
}


data class LoginSnippet(
    @JsonProperty("id_token")
    val idToken: String
)