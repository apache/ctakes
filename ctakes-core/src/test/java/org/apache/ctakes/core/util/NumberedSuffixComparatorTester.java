package org.apache.ctakes.core.util;

import org.junit.Test;

import java.util.Comparator;

import static org.junit.Assert.assertTrue;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 8/25/2017
 */
public class NumberedSuffixComparatorTester {

   // Standard 1
   static private final String ABC_1 = "abc_1";

   // 1 Greater Than
   static private final String ABC_0 = "abc_0";
   static private final String ABC_00 = "abc_00";

   // 1 Less Than
   static private final String ABC_01 = "abc_01";
   static private final String ABC_2 = "abc_2";
   static private final String ABC_10 = "abc_10";
   static private final String ABC_1A = "abc_1_a";
   static private final String ABC_01A = "abc_01_a";
   static private final String BBC_1 = "bbc_1";
   static private final String ACC_1 = "acc_1";
   static private final String ABD_1 = "abd_1";

   // Standard 2
   static private final String _1_ABC = "1_abc";

   // 2 Less Than
   static private final String _01_ABC = "01_abc";
   static private final String _2_ABC = "2_abc";
   static private final String _10_ABC = "10_abc";
   static private final String _1A_ABC = "1a_abc";
   static private final String _1_BBC = "1_bbc";


   @Test
   public void testCompare() {
      final Comparator<String> comparator = new NumberedSuffixComparator();
      assertTrue( comparator.compare( ABC_1, ABC_1 ) == 0 );

      assertTrue( comparator.compare( ABC_1, ABC_01 ) < 0 );
      assertTrue( comparator.compare( ABC_1, ABC_2 ) < 0 );
      assertTrue( comparator.compare( ABC_1, ABC_10 ) < 0 );
      assertTrue( comparator.compare( ABC_1, ABC_1A ) < 0 );
      assertTrue( comparator.compare( ABC_1, ABC_01A ) < 0 );
      assertTrue( comparator.compare( ABC_1, BBC_1 ) < 0 );
      assertTrue( comparator.compare( ABC_1, ACC_1 ) < 0 );
      assertTrue( comparator.compare( ABC_1, ABD_1 ) < 0 );

      assertTrue( comparator.compare( _1_ABC, _1_ABC ) == 0 );
      assertTrue( comparator.compare( _1_ABC, _01_ABC ) < 0 );
      assertTrue( comparator.compare( _1_ABC, _2_ABC ) < 0 );
      assertTrue( comparator.compare( _1_ABC, _10_ABC ) < 0 );
      assertTrue( comparator.compare( _1_ABC, _1A_ABC ) < 0 );
      assertTrue( comparator.compare( _1_ABC, _1_BBC ) < 0 );
   }

}
