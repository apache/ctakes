package org.apache.ctakes.core.util.collection;

import javax.annotation.concurrent.Immutable;
import java.util.*;

/**
 * Author: SPF
 * Affiliation: CHIP-NLP
 * Date: 9/5/2014
 */
@Immutable
final public class ImmutableCollectionMap<K, V, T extends Collection<V>> implements CollectionMap<K, V, T> {

   private final CollectionMap<K, V, T> _protectedMap;

   public ImmutableCollectionMap( final CollectionMap<K, V, T> collectionMap ) {
      _protectedMap = collectionMap;
   }


   /**
    * {@inheritDoc}
    */
   @Override
   public Iterator<Map.Entry<K, T>> iterator() {
      return _protectedMap.iterator();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Collection<T> getAllCollections() {
      return Collections.unmodifiableCollection( _protectedMap.values() );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public T getCollection( final K key ) {
      // unfortunately, we cannot use an unmodifiable from Collections
      return _protectedMap.getCollection( key );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public T getOrCreateCollection( final K key ) {
      return getCollection( key );
   }


   /**
    * {@inheritDoc}
    */
   @Override
   public boolean containsValue( final K key, final V value ) {
      return _protectedMap.containsValue( key, value );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public boolean placeValue( final K key, final V value ) {
      throw new UnsupportedOperationException();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public boolean placeMap( final Map<K, V> map ) {
      throw new UnsupportedOperationException();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void removeValue( final K key, final V value ) {
      throw new UnsupportedOperationException();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public <C extends Collection<V>> int addAllValues( final K key, final C collection ) {
      throw new UnsupportedOperationException();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void clearCollection( final K key ) {
      throw new UnsupportedOperationException();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public int size() {
      return _protectedMap.size();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public boolean isEmpty() {
      return _protectedMap.isEmpty();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public boolean containsKey( final Object key ) {
      return _protectedMap.containsKey( key );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public boolean containsValue( final Object value ) {
      return _protectedMap.containsValue( value );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public T get( final Object key ) {
      return _protectedMap.get( key );
   }

   // Modification Operations

   /**
    * {@inheritDoc}
    */
   @Override
   public T put( final K key, final T value ) {
      throw new UnsupportedOperationException();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public T remove( final Object key ) {
      throw new UnsupportedOperationException();
   }


   // Bulk Operations

   /**
    * {@inheritDoc}
    */
   @Override
   public void putAll( final Map<? extends K, ? extends T> map ) {
      throw new UnsupportedOperationException();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void clear() {
      throw new UnsupportedOperationException();
   }


   // Views

   /**
    * {@inheritDoc}
    */
   @Override
   public Set<K> keySet() {
      return _protectedMap.keySet();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Collection<T> values() {
      return _protectedMap.values();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Set<Map.Entry<K, T>> entrySet() {
      return _protectedMap.entrySet();
   }


   /**
    * {@inheritDoc}
    */
   @Override
   public Map<K, T> toSimpleMap() {
      return Collections.unmodifiableMap( _protectedMap );
   }

}
