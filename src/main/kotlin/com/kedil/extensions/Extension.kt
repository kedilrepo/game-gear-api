package com.kedil.extensions

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseToken
import com.kedil.entities.admin.User
import com.kedil.entities.admin.Users
import io.ktor.application.ApplicationCall
import io.ktor.application.ApplicationCallPipeline
import io.ktor.http.HttpStatusCode
import io.ktor.http.auth.HttpAuthHeader
import io.ktor.http.auth.parseAuthorizationHeader
import io.ktor.request.authorization
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.RouteSelector
import io.ktor.routing.RouteSelectorEvaluation
import io.ktor.routing.RoutingResolveContext
import io.ktor.util.AttributeKey
import io.ktor.util.KtorExperimentalAPI
import org.jetbrains.exposed.sql.transactions.transaction

private val OAUTH_AUTHENTICATION = AttributeKey<FirebaseToken>("OAUTH_AUTHENTICATION")

val ApplicationCall.accessToken: FirebaseToken
    get() = attributes[OAUTH_AUTHENTICATION]

@KtorExperimentalAPI
fun Route.authorized(
    callback: Route.() -> Unit
): Route = with(callback) {
    intercept(ApplicationCallPipeline.Call) {
        val authorization = context.request.authorization()?.let { parseAuthorizationHeader(it) }
        if (authorization == null || authorization !is HttpAuthHeader.Single) {
            context.respond(HttpStatusCode.Unauthorized, "Missing authorization header")
            return@intercept finish()
        }

        val realToken = try {
            FirebaseAuth.getInstance().verifyIdToken(authorization.blob)
        } catch (e: FirebaseAuthException) {
            context.respond(HttpStatusCode.Unauthorized, "Invalid authorization header")
            return@intercept finish()
        }

        val u = transaction {
            User.find { Users.uid eq realToken.uid }.firstOrNull()
        }
        if(u == null) {
            context.respond(HttpStatusCode.Unauthorized, "Invalid authorization header")
            return@intercept finish()
        }

        proceedWith(context.attributes.put(OAUTH_AUTHENTICATION, realToken))
    }
}

inline fun Route.with(callback: Route.() -> Unit, builder: Route.() -> Unit): Route {
    val route = this.createChild(object : RouteSelector(1.0) {
        override fun evaluate(context: RoutingResolveContext, segmentIndex: Int): RouteSelectorEvaluation =
            RouteSelectorEvaluation.Constant
    })

    builder(route)

    return route.apply(callback)
}