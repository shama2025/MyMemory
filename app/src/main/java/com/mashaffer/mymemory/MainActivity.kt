package com.mashaffer.mymemory

import android.animation.ArgbEvaluator
import android.app.Activity
import android.app.ComponentCaller
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.github.jinatonic.confetti.CommonConfetti
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.mashaffer.mymemory.models.BoardSize
import com.mashaffer.mymemory.models.MemoryGame
import com.mashaffer.mymemory.models.UserImageList
import com.mashaffer.mymemory.utils.EXTRA_BOARD_SIZE
import com.mashaffer.mymemory.utils.EXTRA_GAME_NAME
import com.squareup.picasso.Picasso

/**
 * When releasing an app:
 * Make a custom Icon
 * Turn on minify and proguard
 * Test on multiple API versions and devices (Look at build.gradle to test min build level)
 * Translate Strings to support multiple languages
 *      Move all strings to a strings.xml file
 *      Create an additional file for each language variant
 */
/**
 * Publishing an App to google play:
 *  Setup:
 *     one time 25 dollar fee to become a google developer
 *     Automated app review
 *     Take screenshot of app on emulator
 *     Create Release Bundle (use aab which is android app bundle)
 *     Create a KeyStore (Similar to rsa key, Key store helps validate you as a daveloper)
 *
 *  Google Play Console:
 *      Add app details such as game, if its free, app name
 *      Will walk you through questions and policy and then upload aab
 *      Need to add own privacy policy url
 *      Targeting certain age groups brings in additional compliance
 *      Can test app with trusted users (Beta Testers / Quality Assurance)
 *      Production section is the actual build of the app
 */
class MainActivity : AppCompatActivity() {
    companion object{
        private const val TAG = "MainActivity"
        private const val CREATE_REQUEST_CODE = 1009 // Can be any number
    }
    // Part 1: Setup basic UI using a recyclar view and learn
    // how to assign values to those elements

    // Part 2: Setup Recycler View with 4 x 2 grid of memory cards
    /***
     * These are lateinit var's since these
     * will be initialized set in OnCreate Method
     */
    private lateinit var rvBoard: RecyclerView
    private lateinit var tvNumMoves: TextView
    private lateinit var tvNumPairs: TextView
    private lateinit var clRoot: CoordinatorLayout
    private var gameName: String? = null

    private lateinit var memoryGame: MemoryGame
    private lateinit var adapter: MemoryBoardAdapter
    private var customGameImages: List<String>? = null

    private var boardSize: BoardSize = BoardSize.EASY

    private val db = Firebase.firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)


        // Part 1
        // findViewById is similar to getElementById
        // Variables are mapped to elements in the activity_main.xml
        // Similar to mapping variables in JS/TS to the dom
        rvBoard = findViewById(R.id.rvBoard)
        tvNumMoves = findViewById(R.id.tvNumMoves)
        tvNumPairs = findViewById(R.id.tvNumPairs)
        memoryGame = MemoryGame(boardSize, customGameImages)


        setUpBoard()
    }
