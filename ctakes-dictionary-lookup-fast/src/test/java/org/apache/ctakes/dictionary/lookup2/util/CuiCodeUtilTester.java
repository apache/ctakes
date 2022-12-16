package org.apache.ctakes.dictionary.lookup2.util;


import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 7/9/2015
 */
final public class CuiCodeUtilTester {


   @Test
   public void testGetAsCuiDefault() {
      assertEquals( "Standard Prefix \'C\' not preppended as default",
            "C0000123", CuiCodeUtil.getInstance().getAsCui( 123l ) );
   }

   @Test
   public void testGetAsCuiCustom() {
      final long bing123 = CuiCodeUtil.getInstance().getCuiCode( "BING123" );
      final long binger = bing123 - 123;
      assertEquals( "Custom Prefix \'BING\' not preppended",
            "BING123", CuiCodeUtil.getInstance().getAsCui( bing123 ) );
      assertEquals( "Custom Prefix \'BING\' not preppended",
            "BING004", CuiCodeUtil.getInstance().getAsCui( binger + 4 ) );

      final long bang123 = CuiCodeUtil.getInstance().getCuiCode( "BANG123" );
      final long banger = bang123 - 123;
      assertEquals( "Custom Prefix \'BANG\' not preppended",
            "BANG123", CuiCodeUtil.getInstance().getAsCui( bang123 ) );
      assertEquals( "Custom Prefix \'BANG\' not preppended",
            "BANG004", CuiCodeUtil.getInstance().getAsCui( banger + 4 ) );
   }

   @Test
   public void breakCodeTooLarge() {
      final long bing123 = CuiCodeUtil.getInstance().getCuiCode( "BING123" );
      final long binger = bing123 - 123;
      assertEquals( "Cui length not expanded for large code",
            "BING1004", CuiCodeUtil.getInstance().getAsCui( binger + 1004 ) );
   }


   @Test
   public void breakCodeIsNegative() {
      assertEquals( "Negative code did not return as-is",
            "-1", CuiCodeUtil.getInstance().getAsCui( -1l ) );
   }

   @Test
   public void breakPrefixUnknown() {
      assertEquals( "Huge code did not return as-is",
            "10000000000000", CuiCodeUtil.getInstance().getAsCui( 10000000000000l ) );
   }


}
