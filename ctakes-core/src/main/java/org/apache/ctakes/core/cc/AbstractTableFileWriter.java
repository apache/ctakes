package org.apache.ctakes.core.cc;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

abstract public class AbstractTableFileWriter
      extends AbstractFileWriter<List<List<String>>> {

   static private final Logger LOGGER = LogManager.getLogger( "AbstractTableFileWriter" );


   protected enum TableType {
      BSV,
      CSV,
      HTML,
      TAB
   }

   /**
    * Name of configuration parameter that must be set to the path of a directory into which the
    * output files will be written.
    */
   @ConfigurationParameter(
         name = "TableType",
         description = "Type of Table to write to File. Possible values are: BSV, CSV, HTML, TAB",
         mandatory = false
   )
   private String _tableType;


   private final List<String> _headerRow = new ArrayList<>();
   private final List<List<String>> _dataRows = new ArrayList<>();
   private final List<String> _footerRow = new ArrayList<>();

   /**
    * @param jCas ye olde ...
    * @return A list of ordered rows, each being a list of ordered cells.
    */
   abstract protected List<List<String>> createDataRows( JCas jCas );


   /**
    * A Table Header indicates the type of content in each table cell.  Though not necessary, it is nice to have.
    *
    * @param jCas ye olde ...
    * @return an empty List.  This is the default.  Please override if necessary.
    */
   protected List<String> createHeaderRow( final JCas jCas ) {
      return Collections.emptyList();
   }

   /**
    * @return the header list of ordered cells.
    */
   final protected List<String> getHeaderRow() {
      return _headerRow;
   }

   /**
    * A Table Footer is usually some type of "Summary" line.  For instance a column of numbers may have a "Total".
    *
    * @param jCas ye olde ...
    * @return an empty List.  This is the default.  Please override if necessary.
    */
   protected List<String> createFooterRow( final JCas jCas ) {
      return Collections.emptyList();
   }

   /**
    * @return the footer list of ordered cells.
    */
   final protected List<String> getFooterRow() {
      return _footerRow;
   }


   /**
    * @param jCas the jcas passed to the process( jcas ) method.
    */
   @Override
   protected void createData( final JCas jCas ) {
      _headerRow.clear();
      _dataRows.clear();
      _footerRow.clear();
      final List<String> header = createHeaderRow( jCas );
      if ( header != null ) {
         _headerRow.addAll( header );
      }
      final List<List<String>> rows = createDataRows( jCas );
      if ( rows != null ) {
         _dataRows.addAll( rows );
      }
      final List<String> footer = createFooterRow( jCas );
      if ( footer != null ) {
         _footerRow.addAll( footer );
      }
   }

   /**
    * @return completed patient JCases
    */
   @Override
   protected List<List<String>> getData() {
      return _dataRows;
   }

   /**
    * called after writing is complete
    *
    * @param data -
    */
   @Override
   protected void writeComplete( final List<List<String>> data ) {
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void writeFile( final List<List<String>> dataRows,
                          final String outputDir,
                          final String documentId,
                          final String fileName ) throws IOException {
      final TableType tableType = Arrays.stream( TableType.values() )
                                        .filter( s -> s.name()
                                                       .equalsIgnoreCase( _tableType ) )
                                        .findFirst()
                                        .orElse( TableType.BSV );
      final File file = new File( outputDir, documentId + "_table." + tableType.name() );
      LOGGER.info( "Writing " + tableType.name() + " Table to " + file.getPath() + " ..." );
      final String header = createTableHeader( tableType, getHeaderRow() );

      final String footer = createTableFooter( tableType, getFooterRow() );
      try ( Writer writer = new BufferedWriter( new FileWriter( file ) ) ) {
         writer.write( header );

         for ( List<String> dataRow : dataRows ) {
            final String row = createTableRow( tableType, dataRow );
            writer.write( row );
         }

         writer.write( footer );
      }
   }


   ////////////////////////////////////////////////////////////////////////
   //
   //    The following default implementations should be fine as-is.
   //
   ////////////////////////////////////////////////////////////////////////


   protected String createHtmlStyle() {
      return "table {\n"
             + "  margin-left: auto;\n"
             + "  margin-right: auto;\n"
             + "}\n";
   }


   protected String createTableHeader( final TableType tableType, final List<String> headerRow ) {
      switch ( tableType ) {
         case BSV:
            return String.join( "|", headerRow ) + "\n";
         case CSV:
            return String.join( ",", headerRow ) + "\n";
         case TAB:
            return String.join( "\t", headerRow ) + "\n";
         case HTML:
            return createHtmlHeader( headerRow );
      }
      return String.join( "|", headerRow ) + "\n";
   }

   protected String createHtmlHeader( final List<String> headerRow ) {
      final StringBuilder sb = new StringBuilder();
      sb.append( "<!DOCTYPE html>\n"
                 + "<html>\n"
                 + "<head>\n"
                 + "<style>\n" );
      sb.append( createHtmlStyle() );
      sb.append( "</style>\n"
                 + "</head>\n"
                 + "<body>\n"
                 + "\n"
                 + "<table>\n"
                 + " <thead>\n"
                 + "  <tr>\n" );
      for ( String cell : headerRow ) {
         sb.append( "    <th>" )
           .append( cell )
           .append( "</th>\n" );
      }
      sb.append( "  </tr>\n"
                 + " </thead>\n" );
      return sb.toString();
   }


   protected String createTableRow( final TableType tableType, final List<String> dataRow ) {
      switch ( tableType ) {
         case BSV:
            return String.join( "|", dataRow ) + "\n";
         case CSV:
            return String.join( ",", dataRow ) + "\n";
         case TAB:
            return String.join( "\t", dataRow ) + "\n";
         case HTML:
            return createHtmlRow( dataRow );
      }
      return String.join( "|", dataRow ) + "\n";
   }

   protected String createHtmlRow( final List<String> dataRow ) {
      final StringBuilder sb = new StringBuilder();
      for ( String cell : dataRow ) {
         sb.append( "    <td>" )
           .append( cell )
           .append( "</td>\n" );
      }
      return "  <tr>\n" + sb.toString() + "  </tr>\n";
   }


   protected String createTableFooter( final TableType tableType, final List<String> footerRow ) {
      switch ( tableType ) {
         case BSV:
            return String.join( "|", footerRow ) + "\n";
         case CSV:
            return String.join( ",", footerRow ) + "\n";
         case TAB:
            return String.join( "\t", footerRow ) + "\n";
         case HTML:
            return createHtmlFooter( footerRow );
      }
      return String.join( "|", footerRow ) + "\n";
   }

   protected String createHtmlFooter( final List<String> footerRow ) {
      final StringBuilder sb = new StringBuilder();
      sb.append( " <tfoot>\n"
                 + "  <tf>\n" );
      for ( String cell : footerRow ) {
         sb.append( "    <td>" )
           .append( cell )
           .append( "</td>\n" );
      }
      sb.append( "  </tf>\n"
                 + " <tfoot>\n"
                 + "</table>\n"
                 + "</body>\n"
                 + "</html>\n" );
      return sb.toString();
   }


}
