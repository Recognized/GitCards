@file:JvmName("MainKt")

package vladsaif.gitcards

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.InputStreamReader
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Paths

fun main(args: Array<String>) {
  InputStreamReader(Files.newInputStream(Paths.get("words.txt")), Charset.forName("UTF-8")).buffered().use {
    try {
      val dataFile = if (args.isNotEmpty()) Paths.get(args.first()) else Paths.get("cards.json")
      val existingCards = mutableSetOf<Card>()
      var maxId = 0
      while (true) {
        try {
          val line = it.readLine() ?: break
          if (line.trim() == "*") {
            maxId++
            continue
          }
          if (line.toLowerCase() == ":q") break
          val (a, b) = line.split("=")
          if (a.isEmpty() || b.isEmpty()) throw Exception()
          existingCards.add(TextCard(++maxId, a.trim(), b.trim()))
        } catch (ex: Throwable) {
          println("Oops...")
        }
      }
      dataFile.toFile().outputStream().bufferedWriter().use { writer ->
        writer.append(Gson().toJson(existingCards.map { it.toDescriptor() }))
      }
    } catch (ex: Throwable) {
      println(ex.message)
    }
  }
}
