package org.apache.ctakes.core.util.regex;

/**
 * A representation of text that can check its container thread for interruptions.
 * This allows a break within tight charAt(..) calling loops, which can otherwise become infinite in a corrupt find.
 */
final class ThreadString implements CharSequence {
   private final CharSequence _delegate;

   ThreadString( final CharSequence delegate ) {
      _delegate = delegate;
   }

   @Override
   public char charAt( final int index ) {
      if ( Thread.currentThread().isInterrupted() ) {
         throw new RuntimeException( new InterruptedException() );
      }
      return _delegate.charAt( index );
   }

   @Override
   public int length() {
      return _delegate.length();
   }

   @Override
   public CharSequence subSequence( final int start, final int end ) {
      if ( Thread.currentThread().isInterrupted() ) {
         throw new RuntimeException( new InterruptedException() );
      }
      return new ThreadString( _delegate.subSequence( start, end ) );
   }

   @Override
   public String toString() {
      return _delegate.toString();
   }
}
