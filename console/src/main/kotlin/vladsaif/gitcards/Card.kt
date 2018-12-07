package vladsaif.gitcards

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.ktor.client.HttpClient
import io.ktor.client.call.call
import io.ktor.client.engine.android.Android
import io.ktor.client.response.readText
import io.ktor.http.HttpMethod
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

sealed class Card {

  class Descriptor(card: Card) {
    val type: String = card.javaClass.simpleName.substringAfterLast(".")
    val content: String = Gson().toJson(card)

    fun toCard(): Card {
      return Gson().fromJson(
        content, when (type) {
          formatClass(TextCard::class.java) -> TextCard::class.java
          else -> throw IllegalArgumentException()
        }
      )
    }
  }

  fun toDescriptor() = Descriptor(this)

  companion object {

    private fun formatClass(clazz: Class<*>): String = clazz.simpleName.substringAfterLast(".")
  }
}

data class TextCard(val frontText: String, val backText: String) : Card()

suspend fun fetchCards(): List<Card> {
  val call = HttpClient(Android).use { client ->
    client.call("https://raw.githubusercontent.com/Recognized/GitCards/master/cards.json") {
      method = HttpMethod.Get
    }
  }
  return Gson().fromJson<List<Card.Descriptor>>(
    call.response.readText(),
    (object : TypeToken<List<Card.Descriptor>>() {}).type
  ).map {
    it.toCard()
  }
}

data class PrioritizedCard(val card: Card) : Comparable<PrioritizedCard> {
  var priority: Int = DEFAULT_PRIORITY
  var timeLastChange: Long = TimeUnit.NANOSECONDS.convert(System.currentTimeMillis(), TimeUnit.MILLISECONDS)

  override fun compareTo(other: PrioritizedCard): Int {
    return priority - other.priority
  }

  class Descriptor(pCard: PrioritizedCard) {
    val priority: Int = pCard.priority
    val cardDescriptor = pCard.card.toDescriptor()

    fun toPrioritizedCard(): PrioritizedCard = PrioritizedCard(
      cardDescriptor.toCard()
    ).apply {
      this@apply.priority = this@Descriptor.priority
    }
  }

  fun toDescriptor() = Descriptor(this)

  companion object {
    const val DEFAULT_PRIORITY = 1 shl 16
    val COMPARATOR = kotlin.Comparator<PrioritizedCard> { a, b -> a.priority - b.priority }
  }
}

object CardsHolder {
  private val cardLock = ReentrantLock()
  private val allCards = mutableSetOf<PrioritizedCard>()
  private var cardsQueue = PriorityQueue<PrioritizedCard>(PrioritizedCard.COMPARATOR.reversed())

  val cards = sequence {
    if (cardLock.withLock { cardsQueue.isEmpty() }) return@sequence
    while (true) {
      val card = cardLock.withLock {
        cardsQueue.poll()
      }
      yield(card)
      cardLock.withLock {
        cardsQueue.add(card)
      }
    }
  }

  fun loadCards(newCards: List<Card>) {
    val newQueue = newCards.map { PrioritizedCard(it) }.toMutableSet()
    cardLock.withLock {
      allCards.addAll(newQueue)
      for (card in cardsQueue) {
        newQueue.removeIf { it.card == card.card }
      }
      cardsQueue.addAll(newQueue)
    }
  }

  fun loadPrioritizedCards(newCards: List<PrioritizedCard>) {
    val set = newCards.toMutableSet()
    cardLock.withLock {
      for (x in set) {
        allCards.removeIf { it in set }
        allCards.addAll(set)
        cardsQueue.clear()
        cardsQueue.addAll(allCards)
      }
    }
  }

  fun getAllCards(): List<PrioritizedCard> {
    return cardLock.withLock {
      allCards.toList()
    }
  }

  val hasCards
    get() = cardLock.withLock {
      cardsQueue.isNotEmpty()
    }
}