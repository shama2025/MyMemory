package com.mashaffer.mymemory.models

import com.mashaffer.mymemory.utils.DEFAULT_ICONS

class MemoryGame (private val boardSize: BoardSize, private val customImages: List<String>?){

        val cards: List<MemoryCard>
        var numPairs: Int = 0

    // null bc there is no single selected card when creating a new game
    private var indexOfSingleSelectedCard: Int? = null
    private var numCardFlips: Int = 0

    init {
        if(customImages == null ){
            val chosenImages: List<Int> = DEFAULT_ICONS.shuffled().take(boardSize.getNumPairs())
            val randomizedImages: List<Int> = (chosenImages + chosenImages).shuffled()
            cards = randomizedImages.map{ MemoryCard(it)}
            val memoryCards: List<MemoryCard> = randomizedImages.map{ MemoryCard(it) }

        }else{
            val randomizeImages = (customImages + customImages).shuffled()
            cards = randomizeImages.map { MemoryCard(it.hashCode(),it) }
        }

        // Part 4
        // it is the current randomized image
    }

    fun flipCard(position: Int): Boolean{
        numCardFlips+=1
        val card: MemoryCard = cards[position]
        // Three cases for match
        // 0 cards previously flipped over => means we just flip over the card (same as 2 cards previously flipped)
        // 1 card is previously flipped over => flip over card + check if both cards are same image
        // 2 cards previously flipped over => make cards face down and then flip over new cards
        var foundMatch : Boolean = false
        if(indexOfSingleSelectedCard == null){
            // 0 or 2 cards flipped over
            restoreCards()
            indexOfSingleSelectedCard = position
        }else{
            // flip card and check
            foundMatch = checkForMatch(indexOfSingleSelectedCard!!, position)
            //!! tells kotlin to not worry

            indexOfSingleSelectedCard = null
        }
        card.isFaceUp = !card.isFaceUp // Sets to opposite of card
        return foundMatch
    }
    private fun restoreCards(){
        for(card in cards){
            if(!card.isMatch) {
                card.isFaceUp = false
            }
        }
    }

    private fun checkForMatch(position1: Int, position2: Int): Boolean{
        if(cards[position1].identifier == cards[position2].identifier){
            // cards are match
            cards[position1].isMatch = true
            cards[position2].isMatch = true
            numPairs+=1

            return true
        }else{
            return false
        }
    }

    fun haveWonGame(): Boolean {
        return numPairs == boardSize.getNumPairs()
    }

    fun isCardFaceUp(position: Int): Boolean {
        return cards[position].isFaceUp
    }
    fun getNumMoves(): Int{
        return numCardFlips / 2
    }
}