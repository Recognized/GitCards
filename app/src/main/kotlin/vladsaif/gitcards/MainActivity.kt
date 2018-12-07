package vladsaif.gitcards

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.Gravity
import android.view.View
import android.widget.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
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
          buttons.visibility = View.VISIBLE
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
  val buttons: LinearLayout by lazy { findViewById<LinearLayout>(R.id.buttons) }

  private var currentCard: PrioritizedCard? = null

  private val cardsCarousel = iterator {
    for (card in CardsHolder.cards) {
      currentCard = card
      frontFrame.removeAllViews()
      backFrame.removeAllViews()
      frontFrame.addView(card.card.getFrontView(applicationContext).formatFrontView())
      backFrame.addView(card.card.getBackView(applicationContext).formatBackView())
      yield(Unit)
    }
  }

  private fun Card.getFrontView(context: Context): View {
    return when (this) {
      is TextCard -> TextView(context).apply {
        text = frontText
      }
    }
  }

  private fun Card.getBackView(context: Context): View {
    return when (this) {
      is TextCard -> TextView(context).apply {
        text = backText
      }
    }
  }

  @ObsoleteCoroutinesApi
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
    setSupportActionBar(toolbar)
    findViewById<Button>(R.id.bad_button).setOnClickListener { onBadClick() }
    findViewById<Button>(R.id.ok_button).setOnClickListener { onOkClick() }
    findViewById<Button>(R.id.good_button).setOnClickListener { onGoodClick() }
    findViewById<Button>(R.id.sync).setOnClickListener { sync() }
  }

  @SuppressLint("SetTextI18n")
  private fun startShowing() {
    if (CardsHolder.hasCards && buttons.visibility == View.INVISIBLE) {
      buttons.visibility = View.VISIBLE
      cardsCarousel.next()
    } else {
      frontFrame.addView(TextView(applicationContext).apply {
        text = "No cards available"
      }.formatFrontView())
    }
  }

  private fun View.formatFrontView(): View {
    layoutParams =
        LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
          .apply {
            gravity = Gravity.CENTER_HORIZONTAL or Gravity.BOTTOM
          }
    setPadding(5, 5, 5, 5)
    return this
  }

  private fun View.formatBackView(): View {
    layoutParams =
        LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
          .apply {
            gravity = Gravity.CENTER_HORIZONTAL or Gravity.TOP
          }
    setPadding(5, 5, 5, 5)
    return this
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
}
