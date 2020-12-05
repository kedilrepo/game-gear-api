package com.kedil.entities.contenttypes

import ContentEntity
import ContentType
import com.kedil.config.ContentTypes
import com.kedil.entities.Page
import com.kedil.entities.PageStructure
import com.kedil.entities.admin.AdminContent
import com.kedil.entities.blog.Blog
import com.kedil.entities.blog.BlogStructure
import org.jetbrains.exposed.sql.transactions.transaction

class Ad : ContentEntity {
    override fun deleteEntity() {
        return
    }

    override fun toSnippet() = ContentAd()
}

class ContentAd : ContentType {

    override fun createNew(newPosition: Long, newPage: Page): AdminContent {
        return transaction {
            val str = PageStructure.new {
                contentType = ContentTypes.AD
                contentId = 0
                position = newPosition
                page = newPage
            }
            AdminContent(
                    str.pageStructureId.toString(),
                    ContentAd()
            )
        }
    }

    override fun createNewBlog(newPosition: Long, newBlog: Blog): AdminContent {
        return transaction {
            val str = BlogStructure.new {
                contentType = ContentTypes.AD
                contentId = 0
                position = newPosition
                blog = newBlog
            }
            AdminContent(
                str.blogStructureId.toString(),
                ContentAd()
            )
        }
    }

    override fun edit(entity: ContentEntity): Boolean {
        return true
    }
}