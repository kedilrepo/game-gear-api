package com.kedil.locations.pagedata

import com.kedil.entities.ManagerSnippet
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.locations.Location
import io.ktor.locations.get
import io.ktor.request.ContentTransformationException
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Routing
import io.ktor.util.KtorExperimentalAPI

@KtorExperimentalLocationsAPI
@Location("/data") class Data() {
    @Location("/{pagename}") data class DataPage(val parent: Data, val pagename: String)
}

@KtorExperimentalAPI
@KtorExperimentalLocationsAPI
fun Routing.data() {
    get<Data> {
        call.respond(HttpStatusCode.BadRequest, mapOf("Message" to "Please provide the pagename you want to get the data for."))
    }
    get<Data.DataPage> {
        pagename ->


        call.respond(HttpStatusCode.Accepted, mapOf("pagename" to pagename.pagename))
    }
}