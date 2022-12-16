package org.apache.ctakes.core.util.collection;

import java.util.*;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 9/23/2014
 */
final public class EnumSetMap<K extends Enum<K>, V> implements CollectionMap<K, V, Set<V>> {

   private final CollectionMap<K, V, Set<V>> _delegate;


   public EnumSetMap( final Class<K> enumType ) {
      final EnumMap<K, Set<V>> enumMap = new EnumMap<>( enumType );
      final CollectionCreator<V, Set<V>> creator = CollectionCreatorFactory.createSetCreator();
      _delegate = new DefaultCollectionMap<>( enumMap, creator );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Iterator<Map.Entry<K, Set<V>>> iterator() {
      return _delegate.iterator();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Collection<Set<V>> getAllCollections() {
      return new HashSet<>( _delegate.values() );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Set<V> getCollection( final K key ) {
      return _delegate.getCollection( key );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Set<V> getOrCreateCollection( final K key ) {
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
   public Set<V> get( final Object key ) {
      return _delegate.get( key );
   }

   // Modification Operations

   /**
    * {@inheritDoc}
    */
   @Override
   public Set<V> put( final K key, final Set<V> value ) {
      return _delegate.put( key, value );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Set<V> remove( final Object key ) {
      return _delegate.remove( key );
   }


   // Bulk Operations

   /**
    * {@inheritDoc}
    */
   @Override
   public void putAll( final Map<? extends K, ? extends Set<V>> map ) {
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
   public Collection<Set<V>> values() {
      return _delegate.values();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Set<Map.Entry<K, Set<V>>> entrySet() {
      return _delegate.entrySet();
   }


   /**
    * {@inheritDoc}
    */
   @Override
   public Map<K, Set<V>> toSimpleMap() {
      return _delegate;
   }

}
