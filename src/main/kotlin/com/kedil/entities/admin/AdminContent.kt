package com.kedil.entities.admin

import ContentType

data class AdminContent(
        val structureId: String,
        val content: ContentType?
)

data class FtpSnippet(
        val url: String
)