package com.kedil.locations.admin.account

import com.google.api.gax.rpc.InvalidArgumentException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.kedil.entities.admin.LoginSnippet
import com.kedil.entities.admin.Users
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.locations.Location
import io.ktor.locations.post
import io.ktor.request.ContentTransformationException
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Routing
import org.jetbrains.exposed.sql.transactions.transaction
import com.kedil.entities.admin.User as UserEntity


@KtorExperimentalLocationsAPI
@Location("/admin/user") class User() {
    @Location("login") data class Login(val parent: User)
}


@KtorExperimentalLocationsAPI
fun Routing.login() {
    post<User.Login> {
        val loginSnippet = try {
            call.receive<LoginSnippet>()
        } catch (e: ContentTransformationException) {
            return@post call.respond(HttpStatusCode.BadRequest, mapOf("Error" to "False JSON"))
        }

        val decodedToken = try {
            FirebaseAuth.getInstance().verifyIdToken(loginSnippet.idToken)
        } catch (e: FirebaseAuthException) {
            return@post call.respond(HttpStatusCode.BadRequest, mapOf("Error" to "Invalid User"))
        } catch (e: InvalidArgumentException) {
            return@post call.respond(HttpStatusCode.BadRequest, mapOf("Error" to "False JSON"))
        }

        val uid = decodedToken.uid

        print(uid)

        transaction {
            UserEntity.find { Users.uid eq uid }.firstOrNull()
        } ?: return@post call.respond(HttpStatusCode.Unauthorized, mapOf("Error" to "Invalid User"))

        call.respond(HttpStatusCode.Accepted, mapOf("Message" to "Successful Login"))
    }
}