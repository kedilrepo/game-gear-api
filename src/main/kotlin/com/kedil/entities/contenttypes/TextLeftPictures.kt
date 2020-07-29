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

object TextLeftPictures : IdTable<Long>() {
    private val snowflake = Snowflake(3)
    override val id = long("textleftpictureid").clientDefault { snowflake.next() }.entityId()
    val title = varchar("title", 100)
    val imageUrl = varchar("imageUrl", 300)
    val mainText = text("text")

    override val primaryKey = PrimaryKey(id)
}

class TextLeftPicture(id: EntityID<Long>) : LongEntity(id), ContentEntity {
    companion object : LongEntityClass<TextLeftPicture>(TextLeftPictures)

    var title by TextLeftPictures.title
    var imageUrl by TextLeftPictures.imageUrl
    var mainText by TextLeftPictures.mainText

    val tlpId
        get() = id.value

    override fun toSnippet() = ContentTextLeftPicture(
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

class ContentTextLeftPicture(val title: String, @JsonProperty("image_url") val imageUrl: String, @JsonProperty("main_text") val mainText: String) : ContentType {

    override fun createNew(newPosition: Long, newPage: Page): AdminContent {
        return transaction {
            val contentTLP = TextLeftPicture.new {
                this.title = this@ContentTextLeftPicture.title
                this.imageUrl = this@ContentTextLeftPicture.imageUrl
                this.mainText = this@ContentTextLeftPicture.mainText
            }
            val str = PageStructure.new {
                contentType = ContentTypes.TEXT_WITH_LEFT_PICTURE
                contentId = contentTLP.tlpId
                position = newPosition
                page = newPage
            }
            AdminContent(
                    str.pageStructureId.toString(),
                    contentTLP.toSnippet()
            )
        }
    }

    override fun edit(entity: ContentEntity): Boolean {
        if(entity !is TextLeftPicture) {
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
