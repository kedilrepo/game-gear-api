package com.kedil.config

object Config {
    val JDBC_STRING = System.getenv("JDBC_STRING")


    val DB_USER = System.getenv("DB_USER")


    val DB_PASSWORD = System.getenv("DB_PASSWORD")

    const val URL = "https://game-gear.eu/"

    const val FTP_HOST = "ftp.niggelgame.dev";

    const val FTP_USER = "backend-ftp-game-gear";

    const val FTP_PASSWORD = "!v1Oe63s";
}