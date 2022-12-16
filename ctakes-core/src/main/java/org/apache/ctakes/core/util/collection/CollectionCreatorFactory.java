package org.apache.ctakes.core.util.collection;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 9/23/2014
 */
final public class CollectionCreatorFactory {

   private CollectionCreatorFactory() {
   }

   static public <V> CollectionCreator<V, Set<V>> createSetCreator() {
      return new CollectionCreator<V, Set<V>>() {
         @Override
         public Set<V> createCollection() {
            return new HashSet<>();
         }

         @Override
         public Set<V> createCollection( final int size ) {
            return new HashSet<>( size );
         }
      };
   }

   static public <V> CollectionCreator<V, List<V>> createListCreator() {
      return new CollectionCreator<V, List<V>>() {
         @Override
         public List<V> createCollection() {
            return new ArrayList<>();
         }

         @Override
         public List<V> createCollection( final int size ) {
            return new ArrayList<>( size );
         }
      };
   }

}
