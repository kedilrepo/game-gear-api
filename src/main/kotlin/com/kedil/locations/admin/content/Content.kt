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
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction

@KtorExperimentalLocationsAPI
@Location("/admin/manage") class Manage() {
    @Location("/addOn") data class AddOn(val parent: Manage)
    @Location("insert") data class Insert(val parent: Manage)
    @Location("/delete") data class Delete(val parent: Manage)
}

@KtorExperimentalAPI
@KtorExperimentalLocationsAPI
fun Routing.content() {
    post<Manage.AddOn> {
        // TODO: VERIFY USER!!!
        val toManageData = try {
            call.receive<ManagerSnippet>()
        } catch (e: ContentTransformationException) {
            return@post call.respond(HttpStatusCode.BadRequest, mapOf("Error" to "Can't transform JSON"))
        }

        val searchedPage = transaction {
            Page.find { Pages.pageName eq toManageData.page }.firstOrNull()
        }
            ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("Error" to "Page Not Found"))

        // Deleted everything on page if set true
        if(!toManageData.addOn) deleteAllStructures(searchedPage)

        // Maybe even more deleting

        // Get last position
        val lastPosition = transaction {
            val structure = PageStructure.find {PageStructures.page eq searchedPage.pageID }.orderBy(PageStructures.position to SortOrder.DESC).limit(1).firstOrNull()
                ?: return@transaction 0L
            structure.position
        }

        var nextPosition = lastPosition.plus(1)


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
                            page = searchedPage
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
                            contentType = ContentTypes.TEXT_WITH_RIGHT_PICTURE
                            contentId = contentTRP.trpId
                            position = nextPosition
                            page = searchedPage
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
                            contentType = ContentTypes.TEXT_WITH_LEFT_PICTURE
                            contentId = contentTLP.tlpId
                            position = nextPosition
                            page = searchedPage
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
                            contentType = ContentTypes.TEXT_NO_PICTURE
                            contentId = contentNP.tnpId
                            position = nextPosition
                            page = searchedPage
                        }
                    }
                }
                else -> {
                    print("Not found this type of Module")
                }
            }
            nextPosition++
        }

        // To add data in between: alle, bei denen position über (Zahl) ist, selecten und dort die position +1 setzen -> an (Zahl) einsetzen

        call.respond(HttpStatusCode.Created, mapOf("Message" to "Created content"))
    }
    post<Manage.Insert> {
        val insertData = try {
            call.receive<InsertSnippet>()
        } catch (e: ContentTransformationException) {
            return@post call.respond(HttpStatusCode.BadRequest, mapOf("Error" to "Can't transform JSON"))
        }

        val searchedPage = transaction {
            Page.find { Pages.pageName eq insertData.page }.firstOrNull()
        }
                ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("Error" to "Page Not Found"))

        val maxPosition = transaction {
            val structure = PageStructure.find { PageStructures.page eq searchedPage.pageID }.orderBy(PageStructures.position to SortOrder.DESC).limit(1).firstOrNull()
                    ?: return@transaction 0L
            structure.position
        }

        //var nextPosition = insertData.position

        val nextPosition = if(insertData.position > maxPosition + 1)
            (maxPosition + 1)
        else
            insertData.position

        transaction {
            PageStructure.find { (PageStructures.page eq searchedPage.pageID) and (PageStructures.position greaterEq nextPosition) }.map {
                it.position++
            }
        }

        when(insertData.content) {
            is ContentTitle -> {
                transaction {
                    val title = Title.new {
                        title = insertData.content.title
                        backgroundImage = insertData.content.backgroundImage
                        subTitle = insertData.content.subTitle
                    }
                    PageStructure.new {
                        contentType = ContentTypes.TITLE
                        contentId = title.titleId
                        position = nextPosition
                        page = searchedPage
                    }
                }
            }
            is ContentTextRightPicture -> {
                transaction {
                    val contentTRP = TextRightPicture.new {
                        title = insertData.content.title
                        imageUrl = insertData.content.imageUrl
                        mainText = insertData.content.mainText
                    }
                    PageStructure.new {
                        contentType = ContentTypes.TEXT_WITH_RIGHT_PICTURE
                        contentId = contentTRP.trpId
                        position = nextPosition
                        page = searchedPage
                    }
                }
            }
            is ContentTextLeftPicture -> {
                transaction {
                    val contentTLP = TextLeftPicture.new {
                        title = insertData.content.title
                        imageUrl = insertData.content.imageUrl
                        mainText = insertData.content.mainText
                    }
                    PageStructure.new {
                        contentType = ContentTypes.TEXT_WITH_LEFT_PICTURE
                        contentId = contentTLP.tlpId
                        position = nextPosition
                        page = searchedPage
                    }
                }
            }
            is ContentTextNoPicture -> {
                transaction {
                    val contentNP = TextNoPicture.new {
                        title = insertData.content.title
                        mainText = insertData.content.mainText
                    }
                    PageStructure.new {
                        contentType = ContentTypes.TEXT_NO_PICTURE
                        contentId = contentNP.tnpId
                        position = nextPosition
                        page = searchedPage
                    }
                }
            }
            else -> {
                print("Not found this type of Module")
            }
        }

        call.respond(HttpStatusCode.Created, mapOf("Message" to "Successfully inserted Data"))
    }
    post<Manage.Delete>{
        val deleteSnippet = try {
            call.receive<DeleteSnippet>()
        } catch (e: ContentTransformationException) {
            return@post call.respond(HttpStatusCode.BadRequest, mapOf("Error" to "Can't transform JSON"))
        }

        val success = transaction {
            val structure = PageStructure.findById(deleteSnippet.structureId) ?: return@transaction false
            deleteObject(structure)
            return@transaction true
        }

        if(success) {
            call.respond(HttpStatusCode.Accepted, mapOf("Message" to "Successfully deleted Data"))
        } else {
            call.respond(HttpStatusCode.BadRequest, mapOf("Error" to "Cannot find Structure!"))
        }
    }
}

fun deleteAllStructures(p: Page) {
    p.structures.map {
        deleteObject(it)
    }
}

fun deleteObject(it: PageStructure) {
    when (it.contentType) {
        ContentTypes.TITLE -> transaction { Title.findById(it.contentId)?.delete() }
        ContentTypes.TEXT_NO_PICTURE -> transaction { TextNoPicture.findById(it.contentId)?.delete() }
        ContentTypes.TEXT_WITH_LEFT_PICTURE -> transaction { TextLeftPicture.findById(it.contentId)?.delete() }
        ContentTypes.TEXT_WITH_RIGHT_PICTURE -> transaction { TextRightPicture.findById(it.contentId)?.delete() }

        else -> {
            print("")
        }
    }
    transaction { it.delete() }
}

