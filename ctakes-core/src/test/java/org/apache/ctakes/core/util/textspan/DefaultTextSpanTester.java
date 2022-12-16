package org.apache.ctakes.core.util.textspan;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 7/6/2015
 */
final public class DefaultTextSpanTester {

   @Test
   public void testEndOverlap() {
      final TextSpan textSpan1 = new DefaultTextSpan( 5, 15 );
      final TextSpan textSpan2 = new DefaultTextSpan( 10, 20 );
      assertTrue( "Text Span 5-15 should overlap text span 10-20", textSpan1.overlaps( textSpan2 ) );
      assertTrue( "Text Span 10-20 should overlap text span 5-15", textSpan2.overlaps( textSpan1 ) );
   }

   @Test
   public void testInnerOverlap() {
      final TextSpan textSpan1 = new DefaultTextSpan( 5, 25 );
      final TextSpan textSpan2 = new DefaultTextSpan( 10, 20 );
      assertTrue( "Text Span 5-25 should overlap text span 10-20", textSpan1.overlaps( textSpan2 ) );
      assertTrue( "Text Span 10-20 should overlap text span 5-25", textSpan2.overlaps( textSpan1 ) );
   }

   @Test
   public void testNoOverlap() {
      final TextSpan textSpan1 = new DefaultTextSpan( 5, 10 );
      final TextSpan textSpan2 = new DefaultTextSpan( 10, 20 );
      assertFalse( "Text Span 5-10 should not overlap text span 10-20", textSpan1.overlaps( textSpan2 ) );
      assertFalse( "Text Span 10-20 should not overlap text span 5-10", textSpan2.overlaps( textSpan1 ) );
      final TextSpan thirdTextSpan = new DefaultTextSpan( 100, 200 );
      assertFalse( "Text Span 5-10 should not overlap text span 100-200", textSpan1.overlaps( thirdTextSpan ) );
      assertFalse( "Text Span 100-200 should not overlap text span 5-10", thirdTextSpan.overlaps( textSpan1 ) );
   }

   @Test
   public void testWidth() {
      final TextSpan textSpan1 = new DefaultTextSpan( 5, 10 );
      assertEquals( "Text Span 5-10 should have width 5", 5, textSpan1.getWidth() );
      final TextSpan textSpan2 = new DefaultTextSpan( 10, 20 );
      assertEquals( "Text Span 10-20 should have width 10", 10, textSpan2.getWidth() );
   }

}
