package com.mashaffer.mymemory

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.mashaffer.mymemory.models.BoardSize
import kotlin.math.min

class ImagePickerAdapter(
    private val context: Context,
    private val chosenImageUris: List<Uri>,
    private val boardSize: BoardSize,
    private val imageClickListener: OnClickListener
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.card_image, parent, false)
        val cardWidth: Int = parent.width / boardSize.getWidth()
        val cardHeight:  Int = parent.height / boardSize.getHeight()
        val cardSideLength: Int = min(cardWidth,cardHeight)

        // Updates layout of cards by getting the layoutparams of the customImage
        var layoutParams = view.findViewById<ImageView>(R.id.ivCustomImage).layoutParams
        layoutParams.width = cardSideLength
        layoutParams.height = cardSideLength
    }

    override fun getItemCount() = boardSize.getNumPairs()

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if(position < chosenImageUris.size){
            holder.bind(chosenImageUris[position])
        }else{
            holder.bind()
        }
    }

    inner class Viewholder(itemView: View) : RecyclerView.ViewHolder(itemView){
        private val ivCustomImage = itemView.findViewById<ImageView>(R.id.ivCustomImage)

        fun bind(uri: Uri){
            ivCustomImage.setImageURI(uri)
            ivCustomImage.setOnClickListener(null)
        }

        fun bind() {
            ivCustomImage.setOnClickListener {
                imageClickListener.onPlaceholderClicked()
            }
        }

    }


}
