package com.example.memoryapp

import android.animation.ArgbEvaluator
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import android.widget.Toast.makeText
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.memoryapp.models.BoardSize
import com.example.memoryapp.models.MemoryGame
import com.example.memoryapp.models.UserImageList
import com.example.memoryapp.utils.EXTRA_BOARD_SIZE
import com.example.memoryapp.utils.EXTRA_GAME_NAME
import com.github.jinatonic.confetti.CommonConfetti
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.squareup.picasso.Picasso

class MainActivity : AppCompatActivity() {

    private lateinit var rcBoard:RecyclerView
    private lateinit var clRoot:CoordinatorLayout
    private lateinit var tvNoMoves:TextView
    private lateinit var tvNoPair: TextView
    private lateinit var memoryGame: MemoryGame
    private lateinit var adapter:MemoryBoardAdapter

    private  var customGameImages: List<String>? =null

    private val db=Firebase.firestore
    private var gameName:String?=null


    companion object{
        private const val  CREATE_REQUEST_CODE=265
        private const val TAG="MainActivity"
    }

    //for taking the bord size in enum class
    private  var boardSize:BoardSize=BoardSize.EASY

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        rcBoard=findViewById(R.id.rcBorard)
        tvNoMoves=findViewById(R.id.tvNoMoves)
        tvNoPair=findViewById(R.id.tvNoPairs)
        clRoot=findViewById(R.id.clRoot)




