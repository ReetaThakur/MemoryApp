package com.example.memoryapp

import android.content.Context
import android.media.Image
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.example.memoryapp.models.BoardSize
import kotlin.math.min

class ImagePickerAdapter(private val context: Context,private val imageUri:List<Uri>,private val boardSize: BoardSize,private val imageClickListner: ImageClickListner):RecyclerView.Adapter<ImagePickerAdapter.ImagePickerViewHolder>() {

    interface ImageClickListner{
        fun onPlaceHolderClicked()
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImagePickerViewHolder {
        val cardWidth=parent.width/boardSize.getWidth()
        val cardHeigth=parent.height/boardSize.getHeight()
        val cardSideLength= min(cardHeigth,cardWidth)
       val view=LayoutInflater.from(context).inflate(R.layout.card_image,parent,false)
        val layoutParams=view.findViewById<ImageView>(R.id.imageView).layoutParams
        layoutParams.width=cardSideLength
        layoutParams.height=cardSideLength
        return ImagePickerViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImagePickerViewHolder, position: Int) {
        if (position<imageUri.size){
            holder.setData(imageUri[position])
        }else{
            holder.bind()
        }
    }

    override fun getItemCount(): Int{
        return boardSize.getNumPairs()
    }

    inner class ImagePickerViewHolder(itemView:View):RecyclerView.ViewHolder(itemView){
        private val ivCustomImage=itemView.findViewById<ImageView>(R.id.imageView)
        fun setData(uri: Uri) {
          ivCustomImage.setImageURI(uri)
            ivCustomImage.setOnLongClickListener(null)
        }

       fun bind(){
           ivCustomImage.setOnClickListener {
            imageClickListner.onPlaceHolderClicked()
           }
       }

    }
}