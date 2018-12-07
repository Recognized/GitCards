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

sealed class Card(val id: Int) {

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

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is Card) return false

    if (id != other.id) return false

    return true
  }

  override fun hashCode(): Int {
    return id
  }

  companion object {

    private fun formatClass(clazz: Class<*>): String = clazz.simpleName.substringAfterLast(".")
  }
}

class TextCard(id: Int, val frontText: String, val backText: String) : Card(id)

suspend fun fetchCards(url: String): List<Card> {
  val call = HttpClient(Android).use { client ->
    client.call(url) {
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

class PrioritizedCard(val card: Card) : Comparable<PrioritizedCard> {
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

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is PrioritizedCard) return false

    if (card != other.card) return false

    return true
  }

  override fun hashCode(): Int {
    return card.hashCode()
  }

  companion object {
    const val DEFAULT_PRIORITY = 1 shl 16
    val COMPARATOR = kotlin.Comparator<PrioritizedCard> { a, b ->
      if (a.card.id == b.card.id) return@Comparator 0
      val res = b.priority - a.priority
      if (res != 0) res else a.card.id - b.card.id
    }
  }
}

object CardsHolder {
  private val cardLock = ReentrantLock()
  private val allCards = mutableSetOf<PrioritizedCard>()
  private val removed = mutableSetOf<PrioritizedCard>()
  private var cardsQueue = TreeSet<PrioritizedCard>(PrioritizedCard.COMPARATOR)

  val cards = sequence {
    if (cardLock.withLock { cardsQueue.isEmpty() }) return@sequence
    while (true) {
      val card = cardLock.withLock {
        val first = cardsQueue.first()
        cardsQueue.remove(first)
        first
      }
      yield(card)
      cardLock.withLock {
        if (!removed.remove(card)) {
          cardsQueue.add(card)
        }
      }
    }
  }

  fun removeCard(card: PrioritizedCard) {
    cardLock.withLock {
      removed.add(card)
      allCards.remove(card)
      cardsQueue.remove(card)
    }
  }

  fun loadCards(newCards: List<Card>) {
    val newQueue = newCards.map { PrioritizedCard(it) }.toMutableSet()
    cardLock.withLock {
      val iter = newQueue.iterator()
      while (iter.hasNext()) {
        val elem = iter.next()
        if (elem in allCards) {
          iter.remove()
        }
      }
      allCards.addAll(newQueue)
      cardsQueue.addAll(newQueue)
    }
  }

  fun loadPrioritizedCards(newCards: List<PrioritizedCard>) {
    val set = newCards.toMutableSet()
    cardLock.withLock {
      val iter = allCards.iterator()
      while (iter.hasNext()) {
        if (iter.next() in set) {
          iter.remove()
        }
      }
      allCards.addAll(set)
      cardsQueue.clear()
      cardsQueue.addAll(allCards)
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