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

object TextRightPictures : IdTable<Long>() {
    private val snowflake = Snowflake(3)
    override val id = long("textrightpictureid").clientDefault { snowflake.next() }.entityId()
    val title = varchar("title", 100)
    val imageUrl = varchar("imageUrl", 300)
    val mainText = text("text")

    override val primaryKey = PrimaryKey(id)
}

class TextRightPicture(id: EntityID<Long>) : LongEntity(id), ContentEntity {
    companion object : LongEntityClass<TextRightPicture>(TextRightPictures)

    var title by TextRightPictures.title
    var imageUrl by TextRightPictures.imageUrl
    var mainText by TextRightPictures.mainText

    val trpId
        get() = id.value

    override fun toSnippet() = ContentTextRightPicture(
        this.title,
        this.imageUrl,
        this.mainText
    )

    override fun deleteEntity() {
        val it = this
        transaction {
            it.delete()
        }
    }
}

class ContentTextRightPicture(val title: String, @JsonProperty("image_url") val imageUrl: String, @JsonProperty("main_text") val mainText: String) : ContentType {

    override fun createNew(newPosition: Long, newPage: Page): AdminContent {
        return transaction {
            val contentTRP = TextRightPicture.new {
                this.title = this@ContentTextRightPicture.title
                this.imageUrl = this@ContentTextRightPicture.imageUrl
                this.mainText = this@ContentTextRightPicture.mainText
            }
            val str = PageStructure.new {
                contentType = ContentTypes.TEXT_WITH_RIGHT_PICTURE
                contentId = contentTRP.trpId
                position = newPosition
                page = newPage
            }
            AdminContent(
                    str.pageStructureId.toString(),
                    contentTRP.toSnippet()
            )
        }
    }

    override fun createNewBlog(newPosition: Long, newBlog: Blog): AdminContent {
        return transaction {
            val contentTRP = TextRightPicture.new {
                this.title = this@ContentTextRightPicture.title
                this.imageUrl = this@ContentTextRightPicture.imageUrl
                this.mainText = this@ContentTextRightPicture.mainText
            }
            val str = BlogStructure.new {
                contentType = ContentTypes.TEXT_WITH_RIGHT_PICTURE
                contentId = contentTRP.trpId
                position = newPosition
                blog = newBlog
            }
            AdminContent(
                str.blogStructureId.toString(),
                contentTRP.toSnippet()
            )
        }
    }

    override fun edit(entity: ContentEntity): Boolean {
        if(entity !is TextRightPicture) {
            return false
        }

        transaction {
            entity.title = title
            entity.imageUrl = imageUrl
            entity.mainText = mainText
        }

        return true
    }
}
