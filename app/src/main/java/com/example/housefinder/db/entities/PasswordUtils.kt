package com.example.housefinder.db.entities

fun hashPassword(input: String): String = DatabaseInitializer.hashPassword(input)
