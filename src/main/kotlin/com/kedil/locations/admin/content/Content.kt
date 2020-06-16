package com.kedil.locations.admin.content

import ContentTextLeftPicture
import ContentTextNoPicture
import ContentTextRightPicture
import com.kedil.config.ContentTypes
import com.kedil.entities.*
import com.kedil.entities.contenttypes.ContentTitle
import com.kedil.entities.contenttypes.Title
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.locations.Location
import io.ktor.locations.post
import io.ktor.request.ContentTransformationException
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Routing
import io.ktor.util.KtorExperimentalAPI
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.max
import org.jetbrains.exposed.sql.transactions.transaction

@KtorExperimentalLocationsAPI
@Location("/admin") class Admin() {
    @Location("/manage") data class DataManager(val parent: Admin)
}

@KtorExperimentalAPI
@KtorExperimentalLocationsAPI
fun Routing.content() {
    post<Admin.DataManager> {
        // TODO: VERIFY USER!!!
        val toManageData = try {
            call.receive<ManagerSnippet>()
        } catch (e: ContentTransformationException) {
            call.respond(HttpStatusCode.BadRequest)
            return@post
        }

        val page = transaction {
            Page.find { Pages.pageName eq toManageData.page }.firstOrNull()
        }
            ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("Error" to "Page Not Found"))

        // Deleted everything on page if set true
        if(!toManageData.addOn) deleteAllStructures(page)

        // Maybe even more deleting

        // Get last position
        val lastPosition = transaction {
            val structure = PageStructure.find { (PageStructures.position eq PageStructures.position.max()) and (PageStructures.page eq page.pageID) }.firstOrNull()
                ?: return@transaction 0
            structure.position
        }

        var nextPosition = lastPosition + 1L


        // Add Data now just add the end (append)
        toManageData.content.map {
            when(it) {
                is ContentTitle -> {
                    transaction {
                        val title = Title.new {
                            title = it.title
                            backgroundImage = it.backgroundImage
                            subTitle = it.subTitle
                        }
                        PageStructure.new {
                            contentType = ContentTypes.TITLE
                            contentId = title.titleId
                            position = nextPosition
                        }
                    }
                }
                is ContentTextRightPicture -> {
                    transaction {
                        val contentTRP = TextRightPicture.new {
                            title = it.title
                            imageUrl = it.imageUrl
                            mainText = it.mainText
                        }
                        PageStructure.new {
                            contentType = ContentTypes.TITLE
                            contentId = contentTRP.trpId
                            position = nextPosition
                        }
                    }
                }
                is ContentTextLeftPicture -> {
                    transaction {
                        val contentTLP = TextLeftPicture.new {
                            title = it.title
                            imageUrl = it.imageUrl
                            mainText = it.mainText
                        }
                        PageStructure.new {
                            contentType = ContentTypes.TITLE
                            contentId = contentTLP.tlpId
                            position = nextPosition
                        }
                    }
                }
                is ContentTextNoPicture -> {
                    transaction {
                        val contentNP = TextNoPicture.new {
                            title = it.title
                            mainText = it.mainText
                        }
                        PageStructure.new {
                            contentType = ContentTypes.TITLE
                            contentId = contentNP.tnpId
                            position = nextPosition
                        }
                    }
                }
                else -> {
                    print("Not found this type of Module")
                }
            }
            nextPosition += 1L
        }

        // To add data in between: alle, bei denen position über (Zahl) ist, selecten und dort die position +1 setzen -> an (Zahl) einsetzen

        call.respond(HttpStatusCode.Created, mapOf("Message" to "Created content"))
    }
}

fun deleteAllStructures(p: Page) {
    p.structures.map {
        when (it.contentType) {
            ContentTypes.TITLE -> transaction { Title.findById(it.contentId)?.delete() }
            ContentTypes.TEXT_NO_PICTURE -> transaction { TextNoPicture.findById(it.contentId)?.delete() }
            ContentTypes.TEXT_WITH_LEFT_PCITURE -> transaction { TextLeftPicture.findById(it.contentId)?.delete() }
            ContentTypes.TEXT_WITH_RIGHT_PICTURE -> transaction { TextRightPicture.findById(it.contentId)?.delete() }

            else -> {
                print("")
            }
        }
        transaction { it.delete() }
    }
}

