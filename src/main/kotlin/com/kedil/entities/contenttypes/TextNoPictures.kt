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

object TextNoPictures : IdTable<Long>() {
    private val snowflake = Snowflake(3)
    override val id = long("textnopictureid").clientDefault { snowflake.next() }.entityId()
    val title = varchar("title", 100)
    val mainText = text("text")

    override val primaryKey = PrimaryKey(id)
}

class TextNoPicture(id: EntityID<Long>) : LongEntity(id), ContentEntity {
    companion object : LongEntityClass<TextNoPicture>(TextNoPictures)

    var title by TextNoPictures.title
    var mainText by TextNoPictures.mainText

    val tnpId
        get() = id.value

    override fun toSnippet() = ContentTextNoPicture(
        this.title,
        this.mainText
    )

    override fun deleteEntity() {
        val it = this
        transaction {
            it.delete()
        }
    }
}


class ContentTextNoPicture(val title: String, @JsonProperty("main_text") val mainText: String) : ContentType {
    override fun createNew(newPosition: Long, newPage: Page): AdminContent {
        return transaction {
            val contentNP = TextNoPicture.new {
                this.title = this@ContentTextNoPicture.title
                this.mainText = this@ContentTextNoPicture.mainText
            }
            val str = PageStructure.new {
                contentType = ContentTypes.TEXT_NO_PICTURE
                contentId = contentNP.tnpId
                position = newPosition
                page = newPage
            }
            AdminContent(
                str.pageStructureId.toString(),
                contentNP.toSnippet()
            )
        }
    }

    override fun edit(entity: ContentEntity): Boolean {
        if(entity !is TextNoPicture) {
            return false
        }

        transaction {
            entity.title = title
            entity.mainText = mainText
        }

        return true
    }

}
