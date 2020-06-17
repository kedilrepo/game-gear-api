import com.fasterxml.jackson.annotation.JsonProperty
import com.relops.snowflake.Snowflake
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable

object TextLeftPictures : IdTable<Long>() {
    private val snowflake = Snowflake(3)
    override val id = long("textleftpictureid").clientDefault { snowflake.next() }.entityId()
    val title = varchar("title", 100)
    val imageUrl = varchar("imageUrl", 300)
    val mainText = text("text")

    override val primaryKey = PrimaryKey(id)
}

class TextLeftPicture(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<TextLeftPicture>(TextLeftPictures)

    var title by TextLeftPictures.title
    var imageUrl by TextLeftPictures.imageUrl
    var mainText by TextLeftPictures.mainText

    val tlpId
        get() = id.value

    fun toSnippet() = ContentTextLeftPicture(
        this.title,
        this.imageUrl,
        this.mainText
    )
}

data class ContentTextLeftPicture(val title: String, @JsonProperty("image_url") val imageUrl: String, @JsonProperty("main_text") val mainText: String) : ContentType
