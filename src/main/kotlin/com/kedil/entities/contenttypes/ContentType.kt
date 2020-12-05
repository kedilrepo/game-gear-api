import com.fasterxml.jackson.annotation.*
import com.kedil.config.ContentTypes
import com.kedil.entities.Page
import com.kedil.entities.PageStructure
import com.kedil.entities.admin.AdminContent
import com.kedil.entities.blog.Blog
import com.kedil.entities.contenttypes.*
import org.jetbrains.exposed.sql.transactions.transaction


@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type"
)
@JsonSubTypes(
    JsonSubTypes.Type(ContentTitle::class, name = ContentTypes.TITLE),
    JsonSubTypes.Type(ContentTextRightPicture::class, name = ContentTypes.TEXT_WITH_RIGHT_PICTURE),
    JsonSubTypes.Type(ContentTextLeftPicture::class, name = ContentTypes.TEXT_WITH_LEFT_PICTURE),
    JsonSubTypes.Type(ContentTextNoPicture::class, name = ContentTypes.TEXT_NO_PICTURE),
    JsonSubTypes.Type(ContentAd::class, name = ContentTypes.AD),
    JsonSubTypes.Type(ContentInfobox::class, name = ContentTypes.INFO_BOX),
    JsonSubTypes.Type(ContentComparisonTable::class, name = ContentTypes.COMPARISON_TABLE),
    JsonSubTypes.Type(ContentImageBox::class, name = ContentTypes.IMAGE_BOX)
)
interface ContentType {
    fun createNew(newPosition: Long, newPage: Page) : AdminContent

    fun createNewBlog(newPosition: Long, newBlog: Blog) : AdminContent

    fun edit(entity: ContentEntity) : Boolean
}

interface ContentEntity {
    fun deleteEntity()

    fun toSnippet() : ContentType
}

class DelegatedContentList(private val list: List<ContentType>) : List<ContentType> by list