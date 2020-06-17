import com.fasterxml.jackson.annotation.JsonProperty
import com.relops.snowflake.Snowflake
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable

object TextNoPictures : IdTable<Long>() {
    private val snowflake = Snowflake(3)
    override val id = long("textnopictureid").clientDefault { snowflake.next() }.entityId()
    val title = varchar("title", 100)
    val mainText = text("text")

    override val primaryKey = PrimaryKey(id)
}

class TextNoPicture(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<TextNoPicture>(TextNoPictures)

    var title by TextNoPictures.title
    var mainText by TextNoPictures.mainText

    val tnpId
        get() = id.value

    fun toSnippet() = ContentTextNoPicture(
        this.title,
        this.mainText
    )
}


data class ContentTextNoPicture(val title: String, @JsonProperty("main_text") val mainText: String) : ContentType
