package com.kedil.locations.admin.content

import com.kedil.config.Config
import com.kedil.entities.*
import com.kedil.entities.admin.AdminContent
import com.kedil.entities.admin.FtpSnippet
import com.kedil.entities.blog.*
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.locations.*
import io.ktor.response.respond
import io.ktor.routing.Routing
import com.kedil.extensions.authorized
import io.ktor.http.content.*
import io.ktor.request.*
import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import org.apache.commons.net.ftp.FTP
import org.apache.commons.net.ftp.FTPReply
import org.apache.commons.net.ftp.FTPSClient
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.*
import java.lang.NumberFormatException


@KtorExperimentalLocationsAPI
@Location("/admin/manage")
class Manage() {
    @Location("/blog")
    class Blog(val parent: Manage) {
        @Location("/insert")
        data class Insert(val parent: Blog)

        @Location("/delete")
        data class Delete(val parent: Blog)

        @Location("/blogs")
        data class Blogs(val parent: Blog) {
            @Location("/delete")
            data class BlogDelete(val parent: Blogs)

            @Location("/edit")
            data class BlogEdit(val parent: Blogs)
        }

        @Location("/structures")
        data class Structures(val parent: Blog) {
            @Location("/edit")
            data class StructureEditor(val parent: Structures)
        }
    }

    @Location("/addOn")
    data class AddOn(val parent: Manage)

    @Location("/insert")
    data class Insert(val parent: Manage)

    @Location("/delete")
    data class Delete(val parent: Manage)

    @Location("/changePosition")
    data class ChangePosition(val parent: Manage)

    @Location("/pages")
    data class Pages(val parent: Manage) {
        @Location("/delete")
        data class PageDelete(val parent: Pages)

        @Location("/edit")
        data class PageEdit(val parent: Pages)
    }

    @Location("/structures")
    data class Structures(val parent: Manage) {
        @Location("/edit")
        data class StructureEditor(val parent: Structures)
    }

    @Location("/upload")
    data class Upload(val parent: Manage)
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
            if (!toManageData.addOn) deleteAllStructures(searchedPage)

            // Maybe even more deleting

            // Get last position
            val lastPosition = transaction {
                val structure = PageStructure.find { PageStructures.page eq searchedPage.pageID }
                    .orderBy(PageStructures.position to SortOrder.DESC).limit(1).firstOrNull()
                    ?: return@transaction 0L
                structure.position
            }

            var nextPosition = lastPosition.plus(1)


            // Add Data now just add the end (append)
            toManageData.content.map {
                it.createNew(newPosition = nextPosition, newPage = searchedPage)
                nextPosition++
            }

