package com.example.memoryapp

import android.animation.ArgbEvaluator
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.memoryapp.models.BoardSize
import com.example.memoryapp.models.MemoryCard
import com.example.memoryapp.models.MemoryGame
import com.example.memoryapp.utils.DEFAULT_ICON
import com.google.android.material.snackbar.Snackbar

class MainActivity : AppCompatActivity() {

    private lateinit var rcBoard:RecyclerView
    private lateinit var tvNoMoves:TextView
    private lateinit var tvNoPair: TextView
    private lateinit var memoryGame: MemoryGame
    private lateinit var adapter:MemoryBoardAdapter

    private  var boardSize:BoardSize=BoardSize.EASY

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        rcBoard=findViewById(R.id.rcBorard)
        tvNoMoves=findViewById(R.id.tvNoMoves)
        tvNoPair=findViewById(R.id.tvNoPairs)


       setUpBoard()
    }

    private fun setUpBoard() {
        tvNoPair.setTextColor(ContextCompat.getColor(this,R.color.color_progress_none) )
        memoryGame=MemoryGame(boardSize)
        adapter=MemoryBoardAdapter(this,boardSize,memoryGame.cards,object:MemoryBoardAdapter.CardClickListener{
            override fun onCardClick(position: Int) {
                updateGameWithFlip(position)
            }
        })
        rcBoard.adapter=adapter
        rcBoard.setHasFixedSize(true)
        rcBoard.layoutManager=GridLayoutManager(this,boardSize.getWidth())
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main,menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.menuFresh->{
                setUpBoard()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun updateGameWithFlip(position: Int) {
        if (memoryGame.haveWonGame()) {
            return
        }
        if (memoryGame.isCardFaceUp(position)) {
            return
        }
        if (memoryGame.flipCard(position)) {
            val color=ArgbEvaluator().evaluate(memoryGame.numPairsFound.toFloat()/boardSize.getNumPairs()
            ,ContextCompat.getColor(this,R.color.color_progress_none),
            ContextCompat.getColor(this,R.color.color_progress_full)) as Int
            tvNoPair.setTextColor(color)
            tvNoPair.text = "Pairs:${memoryGame.numPairsFound}/${boardSize.getNumPairs()}"
            if (memoryGame.haveWonGame()) {
                Toast.makeText(this, "You won!congratulation", Toast.LENGTH_SHORT).show()
            }
        }
        tvNoMoves.text="Moves:${memoryGame.getNumMoves()}"
            adapter.notifyDataSetChanged()
    }

}