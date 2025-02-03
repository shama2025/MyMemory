package com.mashaffer.mymemory.models

enum class BoardSize(val numCards: Int) {
    EASY( numCards = 8),
    MEDIUM(numCards = 18),
    HARD(numCards = 24);

    companion object{
        fun getByValue(value: Int) = values().first{it.numCards == value}
    }
    fun getWidth(): Int{
        return when (this){
        // refers to board size and when is similar to switch statment
            EASY -> 2
            MEDIUM -> 3
            HARD -> 4

        }
    }

    fun getHeight(): Int{
        // determined by number of cards
        return numCards / getWidth()
    }

    fun getNumPairs(): Int{
        // Represent pairs of cards
        return numCards / 2
    }
}