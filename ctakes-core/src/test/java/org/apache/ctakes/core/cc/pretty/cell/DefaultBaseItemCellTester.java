package org.apache.ctakes.core.cc.pretty.cell;


import org.apache.ctakes.core.util.textspan.DefaultTextSpan;
import org.apache.ctakes.core.util.textspan.TextSpan;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 7/6/2015
 */
final public class DefaultBaseItemCellTester {

   @Test
   public void testWidth() {
      final TextSpan textSpan = new DefaultTextSpan( 5, 10 );
      final ItemCell itemCell1 = new DefaultBaseItemCell( textSpan, "testy", "TEST" );
      assertEquals( "item cell for \"testy\" with pos TEST should have width 5", 5, itemCell1.getWidth() );
      final ItemCell itemCell2 = new DefaultBaseItemCell( textSpan, "testy", "TESTIER" );
      assertEquals( "item cell for \"testy\" with pos TESTIER should have width 7", 7, itemCell2.getWidth() );
   }

   @Test
   public void testHeight() {
      final TextSpan textSpan = new DefaultTextSpan( 5, 10 );
      final ItemCell itemCell1 = new DefaultBaseItemCell( textSpan, "testy", "TEST" );
      assertEquals( "item cell for \"testy\" with pos TEST should have height 2", 2, itemCell1.getHeight() );
   }

   @Test
   public void testLineText() {
      final TextSpan textSpan = new DefaultTextSpan( 5, 10 );
      final ItemCell itemCell1 = new DefaultBaseItemCell( textSpan, "testy", "TEST" );
      assertEquals( "item cell for \"testy\" with pos TEST should have line 0 \"testy\"", "testy", itemCell1
            .getLineText( 0 ) );
      assertEquals( "item cell for \"testy\" with pos TEST should have line 1 \"TEST\"", "TEST", itemCell1
            .getLineText( 1 ) );
      assertEquals( "item cell for \"testy\" with pos TEST should have line 2 \"\"", "", itemCell1.getLineText( 2 ) );
   }


}
