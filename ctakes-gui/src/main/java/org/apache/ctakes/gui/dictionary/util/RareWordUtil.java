package org.apache.ctakes.gui.dictionary.util;

import org.apache.ctakes.gui.dictionary.umls.Concept;

import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


/**
 * Author: SPF
 * Affiliation: CHIP-NLP
 * Date: 1/17/14
 */
final public class RareWordUtil {

   private RareWordUtil() {
   }

   // LookupDesc for the standard excluded pos tags are
   //   VB,VBD,VBG,VBN,VBP,VBZ,CC,CD,DT,EX,LS,MD,PDT,POS,PP,PP$,PRP,PRP$,RP,TO,WDT,WP,WPS,WRB
   // Listing every verb in the language seems a pain, but listing the others is possible.
   // Verbs should be rare in the dictionaries, excepting perhaps the activity and concept dictionaries
   // CD, CC, DT, EX, MD, PDT, PP, PP$, PRP, PRP$, RP, TO, WDT, WP, WPS, WRB
   // why not WP$ (possessive wh- pronoun "whose")
   // PP$ is a Brown POS tag, not Penn Treebank (as are the rest)

   static private final Set<String> BAD_POS_TERM_SET;

   static {
      final String[] BAD_POS_TERMS = {
            // VB  verb
            "be", "has", "have", "had", "do", "does", "did", "is", "isn", "am", "are", "was", "were",
            // CD  cardinal number
            "zero", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten",
            // CC  coordinating conjunction
            "and", "or", "but", "for", "nor", "so", "yet", "while", "because",
            // DT  determiner
            "this", "that", "these", "those", "the", "an", "a",
            // EX  existential there
            "there",
            // MD  modal
            "can", "should", "will", "may", "shall", "might", "must", "could", "would",
            // PDT  predeterminer
            "some", "many", "any", "each", "all", "few", "most", "both", "half", "none", "twice",
            // PP  prepositional phrase (preposition)
            "at", "before", "after", "behind", "beneath", "beside", "between", "into", "through", "across", "of",
            "concerning", "like", "unlike", "except", "with", "within", "without", "toward", "to", "past", "against",
            "during", "until", "throughout", "below", "besides", "beyond", "from", "inside", "near", "outside", "since",
            "upon",
            // PP$  possessive personal pronoun - Brown POS tag, not Penn TreeBank
            "my", "our", "your", "her", "their", "whose",
            // PRP  personal pronoun, plurals added
            "i", "you", "he", "she", "it", "them", "they", "we", "us",
            // PRP$  possesive pronoun
            "mine", "yours", "his", "hers", "its", "ours", "theirs",
            // RP  particle  - this contains some prepositions
            "about", "off", "up", "along", "away", "back", "by", "down", "forward", "in", "on", "out",
            "over", "around", "under",
            // TO  to  - also a preposition
            "to",
            // WDT  wh- determiner
            "what", "whatever", "which", "whichever",
            // WP, WPS  wh- pronoun, nominative wh- pronoun
            "who", "whom", "which", "that", "whoever", "whomever",
            // WRB
            "how", "where", "when", "however", "wherever", "whenever",
            // Mine ... some correlative conjunctions, etc.
            "no", "not", "oh", "mr", "mrs", "miss", "dr", "as", "only", "also", "either", "neither", "whether",
            // additional numbers
            "eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen", "seventeen", "eighteen", "nineteen",
            "twenty", "thirty", "forty", "fifty", "sixty", "seventy", "eighty", "ninety",
            "hundred", "thousand", "million", "billion", "trillion",
            };
      BAD_POS_TERM_SET = new HashSet<>( Arrays.asList( BAD_POS_TERMS ) );
   }

   static private final Pattern SPACE_PATTERN = Pattern.compile( "\\s+" );

   static public Collection<String> getUnwantedPosTexts() {
      return Collections.unmodifiableCollection( BAD_POS_TERM_SET );
   }

   static public boolean isRarableToken( final String token ) {
      if ( token.length() <= 1 ) {
         return false;
      }
      boolean hasLetter = false;
      for ( int i = 0; i < token.length(); i++ ) {
         if ( Character.isLetter( token.charAt( i ) ) ) {
            hasLetter = true;
            break;
         }
      }
      return hasLetter && !BAD_POS_TERM_SET.contains( token.toLowerCase() );
   }


   static public Map<String, Long> getTokenCounts( final Collection<Concept> concepts ) {
      return concepts.stream()
            .map( Concept::getTexts )
            .flatMap( Collection::stream )
            .map( SPACE_PATTERN::split )
            .flatMap( Arrays::stream )
            .filter( RareWordUtil::isRarableToken )
            .collect( Collectors.groupingBy( Function.identity(), Collectors.counting() ) );
   }

   static private void incrementCount( final Map<String, Integer> tokenCounts, final String token ) {
      Integer count = tokenCounts.get( token );
      if ( count == null ) {
         count = 0;
      }
      tokenCounts.put( token, (count + 1) );
   }

   //   static public String getRareToken( final Map<String,Integer> tokenCounts, final String text ) {
   //      final String[] tokens = text.split( "\\s+" );
   //      int bestIndex = 0;
   //      int bestCount = Integer.MAX_VALUE;
   //      for ( int i = 0; i < tokens.length; i++ ) {
   //         Integer count = tokenCounts.get( tokens[i] );
   //         if ( count != null && count < bestCount ) {
   //            bestIndex = i;
   //            bestCount = count;
   //         }
   //      }
   //      return tokens[bestIndex];
   //   }
   //
   //   static public int getRareTokenIndex( final Map<String,Integer> tokenCounts, final String text ) {
   //      final String[] tokens = text.split( "\\s+" );
   //      int bestIndex = 0;
   //      int bestCount = Integer.MAX_VALUE;
   //      for ( int i = 0; i < tokens.length; i++ ) {
   //         Integer count = tokenCounts.get( tokens[i] );
   //         if ( count != null && count < bestCount ) {
   //            bestIndex = i;
   //            bestCount = count;
   //         }
   //      }
   //      return bestIndex;
   //   }


   static public final class IndexedRareWord {
      final public String __word;
      final public int __index;
      final public int __tokenCount;

      private IndexedRareWord( final String word, final int index, final int tokenCount ) {
         __word = word;
         __index = index;
         __tokenCount = tokenCount;
      }
   }

   static public final IndexedRareWord NULL_RARE_WORD = new IndexedRareWord( null, -1, -1 );

   static public IndexedRareWord getIndexedRareWord( final String text,
                                                     final Map<String, Long> tokenCounts ) {
      final String[] tokens = SPACE_PATTERN.split( text );
      int bestIndex = 0;
      long bestCount = Long.MAX_VALUE;
      for ( int i = 0; i < tokens.length; i++ ) {
         if ( tokens[ i ].length() >= 48 ) {
            continue;
         }
         Long count = tokenCounts.get( tokens[ i ] );
         if ( count != null && count < bestCount ) {
            bestIndex = i;
            bestCount = count;
         }
      }
      if ( bestCount == Long.MAX_VALUE ) {
         return NULL_RARE_WORD;
      }
      return new IndexedRareWord( tokens[ bestIndex ], bestIndex, tokens.length );
   }
}
