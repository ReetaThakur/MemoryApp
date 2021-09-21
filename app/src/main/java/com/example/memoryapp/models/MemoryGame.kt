package com.example.memoryapp.models

import com.example.memoryapp.utils.DEFAULT_ICON

class MemoryGame(private val boardSize: BoardSize) {


    val cards:List<MemoryCard>
    var numPairsFound=0
    private var numCardFlips=0
    private var indexOfSingleSelectedCard:Int?=null

    init {
        val chosenImages= DEFAULT_ICON.shuffled().take(boardSize.getNumPairs())
        val randomeImages =(chosenImages+chosenImages).shuffled()
        cards=randomeImages.map {
            MemoryCard(it)
        }

    }
    fun flipCard(position: Int): Boolean {
        numCardFlips++
    var card=cards[position]
        // 0 cards previouly flipped over =>  flip over the selected card
        // 1 cards previouly flipped over =>  flip over the selected card + check if the image match
        // 2 cards previouly flipped over =>  restore cards + flip over the selected card
        var foundMatch=false
        if (indexOfSingleSelectedCard==null){
            restoreCards()
            indexOfSingleSelectedCard=position
        }else{
            foundMatch= checkForMatch(indexOfSingleSelectedCard!!,position)
            indexOfSingleSelectedCard=null
        }
        card.isFaceUp=!card.isFaceUp
        return foundMatch
    }

    private fun checkForMatch(position1: Int, position2: Int): Boolean {
        if (cards[position1].identifier!=cards[position2].identifier) {
            return false
        }
        cards[position1].isMatched=true
        cards[position2].isMatched=true
        numPairsFound++
        return true
    }

    private fun restoreCards() {
        for (card in cards){
            if (!card.isMatched){
                card.isFaceUp=false
            }
        }
    }

    fun haveWonGame(): Boolean {
        return numPairsFound==boardSize.getNumPairs()

    }

    fun isCardFaceUp(position: Int): Boolean {
        return cards[position].isFaceUp

    }

    fun getNumMoves(): Int {
        return numCardFlips/2

    }
}