package com.kedil

import TextLeftPictures
import TextNoPictures
import TextRightPictures
import io.ktor.application.*
import io.ktor.response.*
import io.ktor.request.*
import io.ktor.routing.*
import io.ktor.http.*
import com.fasterxml.jackson.databind.*
import com.kedil.config.Config
import com.kedil.entities.PageStructures
import com.kedil.entities.Pages
import com.kedil.entities.contenttypes.Titles
import com.kedil.locations.admin.content.content
import com.kedil.locations.pagedata.data
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.jackson.*
import io.ktor.features.*
import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.locations.Locations
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.util.KtorExperimentalAPI
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

fun main(args: Array<String>) {
    embeddedServer(Netty, port = 8082, module = Application::mainModule).start(wait = true)
}

@KtorExperimentalAPI
@KtorExperimentalLocationsAPI
@Suppress("unused") // Referenced in application.conf
fun Application.mainModule() {
    val config = HikariConfig().apply {
        jdbcUrl = Config.JDBC_STRING
        username = Config.DB_USER
        password = Config.DB_PASSWORD
    }
    val ds = HikariDataSource(config)

    Database.connect(ds)

    transaction {
        SchemaUtils.createMissingTablesAndColumns(Pages, PageStructures, Titles, TextLeftPictures, TextRightPictures, TextNoPictures)
    }

    install(ContentNegotiation) {
        jackson {
            enable(SerializationFeature.INDENT_OUTPUT)
        }
    }
    install(Locations)

    routing {
        data()
        content()
        get("/") {
            call.respondText("Welcome to the Game-Gear API", ContentType.Text.Plain)
        }
    }
}

