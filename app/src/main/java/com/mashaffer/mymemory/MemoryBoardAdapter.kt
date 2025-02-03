package com.mashaffer.mymemory

import android.content.Context
import android.content.res.ColorStateList
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.widget.ImageButton
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.mashaffer.mymemory.models.BoardSize
import com.mashaffer.mymemory.models.MemoryCard
import com.squareup.picasso.Picasso
import kotlin.math.min

class MemoryBoardAdapter(
    private val context: Context,
    private val boardSize: BoardSize,
    private val cards: List<MemoryCard>,
    private val cardClickListener: CardClickListener
) :
// Viewholder is an object that provides access to all views of one view element
// Represents one memory card
    RecyclerView.Adapter<MemoryBoardAdapter.ViewHolder>() {

    /**
     * This is a singleton object where the class has one instance
     * the entire application
     * Similar to static variables in Java
     */
    companion object {
        private const val MARGIN_SIZE = 10
        private const val TAG = "MemoryBoardAdapter" // TAG is always class name
    }

    /***
     * Interfaces are used to interact with the element
     */
    interface CardClickListener {
        fun onCardClick(position: Int)
    }

    /**
     * Figures out how to create a new view
     * Recyclarview is the parent parameter
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val cardWidth = parent.width / boardSize.getWidth() - (2 * MARGIN_SIZE)
        val cardHeight = parent.height / boardSize.getHeight() - (2 * MARGIN_SIZE)
        val cardSideLength = min(cardWidth, cardHeight)
        val view: View = LayoutInflater.from(context).inflate(R.layout.memory_card, parent, false)
        val layoutParams: ViewGroup.MarginLayoutParams =
            view.findViewById<CardView>(R.id.cardView).layoutParams as MarginLayoutParams
        // Dynamically changing dimensions of the layout element
        // Similar to getting element and updating text
        layoutParams.width = cardSideLength
        layoutParams.height = cardSideLength
        layoutParams.setMargins(MARGIN_SIZE, MARGIN_SIZE, MARGIN_SIZE, MARGIN_SIZE)
        return ViewHolder(view)
    }

    /**
     * Number of elements in recycler view
     */
    override fun getItemCount() = boardSize.numCards

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(position)
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageButton = itemView.findViewById<ImageButton>(R.id.imageButton)

        fun bind(position: Int) {
            val memoryCard: MemoryCard = cards[position]
            if(memoryCard.isFaceUp){
                if(memoryCard.isMatch != null){
                    Picasso.get().load((memoryCard.imageUrl)).placeholder(R.drawable.ic_image).into(imageButton)
                }else{
                    imageButton.setImageResource(memoryCard.identifier)
                }
            }else{
                imageButton.setImageResource(R.drawable.ic_launcher_background)
            }

            imageButton.alpha = if(memoryCard.isMatch) .4f else 1.0f
            val colorStateList: ColorStateList? = if (memoryCard.isMatch) ContextCompat.getColorStateList(context, R.color.color_gray) else null
            ViewCompat.setBackgroundTintList(imageButton, colorStateList)
            imageButton.setOnClickListener {
                Log.i(
                    TAG,
                    "Clicked on position $position"
                ) // This Log uses logcat, similar to terminal
                cardClickListener.onCardClick(position)
            }
        }
    }
}

