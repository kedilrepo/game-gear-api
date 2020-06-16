import com.fasterxml.jackson.annotation.*
import com.kedil.config.ContentTypes
import com.kedil.entities.contenttypes.ContentTitle
import javafx.scene.layout.BackgroundImage


@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type"
)
@JsonSubTypes(
    JsonSubTypes.Type(ContentTitle::class, name = ContentTypes.TITLE),
    JsonSubTypes.Type(ContentTextRightPicture::class, name = ContentTypes.TEXT_WITH_RIGHT_PICTURE),
    JsonSubTypes.Type(ContentTextLeftPicture::class, name = ContentTypes.TEXT_WITH_LEFT_PCITURE),
    JsonSubTypes.Type(ContentTextNoPicture::class, name = ContentTypes.TEXT_NO_PICTURE)
)
interface ContentType {}

