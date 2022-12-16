package org.apache.ctakes.core.util.collection;

import java.util.*;

/**
 * Author: SPF
 * Affiliation: CHIP-NLP
 * Date: 7/23/14
 */
final public class ArrayListMap<K, V> implements CollectionMap<K, V, List<V>> {

   private final CollectionMap<K, V, List<V>> _delegate;


   public ArrayListMap() {
      final Map<K, List<V>> hashMap = new HashMap<>();
      final CollectionCreator<V, List<V>> creator = CollectionCreatorFactory.createListCreator();
      _delegate = new DefaultCollectionMap<>( hashMap, creator );
   }

   /**
    * @param size initial size of the HashSetMap
    */
   public ArrayListMap( final int size ) {
      final Map<K, List<V>> hashMap = new HashMap<>( size );
      final CollectionCreator<V, List<V>> creator = CollectionCreatorFactory.createListCreator();
      _delegate = new DefaultCollectionMap<>( hashMap, creator );
   }


   /**
    * {@inheritDoc}
    */
   @Override
   public Iterator<Map.Entry<K, List<V>>> iterator() {
      return _delegate.iterator();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Collection<List<V>> getAllCollections() {
      return new HashSet<>( _delegate.values() );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public List<V> getCollection( final K key ) {
      return _delegate.getCollection( key );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public List<V> getOrCreateCollection( final K key ) {
      return _delegate.getOrCreateCollection( key );
   }


   /**
    * {@inheritDoc}
    */
   @Override
   public boolean containsValue( final K key, final V value ) {
      return _delegate.containsValue( key, value );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public boolean placeValue( final K key, final V value ) {
      return _delegate.placeValue( key, value );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public boolean placeMap( final Map<K, V> map ) {
      return _delegate.placeMap( map );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void removeValue( final K key, final V value ) {
      _delegate.removeValue( key, value );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public <C extends Collection<V>> int addAllValues( final K key, final C collection ) {
      return _delegate.addAllValues( key, collection );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void clearCollection( final K key ) {
      _delegate.clearCollection( key );
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
   public List<V> get( final Object key ) {
      return _delegate.get( key );
   }

   // Modification Operations

   /**
    * {@inheritDoc}
    */
   @Override
   public List<V> put( final K key, final List<V> value ) {
      return _delegate.put( key, value );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public List<V> remove( final Object key ) {
      return _delegate.remove( key );
   }


   // Bulk Operations

   /**
    * {@inheritDoc}
    */
   @Override
   public void putAll( final Map<? extends K, ? extends List<V>> map ) {
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
   public Collection<List<V>> values() {
      return _delegate.values();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Set<Map.Entry<K, List<V>>> entrySet() {
      return _delegate.entrySet();
   }


   /**
    * {@inheritDoc}
    */
   @Override
   public Map<K, List<V>> toSimpleMap() {
      return _delegate;
   }

}
