package com.kedil.locations.admin.content

import com.kedil.entities.*
import com.kedil.entities.admin.AdminContent
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.locations.*
import io.ktor.request.ContentTransformationException
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Routing
import com.kedil.extensions.authorized
import io.ktor.util.KtorExperimentalAPI
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction
import java.lang.NumberFormatException

@KtorExperimentalLocationsAPI
@Location("/admin/manage") class Manage() {
    @Location("/addOn") data class AddOn(val parent: Manage)
    @Location("/insert") data class Insert(val parent: Manage)
    @Location("/delete") data class Delete(val parent: Manage)
    @Location("/changePosition") data class ChangePosition(val parent: Manage)
    @Location("/pages") data class Pages(val parent: Manage) {
        @Location("/delete") data class PageDelete(val parent: Pages)
        @Location("/edit") data class PageEdit(val parent: Pages)
    }
    @Location("/structures") data class Structures(val parent: Manage) {
        @Location("/edit") data class StructureEditor(val parent: Structures)
    }
}


@KtorExperimentalAPI
@KtorExperimentalLocationsAPI
fun Routing.content() {
    authorized {
        post<Manage.AddOn> {
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
                it.createNew(newPosition = nextPosition, newPage = searchedPage)
                nextPosition++
            }

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

            println(insertData.position)

            val nextPosition = if(insertData.position > maxPosition + 1)
                (maxPosition + 1)
            else
                insertData.position

            println(nextPosition)

            println(nextPosition)

            transaction {
                PageStructure.find { (PageStructures.page eq searchedPage.pageID) and (PageStructures.position greaterEq nextPosition) }.map {
                    it.position++
                }
            }

            val newStructure = insertData.content.createNew(newPosition = nextPosition, newPage = searchedPage)

            call.respond(HttpStatusCode.Created, newStructure)
        }
        post<Manage.Delete>{
            val deleteSnippet = try {
                call.receive<DeleteSnippet>()
            } catch (e: ContentTransformationException) {
                return@post call.respond(HttpStatusCode.BadRequest, mapOf("Error" to "Can't transform JSON"))
            }

            val structureIdLong = try {
                deleteSnippet.structureId.toLong()
            } catch (e: NumberFormatException) {
                return@post call.respond(HttpStatusCode.BadRequest, mapOf("Error" to "No valid StructureId"))
            }

            val success = transaction {
                val structure = PageStructure.findById(structureIdLong) ?: return@transaction false
                transaction {
                    PageStructure.find { (PageStructures.page eq structure.page.pageID) and (PageStructures.position greater structure.position) }.map {
                        str ->
                        str.position--
                    }
                }
                deleteObject(structure)
                return@transaction true
            }

            if(success) {
                call.respond(HttpStatusCode.Accepted, mapOf("Message" to "Successfully deleted Data"))
            } else {
                call.respond(HttpStatusCode.BadRequest, mapOf("Error" to "Cannot find Structure!"))
            }
        }
        post<Manage.ChangePosition> {
            val changerSnippet = try {
                call.receive<ChangePosition>()
            } catch (e: ContentTransformationException) {
                return@post call.respond(HttpStatusCode.BadRequest, mapOf("Error" to "Can't transform JSON"))
            }

            val structureIdLong = try {
                changerSnippet.structureId.toLong()
            } catch (e: NumberFormatException) {
                return@post call.respond(HttpStatusCode.BadRequest, mapOf("Error" to "No valid StructureId"))
            }

            val structure = transaction {
                PageStructure.findById(structureIdLong)
            } ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("Error" to "Cannot find Structure!"))

            val maxPosition = transaction {
                val maxStructure = PageStructure.find { PageStructures.page eq structure.page.pageID }.orderBy(PageStructures.position to SortOrder.DESC).limit(1).firstOrNull()
                        ?: return@transaction 0L
                maxStructure.position
            }

            val newPosition = if(changerSnippet.newPosition > maxPosition)
                maxPosition + 1
            else
                changerSnippet.newPosition

            transaction {
                transaction {
                    PageStructure.find { (PageStructures.page eq structure.page.pageID) and ( (PageStructures.position less structure.position) and (PageStructures.position greaterEq newPosition) ) }.map {
                        it.position++
                    }
                }
                transaction {
                    PageStructure.find { (PageStructures.page eq structure.page.pageID) and ( (PageStructures.position greater structure.position) and (PageStructures.position lessEq newPosition) ) }.map {
                        it.position--
                    }
                }
                structure.position = changerSnippet.newPosition
            }

            call.respond(HttpStatusCode.Accepted, mapOf("Message" to "Successfully changed index of ${changerSnippet.structureId} to ${changerSnippet.newPosition}"))
        }
        get<Manage.Pages> {
            val pages = transaction {
                Page.all().map { it.toPageReturnSnippet() }
            }

            call.respond(HttpStatusCode.Accepted, pages)
        }
        post<Manage.Pages> {
            val pageCreator = try {
                call.receive<PageCreationSnippet>()
            } catch (e: ContentTransformationException){
                return@post call.respond(HttpStatusCode.BadRequest, mapOf("Error" to "Can't transform JSON"))
            }

            val exists = transaction {
                Page.find { Pages.pageName eq pageCreator.pageName }.firstOrNull()
            }
            if(exists != null){
                return@post call.respond(HttpStatusCode.Conflict, mapOf("Error" to "Page already exists"))
            }

            transaction {
                Page.new {
                    pageName = pageCreator.pageName
                }
            }

            call.respond(HttpStatusCode.Accepted, mapOf("Message" to "Successfully created page"))
        }
        post<Manage.Pages.PageDelete> {
            val pageToDeleteSnippet = try {
                call.receive<PageDeletionSnippet>()
            } catch (e: ContentTransformationException) {
                return@post call.respond(HttpStatusCode.BadRequest, mapOf("Error" to "Can't transform JSON"))
            }

            val pageIdLong = try {
                pageToDeleteSnippet.pageID.toLong()
            } catch (e: NumberFormatException) {
                return@post call.respond(HttpStatusCode.BadRequest, mapOf("Error" to "No valid StructureId"))
            }

            val pageToDelete = transaction {
                Page.findById(pageIdLong)
            } ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("Error" to "Can't find Page"))

            transaction {
                pageToDelete.delete()
            }

            call.respond(HttpStatusCode.Accepted, mapOf("Message" to "Successfully deleted Page"))
        }
        post<Manage.Pages.PageEdit> {
            val pageToEditSnippet = try {
                call.receive<PageEditSnippet>()
            } catch (e: ContentTransformationException) {
                return@post call.respond(HttpStatusCode.BadRequest, mapOf("Error" to "Can't transform JSON"))
            }

            val exists = transaction {
                Page.find { Pages.pageName eq pageToEditSnippet.newPageName }.firstOrNull()
            }
            if(exists != null){
                return@post call.respond(HttpStatusCode.Conflict, mapOf("Error" to "Pagename already in use"))
            }

            val pageIdLong = try {
                pageToEditSnippet.pageID.toLong()
            } catch (e: NumberFormatException) {
                return@post call.respond(HttpStatusCode.BadRequest, mapOf("Error" to "No valid StructureId"))
            }

            val pageToEdit = transaction {
                Page.findById(pageIdLong)
            }?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("Error" to "Can't find Page"))

            transaction {
                pageToEdit.pageName = pageToEditSnippet.newPageName;
            }

            call.respond(HttpStatusCode.Accepted, mapOf("Message" to "Successfully edited name"))
        }
        post<Manage.Structures>{
            val pageCreator = try {
                call.receive<PageCreationSnippet>()
            } catch (e: ContentTransformationException){
                return@post call.respond(HttpStatusCode.BadRequest, mapOf("Error" to "Can't transform JSON"))
            }

            val searchedPage = transaction {
                Page.find { Pages.pageName eq pageCreator.pageName }.firstOrNull()
            }
                    ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("Error" to "Page Not Found"))

            val returnSnippet = transaction {
                PageStructure.find { PageStructures.page eq searchedPage.pageID }.orderBy(PageStructures.position to SortOrder.ASC).map {
                    AdminContent(
                            it.pageStructureId.toString(),
                            when (val i = it.content()) {
                                null -> null
                                else -> i.toSnippet()
                            }
                    )
                }
            }

            call.respond(HttpStatusCode.Accepted, returnSnippet)
        }
        post<Manage.Structures.StructureEditor> {
            val editSnippet = try {
                call.receive<EditSnippet>()
            } catch (e: ContentTransformationException) {
                return@post call.respond(HttpStatusCode.BadRequest, mapOf("Error" to "Cannot transform JSON"))
            }

            val structureIdLong = try {
                editSnippet.structureId.toLong()
            } catch (e: NumberFormatException) {
                return@post call.respond(HttpStatusCode.BadRequest, mapOf("Error" to "No valid StructureId"))
            }

            val structure = transaction {
                PageStructure.findById(structureIdLong)
            } ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("Error" to "Structure not found"))

            val i = structure.content()

            if(i == null) {
                call.respond(HttpStatusCode.Accepted, mapOf("Message" to "This structure is not editable (:"))
            }
            val success = if(i != null) {
                editSnippet.content.edit(i)
            } else {
                false
            }

            if(success) {
                call.respond(HttpStatusCode.Accepted, mapOf("Message" to "Successfully edited Structure"))
            } else {
                call.respond(HttpStatusCode.BadRequest, mapOf("Error" to "Something went wrong while trying to edit Structure"))
            }
        }
    }
}

fun deleteAllStructures(p: Page) {
    p.structures.map {
        deleteObject(it)
    }
}

fun deleteObject(it: PageStructure) {
    it.content()?.deleteEntity()
    transaction { it.delete() }
}