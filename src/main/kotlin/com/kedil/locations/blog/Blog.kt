package com.kedil.locations.blog

import DelegatedContentList
import com.kedil.entities.*
import com.kedil.entities.blog.BlogStructure
import com.kedil.entities.blog.BlogStructures
import com.kedil.entities.blog.Blogs
import com.kedil.entities.blog.BlogsSnippet
import com.kedil.entities.blog.Blog as BlogEntity
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.locations.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.*
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

@OptIn(KtorExperimentalLocationsAPI::class)
@Location("/blog")
class Blog{
    @Location("/{blogUrl}") data class NamedBlog(val parent: Blog, val blogUrl: String)
}

@KtorExperimentalAPI
@KtorExperimentalLocationsAPI
fun Routing.blog() {
    get<Blog> {
        val blogs = transaction {
            BlogEntity.all().orderBy(Blogs.lastEdited to SortOrder.DESC).map(BlogEntity::toBlogReturnSnippet)
        }

        call.respond(HttpStatusCode.Accepted, BlogsSnippet(blogs))
    }
    get<Blog.NamedBlog> {
        blogName ->

        val searchedBlog = transaction {
            BlogEntity.find { Blogs.blogUrl eq blogName.blogUrl }.firstOrNull()
        }
            ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("Error" to "Page Not Found"))


        val returnSnippet = transaction {
            BlogStructure.find { BlogStructures.blog eq searchedBlog.blogID }.orderBy(BlogStructures.position to SortOrder.ASC).map {
                when (val i = it.content()) {
                    null -> null

                    else -> i.toSnippet()
                }
            }
        }

        val toBeSerialized = DelegatedContentList(returnSnippet.filterNotNull())

        call.respond(HttpStatusCode.Accepted, searchedBlog.toBlogSnippet(toBeSerialized))
    }
}