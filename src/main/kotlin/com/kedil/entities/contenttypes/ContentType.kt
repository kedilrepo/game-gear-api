import com.fasterxml.jackson.annotation.*
import com.kedil.config.ContentTypes
import com.kedil.entities.contenttypes.ContentTitle


@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type"
)
@JsonSubTypes(
    JsonSubTypes.Type(ContentTitle::class, name = ContentTypes.TITLE),
    JsonSubTypes.Type(ContentTextRightPicture::class, name = ContentTypes.TEXT_WITH_RIGHT_PICTURE),
    JsonSubTypes.Type(ContentTextLeftPicture::class, name = ContentTypes.TEXT_WITH_LEFT_PICTURE),
    JsonSubTypes.Type(ContentTextNoPicture::class, name = ContentTypes.TEXT_NO_PICTURE)
)
interface ContentType {}

class DelegatedContentList(private val list: List<ContentType>) : List<ContentType> by list