package org.apache.ctakes.dockhand.gui.output;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 11/1/2019
 */
public enum Output {
   FHIR( "FHIR",
         "Fast Healthcare Interoperability Resources.",
         "package org.apache.ctakes.fhir.cc",
         "add FhirJsonFileWriter SubDirectory=FHIR" ),
   TEXT( "Text",
         "Text of the note with CUI, semantic group, POS marked.",
         "add pretty.plaintext.PrettyTextWriterFit SubDirectory=TEXT" ),
   PROPERTIES( "Properties",
         "List of the note sentences with entities and relations.",
         "add property.plaintext.PropertyTextWriterFit SubDirectory=PROP" ),
   HTML( "HTML",
         "Web Page of the note with visual information.",
         "add pretty.html.HtmlTextWriter SubDirectory=HTML" ),
   XMI( "XMI",
         "UIMA XML.  Extremely verbose.",
         "add FileTreeXmiWriter SubDirectory=XMI" ),
   CUI_LIST( "CUI List",
         "List of CUIs in the note.",
         "add CuiLookupLister SubDirectory=CUI" ),
   FINISHED( "Run Summary",
         "Summary Information Log after run completion.",
         "addLast util.log.FinishedLogger" );

   private final String _name;
   private final String _description;
   private final String[] _piperLines;

   Output( final String name, final String description, final String... piperLines ) {
      _name = name;
      _description = description;
      _piperLines = piperLines;
   }

   public String getName() {
      return _name;
   }

   public String getDescription() {
      return _description;
   }


   public List<String> getPiperLines() {
      final List<String> piperLines = new ArrayList<>();
      piperLines.add( "" );
      piperLines.add( "//   " + _description );
      piperLines.addAll( Arrays.asList( _piperLines ) );
      return piperLines;
   }

}
