package vladsaif.gitcards

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class SerializationTest {

  @Test
  fun cardDeserialization() {
    println("""[{"type":"TextCard","content":"{\"frontText\":\"concise\",\"backText\":\"short and informative\"}"}]""")
    val card = Gson().fromJson<List<Card.Descriptor>>(
      """[{"type":"TextCard","content":"{\"frontText\":\"concise\",\"backText\":\"short and informative\"}"}]""",
      (object : TypeToken<List<Card.Descriptor>>() {}).type
    ).map {
      it.toCard()
    }.first()
    assertEquals(TextCard("concise", "short and informative"), card)
  }
}