       setUpBoard()
    }



    private fun setUpBoard() {
        supportActionBar?.title= gameName?:getString(R.string.app_name)
        when(boardSize){
            BoardSize.EASY->{
                tvNoMoves.text="Easy:4X2"
                tvNoPair.text="Pairs:0/4"
            }
            BoardSize.HARD->{
                tvNoMoves.text="Hard:6X6"
                tvNoPair.text="Pairs:0/12"
            }
            BoardSize.MEDIUM->{
                tvNoMoves.text="Medium:6X3"
                tvNoPair.text="Pairs:0/9"
            }
        }
        tvNoPair.setTextColor(ContextCompat.getColor(this,R.color.color_progress_none) )
        memoryGame=MemoryGame(boardSize, customGameImages)
        adapter=MemoryBoardAdapter(this,boardSize,memoryGame.cards,object:MemoryBoardAdapter.CardClickListener{
            override fun onCardClick(position: Int) {
                updateGameWithFlip(position)
            }
        })
        rcBoard.adapter=adapter
        rcBoard.setHasFixedSize(true)
        rcBoard.layoutManager=GridLayoutManager(this,boardSize.getWidth())
    }

    //for inflating the menu bar in the mainActivity
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main,menu)
        return true
    }

    //if refrech button click then restart the game
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.menuFresh->{
                // if user click refrech button then first ask to that he really wants to quit or not
                if (memoryGame.getNumMoves()>0&& !memoryGame.haveWonGame()){
                    showAlertDialog("Quit your current game ? ",null,View.OnClickListener {
                        setUpBoard()
                    })
                }else {
                    setUpBoard()
                }
                return true
            }
            R.id.new_size->{
                showNewsizeDialog()
                return true
            }
            R.id.mi_custom ->{
                showCreateDialog()
                return true
            }
            R.id.mi_download ->{
                showDownloadDialog()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showDownloadDialog() {
      val boardDownloadView=  LayoutInflater.from(this).inflate(R.layout.dialog_download_board,null)
      showAlertDialog("Fetch memory game",boardDownloadView,View.OnClickListener {

          val etDownLoad =boardDownloadView.findViewById<EditText>(R.id.etDownloadGame)
          val gameToDownload =etDownLoad.text.toString().trim()
          downloadGame(gameToDownload)

      })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode==CREATE_REQUEST_CODE && resultCode ==Activity.RESULT_OK){
            val customGameName =data?.getStringExtra(EXTRA_GAME_NAME)
            if (customGameName==null){
                Log.e("MainActivity","Got null custom game from CreatActivity")
                return
            }
            downloadGame(customGameName)
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun downloadGame(customGameName: String) {
            db.collection("games").document(customGameName).get().addOnSuccessListener { document ->
                val userImageList=  document.toObject(UserImageList::class.java)
                if (userImageList?.images == null){
                    Log.e(TAG,"Invalid custom when game data from Firestore")
                    Snackbar.make(clRoot,"Sorry we couldn't find any game '$gameName' ", Snackbar.LENGTH_SHORT).show()
                   return@addOnSuccessListener
                }
                val numCards:Int = userImageList!!.images!!.size * 2
                boardSize = BoardSize.getByValue(numCards)
                customGameImages = userImageList.images
                for (imageUrl in userImageList.images){
                    Picasso.get().load(imageUrl).fetch()
                }
                Snackbar.make(clRoot,"You're now playing '$customGameName!'",Snackbar.LENGTH_LONG).show()
                gameName=customGameName
                setUpBoard()
            }.addOnFailureListener { exception ->
                Log.e("MainActivity","Exception when retrieving game",exception)
            }

    }

    private fun showCreateDialog() {
        val boardSizeView = LayoutInflater.from(this).inflate(R.layout.dialog_board_size, null)
        val radioGroupSize = boardSizeView.findViewById<RadioGroup>(R.id.radioGroup)
        showAlertDialog("Create your own memory board", boardSizeView, View.OnClickListener {
            val desireBoardSize = when (radioGroupSize.checkedRadioButtonId) {
                R.id.rbEasy -> BoardSize.EASY
                R.id.rbMediym -> BoardSize.MEDIUM
                R.id.rbHard -> BoardSize.HARD
                else -> BoardSize.HARD
            }
            val intent=Intent(this,CreateActivity::class.java)
            intent.putExtra(EXTRA_BOARD_SIZE,desireBoardSize)
            startActivityForResult(intent, CREATE_REQUEST_CODE)

        })
    }


    private fun showNewsizeDialog() {
        val boardSizeView=LayoutInflater.from(this).inflate(R.layout.dialog_board_size,null)
       val radioGroupSize=boardSizeView.findViewById<RadioGroup>(R.id.radioGroup)
       when(boardSize){
           BoardSize.EASY -> radioGroupSize.check(R.id.rbEasy)
           BoardSize.MEDIUM -> radioGroupSize.check(R.id.rbMediym)
           BoardSize.HARD-> radioGroupSize.check(R.id.rbHard)
       }
        showAlertDialog("Choose new size",boardSizeView,View.OnClickListener {
            boardSize = when (radioGroupSize.checkedRadioButtonId) {
                R.id.rbEasy -> BoardSize.EASY
                R.id.rbMediym -> BoardSize.MEDIUM
                R.id.rbHard -> BoardSize.HARD
                else -> BoardSize.HARD
            }
            gameName=null
            customGameImages=null
            setUpBoard()

        })
    }

    private fun showAlertDialog(title:String,view: View?,positiveClickListener: View.OnClickListener) {
        AlertDialog.Builder(this).setTitle(title).setView(view).setNegativeButton("Cancel",null)
            .setPositiveButton("Ok"){_,_ ->
                positiveClickListener.onClick(null)

            }.show()
    }

    private fun updateGameWithFlip(position: Int) {
        //if user try to click when they already won the game
        if (memoryGame.haveWonGame()) {
            // Alert the user of an invalid move
                Snackbar.make(clRoot,"You already won!",Snackbar.LENGTH_SHORT).show()
            return
        }

        // if user try to click same card again and again
        if (memoryGame.isCardFaceUp(position)) {
            // Alert the user of an invalid move
            Snackbar.make(clRoot,"Invalid move!",Snackbar.LENGTH_SHORT).show()
            return
        }
        if (memoryGame.flipCard(position)) {
            val color=ArgbEvaluator().evaluate(memoryGame.numPairsFound.toFloat()/boardSize.getNumPairs()
            ,ContextCompat.getColor(this,R.color.color_progress_none),
            ContextCompat.getColor(this,R.color.color_progress_full)) as Int
            tvNoPair.setTextColor(color)
            tvNoPair.text = "Pairs:${memoryGame.numPairsFound}/${boardSize.getNumPairs()}"
            if (memoryGame.haveWonGame()) {
                Snackbar.make(clRoot,"You won!congratulation",Snackbar.LENGTH_LONG).show()
                CommonConfetti.rainingConfetti(clRoot, intArrayOf(Color.RED,Color.YELLOW,Color.MAGENTA,Color.GREEN,Color.BLACK,Color.BLUE)).oneShot()
            }
        }
        tvNoMoves.text="Moves:${memoryGame.getNumMoves()}"
            adapter.notifyDataSetChanged()
    }

}