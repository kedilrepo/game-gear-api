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

object Infoboxs : IdTable<Long>() {
    private val snowflake = Snowflake(8)
    override val id = long("infoboxid").clientDefault { snowflake.next() }.entityId()
    val info = text("info")

    override val primaryKey = PrimaryKey(id)
}

class Infobox(id: EntityID<Long>) : LongEntity(id), ContentEntity {
    companion object : LongEntityClass<Infobox>(Infoboxs)

    var info by Infoboxs.info

    val infoboxId
        get() = id.value

    override fun toSnippet() = ContentInfobox(
            this.info
    )

    override fun deleteEntity() {
        val it = this
        transaction {
            it.delete()
        }
    }
}


class ContentInfobox(val info: String) : ContentType {

    override fun createNew(newPosition: Long, newPage: Page): AdminContent {
        return transaction {
            val contentInfo = Infobox.new {
                this.info = this@ContentInfobox.info
            }
            val str = PageStructure.new {
                contentType = ContentTypes.INFO_BOX
                contentId = contentInfo.infoboxId
                position = newPosition
                page = newPage
            }
            AdminContent(
                    str.pageStructureId.toString(),
                    contentInfo.toSnippet()
            )
        }
    }

    override fun edit(entity: ContentEntity): Boolean {
        if(entity !is Infobox) {
            return false
        }

        transaction {
            entity.info = info
        }

        return true
    }

}
