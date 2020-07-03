import com.fasterxml.jackson.annotation.JsonProperty
import com.relops.snowflake.Snowflake
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable

object Infoboxs : IdTable<Long>() {
    private val snowflake = Snowflake(8)
    override val id = long("infoboxid").clientDefault { snowflake.next() }.entityId()
    val info = text("info")

    override val primaryKey = PrimaryKey(id)
}

class Infobox(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<Infobox>(Infoboxs)

    var info by Infoboxs.info

    val infoboxId
        get() = id.value

    fun toSnippet() = ContentInfobox(
            this.info
    )
}


data class ContentInfobox(val info: String) : ContentType