            searchedPage.updated()

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
                val structure = PageStructure.find { PageStructures.page eq searchedPage.pageID }
                    .orderBy(PageStructures.position to SortOrder.DESC).limit(1).firstOrNull()
                    ?: return@transaction 0L
                structure.position
            }

            println(insertData.position)

            val nextPosition = if (insertData.position > maxPosition + 1)
                (maxPosition + 1)
            else
                insertData.position

            println(nextPosition)

            println(nextPosition)

            transaction {
                PageStructure.find { (PageStructures.page eq searchedPage.pageID) and (PageStructures.position greaterEq nextPosition) }
                    .map {
                        it.position++
                    }
            }

            val newStructure = insertData.content.createNew(newPosition = nextPosition, newPage = searchedPage)

            searchedPage.updated()

            call.respond(HttpStatusCode.Created, newStructure)
        }
        post<Manage.Delete> {
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
                    PageStructure.find { (PageStructures.page eq structure.page.pageID) and (PageStructures.position greater structure.position) }
                        .map { str ->
                            str.position--
                        }
                }
                structure.page.updated()
                deleteObject(structure)
                return@transaction true
            }

            if (success) {
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
                val maxStructure = PageStructure.find { PageStructures.page eq structure.page.pageID }
                    .orderBy(PageStructures.position to SortOrder.DESC).limit(1).firstOrNull()
                    ?: return@transaction 0L
                maxStructure.position
            }

            val newPosition = if (changerSnippet.newPosition > maxPosition)
                maxPosition + 1
            else
                changerSnippet.newPosition

            transaction {
                transaction {
                    PageStructure.find { (PageStructures.page eq structure.page.pageID) and ((PageStructures.position less structure.position) and (PageStructures.position greaterEq newPosition)) }
                        .map {
                            it.position++
                        }
                }
                transaction {
                    PageStructure.find { (PageStructures.page eq structure.page.pageID) and ((PageStructures.position greater structure.position) and (PageStructures.position lessEq newPosition)) }
                        .map {
                            it.position--
                        }
                }
                structure.position = changerSnippet.newPosition
            }
            transaction { structure.page.updated() }


            call.respond(
                HttpStatusCode.Accepted,
                mapOf("Message" to "Successfully changed index of ${changerSnippet.structureId} to ${changerSnippet.newPosition}")
            )
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
            } catch (e: ContentTransformationException) {
                return@post call.respond(HttpStatusCode.BadRequest, mapOf("Error" to "Can't transform JSON"))
            }

            val exists = transaction {
                Page.find { Pages.pageName eq pageCreator.pageName }.firstOrNull()
            }
            if (exists != null) {
                return@post call.respond(HttpStatusCode.Conflict, mapOf("Error" to "Page already exists"))
            }

            transaction {
                Page.new {
                    pageName = pageCreator.pageName
                    lastEdited = System.currentTimeMillis()
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
            if (exists != null) {
                return@post call.respond(HttpStatusCode.Conflict, mapOf("Error" to "Pagename already in use"))
            }

            val pageIdLong = try {
                pageToEditSnippet.pageID.toLong()
            } catch (e: NumberFormatException) {
                return@post call.respond(HttpStatusCode.BadRequest, mapOf("Error" to "No valid StructureId"))
            }

            val pageToEdit = transaction {
                Page.findById(pageIdLong)
            } ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("Error" to "Can't find Page"))

            transaction {
                pageToEdit.pageName = pageToEditSnippet.newPageName;
            }

            pageToEdit.updated()

            call.respond(HttpStatusCode.Accepted, mapOf("Message" to "Successfully edited name"))
        }
        post<Manage.Structures> {
            val structurePage = try {
                call.receive<PageCreationSnippet>()
            } catch (e: ContentTransformationException) {
                return@post call.respond(HttpStatusCode.BadRequest, mapOf("Error" to "Can't transform JSON"))
            }

            val searchedPage = transaction {
                Page.find { Pages.pageName eq structurePage.pageName }.firstOrNull()
            }
                ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("Error" to "Page Not Found"))

            val returnSnippet = transaction {
                PageStructure.find { PageStructures.page eq searchedPage.pageID }
                    .orderBy(PageStructures.position to SortOrder.ASC).map {
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

            if (i == null) {
                call.respond(HttpStatusCode.Accepted, mapOf("Message" to "This structure is not editable (:"))
            }
            val success = if (i != null) {
                editSnippet.content.edit(i)
            } else {
                false
            }

            if (success) {
                transaction { structure.page.updated() }

                call.respond(HttpStatusCode.Accepted, mapOf("Message" to "Successfully edited Structure"))
            } else {
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("Error" to "Something went wrong while trying to edit Structure")
                )
            }
        }
        post<Manage.Blog> {
            val blogNamer = try {
                call.receive<BlogCreationSnippet>()
            } catch (e: ContentTransformationException) {
                return@post call.respond(HttpStatusCode.BadRequest, mapOf("Error" to "Can't transform JSON"))
            }

            val blog = transaction {
                Blog.find { Blogs.blogUrl eq blogNamer.blogName }.firstOrNull()
            } ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("Error" to "Cannot find Blog"))

            call.respond(HttpStatusCode.Accepted, transaction { blog.toBlogReturnSnippet() })
        }
        post<Manage.Blog.Insert> {
            val insertData = try {
                call.receive<BlogInsertSnippet>()
            } catch (e: ContentTransformationException) {
                return@post call.respond(HttpStatusCode.BadRequest, mapOf("Error" to "Can't transform JSON"))
            }

            val searchedBlog = transaction {
                Blog.find { Blogs.blogUrl eq insertData.blog }.firstOrNull()
            }
                ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("Error" to "Page Not Found"))

            val maxPosition = transaction {
                val structure = BlogStructure.find { BlogStructures.blog eq searchedBlog.blogID }
                    .orderBy(BlogStructures.position to SortOrder.DESC).limit(1).firstOrNull()
                    ?: return@transaction 0L
                structure.position
            }

            val nextPosition = if (insertData.position > maxPosition + 1)
                (maxPosition + 1)
            else
                insertData.position

            transaction {
                BlogStructure.find { (BlogStructures.blog eq searchedBlog.blogID) and (BlogStructures.position greaterEq nextPosition) }
                    .map {
                        it.position++
                    }
            }

            val newStructure = insertData.content.createNewBlog(newPosition = nextPosition, newBlog = searchedBlog)

            searchedBlog.updated()

            call.respond(HttpStatusCode.Created, newStructure)
        }
        post<Manage.Blog.Delete> {
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
                val structure = BlogStructure.findById(structureIdLong) ?: return@transaction false
                transaction {
                    BlogStructure.find { (BlogStructures.blog eq structure.blog.blogID) and (BlogStructures.position greater structure.position) }
                        .map { str ->
                            str.position--
                        }
                }
                structure.blog.updated()
                deleteObject(structure)
                return@transaction true
            }

            if (success) {
                call.respond(HttpStatusCode.Accepted, mapOf("Message" to "Successfully deleted Data"))
            } else {
                call.respond(HttpStatusCode.BadRequest, mapOf("Error" to "Cannot find Structure!"))
            }
        }
        get<Manage.Blog.Blogs> {
            val blogs = transaction {
                Blog.all().orderBy(Blogs.lastEdited to SortOrder.DESC).map(Blog::toBlogReturnSnippet)
            }

            call.respond(HttpStatusCode.Accepted, BlogsSnippet(blogs))
        }
        post<Manage.Blog.Blogs> {
            val blogCreator = try {
                call.receive<BlogCreationSnippet>()
            } catch (e: ContentTransformationException) {
                return@post call.respond(HttpStatusCode.BadRequest, mapOf("Error" to "Can't transform JSON"))
            }

            if (blogCreator.blogName.isEmpty()) return@post call.respond(
                HttpStatusCode.Conflict,
                mapOf("Error" to "Page already exists")
            )

            val exists = transaction {
                Blog.find { Blogs.blogName eq blogCreator.blogName }.firstOrNull()
            }
            if (exists != null) {
                return@post call.respond(HttpStatusCode.Conflict, mapOf("Error" to "Page already exists"))
            }

            val nextAvailableBlogUrl = blogCreator.blogName.nextAvailableBlogUrl()

            transaction {
                Blog.new {
                    blogName = blogCreator.blogName
                    blogUrl = nextAvailableBlogUrl
                    lastEdited = System.currentTimeMillis()
                }
            }

            call.respond(HttpStatusCode.Accepted, mapOf("Message" to "Successfully created page"))
        }
        post<Manage.Blog.Blogs.BlogDelete> {
            val blogToDeleteSnippet = try {
                call.receive<BlogDeletionSnippet>()
            } catch (e: ContentTransformationException) {
                return@post call.respond(HttpStatusCode.BadRequest, mapOf("Error" to "Can't transform JSON"))
            }

            val blogIdLong = try {
                blogToDeleteSnippet.blogID.toLong()
            } catch (e: NumberFormatException) {
                return@post call.respond(HttpStatusCode.BadRequest, mapOf("Error" to "No valid StructureId"))
            }

            val blogToDelete = transaction {
                Blog.findById(blogIdLong)
            } ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("Error" to "Can't find Page"))

            transaction {
                blogToDelete.delete()
            }

            call.respond(HttpStatusCode.Accepted, mapOf("Message" to "Successfully deleted Page"))
        }
        post<Manage.Blog.Blogs.BlogEdit> {
            val blogToEditSnippet = try {
                call.receive<BlogEditSnippet>()
            } catch (e: ContentTransformationException) {
                return@post call.respond(HttpStatusCode.BadRequest, mapOf("Error" to "Can't transform JSON"))
            }

            if (blogToEditSnippet.newBlogName.isEmpty()) return@post call.respond(
                HttpStatusCode.Conflict,
                mapOf("Error" to "Page already exists")
            )

            val blogIdLong = try {
                blogToEditSnippet.blogID.toLong()
            } catch (e: NumberFormatException) {
                return@post call.respond(HttpStatusCode.BadRequest, mapOf("Error" to "No valid StructureId"))
            }

            val blogToEdit = transaction {
                Blog.findById(blogIdLong)
            } ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("Error" to "Can't find Blog"))

            if (transaction { blogToEditSnippet.newBlogName == blogToEdit.blogName }) return@post call.respond(
                HttpStatusCode.Accepted,
                mapOf("Message" to "Successfully edited name")
            )

            val exists = transaction {
                Blog.find { Blogs.blogName eq blogToEditSnippet.newBlogName }.firstOrNull()
            }
            if (exists != null) {
                return@post call.respond(HttpStatusCode.Conflict, mapOf("Error" to "Blogname already in use"))
            }

            val nextAvailableUrl = blogToEditSnippet.newBlogName.nextAvailableBlogUrl()

            transaction {
                blogToEdit.blogName = blogToEditSnippet.newBlogName
                blogToEdit.blogUrl = nextAvailableUrl
            }

            blogToEdit.updated()

            call.respond(HttpStatusCode.Accepted, mapOf("Message" to "Successfully edited name"))
        }
        post<Manage.Blog.Structures> {
            val structurePage = try {
                call.receive<BlogCreationSnippet>()
            } catch (e: ContentTransformationException) {
                return@post call.respond(HttpStatusCode.BadRequest, mapOf("Error" to "Can't transform JSON"))
            }

            val searchedBlog = transaction {
                Blog.find { Blogs.blogUrl eq structurePage.blogName }.firstOrNull()
            }
                ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("Error" to "Page Not Found"))

            val returnSnippet = transaction {
                BlogStructure.find { BlogStructures.blog eq searchedBlog.blogID }
                    .orderBy(BlogStructures.position to SortOrder.ASC).map {
                        AdminContent(
                            it.blogStructureId.toString(),
                            when (val i = it.content()) {
                                null -> null
                                else -> i.toSnippet()
                            }
                        )
                    }
            }

            call.respond(HttpStatusCode.Accepted, returnSnippet)
        }
        post<Manage.Blog.Structures.StructureEditor> {
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
                BlogStructure.findById(structureIdLong)
            } ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("Error" to "Structure not found"))

            val i = structure.content()

            if (i == null) {
                call.respond(HttpStatusCode.Accepted, mapOf("Message" to "This structure is not editable (:"))
            }
            val success = if (i != null) {
                editSnippet.content.edit(i)
            } else {
                false
            }

            if (success) {
                transaction { structure.blog.updated() }

                call.respond(HttpStatusCode.Accepted, mapOf("Message" to "Successfully edited Structure"))
            } else {
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("Error" to "Something went wrong while trying to edit Structure")
                )
            }
        }
        post<Manage.Upload> {
            val multipart = call.receiveMultipart()

            multipart.forEachPart { part ->
                if (part is PartData.FileItem) {
                    if (part.originalFileName == null) return@forEachPart call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("Error" to "No Filename provided")
                    )
                    val ext = File(part.originalFileName).extension
                    if (!allowedFileExtensions.contains(ext)) return@forEachPart call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("Error" to "Wrong file format")
                    )
                    val file = File(part.originalFileName)
                    part.streamProvider()
                        .use { input -> file.outputStream().buffered().use { output -> input.copyToSuspend(output) } }


                    val client = FTPSClient(false)
                    try {
                        client.connect(Config.FTP_HOST)

                        val reply = client.replyCode

                        if (!FTPReply.isPositiveCompletion(reply)) {
                            client.disconnect()
                            return@forEachPart call.respond(HttpStatusCode.InternalServerError)
                        }

                        try {
                            if (!client.login(Config.FTP_USER, Config.FTP_PASSWORD)) {
                                client.logout()
                                return@forEachPart call.respond(HttpStatusCode.InternalServerError)
                            }

                            client.setFileType(FTP.BINARY_FILE_TYPE)
                            client.enterLocalPassiveMode()
                            client.execPBSZ(0)
                            client.execPROT("P")

                            val inputStream = FileInputStream(file)
                            val done = client.storeFile(part.originalFileName, inputStream)
                            inputStream.close()

                            if (done) {
                                call.respond(
                                    HttpStatusCode.Created,
                                    FtpSnippet(Config.URL + "public/" + part.originalFileName)
                                )
                            } else {
                                return@forEachPart call.respond(HttpStatusCode.InternalServerError)
                            }

                        } catch (e: Throwable) {
                            e.printStackTrace()
                            return@forEachPart call.respond(HttpStatusCode.InternalServerError)
                        }


                    } catch (e: IOException) {
                        e.printStackTrace()
                        return@forEachPart call.respond(HttpStatusCode.InternalServerError)
                    }

                }

                part.dispose()
            }
        }
    }
}

