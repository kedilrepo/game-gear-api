import com.fasterxml.jackson.annotation.JsonProperty
import com.relops.snowflake.Snowflake
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable

object TextRightPictures : IdTable<Long>() {
    private val snowflake = Snowflake(3)
    override val id = long("textrightpictureid").clientDefault { snowflake.next() }.entityId()
    val title = varchar("title", 100)
    val imageUrl = varchar("imageUrl", 300)
    val mainText = text("text")

    override val primaryKey = PrimaryKey(id)
}

class TextRightPicture(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<TextRightPicture>(TextRightPictures)

    var title by TextRightPictures.title
    var imageUrl by TextRightPictures.imageUrl
    var mainText by TextRightPictures.mainText

    val trpId
        get() = id.value
}

data class ContentTextRightPicture(val title: String, @JsonProperty("image_url") val imageUrl: String, @JsonProperty("main_text") val mainText: String) : ContentType
