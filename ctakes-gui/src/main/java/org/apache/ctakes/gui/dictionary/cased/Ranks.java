package org.apache.ctakes.gui.dictionary.cased;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 8/26/2020
 */
public enum Ranks {
   INSTANCE;

   static public Ranks getInstance() {
      return INSTANCE;
   }

   private final Map<String, Integer> _ranks = new HashMap<>();
   private List<String> _rankList;


   public void setUmlsRank( final String vocabulary, final String tty, final int rank ) {
      _ranks.put( getRankCode( vocabulary, tty ), rank );
   }

//   public int getRank( final String vocabulary, final String tty ) {
//      return _ranks.getOrDefault( getCode( vocabulary, tty ), -1 );
//   }

   public int getRank( final String vocabulary, final String tty ) {
      return getCodeRank( getRankCode( vocabulary, tty ) );
   }

   public int getCodeRank( final String rankCode ) {
      if ( _rankList == null ) {
         _rankList = _ranks.entrySet()
                           .stream()
                           .sorted( Comparator.comparingInt( Map.Entry::getValue ) )
                           .map( Map.Entry::getKey )
                           .collect( Collectors.toList() );
      }
      return _rankList.size() - _rankList.indexOf( rankCode );
   }

   static public String getRankCode( final String vocabulary, final String tty ) {
      return vocabulary + "_" + tty;
   }

}
