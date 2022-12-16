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
final class CssWriter {

   static private final Logger LOGGER = Logger.getLogger( "CssWriter" );


   private CssWriter() {
   }

   /**
    * @param filePath path to css file
    */
   static void writeCssFile( final String filePath ) {
      final File outputFile = new File( filePath );
      outputFile.getParentFile()
            .mkdirs();
      try ( final BufferedWriter writer = new BufferedWriter( new FileWriter( outputFile ) ) ) {
         writer.write( setLayout() );
         writer.write( setUnderline( GENERIC, "gray", "solid", "0.12" ) );
         writer.write( setUnderline( AFFIRMED, "green", "solid", "0.12" ) );
         writer.write( setUnderline( UNCERTAIN, "gold", "dotted", "0.16" ) );
         writer.write( setUnderline( NEGATED, "red", "dashed", "0.14" ) );
         writer.write( setUnderline( UNCERTAIN_NEGATED, "orange", "dashed", "0.14" ) );

//         writer.write( setSuperColor( SemanticGroup.FINDING.getCode(), "magenta" ) );
//         writer.write( setSuperColor( SemanticGroup.DISORDER.getCode(), "black" ) );
//         writer.write( setSuperColor( SemanticGroup.MEDICATION.getCode(), "red" ) );
//         writer.write( setSuperColor( SemanticGroup.PROCEDURE.getCode(), "blue" ) );
//         writer.write( setSuperColor( SemanticGroup.ANATOMICAL_SITE.getCode(), "gray" ) );
//         writer.write( setSuperColor( SemanticGroup.UNKNOWN_SEMANTIC_CODE, "gray" ) );
         for ( SemanticMarkup markup : SemanticMarkup.values() ) {
            writer.write( setSuperColor( markup ) );
         }

//         writer.write( getListCss() );
         writer.write( getToolTipCss() );
      } catch ( IOException ioE ) {
         LOGGER.error( "Could not not write css file " + outputFile.getPath() );
         LOGGER.error( ioE.getMessage() );
      }
   }

   static private String setLayout() {
      return ".flex-container {\n" +
            "    display: -webkit-flex;\n" +
            "    display: flex;  \n" +
            "    -webkit-flex-flow: row wrap;\n" +
            "    flex-flow: row wrap;\n" +
            "    text-align: center;\n" +
            "}\n" +
            "\n" +
            ".flex-container > * {\n" +
            "    padding: 15px;\n" +
            "    -webkit-flex: 1 100%;\n" +
            "    flex: 1 100%;\n" +
            "}\n" +
            "\n" +
            ".article {\n" +
            "    text-align: left;\n" +
            "    line-height: 120%;\n" +
            "    word-spacing: 0.25em;\n" +
            "}\n" +
            "header {\n" +
            "    background: MidnightBlue;\n" +
            "    color: white;\n" +
            "    height: 30px;\n" +
            "}\n" +
            "header h1 {\n" +
            "    margin-top: 0px;\n" +
            "}\n" +
            "footer {\n" +
            "    background: SteelBlue;\n" +
            "    color: white;\n" +
            "    height: 10px;\n" +
            "}\n" +
            ".nav {\n" +
            "    display: flex;\n" +
            "    flex-direction: column;\n" +
            "    flex-shrink: 0;\n" +
            "    justify-content: space-between;\n" +
            "    max-width: 320px;\n" +
            "    background: PowderBlue;\n" +
            "}\n" +
            "@media all {\n" +
            "    .article {\n" +
            "        -webkit-flex: 5 0px;\n" +
            "        flex: 5 0px;\n" +
            "        -webkit-order: 1;\n" +
            "        order: 1;" +
            "    }\n" +
            "    .nav {\n" +
            "        text-align: left;\n" +
            "        -webkit-flex: 1 auto;\n" +
            "        flex: 1 auto;\n" +
            "        -webkit-order: 2;\n" +
            "        order: 2;\n" +
            "    }\n" +
            "    footer {\n" +
            "        -webkit-order: 3;\n" +
            "        order: 3;\n" +
            "    }\n" +
            "}\n\n" +
            "#ia {\n" +
            "    position: sticky;\n" +
            "    position: -webkit-sticky;\n" +
            "    top: 0;\n" +
            "    background: powderBlue;\n" +
            "    border-bottom: 2px solid navy;\n" +
            "    z-index: 10;\n" +
            "}\n" +
            ".legend {\n" +
            "    margin-left: auto;\n" +
            "    margin-right: auto;\n" +
            "    max-width: 300px;\n" +
            "    background: white;\n" +
            "    border: 2px solid navy;\n" +
            "    padding: 0 15px 15px 15px;\n" +
            "    z-index: 1;\n\n" +
            "}\n" +
            ".legend h3 {\n" +
            "    text-align: center;\n" +
            "}\n" +
            ".legend table {\n" +
            "    width: 100%;\n" +
            "}\n" +
            ".legend td {\n" +
            "    width: 50%;\n" +
            "}\n";
   }

