package com.kedil.config

object Config {
    val JDBC_STRING = System.getenv("JDBC_STRING")


    val DB_USER = System.getenv("DB_USER")


    val DB_PASSWORD = System.getenv("DB_PASSWORD")
}