//// Displays the menu items Can't see menu items Come back to this
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }



    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.mi_refresh ->{
                if(memoryGame.getNumMoves() > 0 && !memoryGame.haveWonGame()){
                    Snackbar.make(clRoot, "Progress Will be Deleted!", Snackbar.LENGTH_LONG ).show()
                showAlertDialog("Quit your current game?", null, View.OnClickListener{
                    setUpBoard()
                })
                }else{
                    setUpBoard()
                }
            }
            R.id.mi_new_size ->{
                showNewSizeDialog()
                return true
            }
            R.id.mi_custom ->{
                showCustomDialog()
            return true
            }
            R.id.mi_download ->{
                showDownloadDialog()
            }
        }

        return super.onOptionsItemSelected(item)
    }



    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?,
        caller: ComponentCaller
    ) {
        if(requestCode == CREATE_REQUEST_CODE && resultCode == Activity.RESULT_OK){
            val customGameName: String? = data?.getStringExtra(EXTRA_GAME_NAME);
            if(customGameName == null){
                Snackbar.make(clRoot, "Error Creating Game Name", Snackbar.LENGTH_SHORT).show()
            return;
            }
            downloadGame(customGameName)
        }
        super.onActivityResult(requestCode, resultCode, data, caller)
    }

    private fun showDownloadDialog() {
      val boardDownloadView = LayoutInflater.from(this).inflate(R.layout.dialog_download_board,null)
        showAlertDialog("Fetch Memory Game", boardDownloadView, View.OnClickListener{
            val etDownloadGame: EditText = boardDownloadView.findViewById<EditText>(R.id.etDownloadGame)
            val gameToDownload = etDownloadGame.text.toString().trim()
        })
    }

    private fun downloadGame(customGameName: String) {
        db.collection("games").document(gameName).get().addOnSuccessListener { document ->
            val userImageList = document.toObject(UserImageList::class.java)
            if(userImageList?.images == null){
                Snackbar.make(clRoot,"Error finding game ${gameName}.",Snackbar.LENGTH_LONG ).show()
                return@addOnSuccessListener
            }
            val numCards: Int = userImageList.images.size * 2
            boardSize = BoardSize.getByValue(numCards)
            customGameImages = userImageList.images
            for (imageUrl:String in userImageList.images){
                Picasso.get().load(imageUrl).fetch()
            }
            Snackbar.make(clRoot, "You are now playing ${customGameName}", Snackbar.LENGTH_SHORT).show()
            gameName = customGameName
            setUpBoard()
        }.addOnFailureListener({
            Snackbar.make(clRoot, "Error retrieving Custom game", Snackbar.LENGTH_SHORT).show()
        })
    }

    private fun showCustomDialog() {
        val boardSizeView = LayoutInflater.from(this).inflate(R.layout.dialog_board_size, null)
        val radioGroupSize = boardSizeView.findViewById<RadioGroup>(R.id.radioGroup)
        showAlertDialog("Create new Memory Board", boardSizeView, View.OnClickListener {
            // Set new value for board size
            val desiredBoardSize = when(radioGroupSize.checkedRadioButtonId){
                R.id.rbEasy -> BoardSize.EASY
                R.id.rbMedium -> BoardSize.MEDIUM
                else -> BoardSize.HARD
            }
            // Navigate to new activity

            // Using the Intent method we can navigate between activities
            val intent = Intent(this, CreateActivity::class.java)

            // Using bundle due to some method deprecations
            val bundle = Bundle()
            bundle.putString("extra_board_size", EXTRA_BOARD_SIZE)
            intent.putExtra("myBundle", bundle)
            // startActivityForResult => Get data back from activity
            startActivityForResult(intent, CREATE_REQUEST_CODE)

        })
    }

    private fun showNewSizeDialog() {
        val boardSizeView = LayoutInflater.from(this).inflate(R.layout.dialog_board_size, null)
        val radioGroupSize = boardSizeView.findViewById<RadioGroup>(R.id.radioGroup)
        when(boardSize){
            BoardSize.EASY -> radioGroupSize.check(R.id.rbEasy)
            BoardSize.MEDIUM -> radioGroupSize.check(R.id.rbMedium)
            BoardSize.HARD -> radioGroupSize.check(R.id.rbHard)
        }

        showAlertDialog("Choose new Size", boardSizeView, View.OnClickListener {
            // Set new value for board size
            boardSize = when(radioGroupSize.checkedRadioButtonId){
                R.id.rbEasy -> BoardSize.EASY
                R.id.rbMedium -> BoardSize.MEDIUM
                else -> BoardSize.HARD
            }
            gameName= null
            customGameImages = null
            setUpBoard()
        })
    }


    private fun showAlertDialog(title: String, view: View?, positiveClickListener: View.OnClickListener) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setView(view)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Ok"){_, _ -> // used to ignore parameter or value
                positiveClickListener.onClick(null)

            }.show()
    }

    private fun updateGameWithFlip(position: Int) {
        if (memoryGame.haveWonGame()) {
            // alert user they have won
            // A Snackbar component that shows up at bottom of screen
            Snackbar.make(clRoot, "You already won!", Snackbar.LENGTH_LONG).show()
            return
        }
        if (memoryGame.isCardFaceUp(position)) {
            Snackbar.make(clRoot, "Invalid Move", Snackbar.LENGTH_LONG).show()
            return
        }
        if (memoryGame.flipCard(position)) {
            Log.i(TAG, "Found a match! Num pairs found: ${memoryGame.numPairs}")

            val color: Int = ArgbEvaluator().evaluate(
                memoryGame.numPairs.toFloat() / boardSize.getNumPairs(),
                ContextCompat.getColor(this,R.color.color_progress_none),
                ContextCompat.getColor(this, R.color.color_progress_full)
            )as Int

            tvNumPairs.text = "Paris: ${memoryGame.numPairs}/ ${boardSize.getNumPairs()}"
            tvNumPairs.setTextColor(color)
        }
        if (memoryGame.haveWonGame()) {
            Snackbar.make(clRoot, "You Won!", Snackbar.LENGTH_LONG).show()
            CommonConfetti.rainingConfetti(clRoot, intArrayOf(Color.YELLOW, Color.GREEN, Color.MAGENTA)).oneShot()
        }
        Log.i(TAG, "Moved: ${memoryGame.getNumMoves()}")
        tvNumMoves.text = "Moved: ${memoryGame.getNumMoves()}"
        adapter.notifyDataSetChanged() // Updates adapter if anything change
}

    fun setUpBoard(){
        supportActionBar?.title = gameName?: getString(R.string.app_name)
        tvNumPairs.setTextColor(ContextCompat.getColor(this,R.color.color_progress_none))
//        //Part 3
//        // Creating icons
//        val chosenImages: List<Int> = DEFAULT_ICONS.shuffled().take(boardSize.getNumPairs())
//        val randomizedImages: List<Int> = (chosenImages + chosenImages).shuffled()
//
//        // Part 4
        // Move to own class to clean up code
//        // it is the current randomized image
//        val memoryCards: List<MemoryCard> = randomizedImages.map{ MemoryCard(it) }
        // Part 2
        // Created the memory Board to add clickable elements to the board
        // Created a kt file that allowed for updating memory board dyanamically
        when (boardSize){
            BoardSize.EASY ->{
                tvNumMoves.text = "Easy: 4 X 2"
                tvNumPairs.text = "Pairs: 0 / 4"
            }
            BoardSize.MEDIUM ->{
                tvNumMoves.text = "Medium: 6 X 3"
                tvNumPairs.text = "Pairs: 0 / 9"
            }
            BoardSize.HARD ->{
                tvNumMoves.text = "Hard: 6 X 6"
                tvNumPairs.text = "Pairs: 0 / 12"
            }

        }
        memoryGame = MemoryGame(boardSize, customGameImages)
        clRoot = findViewById(R.id.clRoot)
        adapter = MemoryBoardAdapter(
            this,
            boardSize,
            memoryGame.cards,
            object : MemoryBoardAdapter.CardClickListener {
                override fun onCardClick(position: Int) {
                    updateGameWithFlip(position)
                }
            })
        rvBoard.adapter = adapter
        rvBoard.setHasFixedSize(true)
        rvBoard.layoutManager = GridLayoutManager(this, boardSize.getWidth())

    }
    }
