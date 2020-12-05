package com.kedil.entities.blog

import ContentType
import DelegatedContentList
import com.kedil.entities.PageReturnSnippet
import com.kedil.entities.PageSnippet
import com.relops.snowflake.Snowflake
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable

object Blogs : IdTable<Long>() {
    private val snowflake = Snowflake(2)
    override val id = long("page_id").clientDefault { snowflake.next() }.entityId()
    val blogName = text("blogName")
    val blogUrl = varchar("blogUrl", 100)
    val lastEdited = long("last_edited").default(0)

    override val primaryKey = PrimaryKey(id)
}

class Blog(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<Blog>(Blogs)

    var blogName by Blogs.blogName
    var blogUrl by Blogs.blogUrl
    var lastEdited by Blogs.lastEdited
    val structures by BlogStructure referrersOn BlogStructures.blog
    val blogID
        get() = id.value

    fun toBlogReturnSnippet() = BlogReturnSnippet(this.blogName, blogUrl, this.blogID.toString())

    fun toBlogSnippet(data: DelegatedContentList) = PageSnippet(data, this.lastEdited)
}

data class BlogCreationSnippet(
    val blogName: String
)

data class BlogReturnSnippet(val blogName: String, val blogUrl: String, val blogId: String)

data class BlogsSnippet(val blogs: List<BlogReturnSnippet>)

data class BlogDeletionSnippet(
    val blogID: String
)

data class BlogEditSnippet(
    val blogID: String,
    val newBlogName: String
)