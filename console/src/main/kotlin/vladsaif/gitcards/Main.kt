package vladsaif.gitcards

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.nio.file.Files
import java.nio.file.Paths

class Main {

  companion object {

    @JvmStatic
    fun main(args: Array<String>) {
      try {
        val dataFile = if (args.isNotEmpty()) Paths.get(args.first()) else Paths.get("cards.json")
        val existingCards = mutableSetOf<Card>()
        if (Files.exists(dataFile)) {
          existingCards.addAll(dataFile.toFile().bufferedReader().use { reader ->
            Gson().fromJson<List<String>>(reader, (object : TypeToken<List<String>>() {}).type)
              .map { Card.deserialize(it) }
          })
        }
        while (true) {
          try {
            val line = readLine() ?: break
            if (line.toLowerCase() == ":q") break
            val (a, b) = line.split("=")
            if (a.isEmpty() || b.isEmpty()) throw Exception()
            existingCards.add(TextCard(a.trim(), b.trim()))
          } catch (ex: Throwable) {
            println("Oops...")
          }
        }
        dataFile.toFile().outputStream().bufferedWriter().use {
          it.append(Gson().toJson(existingCards.toList()))
        }
      } catch (ex: Throwable) {
        println(ex.message)
      }
    }
  }
}