package com.example.housefinder.db.entities

fun conversationIdFor(userA: Int, userB: Int, listingId: Int): String {
    val min = minOf(userA, userB)
    val max = maxOf(userA, userB)
    return "uid_${min}-${max}_listing_${listingId}"
}
