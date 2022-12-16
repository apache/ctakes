package org.apache.ctakes.core.cc.pretty.cell;


import org.apache.ctakes.core.cc.pretty.SemanticGroup;
import org.apache.ctakes.core.util.textspan.DefaultTextSpan;
import org.apache.ctakes.core.util.textspan.TextSpan;
import org.junit.Test;

import java.util.*;

import static org.apache.ctakes.core.cc.pretty.cell.UmlsItemCell.ENTITY_FILL;
import static org.junit.Assert.*;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 7/6/2015
 */
final public class DefaultUmlsItemCellTester {

   static private final Collection<String> FIBOS = Arrays
         .asList( "C001", "C002", "C003", "C005", "C008", "C013", "C021" );
   static private final Collection<String> BOTTLES = Arrays.asList( "C099", "C098", "C097" );
   static private final Collection<String> SINGLE = Collections.singletonList( "C100" );

   @Test
   public void testWidth() {
      final TextSpan textSpan = new DefaultTextSpan( 5, 10 );
      final Map<String, Collection<String>> semanticCuis = new HashMap<>( 2 );
      semanticCuis.put( "Bottle", BOTTLES );
      final ItemCell itemCell1 = new DefaultUmlsItemCell( textSpan, 1, semanticCuis );
      assertEquals( "item cell for 5-10 with semantic \"Bottle\" should have width 8", 8, itemCell1.getWidth() );

      semanticCuis.put( "Fibonacci", FIBOS );
      final ItemCell itemCell2 = new DefaultUmlsItemCell( textSpan, 1, semanticCuis );
      assertEquals( "item cell for 5-10 with semantic \"Fibonacci\" should have width 9", 9, itemCell2.getWidth() );

      final ItemCell itemCell3 = new DefaultUmlsItemCell( new DefaultTextSpan( 5, 25 ), 1, semanticCuis );
      assertEquals( "item cell for 5-25 with semantic \"Fibonacci\" should have width 20", 20, itemCell3.getWidth() );
   }

   @Test
   public void testHeight() {
      final TextSpan textSpan = new DefaultTextSpan( 5, 10 );
      final Map<String, Collection<String>> semanticCuis = new HashMap<>( 2 );
      semanticCuis.put( "Bottle", BOTTLES );
      final ItemCell itemCell1 = new DefaultUmlsItemCell( textSpan, 1, semanticCuis );
      final int height1 = 1 + 1 + BOTTLES.size();
      assertEquals( "item cell for 5-10 with semantic \"Bottle\" should have height " + height1,
            height1, itemCell1.getHeight() );

      semanticCuis.put( "Fibonacci", FIBOS );
      final ItemCell itemCell2 = new DefaultUmlsItemCell( textSpan, 1, semanticCuis );
      final int height2 = height1 + 1 + FIBOS.size();
      assertEquals( "item cell for 5-10 with semantic \"Fibonacci\" should have height " + height2,
            height2, itemCell2.getHeight() );

      final ItemCell itemCell3 = new DefaultUmlsItemCell( textSpan, -1, semanticCuis );
      final int height3 = height2 + 1;
      assertEquals( "item cell for 5-10 with negated semantic \"Fibonacci\" should have height " + height3,
            height3, itemCell3.getHeight() );
   }

   @Test
   public void testNegated() {
      final TextSpan textSpan = new DefaultTextSpan( 5, 10 );
      final Map<String, Collection<String>> semanticCuis = new HashMap<>( 1 );
      semanticCuis.put( "Bottle", BOTTLES );
      final UmlsItemCell itemCell1 = new DefaultUmlsItemCell( textSpan, 1, semanticCuis );
      assertFalse( "\"item cell for 5-10 with semantic \"Bottle\" should not be negated", itemCell1.isNegated() );

      final UmlsItemCell itemCell2 = new DefaultUmlsItemCell( textSpan, -1, semanticCuis );
      assertTrue( "\"item cell for 5-10 with semantic \"Bottle\" should be negated", itemCell2.isNegated() );
   }


   @Test
   public void testCustomLineText() {
      final TextSpan textSpan = new DefaultTextSpan( 5, 10 );
      final Map<String, Collection<String>> semanticCuis = new HashMap<>( 3 );
      semanticCuis.put( "Bottle", BOTTLES );
      final ItemCell itemCell1 = new DefaultUmlsItemCell( textSpan, 1, semanticCuis );
      assertEquals(
            "item cell for 5-10 with semantic \"Bottle\" should have line 0 " + ENTITY_FILL, ENTITY_FILL, itemCell1
            .getLineText( 0 ) );
      assertEquals( "item cell for 5-10 with semantic \"Bottle\" should have line 1 \"Bottle\"", "Bottle", itemCell1
            .getLineText( 1 ) );
      assertEquals( "item cell for 5-10 with semantic \"Bottle\" should have line 2 \"C097\"", "C097", itemCell1
            .getLineText( 2 ) );
      assertEquals( "item cell for 5-10 with semantic \"Bottle\" should have line 3 \"C098\"", "C098", itemCell1
            .getLineText( 3 ) );
      assertEquals( "item cell for 5-10 with semantic \"Bottle\" should have line 4 \"C099\"", "C099", itemCell1
            .getLineText( 4 ) );
      assertEquals( "item cell for 5-10 with semantic \"Bottle\" should have line 5 \"\"", "", itemCell1
            .getLineText( 5 ) );

      final ItemCell itemCell2 = new DefaultUmlsItemCell( textSpan, -1, semanticCuis );
      assertEquals( "item cell for 5-10 with negated semantic \"Bottle\" should have line 5 \"Negated\"", "Negated", itemCell2
            .getLineText( 5 ) );

      semanticCuis.put( "Singleton", SINGLE );
      semanticCuis.put( "Fibonacci", FIBOS );
      final ItemCell itemCell3 = new DefaultUmlsItemCell( textSpan, 1, semanticCuis );
      assertEquals( "item cell for 5-10 with negated semantic \"Fibonacci\" should have line 5 \"Fibonacci\"", "Fibonacci", itemCell3
            .getLineText( 5 ) );
      assertEquals( "item cell for 5-10 with negated semantic \"Fibonacci\" should have line 13 \"Singleton\"", "Singleton", itemCell3
            .getLineText( 13 ) );
   }

   @Test
   public void testStandardLineText() {
      final TextSpan textSpan = new DefaultTextSpan( 5, 10 );
      final Map<String, Collection<String>> semanticCuis = new HashMap<>( 3 );
      semanticCuis.put( SemanticGroup.MEDICATION.getName(), BOTTLES );
      semanticCuis.put( SemanticGroup.ANATOMICAL_SITE.getName(), SINGLE );
      semanticCuis.put( SemanticGroup.FINDING.getName(), FIBOS );
      final ItemCell itemCell1 = new DefaultUmlsItemCell( textSpan, 1, semanticCuis );
      assertEquals( "item cell for 5-10 with ctakes semantics should have line 1 "
                    + SemanticGroup.ANATOMICAL_SITE.getName(),
            SemanticGroup.ANATOMICAL_SITE.getName(), itemCell1.getLineText( 1 ) );
      assertEquals( "item cell for 5-10 with ctakes semantics should have line 3 "
                    + SemanticGroup.FINDING.getName(),
            SemanticGroup.FINDING.getName(), itemCell1.getLineText( 3 ) );
      assertEquals( "item cell for 5-10 with ctakes semantics should have line 11 "
                    + SemanticGroup.MEDICATION.getName(),
            SemanticGroup.MEDICATION.getName(), itemCell1.getLineText( 11 ) );
   }

}
