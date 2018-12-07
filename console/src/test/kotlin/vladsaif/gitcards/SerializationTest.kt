package vladsaif.gitcards

import org.junit.Test

import org.junit.Assert.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class SerializationTest {
  @Test
  fun forthAndForward() {
    val original = PrioritizedCard(TextCard("hello", "привет"))
    val deserialized = PrioritizedCard.deserialize(original.serialize())
    assertEquals(original, deserialized)
    assertEquals(original.priority, deserialized.priority)
  }
}