suspend fun InputStream.copyToSuspend(
    out: OutputStream,
    bufferSize: Int = DEFAULT_BUFFER_SIZE,
    yieldSize: Int = 4 * 1024 * 1024,
    dispatcher: CoroutineDispatcher = Dispatchers.IO
): Long {
    return withContext(dispatcher) {
        val buffer = ByteArray(bufferSize)
        var bytesCopied = 0L
        var bytesAfterYield = 0L
        while (true) {
            val bytes = read(buffer).takeIf { it >= 0 } ?: break
            out.write(buffer, 0, bytes)
            if (bytesAfterYield >= yieldSize) {
                yield()
                bytesAfterYield %= yieldSize
            }
            bytesCopied += bytes
            bytesAfterYield += bytes
        }
        return@withContext bytesCopied
    }
}

val allowedFileExtensions = listOf("png", "gif", "jpeg", "jpg")


fun deleteAllStructures(p: Page) {
    p.structures.map {
        deleteObject(it)
    }
}

fun deleteObject(it: PageStructure) {
    it.content()?.deleteEntity()
    transaction { it.delete() }
}

fun deleteObject(it: BlogStructure) {
    it.content()?.deleteEntity()
    transaction { it.delete() }
}

fun Page.updated() {
    transaction {
        lastEdited = System.currentTimeMillis()
    }
}

fun Blog.updated() {
    transaction {
        lastEdited = System.currentTimeMillis()
    }
}

fun String.nextAvailableBlogUrl(): String {

    val minified =
        this.replace("""[^a-zA-Z1-9-]""".toRegex(), "-").replace("""-{2,}""".toRegex(), "-").replace("-$", "")


    val url = minified.toLowerCase()
    val isAvailable = transaction {
        Blog.find { Blogs.blogUrl eq url }.firstOrNull() == null
    }
    if (isAvailable) {
        return url
    }
    val finishedUrl = transaction {
        var counter = 0
        while (Blog.find { Blogs.blogUrl eq ("$url-$counter") }.firstOrNull() != null) {
            counter++
        }
        "$url-$counter"
    }
    return finishedUrl
}