   // dashType is solid or dashed or double or dotted     size is relative: 0.1 or 0.2 for 10%, 20%
   // See https://css-tricks.com/styling-underlines-web/ shadow for another possibility
   static private String setUnderline( final String className, final String color, final String dashType,
                                       final String size ) {
      return "\n." + className + " {\n" +
            "  position: relative;\n" +
            "  display: inline-block " + color + ";\n" +
            "  border-bottom: " + size + "em " + dashType + " " + color + ";\n" +
            "  border-radius: 5px;\n" +
            "}\n";
   }

   static private String setSuperColor( final SemanticMarkup markup ) {
      return "\n." + markup.getEncoding() + " {\n" +
            "  color: " + markup.getColor() + ";\n" +
            "}\n" +
            "." + markup.getEncoding() + "::after {\n" +
            "  content: \"\\" + markup.getAsterisk() + "\";\n" +
            "  vertical-align: super;" +
            "  font-size: smaller;" +
            "}\n";
   }

   static private String setSuperColor( final String className, final String color ) {
      return "\n." + className + " {\n" +
            "  color: " + color + ";\n" +
            "}\n";
   }

   static private String setHighlight( final String idName, final String color ) {
      // PowderBlue
      return "#" + idName + "{\n  background-color: " + color + ";\n}\n";
   }

   static private String getListCss() {
      return "\nul {\n" +
            "  list-style-type: none;\n" +
            "  margin: 0;\n" +
            "  padding: 0;\n" +
            "}\n" +
            "\nli {\n" +
            "  border: 1px solid lightgray;\n" +
            "  margin: 1px;\n" +
            "  margin-right: 5px;\n" +
            "  padding: 2px;\n" +
            "  padding-left: 5px;\n" +
            "}\n";
   }

   static private String getToolTipCss() {
      return
            // position z
            "\n[" + TOOL_TIP + "] {\n" +
                  "  position: relative;\n" +
                  "  z-index: 2;\n" +
                  "  cursor: pointer;\n" +
                  "}\n" +
                  // invisible
                  "[" + TOOL_TIP + "]::before,\n" +
                  "[" + TOOL_TIP + "]::after {\n" +
                  "  visibility: hidden;\n" +
                  "  -ms-filter: \"progid:DXImageTransform.Microsoft.Alpha(Opacity=0)\";\n" +
                  "  filter: progid: DXImageTransform.Microsoft.Alpha(Opacity=0);\n" +
                  "  opacity: 0;\n" +
                  "  pointer-events: none;\n" +
                  "}\n" +
                  // position & sketch
                  "[" + TOOL_TIP + "]::before {\n" +
                  "  position: absolute;\n" +
                  "  bottom: 0%;\n" +
                  "  left: 100%;\n" +
                  "  margin-bottom: 5px;\n" +
                  "  padding: 7px;\n" +
                  "  -webkit-border-radius: 3px;\n" +
                  "  -moz-border-radius: 3px;\n" +
                  "  border-radius: 3px;\n" +
                  "  background-color: #000;\n" +
                  "  background-color: hsla(0, 0%, 20%, 0.9);\n" +
                  "  color: #fff;\n" +
                  "  content: attr(" + TOOL_TIP + ");\n" +
                  "  text-align: center;\n" +
                  "  font-size: 14px;\n" +
                  "  line-height: 1.2;\n" +
                  "}\n" +
                  // hover show
                  "[" + TOOL_TIP + "]:hover::before,\n" +
                  "[" + TOOL_TIP + "]:hover::after {\n" +
                  "  visibility: visible;\n" +
                  "  -ms-filter: \"progid:DXImageTransform.Microsoft.Alpha(Opacity=100)\";\n" +
                  "  filter: progid: DXImageTransform.Microsoft.Alpha(Opacity=100);\n" +
                  "  opacity: 1;\n" +
                  "}\n";
   }


}
