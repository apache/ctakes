package org.apache.ctakes.core.cc.html;


import org.apache.log4j.Logger;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import static org.apache.ctakes.core.cc.html.HtmlTextWriter.*;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 10/15/2016
 */
final class JsWriter {

   static private final Logger LOGGER = Logger.getLogger( "JsWriter" );


   private JsWriter() {
   }

   /**
    * @param filePath path to css file
    */
   static void writeJsFile( final String filePath ) {
      final File outputFile = new File( filePath );
      outputFile.getParentFile()
            .mkdirs();
      try ( final BufferedWriter writer = new BufferedWriter( new FileWriter( outputFile ) ) ) {
         writer.write( getSwapInfoScript() );
      } catch ( IOException ioE ) {
         LOGGER.error( "Could not not write js file " + outputFile.getPath() );
         LOGGER.error( ioE.getMessage() );
      }
   }

   /**
    * A javascript function is used to expand annotation tooltips into formatted html
    *
    * @return javascript
    */
   static private String getSwapInfoScript() {
      String functionText = "  function iaf(txt) {\n" +
            "    var aff=txt.replace( /" + AFFIRMED + "/g,\"<br><h3>Affirmed</h3>\" );\n" +
            "    var neg=aff.replace( /" + NEGATED + "/g,\"<br><h3>Negated</h3>\" );\n" +
            "    var unc=neg.replace( /" + UNCERTAIN + "/g,\"<br><h3>Uncertain</h3>\" );\n" +
            "    var unn=unc.replace( /" + UNCERTAIN_NEGATED + "/g,\"<br><h3>Uncertain, Negated</h3>\" );\n" +
            "    var gnr=unn.replace( /" + GENERIC + "/g,\"\" );\n" +

            "    var wik1=gnr.replace( /" + WIKI_BEGIN
            + "/g,\"<a href=\\\"https://vsearch.nlm.nih.gov/vivisimo/cgi-bin/query-meta?v%3Aproject=medlineplus&v%3Asources=medlineplus-bundle&query=\" );\n" +
            "    var wik2=wik1.replace( /" + WIKI_CENTER + "/g,\"\\\" target=\\\"_blank\\\">\" );\n" +
            "    var wik3=wik2.replace( /" + WIKI_END + "/g,\"</a>\" );\n";

      String previousCode = "wik3";
      for ( SemanticMarkup markup : SemanticMarkup.values() ) {
         functionText += createSemanticReplacements( markup, previousCode );
         previousCode = markup.getEncoding()
               .toLowerCase();
      }
      functionText +=
            "    var spc=_unk_.replace( /" + SPACER + "/g,\"&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;\" );\n" +
                  "    var prf1=spc.replace( /\\[/g,\"<i>\" );\n" +
                  "    var prf2=prf1.replace( /\\]/g,\"</i>\" );\n" +
                  "    var nl=prf2.replace( /" + NEWLINE + "/g,\"<br>\" );\n" +
                  "    document.getElementById(\"ia\").innerHTML = nl;\n" +
                  "  }\n";
      return functionText;
   }

   static private String createSemanticReplacements( final SemanticMarkup markup, final String previousCode ) {
      return "    var " + markup.getEncoding()
            .toLowerCase()
            + "=" + previousCode
            + ".replace( /" + markup.getEncoding() + "/g,\"<b>" + markup.getGroup()
            .getName() + "</b>\" );\n";
   }

   //       Available decent search engines:
   // https://en.wikipedia.org/wiki/ct_scan
   // http://www.merckmanuals.com/home/SearchResults?query=ct+scan
   // https://www.omim.org/search/?search=ct+scan
   // https://medical-dictionary.thefreedictionary.com/ct+scan
   // https://vsearch.nlm.nih.gov/vivisimo/cgi-bin/query-meta?v%3Aproject=medlineplus&v%3Asources=medlineplus-bundle&query=ct+scan
   // https://www.medicinenet.com/script/main/srchcont.asp?src=ct+scan
   // https://www.webmd.com/search/search_results/default.aspx?query=ct%20scan
   // https://vsearch.nlm.nih.gov/vivisimo/cgi-bin/query-meta?query=ct+scan&v%3Aproject=nlm-main-website
   //       Make a list? https://www.w3schools.com/howto/howto_css_dropdown.asp


}
