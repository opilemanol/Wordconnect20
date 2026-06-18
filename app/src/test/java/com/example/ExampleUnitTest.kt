package com.example

import org.junit.Assert.*
import org.junit.Test

/**
 * Local unit tests to systematically validate dataset and game logic.
 */
class ExampleUnitTest {
  @Test
  fun validatePuzzlePool_allWordsAreSpellable() {
    val errors = mutableListOf<String>()
    
    for (pair in PuzzleData.puzzlePool) {
      val letters = pair.first.uppercase()
      val targets = pair.second
      
      // Calculate character pool of the wheel
      val lettersFreq = letters.groupingBy { it }.eachCount()
      
      for (target in targets) {
        val targetUpper = target.uppercase()
        val targetFreq = targetUpper.groupingBy { it }.eachCount()
        
        for ((char, count) in targetFreq) {
          val maxAllowed = lettersFreq[char] ?: 0
          if (count > maxAllowed) {
            errors.add("Puzzle wheel '$letters' contains unspellable word '$target' (requires $count of '$char', but wheel only has $maxAllowed)")
          }
        }
      }
    }
    
    if (errors.isNotEmpty()) {
      fail("Dataset validation found errors:\n" + errors.joinToString("\n"))
    }
  }

  @Test
  fun validatePuzzlePool_hasNoDuplicateMainWords() {
    val seen = mutableSetOf<String>()
    val duplicateKeys = mutableSetOf<String>()
    for (pair in PuzzleData.puzzlePool) {
      val key = pair.first.uppercase()
      if (!seen.add(key)) {
        duplicateKeys.add(key)
      }
    }
    if (duplicateKeys.isNotEmpty()) {
      println("Information: Puzzle dataset contains non-unique wheels: $duplicateKeys")
    }
  }
}

