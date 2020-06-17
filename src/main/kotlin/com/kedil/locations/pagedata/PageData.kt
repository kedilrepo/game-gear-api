package com.kedil.locations.pagedata

import com.kedil.config.ContentTypes
import com.kedil.entities.*
import com.kedil.entities.contenttypes.Title
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.locations.Location
import io.ktor.locations.get
import io.ktor.response.respond
import io.ktor.routing.Routing
import org.jetbrains.exposed.sql.transactions.transaction

// TODO: ADD OPTIN INSTEAD OF EXPERIMENTAL WHATEVER


@OptIn(KtorExperimentalLocationsAPI::class)
@Location("/data") class Data() {
    @Location("/{pagename}") data class DataPage(val parent: Data, val pagename: String)
}


@OptIn(KtorExperimentalLocationsAPI::class)
fun Routing.data() {
    get<Data> {
        call.respond(HttpStatusCode.BadRequest, mapOf("Message" to "Please provide the pagename you want to get the data for."))
    }
    get<Data.DataPage> {
        pageName ->
        val searchedPage = transaction {
            Page.find { Pages.pageName eq pageName.pagename }.firstOrNull()
        }
                ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("Error" to "Page Not Found"))

        val returnSnippet = transaction {
            PageStructure.find { PageStructures.page eq searchedPage.pageID }.map {
                when (it.contentType) {
                    ContentTypes.TITLE -> transaction { Title.findById(it.contentId) }?.toSnippet()
                    ContentTypes.TEXT_NO_PICTURE -> transaction { TextNoPicture.findById(it.contentId)}?.toSnippet()
                    ContentTypes.TEXT_WITH_LEFT_PICTURE -> transaction { TextLeftPicture.findById(it.contentId)}?.toSnippet()
                    ContentTypes.TEXT_WITH_RIGHT_PICTURE -> transaction { TextRightPicture.findById(it.contentId)}?.toSnippet()

                    else -> {
                        null
                    }
                }
            }
        }

        call.respond(HttpStatusCode.Accepted, returnSnippet)
    }
}