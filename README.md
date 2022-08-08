# five-word-sets

This project is inspired by the YouTube video
[Can you find: five five-letter words with twenty-five unique letters?](https://www.youtube.com/watch?v=_-AfhLQfb6w)
by Matt Parker / Stand-up Maths. It solves the problem of finding sets of five five-letter words with 25 distinct
characters (i.e. none of the words share any letters), which is supposed to be hard.

The initial list of words is provided by the text file [words_alpha.txt](words_alpha.txt), which is the same Parker
used (the original link is here: [https://github.com/dwyl/english-words](https://github.com/dwyl/english-words)). It
contains 10175 five-letter words with no duplicate characters, which can be further reduced to 5977 words by removing
anagrams.

My [solution](src/main/kotlin/FiveLetterWords.kt) solves the problem in **~25 seconds** (based on the 5977
non-anagram word set, on an 8-core CPU). It does not use fancy math but a few rather simple tricks:

 - Valid five-word sets must use 25 out of 26 characters. Therefore, we can select words containing two rare letters
   (`x` and / or `q`, there are only 315 such words) and use only them as 'seed words'. Then we only have to find four
   more words per seed word. Because every valid set must contain an `x` and / or a `q` we can't miss a set.
  
 - Initially, we build 26 sets (one for each letter) containing all words that include the particular letter. This
   helps in efficiently removing invalid word candidates later on.

 - The search function works recursively and removes invalid words from the list of candidates for deeper recursion
   levels (using the words-by-letter sets built before). This way the search can terminate early once there are no more
   word candidates left.

 - It does not use Python :wink:

## Running the code
You can run the code from the console (You will need to have a Java JDK installed): 
```
./gradlew run
```
After running the code, the resulting five-word sets can be found in `result.txt`