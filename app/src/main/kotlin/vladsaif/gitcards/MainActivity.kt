package vladsaif.gitcards

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import android.widget.Toolbar
import butterknife.BindView
import butterknife.OnClick
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.actor
import kotlin.coroutines.CoroutineContext
import kotlin.random.Random

class MainActivity : Activity(), CoroutineScope {

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
          cardsCarousel.next()
        }
      }
    }
  }
  override val coroutineContext: CoroutineContext
    get() = mJob
  private val dataFile get() = filesDir.resolve("priorities")

  @BindView(R.id.toolbar)
  lateinit var toolbar: Toolbar
  @BindView(R.id.frontFrame)
  lateinit var frontFrame: FrameLayout
  @BindView(R.id.backFrame)
  lateinit var backFrame: FrameLayout

  private var currentCard: PrioritizedCard? = null

  private val cardsCarousel = iterator {
    for (card in CardsHolder.cards) {
      currentCard = card
      frontFrame.removeAllViews()
      backFrame.removeAllViews()
      frontFrame.addView(card.card.getFrontView(applicationContext))
      backFrame.addView(card.card.getBackView(applicationContext))
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

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
    startShowing()
  }

  @SuppressLint("SetTextI18n")
  private fun startShowing() {
    if (CardsHolder.hasCards) {
      cardsCarousel.next()
    } else {
      frontFrame.addView(TextView(applicationContext).apply { text = "No cards available" })
    }
  }

  override fun onResume() {
    if (dataFile.exists()) {
      val cards = dataFile.inputStream().bufferedReader().use { reader ->
        Gson().fromJson<List<String>>(reader, (object : TypeToken<List<String>>() {}).type)
          .map { PrioritizedCard.deserialize(it) }
      }
      CardsHolder.loadPrioritizedCards(cards)
    }
    super.onResume()
  }

  override fun onPause() {
    dataFile.outputStream().bufferedWriter().use {
      it.append(Gson().toJson(CardsHolder.getAllCards().map { it.serialize() }))
    }
    super.onPause()
  }

  @ObsoleteCoroutinesApi
  @OnClick(R.id.sync)
  fun sync() {
    syncActor.offer(Unit)
  }

  @OnClick(R.id.ok_button)
  fun onBadClick() {
    onMemoryButtonClick(MemoryStrength.BAD)
  }

  @OnClick(R.id.ok_button)
  fun onOkClick() {
    onMemoryButtonClick(MemoryStrength.OK)
  }

  @OnClick(R.id.ok_button)
  fun onGoodClick() {
    onMemoryButtonClick(MemoryStrength.GOOD)
  }

  private fun onMemoryButtonClick(strength: MemoryStrength) {
    currentCard?.let {
      it.priority += strength.change + Random.nextInt(Math.abs(strength.change)) - Math.abs(strength.change) / 2
    }
    cardsCarousel.next()
  }

  private enum class MemoryStrength(val change: Int) {
    BAD(-10), OK(-100), GOOD(-1000);
  }
}
