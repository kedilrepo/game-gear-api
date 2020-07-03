import com.fasterxml.jackson.annotation.*
import com.kedil.config.ContentTypes
import com.kedil.entities.contenttypes.ContentComparisonTable
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
    JsonSubTypes.Type(ContentTextNoPicture::class, name = ContentTypes.TEXT_NO_PICTURE),
    JsonSubTypes.Type(ContentAd::class, name = ContentTypes.AD),
    JsonSubTypes.Type(ContentInfobox::class, name = ContentTypes.INFO_BOX),
    JsonSubTypes.Type(ContentComparisonTable::class, name = ContentTypes.COMPARISON_TABLE)
)
interface ContentType {}

class DelegatedContentList(private val list: List<ContentType>) : List<ContentType> by list

class ContentAd : ContentType