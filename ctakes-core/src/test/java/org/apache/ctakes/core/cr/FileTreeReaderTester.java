package org.apache.ctakes.core.cr;

import org.apache.log4j.Logger;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import static org.junit.Assert.*;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 2/10/2016
 */
public class FileTreeReaderTester {

   static private final Logger LOGGER = Logger.getLogger( "FileTreeReaderTester" );

   static private final String DOCUMENT_ID = "someFile";
   static private final String PREFIX_SHORT = "subdir";
   static private final String PREFIX_LONG = "parent/child";

   static private final File TXT_EXTENDED = new File( "/home/subdir/someFile.txt" );
   static private final File BSV_EXTENDED = new File( "/home/subdir/someFile.bsv" );
   static private final File JPG_EXTENDED = new File( "/home/parent/child/someFile.jpg" );
   static private final File NOT_EXTENDED = new File( "/home/parent/child/someFile" );
   static private final File TXTXMI_EXTENDED = new File( "/home/parent/child/someFile.txt.xmi" );

   static private final Collection<String> TXT_BSV_EXTENSIONS = Arrays.asList( ".txt", ".bsv" );
   static private final Collection<String> TXT_XMI_EXTENSIONS = Arrays.asList( ".txt", ".xmi" );
   static private final Collection<String> TXT_TXTXMI_EXTENSIONS = Arrays.asList( ".txt", ".txt.xmi" );


   //
   //    Extension collection creation
   //

   @Test
   public void testCreateEmptyExtensions() {
      assertEquals( "Empty wanted extension array should create empty extension collection",
            FileTreeReader.createValidExtensions( new String[ 0 ] ).size(), 0 );
      assertEquals( "Star * wanted extension array should create empty extension collection",
            FileTreeReader.createValidExtensions( "*" ).size(), 0 );
      assertEquals( "dot Star .* wanted extension array should create empty extension collection",
            FileTreeReader.createValidExtensions( ".*" ).size(), 0 );
   }

   @Test
   public void testCreateSimpleExtensions() {
      assertTrue( "txt array should produce collection containing .txt",
            FileTreeReader.createValidExtensions( "txt" ).contains( ".txt" ) );
      assertTrue( ".txt .bsv array should produce collection containing .txt",
            FileTreeReader.createValidExtensions( ".txt" ).contains( ".txt" ) );
   }

   @Test
   public void testCreateComplexExtensions() {
      assertTrue( "txt.xmi array should produce collection containing .txt.xmi",
            FileTreeReader.createValidExtensions( "txt.xmi" ).contains( ".txt.xmi" ) );
      assertTrue( ".txt.xmi array should produce collection containing .txt.xmi",
            FileTreeReader.createValidExtensions( ".txt.xmi" ).contains( ".txt.xmi" ) );
   }

   @Test
   public void testCreateMultiExtensions() {
      assertTrue( ".txt .bsv array should produce collection containing .txt",
            FileTreeReader.createValidExtensions( ".txt", ".bsv" ).contains( ".txt" ) );
      assertTrue( ".txt .bsv array should produce collection containing .bsv",
            FileTreeReader.createValidExtensions( ".txt", ".bsv" ).contains( ".bsv" ) );
   }

   //
   //    Extension validity
   //

   @Test
   public void testNoExtension() {
      assertTrue( "no-extension Files should be valid when extension list is empty",
            FileTreeReader.isExtensionValid( NOT_EXTENDED, Collections.emptyList() ) );
      assertFalse( "no-extension Files should be invalid when extension list is not empty",
            FileTreeReader.isExtensionValid( NOT_EXTENDED, TXT_BSV_EXTENSIONS ) );
   }

   @Test
   public void testRightExtension() {
      assertTrue( ".txt extension Files should be valid when extension list is empty",
            FileTreeReader.isExtensionValid( TXT_EXTENDED, Collections.emptyList() ) );
      assertTrue( ".txt extension Files should be valid when extension list contains .txt",
            FileTreeReader.isExtensionValid( TXT_EXTENDED, TXT_BSV_EXTENSIONS ) );
      assertTrue( ".bsv extension Files should be valid when extension list contains .bsv",
            FileTreeReader.isExtensionValid( BSV_EXTENDED, TXT_BSV_EXTENSIONS ) );
   }

   @Test
   public void testWrongExtension() {
      assertFalse( ".jpg extension Files should be invalid when extension list does not contain .jpg",
            FileTreeReader.isExtensionValid( JPG_EXTENDED, TXT_BSV_EXTENSIONS ) );
   }

   //
   //    Document Id
   //

   @Test
   public void testCreateDocId() {
      checkDocumentId( NOT_EXTENDED, Collections.emptyList() );
      checkDocumentId( TXT_EXTENDED, Collections.emptyList() );
      checkDocumentId( JPG_EXTENDED, Collections.emptyList() );

      checkDocumentId( NOT_EXTENDED, TXT_BSV_EXTENSIONS );
      checkDocumentId( TXT_EXTENDED, TXT_BSV_EXTENSIONS );
      checkDocumentId( JPG_EXTENDED, TXT_BSV_EXTENSIONS );

      checkDocumentId( TXTXMI_EXTENDED, TXT_TXTXMI_EXTENSIONS );

      assertEquals( "Document ID for " + TXTXMI_EXTENDED.getPath() + " should be " + DOCUMENT_ID + ".txt with " +
                    TXT_XMI_EXTENSIONS,
            DOCUMENT_ID + ".txt", FileTreeReader.createDocumentID( TXTXMI_EXTENDED, TXT_XMI_EXTENSIONS ) );
   }

   static private void checkDocumentId( final File file, final Collection<String> extensions ) {
      assertEquals( "Document ID for " + file.getPath() + " should be " + DOCUMENT_ID + " with " + extensions,
            DOCUMENT_ID, FileTreeReader.createDocumentID( file, extensions ) );
   }

   // TODO createDocumentIdPrefix(..)

}
