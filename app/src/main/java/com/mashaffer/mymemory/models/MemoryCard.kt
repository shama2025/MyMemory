package com.mashaffer.mymemory.models

data class MemoryCard constructor(
    // List every memory card attribute
    // Val cant be changed, var can be changed
    val identifier: Int,
    val imageUrl: String? = null,
    var isFaceUp: Boolean = false,
    var isMatch: Boolean = false) {

}