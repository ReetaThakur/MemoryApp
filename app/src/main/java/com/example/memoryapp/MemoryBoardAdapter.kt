package com.example.memoryapp

import android.content.Context
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.memoryapp.models.BoardSize
import com.example.memoryapp.models.MemoryCard
import kotlin.math.max
import kotlin.math.min

class MemoryBoardAdapter(
    private val context: Context,
    private val boardSize: BoardSize,
    private val card: List<MemoryCard>,
    private val cardClickListener: CardClickListener
) :RecyclerView.Adapter<MemoryBoardAdapter.ViewHolder>() {

    companion object{
        private const val MARGIN_SIZE=10
    }

    interface CardClickListener{
        fun onCardClick(position:Int)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val cardWidth=parent.width/boardSize.getWidth()-(2* MARGIN_SIZE)
        val cardHeigth=parent.height/boardSize.getHeight()-(2* MARGIN_SIZE)
        val cardSideLenght= min(cardWidth,cardHeigth)
        val view:View=LayoutInflater.from(context).inflate(R.layout.memory_card,parent,false)
        val layoutParams=view.findViewById<CardView>(R.id.cardView).layoutParams as ViewGroup.MarginLayoutParams
        layoutParams.width=cardSideLenght
        layoutParams.height=cardSideLenght
        layoutParams.setMargins(MARGIN_SIZE, MARGIN_SIZE, MARGIN_SIZE, MARGIN_SIZE)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.setData(position)
    }

    override fun getItemCount(): Int {
        return boardSize.numCard
    }

    inner class ViewHolder(itemView:View):RecyclerView.ViewHolder(itemView){
        private val imageButton=itemView.findViewById<ImageButton>(R.id.imageButton)

        fun setData(position: Int) {
            val memoryCard=card[position]
            imageButton.setImageResource(if (card[position].isFaceUp) card[position].identifier else R.drawable.ic_launcher_background)

            imageButton.alpha= if (memoryCard.isMatched) .4f else 1.0f
            val colorStateList= if(memoryCard.isMatched) ContextCompat.getColorStateList(context,R.color.color_gray) else null
            ViewCompat.setBackgroundTintList(imageButton,colorStateList)
            imageButton.setOnClickListener {
                cardClickListener.onCardClick(position)

            }
        }
   }
}
