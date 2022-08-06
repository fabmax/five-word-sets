import kotlinx.coroutines.*
import java.io.File
import java.io.FileWriter
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

val startTime = System.nanoTime()
val letterFrequencies = mutableMapOf<Char, AtomicInteger>()
val wordsByChars = mutableMapOf<Char, MutableSet<String>>()

fun main() {
    // Load words
    val words = File("words_alpha.txt").readLines()
        .filter { it.length == 5 && it.toSet().size == 5 }      // only keep words with five distinct letters
        .distinctBy { it.toSortedSet() }                        // remove anagrams
        .toSet()

    println("Processing ${words.size} unique five letter words...")

    // Build a dictionary with lists of all words containing a given letter:
    //   'a' -> list of all words containing an 'a'
    //   'b' -> list of all words containing a 'b'
    //   and so on...
    words.forEach { word ->
        word.forEach { char ->
            // Add current word to the list for the current letter
            wordsByChars.getOrPut(char) { mutableSetOf() } += word
            // Also count the frequencies of each letter
            letterFrequencies.getOrPut(char) { AtomicInteger() }.incrementAndGet()
        }
    }

    // Optionally: printLetterFrequencies()

    // Do all the work!
    val fiveWordSets = findFiveWordSets(words)

    // And we are done
    println(String.format(Locale.ENGLISH, "100.0%%, %5.1f secs", (System.nanoTime() - startTime) / 1e9))
    FileWriter("result.txt").use { writer ->
        fiveWordSets.forEach {
            writer.write("$it\n")
        }
    }
    println("Wrote result.txt containing ${fiveWordSets.size} five-word sets")
}

/**
 * Main task: Search for sets of five words containing no duplicate letters. Uses coroutines to split the work
 * across all available CPU cores.
 * Result sets are returned as a list of comma-separated strings containing one five-word set per line.
 */
fun findFiveWordSets(words: Set<String>): Set<String> = runBlocking {
    // Intelligently select words to begin with:
    // Since valid sets of five words will use 25 letters, there can't be a set which contains neither an 'x' nor a
    // 'q'. By selecting all words containing an 'x' or a 'q' as seeds and then searching for four more words per seed
    // word we can't miss a set.
    // Instead of 'x' and 'q' we could choose two arbitrary letters here, but 'x' and 'q' are used for being the two
    // least frequent ones, which minimizes the size of the list of seed words. This reduces processing time by quite
    // a bit.
    // As an additional minor optimization we sort the seed words, so that seeds containing more low frequency letters
    // are tested first. This isn't strictly necessary but helps in better utilizing all CPU cores until the end.
    val seedWords = (wordsByChars['q']!! + wordsByChars['x']!!)
        .sortedBy { w -> w.sumOf { letterFrequencies[it]!!.get() } }

    val wordResults = mutableListOf<Deferred<Set<Set<String>>>>()
    val progressCount = AtomicInteger()
    withContext(Dispatchers.Default) {
        seedWords.forEach { seed ->
            // Use coroutines to process each word in parallel
            wordResults += async {
                // Collect all valid sets of five words for the current seed word
                val result = findWords(words, seed, 0)
                // Update progress after we are done with the current word
                printProgress(progressCount.incrementAndGet(), seedWords.size)
                // Return result
                result
            }
        }
    }

    // Collect all coroutine / seed word results and return them as the final result
    wordResults
        .flatMap { deferred -> deferred.await() }
        .map { setOfFiveWords -> setOfFiveWords.sorted().joinToString { it } }
        .toSortedSet()
}

/**
 * This is where the magic happens:
 * Recursively collect words (not sharing any letters with [addWord]) from the given set of word [candidates]. If we
 * reach a recursion [depth] of 4, we found a valid set five words with only distinct letters and return it.
 */
fun findWords(candidates: Set<String>, addWord: String, depth: Int): Set<Set<String>> {
    if (depth == 4) {
        // We reached the maximum recursion depth, i.e. we found a valid five-word set!
        // Return addWord, which will be combined with the other words of the set in the higher recursion levels.
        return setOf(setOf(addWord))

    } else {
        // Filter candidate words: Remove all words containing letters of our current addWord. We can do this
        // relatively fast because of the words-by-character lists we built in the beginning.
        val remainingWords = candidates.toMutableSet()
        addWord.forEach { remainingWords -= wordsByChars[it]!! }

        val result = mutableSetOf<SortedSet<String>>()
        remainingWords.forEach { candidate ->
            // Iterate over remaining candidate words and recursively select additional words not sharing any letters
            // with the current candidate word
            findWords(remainingWords, candidate, depth + 1).forEach { subSet ->
                // If we get here, the deeper recursion levels found valid words. Combine them with the current addWord
                // to a sorted set and add it to the result. We use a sorted set here, so that different combinations
                // of the same words will result in equal sets and therefore only appear once in the result.
                result += (subSet + addWord).toSortedSet()
            }
        }

        // Return the result of this iteration. It will be either empty or contain sets of words of size (5 - depth),
        // i.e. at depth 3, result will contain two word sets up to depth 0, where result contains five word sets.
        return result
    }
}

/**
 * From time to time, prints progress info to the console.
 */
fun printProgress(progress: Int, size: Int) {
    if (progress % 10 == 0) {
        println(String.format(Locale.ENGLISH, "%5.1f%%, %5.1f secs...",
            100f * progress / size, (System.nanoTime() - startTime) / 1e9))
    }
}

/**
 * Helper function to print the frequencies of all letters in the given collection of words.
 */
fun printLetterFrequencies() {
    letterFrequencies.map { it.key to it.value }
        .sortedBy { (_, freq) -> freq.get() }
        .forEach { (char, freq) ->
            println("$char: $freq")
        }
}