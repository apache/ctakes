package org.apache.ctakes.gui.dictionary.umls;

import org.apache.log4j.Logger;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 12/12/2015
 */
public enum VocabularyStore {
   INSTANCE;

   static public VocabularyStore getInstance() {
      return INSTANCE;
   }

   private final Logger LOGGER = Logger.getLogger( "Vocabulary" );

   private final Map<String, Class<?>> _vocabularyClasses = new HashMap<>();

   public Collection<String> getAllVocabularies() {
      return _vocabularyClasses.keySet();
   }

   public Class<?> getVocabularyClass( final String vocabulary ) {
      return _vocabularyClasses.get( vocabulary );
   }

   public void addVocabulary( final String vocabulary, final String code ) {
      final Class<?> vocabularyClass = _vocabularyClasses.get( vocabulary );
      if ( String.class.equals( vocabularyClass ) ) {
         return;
      }
      _vocabularyClasses.put( vocabulary, getBestClass( code ) );
   }

   public String getJdbcClass( final String vocabulary ) {
      final Class<?> vocabularyClass = _vocabularyClasses.get( vocabulary );
      if ( String.class.equals( vocabularyClass ) ) {
         return "VARCHAR(48)";
      } else if ( Double.class.equals( vocabularyClass ) ) {
         return "FLOAT";
      } else if ( Long.class.equals( vocabularyClass ) ) {
         return "BIGINT";
      } else if ( Integer.class.equals( vocabularyClass ) ) {
         return "INTEGER";
      } else {
         LOGGER.error( "Could not derive database class for " + vocabularyClass.getName() );
      }
      return "VARCHAR(48)";
   }

   public String getCtakesClass( final String vocabulary ) {
      final Class<?> vocabularyClass = _vocabularyClasses.get( vocabulary );
      if ( String.class.equals( vocabularyClass ) ) {
         return "text";
      } else if ( Double.class.equals( vocabularyClass ) ) {
         return "double";
      } else if ( Long.class.equals( vocabularyClass ) ) {
         return "long";
      } else if ( Integer.class.equals( vocabularyClass ) ) {
         return "int";
      } else {
         LOGGER.error( "Could not derive database class for " + vocabularyClass.getName() );
      }
      return "text";
   }

   static private Class<?> getBestClassFuture( final String code, final Class<?> currentClass ) {
      boolean haveDot = false;
      for ( char c : code.toCharArray() ) {
         if ( !Character.isDigit( c ) ) {
            if ( c == '.' ) {
               if ( haveDot ) {
                  return String.class;
               }
               haveDot = true;
            }
            return String.class;
         }
      }
      if ( haveDot || Double.class.equals( currentClass ) ) {
         return Double.class;
      }
      if ( code.length() > 9 || Long.class.equals( currentClass ) ) {
         return Long.class;
      }
      return Integer.class;
   }

   // TODO replace with getBestClassFuture when ctakes is upgraded to accept double and int
   static private Class<?> getBestClass( final String code ) {
      for ( char c : code.toCharArray() ) {
         if ( !Character.isDigit( c ) ) {
            return String.class;
         }
      }
      return Long.class;
   }

}
