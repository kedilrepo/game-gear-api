package com.kedil.locations.sitemap

import com.kedil.config.Config
import com.kedil.entities.Page
import com.kedil.entities.blog.Blog
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.locations.*
import io.ktor.response.*
import io.ktor.routing.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.xml.sax.InputSource
import java.io.StringReader
import java.text.SimpleDateFormat
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory

@KtorExperimentalLocationsAPI
@Location("/sitemap.xml")
class Sitemap {
    @Location("/pages")
    class Pages(val sitemap: Sitemap)

    @Location("/blogs")
    class Blogs(val sitemap: Sitemap)
}

@KtorExperimentalLocationsAPI
fun Routing.sitemap() {
    get<Sitemap> {
        val simpleDateFormat = SimpleDateFormat("yyyy-mm-dd")

        val pages = transaction {
            Page.all()
                .map { "<url><loc>${Config.URL + it.pageName}</loc><lastmod>${simpleDateFormat.format(it.lastEdited)}</lastmod><changefreq>monthly</changefreq></url>" }
        }

        val blogs = transaction {
            Blog.all().map { "<url><loc>${Config.URL + "blog/" + it.blogUrl}</loc><lastmod>${simpleDateFormat.format(it.lastEdited)}</lastmod><changefreq>monthly</changefreq></url>" }
        }

        val defaultPages = defaultUrls.map { "<url><loc>${Config.URL + it}</loc></url>" }

        val xml = toSitemap(pages + blogs + defaultPages)

        call.respond(HttpStatusCode.Accepted, xml)
    }
    get<Sitemap.Blogs> {
        val blogs = transaction {
            Blog.all().map(Blog::blogUrl)
        }

        call.respond(HttpStatusCode.Accepted, SitemapSnippet(blogs))
    }
}

fun toSitemap(sites: List<String>): String {
    val combined = sites.joinToString("\r\n")
    return """<?xml version="1.0" encoding="UTF-8"?>

<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
$combined
</urlset> """
}

data class SitemapSnippet(
    val sites: List<String>
)

val defaultUrls = listOf("", "home", "blog", "404")