package org.apache.ctakes.core.util.collection;

import java.util.*;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 9/23/2014
 */
final public class DefaultCollectionMap<K, V, T extends Collection<V>> implements CollectionMap<K, V, T> {

   private final Map<K, T> _delegate;
   private final CollectionCreator<V, T> _collectionCreator;
   private final T EMPTY_COLLECTION;

   public DefaultCollectionMap( final Map<K, T> delegate, final CollectionCreator<V, T> collectionCreator ) {
      _delegate = delegate;
      _collectionCreator = collectionCreator;
      EMPTY_COLLECTION = collectionCreator.createCollection();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Iterator<Entry<K, T>> iterator() {
      final Iterator<Map.Entry<K, T>> setIterator = _delegate.entrySet().iterator();
      return new Iterator<Map.Entry<K, T>>() {
         public boolean hasNext() {
            return setIterator.hasNext();
         }

         public Map.Entry<K, T> next() {
            final Map.Entry<K, T> next = setIterator.next();
            return new Map.Entry<K, T>() {
               public K getKey() {
                  return next.getKey();
               }

               public T getValue() {
                  return next.getValue();
               }

               public T setValue( final T value ) {
                  return null;
               }
            };
         }

         public void remove() {
         }
      };
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Collection<T> getAllCollections() {
      return new HashSet<>( _delegate.values() );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public T getCollection( final K key ) {
      final T collection = _delegate.get( key );
      if ( collection != null ) {
         return collection;
      }
      return EMPTY_COLLECTION;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public T getOrCreateCollection( final K key ) {
      T collection = _delegate.get( key );
      if ( collection == null ) {
         collection = _collectionCreator.createCollection();
         _delegate.put( key, collection );
      }
      return collection;
   }


   /**
    * {@inheritDoc}
    */
   @Override
   public boolean containsValue( final K key, final V value ) {
      final T collection = _delegate.get( key );
      return collection != null && collection.contains( value );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public boolean placeValue( final K key, final V value ) {
      T collection = _delegate.get( key );
      if ( collection == null ) {
         collection = _collectionCreator.createCollection();
         _delegate.put( key, collection );
      }
      return collection.add( value );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public boolean placeMap( final Map<K, V> map ) {
      boolean placedAny = false;
      for ( Map.Entry<K, V> entry : map.entrySet() ) {
         final boolean placed = placeValue( entry.getKey(), entry.getValue() );
         placedAny = placedAny || placed;
      }
      return placedAny;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void removeValue( final K key, final V value ) {
      final T collection = _delegate.get( key );
      if ( collection == null ) {
         return;
      }
      collection.remove( value );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public <C extends Collection<V>> int addAllValues( final K key, final C values ) {
      if ( values == null || values.isEmpty() ) {
         return 0;
      }
      T collection = _delegate.get( key );
      if ( collection == null ) {
         collection = _collectionCreator.createCollection();
         _delegate.put( key, collection );
      }
      final int oldSize = collection.size();
      collection.addAll( values );
      return collection.size() - oldSize;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void clearCollection( final K key ) {
      final T collection = _delegate.get( key );
      if ( collection != null ) {
         collection.clear();
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public int size() {
      return _delegate.size();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public boolean isEmpty() {
      return _delegate.isEmpty();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public boolean containsKey( final Object key ) {
      return _delegate.containsKey( key );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public boolean containsValue( final Object value ) {
      return _delegate.containsValue( value );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public T get( final Object key ) {
      return _delegate.get( key );
   }

   // Modification Operations

   /**
    * {@inheritDoc}
    */
   @Override
   public T put( final K key, final T value ) {
      return _delegate.put( key, value );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public T remove( final Object key ) {
      return _delegate.remove( key );
   }


   // Bulk Operations

   /**
    * {@inheritDoc}
    */
   @Override
   public void putAll( final Map<? extends K, ? extends T> map ) {
      _delegate.putAll( map );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void clear() {
      _delegate.clear();
   }


   // Views

   /**
    * {@inheritDoc}
    */
   @Override
   public Set<K> keySet() {
      return _delegate.keySet();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Collection<T> values() {
      return _delegate.values();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Set<Map.Entry<K, T>> entrySet() {
      return _delegate.entrySet();
   }


   /**
    * {@inheritDoc}
    */
   @Override
   public Map<K, T> toSimpleMap() {
      return _delegate;
   }


}
