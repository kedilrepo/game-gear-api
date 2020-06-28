package com.kedil.locations.admin.account

import com.google.api.gax.rpc.InvalidArgumentException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.kedil.entities.admin.LoginSnippet
import com.kedil.entities.admin.UserAddSnippet
import com.kedil.entities.admin.Users
import com.kedil.extensions.authorized
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.locations.Location
import io.ktor.locations.post
import io.ktor.locations.put
import io.ktor.request.ContentTransformationException
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Routing
import io.ktor.util.KtorExperimentalAPI
import org.jetbrains.exposed.sql.transactions.transaction
import com.kedil.entities.admin.User as UserEntity


@KtorExperimentalLocationsAPI
@Location("/admin/user") class User() {
    @Location("login") data class Login(val parent: User)
    @Location("/manage") data class Manage(val parent: User)
}


@KtorExperimentalAPI
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

        val userID = decodedToken.uid

        print(userID)

        val isEmpty = transaction {
            UserEntity.all().empty()
        }

        if(isEmpty) {
            transaction {
                UserEntity.new {
                    uid = userID
                }
            }
        }

        transaction {
            UserEntity.find { Users.uid eq userID }.firstOrNull()
        } ?: return@post call.respond(HttpStatusCode.Unauthorized, mapOf("Error" to "Invalid User"))

        call.respond(HttpStatusCode.Accepted, mapOf("Message" to "Successful Login"))
    }
    authorized {
        put<User.Manage> {
            val userCreateSnippet = try {
                call.receive<UserAddSnippet>()
            } catch (e: ContentTransformationException) {
                return@put call.respond(HttpStatusCode.BadRequest, mapOf("Error" to "False JSON"))
            }

            val alreadyExists = transaction {
                UserEntity.find { Users.uid eq userCreateSnippet.userID }.firstOrNull()
            }

            if(alreadyExists != null) {
                return@put call.respond(HttpStatusCode.MultiStatus, mapOf("Error" to "User already valid"))
            }


            transaction {
                UserEntity.new {
                    uid = userCreateSnippet.userID
                }
            }

            call.respond(HttpStatusCode.Accepted, mapOf("Message" to "Successfully added User"))
        }
        post<User.Manage> {
            val userCreateSnippet = try {
                call.receive<UserAddSnippet>()
            } catch (e: ContentTransformationException) {
                return@post call.respond(HttpStatusCode.BadRequest, mapOf("Error" to "False JSON"))
            }

            val alreadyExists = transaction {
                UserEntity.find { Users.uid eq userCreateSnippet.userID }.firstOrNull()
            } ?: return@post call.respond(HttpStatusCode.Conflict, mapOf("Error" to "User does not exist"))

            transaction {
                alreadyExists.delete()
            }

            call.respond(HttpStatusCode.Accepted, mapOf("Message" to "Successfully removed User!"))
        }
    }
}