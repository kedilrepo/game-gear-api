package com.kedil.locations.pagedata

import ContentAd
import DelegatedContentList
import Infobox
import com.kedil.config.ContentTypes
import com.kedil.entities.*
import com.kedil.entities.contenttypes.ComparisonTable
import com.kedil.entities.contenttypes.HeaderTitle
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.locations.Location
import io.ktor.locations.get
import io.ktor.locations.post
import io.ktor.request.ContentTransformationException
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Routing
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.transactions.transaction

// TODO: ADD OPTIN INSTEAD OF EXPERIMENTAL WHATEVER


@OptIn(KtorExperimentalLocationsAPI::class)
@Location("/data") class Data()


@OptIn(KtorExperimentalLocationsAPI::class)
fun Routing.data() {
    get<Data> {
        call.respond(HttpStatusCode.BadRequest, mapOf("Message" to "Please provide the pagename you want to get the data for. (and do a post-request)"))
    }
    post<Data> {

        val pageName = try {
            call.receive<PageCreationSnippet>()
        } catch (e: ContentTransformationException) {
            return@post call.respond(HttpStatusCode.BadRequest, mapOf("Error" to "Can't transform JSON"))
        }

        val searchedPage = transaction {
            Page.find { Pages.pageName eq pageName.pageName }.firstOrNull()
        }
                ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("Error" to "Page Not Found"))


        val returnSnippet = transaction {
            PageStructure.find { PageStructures.page eq searchedPage.pageID }.orderBy(PageStructures.position to SortOrder.ASC).map {
                when (it.contentType) {
                    ContentTypes.TITLE -> transaction { HeaderTitle.findById(it.contentId) }?.toSnippet()
                    ContentTypes.TEXT_NO_PICTURE -> transaction { TextNoPicture.findById(it.contentId)}?.toSnippet()
                    ContentTypes.TEXT_WITH_LEFT_PICTURE -> transaction { TextLeftPicture.findById(it.contentId)}?.toSnippet()
                    ContentTypes.TEXT_WITH_RIGHT_PICTURE -> transaction { TextRightPicture.findById(it.contentId)}?.toSnippet()
                    ContentTypes.AD -> ContentAd()
                    ContentTypes.INFO_BOX -> transaction { Infobox.findById(it.contentId)}?.toSnippet()
                    ContentTypes.COMPARISON_TABLE -> transaction { ComparisonTable.findById(it.contentId)}?.toSnippet()

                    else -> null
                }
            }
        }

        val toBeSerialized = DelegatedContentList(returnSnippet.filterNotNull())

        call.respond(HttpStatusCode.Accepted, toBeSerialized)
    }
}