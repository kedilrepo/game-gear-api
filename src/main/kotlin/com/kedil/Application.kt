package com.kedil

import TextLeftPictures
import TextNoPictures
import TextRightPictures
import com.fasterxml.jackson.databind.SerializationFeature
import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.kedil.config.Config
import com.kedil.entities.PageStructures
import com.kedil.entities.Pages
import com.kedil.entities.admin.Users
import com.kedil.entities.contenttypes.HeaderTitles
import com.kedil.locations.admin.account.login
import com.kedil.locations.admin.content.content
import com.kedil.locations.pagedata.data
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.CORS
import io.ktor.features.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.jackson.jackson
import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.locations.Locations
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.util.KtorExperimentalAPI
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.FileInputStream


@KtorExperimentalAPI
fun main() {
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
        SchemaUtils.createMissingTablesAndColumns(Pages, PageStructures, HeaderTitles, TextLeftPictures, TextRightPictures, TextNoPictures, Users)
    }

    install(ContentNegotiation) {
        jackson {
            enable(SerializationFeature.INDENT_OUTPUT)
        }
    }
    install(Locations)
    install(CORS) {
        allowCredentials = true
        allowNonSimpleContentTypes = true
        allowSameOrigin = true
        anyHost()
    }

    val serviceAccount = FileInputStream("src/game-gear-firebase-adminsdk.json")

    val options = FirebaseOptions.Builder()
            .setCredentials(GoogleCredentials.fromStream(serviceAccount))
            .setDatabaseUrl("https://game-gear.firebaseio.com")
            .build()

    FirebaseApp.initializeApp(options)

    routing {
        data()
        content()
        login()
        get("/") {
            call.respondText("Welcome to the Game-Gear API", ContentType.Text.Plain)
        }
    }
}