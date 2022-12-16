package org.apache.ctakes.core.util;


import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

/**
 * Why oh why is there not a simple immutable class representing a pair of values in the jdk ?
 *
 * @author SPF , chip-nlp
 * @version %I%
 * @since 7/22/2016
 */
@Immutable
final public class Pair<T> {

   private final T _value1;
   private final T _value2;

   /**
    * @param value1 not null
    * @param value2 not null
    */
   public Pair( @Nonnull final T value1, @Nonnull final T value2 ) {
      if ( value1 == null || value2 == null ) {
         throw new NullPointerException( "Cannot pass null value to Pair: " + value1 + "," + value2 );
      }
      _value1 = value1;
      _value2 = value2;
   }

   /**
    * @return the first value in the Pair
    */
   public T getValue1() {
      return _value1;
   }

   /**
    * @return the second value in the Pair
    */
   public T getValue2() {
      return _value2;
   }

   @Override
   public String toString() {
      return _value1.toString() + "," + _value2.toString();
   }

   @Override
   public int hashCode() {
      return _value1.hashCode() + 13 * _value2.hashCode();
   }

   /**
    * @param other -
    * @return true iff the other object is a Pair and its values equal this Pair's values
    */
   @Override
   public boolean equals( final Object other ) {
      return other instanceof Pair
             && ((Pair)other)._value1.equals( _value1 )
             && ((Pair)other)._value2.equals( _value2 );
   }

}
