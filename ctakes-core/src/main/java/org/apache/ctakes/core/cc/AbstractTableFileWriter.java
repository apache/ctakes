package org.apache.ctakes.core.cc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

abstract public class AbstractTableFileWriter
      extends AbstractFileWriter<List<List<String>>> {

   static private final Logger LOGGER = LoggerFactory.getLogger( "AbstractTableFileWriter" );


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

   protected void clearDataRows() {
      _dataRows.clear();
   }

   /**
    *
    * @param dataRow a list of ordered cells.
    */
   protected void addDataRow( final List<String> dataRow ) {
      _dataRows.add( dataRow );
   }

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
   protected List<String> getHeaderRow() {
      return _headerRow;
   }

   /**
    *
    * @param headerRow the header list of ordered cells.
    */
   protected void setHeaderRow( final List<String> headerRow ) {
      _headerRow.clear();
      _headerRow.addAll( headerRow );
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
   protected List<String> getFooterRow() {
      return _footerRow;
   }

   /**
    *
    * @param footerRow the footer list of ordered cells.
    */
   protected void setFooterRow( final List<String> footerRow ) {
      _footerRow.clear();
      _footerRow.addAll( footerRow );
   }

   /**
    * @param jCas the jcas passed to the process( jcas ) method.
    */
   @Override
   protected void createData( final JCas jCas ) {
//      _headerRow.clear();
      clearDataRows();
//      _footerRow.clear();
      final List<String> header = createHeaderRow( jCas );
      if ( header != null ) {
         setHeaderRow( header );
      }
      final List<List<String>> rows = createDataRows( jCas );
      if ( rows != null ) {
         rows.forEach( this::addDataRow );
      }
      final List<String> footer = createFooterRow( jCas );
      if ( footer != null ) {
         setFooterRow( footer );
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
      LOGGER.info( "Writing {} Table to {} ...", tableType.name(), file.getPath() );
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
      return """
            table {
              margin-left: auto;
              margin-right: auto;
            }
            """;
   }


   protected String createTableHeader( final TableType tableType, final List<String> headerRow ) {
      return switch ( tableType ) {
         case BSV -> String.join( "|", headerRow ) + "\n";
         case CSV -> String.join( ",", headerRow ) + "\n";
         case TAB -> String.join( "\t", headerRow ) + "\n";
         case HTML -> createHtmlHeader( headerRow );
      };
   }

   protected String createHtmlHeader( final List<String> headerRow ) {
      final StringBuilder sb = new StringBuilder();
      sb.append( """
            <!DOCTYPE html>
            <html>
            <head>
            <style>
            """ );
      sb.append( createHtmlStyle() );
      sb.append( """
            </style>
            </head>
            <body>

            <table>
             <thead>
              <tr>
            """ );
      for ( String cell : headerRow ) {
         sb.append( "    <th>" )
           .append( cell )
           .append( "</th>\n" );
      }
      sb.append( """
              </tr>
             </thead>
            """ );
      return sb.toString();
   }


   protected String createTableRow( final TableType tableType, final List<String> dataRow ) {
      return switch ( tableType ) {
         case BSV -> String.join( "|", dataRow ) + "\n";
         case CSV -> String.join( ",", dataRow ) + "\n";
         case TAB -> String.join( "\t", dataRow ) + "\n";
         case HTML -> createHtmlRow( dataRow );
      };
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
      return switch ( tableType ) {
         case BSV -> String.join( "|", footerRow ) + "\n";
         case CSV -> String.join( ",", footerRow ) + "\n";
         case TAB -> String.join( "\t", footerRow ) + "\n";
         case HTML -> createHtmlFooter( footerRow );
      };
   }

   protected String createHtmlFooter( final List<String> footerRow ) {
      final StringBuilder sb = new StringBuilder();
      sb.append( """
             <tfoot>
              <tf>
            """ );
      for ( String cell : footerRow ) {
         sb.append( "    <td>" )
           .append( cell )
           .append( "</td>\n" );
      }
      sb.append( """
              </tf>
             <tfoot>
            </table>
            </body>
            </html>
            """ );
      return sb.toString();
   }


}
