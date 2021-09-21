package com.example.memoryapp.models

enum class BoardSize(val numCard:Int){
    EASY(8),
    MEDIUM(18),
    HARD(24);
    fun getWidth():Int{
        return when(this){
            EASY -> 2
            MEDIUM -> 3
            HARD -> 4

        }
    }
    fun getHeight():Int{
        return numCard/getWidth()
    }
    fun getNumPairs():Int{
        return numCard/2
    }

}