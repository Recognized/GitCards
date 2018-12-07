package vladsaif.gitcards

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.lb.auto_fit_textview.AutoResizeTextView
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.actor
import java.util.concurrent.TimeUnit.*
import kotlin.coroutines.CoroutineContext
import kotlin.math.min
import kotlin.random.Random


class MainActivity : AppCompatActivity(), CoroutineScope {

  private val mJob = Job()
  @ObsoleteCoroutinesApi
  private val syncActor = actor<Unit>(Dispatchers.IO) {
    for (x in channel) {
      val cards = fetchCards()
      val hasCardsBefore = CardsHolder.hasCards
      CardsHolder.loadCards(cards)
      withContext(Dispatchers.Main) {
        Toast.makeText(applicationContext, "Loaded cards: ${cards.size}", Toast.LENGTH_LONG).show()
        if (!hasCardsBefore && CardsHolder.hasCards) {
          buttonsFrame.visibility = View.VISIBLE
          cardsCarousel.next()
        }
      }
    }
  }
  override val coroutineContext: CoroutineContext
    get() = mJob
  private val dataFile get() = filesDir.resolve("priorities")

  val toolbar: Toolbar by lazy { findViewById<Toolbar>(R.id.toolbar) }
  val frontFrame: FrameLayout by lazy { findViewById<FrameLayout>(R.id.frontFrame) }
  val backFrame: FrameLayout by lazy { findViewById<FrameLayout>(R.id.backFrame) }
  val buttonsFrame: LinearLayout by lazy { findViewById<LinearLayout>(R.id.buttons) }

  private var currentCard: PrioritizedCard? = null

  @SuppressLint("SetTextI18n")
  private val cardsCarousel = iterator {
    for (card in CardsHolder.cards) {
      currentCard = card
      frontFrame.removeAllViews()
      backFrame.removeAllViews()
      buttonsFrame.visibility = View.GONE
      frontFrame.addView(card.card.getFrontView(applicationContext))
      backFrame.addView(Button(applicationContext).apply {
        setOnClickListener {
          onShowAnswerButtonClick()
        }
        text = "Show back card"
        layoutParams =
            FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
              .apply {
                gravity = Gravity.CENTER_HORIZONTAL or Gravity.CENTER_VERTICAL
              }
      })
      yield(Unit)
      backFrame.removeAllViews()
      buttonsFrame.visibility = View.VISIBLE
      backFrame.addView(card.card.getBackView(applicationContext))
      yield(Unit)
    }
  }

  private fun onShowAnswerButtonClick() {
    cardsCarousel.next()
  }

  private fun Card.getFrontView(context: Context): View {
    return when (this) {
      is TextCard -> AutoResizeTextView(context).apply {
        text = frontText
        layoutParams =
            FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
              .apply {
                gravity = Gravity.CENTER_HORIZONTAL or Gravity.BOTTOM
              }
        format()
        gravity = Gravity.CENTER_HORIZONTAL or Gravity.BOTTOM
      }
    }
  }

  private fun AutoResizeTextView.format() {
    textSize = 500.0f
    setMinTextSize(20.0f)
    setTextColor(Color.BLACK)
    setPadding(30.toPx(), 10.toPx(), 30.toPx(), 10.toPx())
  }

  private fun Card.getBackView(context: Context): View {
    return when (this) {
      is TextCard -> AutoResizeTextView(context).apply {
        text = backText
        layoutParams =
            FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
              .apply {
                gravity = Gravity.CENTER_HORIZONTAL or Gravity.TOP
              }
        format()
        gravity = Gravity.CENTER_HORIZONTAL or Gravity.TOP
      }
    }
  }

  @ObsoleteCoroutinesApi
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
    toolbar.setTitleTextColor(Color.WHITE)
    setSupportActionBar(toolbar)
    findViewById<Button>(R.id.bad_button).setOnClickListener { onBadClick() }
    findViewById<Button>(R.id.ok_button).setOnClickListener { onOkClick() }
    findViewById<Button>(R.id.good_button).setOnClickListener { onGoodClick() }
    findViewById<Button>(R.id.sync).setOnClickListener { sync() }
  }

  @SuppressLint("SetTextI18n")
  private fun startShowing() {
    if (CardsHolder.hasCards) {
      backFrame.removeAllViews()
      buttonsFrame.visibility = View.VISIBLE
      cardsCarousel.next()
    } else {
      frontFrame.addView(TextView(applicationContext).apply {
        text = "No cards available"
        layoutParams =
            FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT)
              .apply {
                gravity = Gravity.CENTER_HORIZONTAL or Gravity.BOTTOM
              }
      })
    }
  }

  override fun onStart() {
    if (dataFile.exists()) {
      val cards = dataFile.inputStream().bufferedReader().use { reader ->
        val lines = reader.lineSequence().joinToString("\n")
        if (lines.isBlank()) listOf()
        else Gson().fromJson<List<PrioritizedCard.Descriptor>>(
          lines,
          (object : TypeToken<List<PrioritizedCard.Descriptor>>() {}).type
        ).map { it.toPrioritizedCard() }
      }
      CardsHolder.loadPrioritizedCards(cards)
    }
    startShowing()
    super.onStart()
  }

  override fun onPause() {
    dataFile.outputStream().bufferedWriter().use { writer ->
      writer.append(Gson().toJson(CardsHolder.getAllCards().map { it.toDescriptor() }))
    }
    super.onPause()
  }

  @ObsoleteCoroutinesApi
  private fun sync() {
    syncActor.offer(Unit)
  }

  private fun onBadClick() {
    onMemoryButtonClick(MemoryStrength.BAD)
  }

  private fun onOkClick() {
    onMemoryButtonClick(MemoryStrength.OK)
  }

  private fun onGoodClick() {
    onMemoryButtonClick(MemoryStrength.GOOD)
  }

  private fun onMemoryButtonClick(strength: MemoryStrength) {
    currentCard?.let {
      it.priority += scaleByTime(
        it.timeLastChange,
        strength.change + Random.nextInt(Math.abs(strength.change)) - Math.abs(strength.change) / 2
      )
      it.timeLastChange = NANOSECONDS.convert(System.currentTimeMillis(), MILLISECONDS)
    }
    cardsCarousel.next()
  }

  private fun scaleByTime(lastTime: Long, delta: Int): Int {
    val difference = NANOSECONDS.convert(System.currentTimeMillis(), MILLISECONDS) - lastTime
    val days = DAYS.convert(difference, NANOSECONDS)
    val newDelta = when {
      days <= 0L -> delta / 1000
      days == 1L -> delta / 100
      days <= 7L -> delta / 10
      else -> delta
    }
    return min(newDelta, -1)
  }

  private enum class MemoryStrength(val change: Int) {
    BAD(-1000), OK(-10000), GOOD(-100000);
  }

  companion object {
    fun Int.toPx(): Int {
      return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, this.toFloat(), DisplayMetrics()).toInt()
    }
  }
}
