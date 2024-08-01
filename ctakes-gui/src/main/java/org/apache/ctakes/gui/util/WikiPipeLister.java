package org.apache.ctakes.gui.util;

import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.core.pipeline.PipeBitInfoUtil;
import org.apache.ctakes.core.util.StringUtil;
import org.apache.ctakes.gui.pipeline.bit.PipeBitFinder;
import org.apache.ctakes.gui.pipeline.bit.parameter.DefaultParameterHolder;
import org.apache.ctakes.gui.pipeline.bit.parameter.ParameterHolder;

import java.io.*;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.Collectors;



/**
 * @author SPF , chip-nlp
 * @since {5/3/2024}
 */
final public class WikiPipeLister {

   //   core_desc.md

//    > This project contains several annotators, including:
//    >	- a sentence detector annotator (a wrapper around the OpenNLP sentence detector)
//    >	- a tokenizer
//    >	- an annotator that does not update the CAS in any way, which can be useful if you are using the UIMA
//    >	  CPE GUI and you are required to specify an analysis engine but you don't actually want to specify one.
//    >	- an annotator that creates a single Segment annotation encompassing the entire document text, which can
//    >	  be used when processing a plaintext document which therefore doesn't have section (aka segment) tags.
//    >
//    > Of particular interest is that
//    > - End-of-line characters are considered end-of-sentence markers.
//    >
//    > A sentence detector model is included with this project.
//    >
//    > The model derives from a combination of GENIA, Penn Treebank (Wall Street Journal) and anonymized
//    > clinical data per Safe Harbor HIPAA guidelines. Prior to model building, the clinical data was
//    > deidentified for patient names to preserve patient confidentiality. Any person name in the model
//    > will originate from non-patient data sources.
//


//       core_AE.md

   static private final String WIKI = "https://github.com/apache/ctakes/wiki/";
   static private final String PREFIX = "org.apache.ctakes.";
   // This list of packages needs to be managed.
   static private final List<String> PACKAGES = Arrays.asList( ".nn", ".cr", ".ae", ".cc", ".util",
         ".concurrent", ".patient", ".medfacts", ".attributes", ".eval", ".uima", ".data", ".metastasis", ".pipeline"  );

   static private final Map<String,String> MODULE_MAP = new HashMap<>();
   static {
      MODULE_MAP.put( "necontexts", "ne-contexts" );
      MODULE_MAP.put( "postagger", "pos-tagger" );
      MODULE_MAP.put( "relationextractor", "relation-extractor" );
      MODULE_MAP.put( "clinicalpipeline", "clinical-pipeline" );
      MODULE_MAP.put( "drugner", "drug-ner" );
      MODULE_MAP.put( "smokingstatus", "smoking-status" );
      MODULE_MAP.put( "dictionary.lookup2", "dictionary-lookup-fast" );
      MODULE_MAP.put( "dictionary.cased", "dictionary-lookup-fast" );
      MODULE_MAP.put( "clinical", "clinical-pipeline" );
   }

   // https://www.overleaf.com/learn/latex/Using_colors_in_LaTeX
   // Submitter GUI colors:
   // set=yellow/brown (supposed to be dark orange) load=magenta reader=dark green add=dark cyan bits=blue package=dark yellow
   static private final Map<String,String> COMMAND_MARKS = new HashMap<>();
   static {
      COMMAND_MARKS.put( "package", "$\\textcolor{orange}{\\textsf{package}}$");
      COMMAND_MARKS.put( "set", "$\\textcolor{olive}{\\textsf{set}}$" );
      COMMAND_MARKS.put( "cli", "$\\textcolor{brown}{\\textsf{cli}}$" );
      COMMAND_MARKS.put( "load", "$\\textcolor{magenta}{\\textsf{load}}$");
      COMMAND_MARKS.put( "reader", "$\\textcolor{teal}{\\textsf{reader}}$");
      COMMAND_MARKS.put( "add", "$\\textcolor{green}{\\textsf{add}}$" );
      COMMAND_MARKS.put( "addDescription", "$\\textcolor{green}{\\textsf{addDescription}}$" );
      COMMAND_MARKS.put( "addLogged", "$\\textcolor{green}{\\textsf{addLogged}}$" );
      COMMAND_MARKS.put( "addLast", "$\\textcolor{green}{\\textsf{addLast}}$" );
   }

   // mag

   static private final Collection<String> SETTERS = Arrays.asList( "set", "cli" );
   static private final Collection<String> ADDERS = Arrays.asList( "reader",
         "add", "addDescription", "addLogged", "addLast" );

   private String getParmMarkup( final String command, final String parm, final int index ) {
      if ( index == 1 ) {
         if ( command.equals( "package" ) ) {
            return "$\\textcolor{orange}{\\textsf{" + parm + "}}$";
         } else if ( command.equals( "load" ) ) {
            final String wikiLink = getPiperWikiLink( parm );
            if ( !wikiLink.isEmpty() ) {
               return wikiLink;
            }
            return "$\\textcolor{blue}{\\textsf{" + parm + "}}$";
         } else if ( ADDERS.contains( command ) ) {
            final String wikiLink = getPipeBitWikiLink( parm );
            if ( !wikiLink.isEmpty() ) {
               return wikiLink;
            }
            return "$\\textcolor{blue}{\\textsf{" + parm + "}}$";
         }
      }
      return getSetColors( parm );
   }

   static private String getSetColors( final String parm ) {
      final String[] splits = StringUtil.fastSplit( parm, '=' );
      if ( splits.length == 2 ) {
         // For some reason whitespace is required before $\\   Must escape the $ character.
         return "$\\textcolor{purple}{\\textsf{" + splits[ 0 ] + "}}$"
               + "= $\\textcolor{violet}{\\textsf{" + splits[ 1 ].replace( "$", "\\\\$" ) + "}}$";
      }
      return parm;
   }

   private String getPiperWikiLink( final String name ) {
      return _pipers.values().stream()
             .flatMap( Collection::stream )
             .filter( p -> p.isPiper( name ) )
                    .map( p -> getPiperNameWikiLink( name, p) )
             .findFirst()
             .orElse( "" );
   }


   private String getPipeBitWikiLink( final String bitName ) {
      final PipeBitDesc bitDesc = getBitDesc( bitName );
      if ( bitDesc == null ) {
         return "";
      }
      return getPipeBitClassWikiLink( bitDesc );
   }


   private PipeBitDesc getBitDesc( final String name ) {
      return _modules.stream()
                    .map( m -> m._bitDescMap.values() )
                    .flatMap( Collection::stream )
                     .flatMap( Collection::stream )
                     .filter( p -> p._class.equals( name ) )
                     .findFirst()
                     .orElse( null );
   }


   private final StringBuilder _sidebar = new StringBuilder();

   private void writeModule( final String dir, final ModuleDesc moduleDesc ) {
      if ( !moduleDesc._name.equals( "ctakes-examples" ) ) {
         _sidebar.append( "- [" ).append(moduleDesc._name )
                 .append( "](" )
                 .append( moduleDesc._name ).append( ")\n" );
      }
      try ( Writer writer = new BufferedWriter( new FileWriter( dir + "/" + moduleDesc._name + ".md" ) ) ) {
         writer.write( markdownModule( moduleDesc ) );
      } catch ( IOException ioE ) {
         System.err.println( ioE.getMessage() );
         System.exit( 1 );
      }
   }


   static private final String COLLECTION_READERS = "Collection Readers";
   static private final String ANNOTATION_ENGINES = "Annotation Engines";
   static private final String WRITERS_CONSUMERS = "Writers or Consumers";
   static private final String UTILITIES_SPECIAL = "Utilities";
   static private final String PIPER_FILES = "Piper Files";

   //
   //          MODULE GENERAL INFO
   //


   private String markdownModule( final ModuleDesc moduleDesc ) {
      final String moduleName = moduleDesc._name;
      final StringBuilder toc = new StringBuilder();
      toc.append( "# " ).append( moduleName ).append( "\n" ).append( "> \n" );
      if ( !moduleDesc._description.isEmpty() ) {
         toc.append( moduleDesc._description ).append( "\n" ).append( "> \n" );
      }
      final StringBuilder sb = new StringBuilder();
      if ( !moduleDesc.getBitDescs( "Reader" ).isEmpty() ) {
         toc.append( "[" ).append( COLLECTION_READERS ).append( "](#" )
            .append( COLLECTION_READERS.replace( ' ', '-' ) ).append( ")  \n" );
         sb.append( "\n---\n## " ).append( COLLECTION_READERS ).append( "\n" );
         moduleDesc.getBitDescs( "Reader" )
                   .forEach( b -> sb.append( markdownPipeBit( moduleName, b ) ) );
      }
      if ( !moduleDesc.getBitDescs( "Annotator" ).isEmpty() ) {
         toc.append( "[" ).append( ANNOTATION_ENGINES ).append( "](#" )
            .append( ANNOTATION_ENGINES.replace( ' ', '-' ) ).append( ")  \n" );
         sb.append( "\n---\n## " ).append( ANNOTATION_ENGINES ).append( "\n" );
         moduleDesc.getBitDescs( "Annotator" )
                   .forEach( b -> sb.append( markdownPipeBit( moduleName, b ) ) );
      }
      if ( !moduleDesc.getBitDescs( "Writer" ).isEmpty() ) {
         toc.append( "[" ).append( WRITERS_CONSUMERS ).append( "](#" )
            .append( WRITERS_CONSUMERS.replace( ' ', '-' ) ).append( ")  \n" );
         sb.append( "\n---\n## " ).append( WRITERS_CONSUMERS ).append( "\n" );
         moduleDesc.getBitDescs( "Writer" )
                   .forEach( b -> sb.append( markdownPipeBit( moduleName, b ) ) );
      }
      if ( !moduleDesc.getBitDescs( "Special" ).isEmpty() ) {
         toc.append( "[" ).append( UTILITIES_SPECIAL ).append( "](#" )
            .append( UTILITIES_SPECIAL.replace( ' ', '-' ) ).append( ")  \n" );
         sb.append( "\n---\n## " ).append( UTILITIES_SPECIAL ).append( "\n" );
         moduleDesc.getBitDescs( "Special" )
                   .forEach( b -> sb.append( markdownPipeBit( moduleName, b ) ) );
      }
      final Collection<PiperDesc> pipers = _pipers.get( moduleDesc._name );
      if ( pipers != null && !pipers.isEmpty() ) {
         toc.append( "[" ).append( PIPER_FILES ).append( "](#" )
            .append( PIPER_FILES.replace( ' ', '-' ) ).append( ")\n" );
         sb.append( "\n---\n## " ).append( PIPER_FILES ).append( "\n" );
         pipers.stream().sorted( Comparator.comparing( p -> p._name ) )
               .map( this::markdownPiper )
               .forEach( sb::append );
      }
      toc.append( "\n" );
      sb.append( "\n" );
      return toc.toString() + sb.toString();
   }

   private void writeAllPipers( final String dir ) {
      try ( Writer writer = new BufferedWriter( new FileWriter( dir + "/Piper File List.md" ) ) ) {
         writer.write( markdownPipers() );
      } catch ( IOException ioE ) {
         System.err.println( ioE.getMessage() );
         System.exit( 1 );
      }
   }

   private String markdownPipers() {
      final Collection<PiperDesc> pipers = _pipers.values().stream().flatMap( Collection::stream )
              .sorted( Comparator.comparing( PiperDesc::getName ) ).collect( Collectors.toList() );
      final StringBuilder sb = new StringBuilder();
      sb.append( "# Piper File List\n\n| Name | Description |\n|---|---|\n" );
      for ( PiperDesc piper : pipers ) {
         if ( piper._module.equals( "ctakes-examples" ) ) {
            continue;
         }
         sb.append( "|" ).append( getPiperWikiLink( piper ) ).append( "|" )
           .append( piper._description ).append("|\n" );
      }
      return sb.toString();
   }

   static private String getPiperWikiLink( final PiperDesc piper ) {
      return "[" + piper._name + "](" + piper._module
            + "#" + piper._name.replace( ' ', '-' ) + ")";
   }

   static private String getPiperNameWikiLink( final String name, final PiperDesc piper ) {
      return "[" + name + "](" + piper._module
            + "#" + piper._name.replace( ' ', '-' ) + ")";
   }

   private String getPipeBitClassWikiLink( final PipeBitDesc bitDesc ) {
      return "[" + bitDesc._class + "](" + bitDesc._module
            + "#" + bitDesc.getName().replace( ' ', '-' ) + ")";
   }

   private void writeAllBitTypes( final String dir ) {
      writeBitType( dir, getBitType( "Reader" ), COLLECTION_READERS );
      writeBitType( dir, getBitType( "Annotator" ), ANNOTATION_ENGINES );
      writeBitType( dir, getBitType( "Writer" ), WRITERS_CONSUMERS );
      writeBitType( dir, getBitType( "Special" ), UTILITIES_SPECIAL );
   }

   private void writeBitType( final String dir, final List<PipeBitDesc> bitDescs, final String title ) {
      _sidebar.append( "- [" ).append( title )
              .append( "](" )
              .append( title.replace( ' ', '-' ) ).append( ")\n" );
      try ( Writer writer = new BufferedWriter( new FileWriter( dir + "/" + title + ".md" ) ) ) {
         writer.write( markdownBitType( bitDescs, title ) );
      } catch ( IOException ioE ) {
         System.err.println( ioE.getMessage() );
         System.exit( 1 );
      }
   }

   private List<PipeBitDesc> getBitType( final String type ) {
      return _modules.stream()
                    .filter( m -> !m._name.equals( "ctakes-examples" ) )
                    .map( d -> d.getBitDescs( type ) )
                    .flatMap( Collection::stream )
                    .sorted( BIT_COMPARATOR )
                    .collect( Collectors.toList() );
   }

   // In case there aare any abstract pipe bits with @PipeBitInfo display them first.
   static private final Comparator<PipeBitDesc> BIT_COMPARATOR = ( b1, b2 ) ->
         (b1._abstract == b2._abstract)
         ? String.CASE_INSENSITIVE_ORDER.compare( b1.getName(), b2.getName() )
         : ( b1._abstract ) ? -1 : 1;

   private String markdownBitType( final List<PipeBitDesc> bitDescs, final String title ) {
      final StringBuilder sb = new StringBuilder();
      sb.append( "# " ).append( title ).append( "\n\n| Name | Description |\n|---|---|\n" );
      for ( PipeBitDesc bitDesc : bitDescs ) {
         sb.append( "|" ).append( getPipeBitWikiLink( bitDesc ) ).append( "|" )
           .append( bitDesc._info.description() ).append("|\n" );      }
      return sb.toString();
   }

   private String getPipeBitWikiLink( final PipeBitDesc bitDesc ) {
      return "[" + bitDesc.getName() + "](" + bitDesc._module
            + "#" + bitDesc.getName().replace( ' ', '-' ) + ")";
   }

   static private final String REPO = "https://github.com/apache/ctakes/blob/main/";
   static private String createLink( final String moduleName, final PipeBitDesc bitDesc ) {
      return "[```" + bitDesc._class + "```](" + REPO + moduleName + "/src/main/java/"
              + bitDesc._package.replace( '.', '/' ) + "/" + bitDesc._class + ".java)";
   }

   private String createParentLink( final String parentClass ) {
      if ( !parentClass.startsWith( "org.apache.ctakes." ) ) {
         return "```" + parentClass + "```";
      }
      final int lastDot = parentClass.lastIndexOf( '.' );
      if ( lastDot <= 0 ) {
         return "```" + parentClass + "```";
      }
      final String moduleName = getModuleName( parentClass.substring( 0, lastDot ) ).replace( '.', '-' );
      final String sourcePath = moduleName + "/src/main/java/" + parentClass.replace( '.', '/' ) + ".java";
      final File testFile = new File( sourcePath );
      if ( !testFile.exists() ) {
         return "```" + parentClass + "```";
      }
      return "[```" + parentClass + "```](" + REPO + sourcePath + ")";
   }

   //
   //          PIPE BIT GENERAL INFO
   //
   
   private String markdownPipeBit( final String moduleName, final PipeBitDesc bitDesc ) {
      final StringBuilder sb = new StringBuilder();
      sb.append( "\n### " ).append( bitDesc.getName() ).append( "\n" );
      final PipeBitInfo info = bitDesc._info;
      sb.append( "<details>\n<summary>" ).append( info.description() ).append( "</summary>\n\n" );
      sb.append( "**Source class:** " ).append( createLink( moduleName, bitDesc ) ).append( "  \n" );
      sb.append( "**Source package:** ```" ).append( bitDesc._package ).append( "```  \n" );
      sb.append( "**Parent class:** " ).append( createParentLink( bitDesc._parent ) ).append( "  \n" );

      final String dependencies = getProducts( "**Dependencies:** ", info.dependencies() );
      if ( !dependencies.isEmpty() ) {
         sb.append( dependencies ).append( "  \n" );
      }
      final String products = getProducts( "**Products:** ", info.products() );
      if ( !products.isEmpty() ) {
         sb.append( products ).append( "  \n" );
      }
      final String usables = getProducts( "**Usables:** ", info.usables() );
      if ( !usables.isEmpty() ) {
         sb.append( usables ).append( "  \n" );
      }
      sb.append( "\n" ).append( markdownParameters( bitDesc._parmHolder ) ).append( "\n</details>\n\n" );
      return sb.toString();
   }

   private String getProducts( final String label, final PipeBitInfo.TypeProduct[] products ) {
      if ( products.length == 0 ) {
         return "";
      }
      return label + Arrays.stream( products ).map( PipeBitInfo.TypeProduct::name )
                          .map( WikiPipeLister::deCap )
                          .collect( Collectors.joining( ", " ) );
   }


   //
   //             CONFIGURATION PARAMETERS TABLE
   //

   private String markdownParameters( final ParameterHolder parmHolder ) {
      final int count = parmHolder.getParameterCount();
      if ( count == 0 ) {
         return "No available configuration parameters.  \n";
      }
      final StringBuilder sb = new StringBuilder();
      sb.append( createHeader() );
      for ( int i=0; i<count; i++ ) {
         String desc = parmHolder.getParameterDescription( i );
         desc = desc.replace( "\"", "\\\"" );
         String dflt = String.join( " ; ", parmHolder.getParameterValue( i ) );
         if ( dflt.equals( "org.apache.uima.fit.descriptor.ConfigurationParameter.NO_DEFAULT_VALUE" ) ) {
            dflt = "";
         }
         if ( dflt.startsWith( "org.") ) {
            final int lastDot = dflt.lastIndexOf( '.' );
            if ( lastDot > 3 && lastDot < dflt.length() - 2 ) {
               dflt = dflt.substring( 0, lastDot ) + ". " + dflt.substring( lastDot+1 );
            }
         }
         if ( dflt.startsWith( "org/") ) {
            final int lastSlash = dflt.lastIndexOf( '/' );
            if ( lastSlash > 3 && lastSlash < dflt.length() - 2 ) {
               dflt = dflt.substring( 0, lastSlash ) + "/ " + dflt.substring( lastSlash+1 );
            }
         }
         dflt = dflt.replace( "\"", "\\\"" );
         sb.append( '|' ).append( parmHolder.getParameterName( i ) )
           .append( '|' ).append( desc )
           .append( '|' ).append( parmHolder.getParameterClass( i ) )
           .append( '|' ).append( parmHolder.isParameterMandatory( i ) ? "Yes" : "No" )
           .append( '|' ).append( dflt )
           .append( "|\n" );
      }
      return sb.toString();
   }


   static private final String[] COL_HEADERS = { "Parameter", "Description", "Class", "Required", "Default" };
   static private final String COL_LINE = "|---|---|:---:|:---:|---|";

   private String createHeader() {
      final StringBuilder sb = new StringBuilder();
      for ( String colHeader : COL_HEADERS ) {
         sb.append( "|" ).append( colHeader );
      }
      sb.append( "|\n" ).append( COL_LINE ).append( "\n" );
      return sb.toString();
   }

//   private String padCell( final String text, final int index ) {
//      return "| " + String.format( "%1$-" + COL_WIDTHS[ index ] + "s", text );
//   }



//   private List<String> wordWrap( final String text, final char splitter, final int width ) {
//      final String[] splits = StringUtil.fastSplit( text, splitter );
//      final List<String> lines = new ArrayList<>();
//      StringBuilder line = new StringBuilder();
//      for ( String split : splits ) {
//         if ( line.length() + split.length() + 1 > width ) {
//            lines.add( line + "\n" );
//            line = new StringBuilder( split );
//         } else {
//            line.append( splitter ).append( split );
//         }
//      }
//      lines.add( line.toString() );
//      return lines;
//   }

   static private String deCap( final String text ) {
      final String[] splits = StringUtil.fastSplit( text, '_' );
      return Arrays.stream( splits )
                   .map( t -> t.charAt( 0 ) + t.substring( 1 ).toLowerCase() )
                   .collect( Collectors.joining( " " ) );
   }


   private String getModuleName( final String packageName ) {
      String module = packageName.replace( PREFIX, "" );
      if ( module.length() == packageName.length() ) {
         System.err.println( "1 Bad Package " + packageName + " " + module );
         System.exit( 1 );
      }
      int pIndex = PACKAGES.stream().mapToInt( module::indexOf ).filter( i -> i>0 ).min().orElse( -1 );
      if ( pIndex > 0 ) {
         module = module.substring( 0, pIndex );
      }
      return "ctakes-" + replaceBad( module );
   }

   static private String replaceBad( final String modulePackage )  {
      return MODULE_MAP.getOrDefault( modulePackage, modulePackage.replace( '.', '-' ) );
   }

   private String readModuleIntro( final String name ) {
      final File readme = new File( name, "README.md" );
      if ( !readme.isFile() ) {
         return "";
      }
      final StringBuilder sb = new StringBuilder();
      try ( BufferedReader reader = new BufferedReader( new FileReader( readme ) ) ) {
         String line = "";
         while ( line != null ) {
            sb.append( line ).append( '\n' );
            line = reader.readLine();
         }
      } catch ( IOException ioE ) {
         System.err.println( ioE.getMessage() );
      }
      return sb.toString();
   }

   private class ModuleDesc {
      private final String _name;
      private final String _description;
      private final Map<String,Collection<PipeBitDesc>> _bitDescMap = new HashMap<>();
      private ModuleDesc( final String name ) {
         _name = name;
         _description = readModuleIntro( name );
      }
      public String getName() {
         return _name;
      }
      private void addBitDesc( final PipeBitDesc bitDesc ) {
         _bitDescMap.computeIfAbsent( bitDesc.getRole(), t -> new ArrayList<>() ).add( bitDesc );
      }
      private List<PipeBitDesc> getBitDescs( final String type ) {
         return _bitDescMap.getOrDefault( type, Collections.emptyList() )
                           .stream()
                           .sorted( Comparator.comparing( PipeBitDesc::getName ) )
                           .collect( Collectors.toList() );
      }
   }


   private final class PipeBitDesc {
      private final PipeBitInfo _info;
      private final String _package;
      private final String _module;
      private final String _parent;
      private final String _class;
      private final boolean _abstract;
      private final ParameterHolder _parmHolder;
      private PipeBitDesc( final Class<?> bitClass ) {
         final int modifiers = bitClass.getModifiers();
         _abstract = Modifier.isAbstract( modifiers );
         _package = bitClass.getPackage().getName();
         _module = getModuleName( _package );
         _parent = bitClass.getSuperclass().getName();
         _class = bitClass.getSimpleName();
         _info = PipeBitInfoUtil.getInfo( bitClass );
         _parmHolder = new DefaultParameterHolder( bitClass );
      }
      private String getName() {
         return _info.name();
      }
      private String getRole() {
         return deCap( _info.role().name() );
      }
   }

   private final class PiperDesc {
      private final String _filename;
      private final String _name;
      private final String _path;
      private final String _contents;
      private final String _description;
      private final String _module;
      private PiperDesc( final File file ) {
         _filename = file.getName();
         final String name = _filename.replace( ".piper", "" );
         _name = name.replaceAll( "(.)([A-Z])", "$1 $2" );
         final String path = file.getAbsolutePath().replace( '\\', '/' );
         int orgIndex = path.indexOf( "org/" );
         if ( orgIndex < 0 ) {
            System.err.println( "Bad Piper Path " + path );
         }
         _path = path.substring( orgIndex );
         _module = getModuleName( new File( _path ).getParent()
                 .replace( '\\', '/' ).replace( '/', '.' ) );
         String description = "";
         String firstComment = "";
         final StringBuilder sb = new StringBuilder();
         try ( BufferedReader reader = new BufferedReader( new FileReader( file ) ) ) {
            String line = reader.readLine();
            while ( line != null ) {
               if ( line.trim().startsWith( "# " ) && description.isEmpty() ) {
                  description = line.substring( 2 ).trim();
               } else if ( (line.trim().startsWith( "//" ) || line.trim().startsWith( "! " )) && firstComment.isEmpty() ) {
                  firstComment = line.substring( 2 );
               }
               sb.append( line ).append( "\n" );
               line = reader.readLine();
            }
         } catch ( IOException ioE ) {
            System.err.println( ioE.getMessage() );
         }
         _description = description.isEmpty() ? firstComment : description;
         _contents = sb.toString();
      }
      private String getName() {
         return _name;
      }
      private boolean isPiper( final String name ) {
         return _filename.equals( name ) || _filename.equals( name + ".piper" );
      }

   }

   // ^ ~ \  are also latex reserved, but require name substitution.
   static private final Collection<String> BAD_LATEX = Arrays.asList( "#", "$", "%", "&", "_", "{", "}" );

   static private String safeLatex( final String line ) {
      String latex = line;
      for ( String l : BAD_LATEX ) {
         latex = latex.replace( l, "\\\\" + l );
      }
      return latex.replace( " http", " (http)" );
   }

   private String markdownPiper( final PiperDesc piperDesc ) {
      final StringBuilder sb = new StringBuilder();
      sb.append( "\n### " ).append( piperDesc._name ).append( "\n" );
      sb.append( "<details>\n<summary>" ).append( piperDesc._description ).append( "</summary>\n\n" );
      sb.append( "[" ).append( piperDesc._name ).append( "](" ).append( REPO ).append( piperDesc._module )
              .append( "/src/user/resources/" ).append( piperDesc._path ).append( ")  \n\n" );
      final String[] lines = StringUtil.fastSplit( piperDesc._contents, '\n' );
      for ( String line : lines ) {
         sb.append( "> " );
         if ( !line.isEmpty() ) {
            final String[] splits = StringUtil.fastSplit( line, ' ' );
            if ( splits.length > 0 ) {
               if ( splits[ 0 ].startsWith( "//" ) || splits[ 0 ].startsWith( "#" ) || splits[ 0 ].startsWith( "!" ) ) {
                  sb.append( "$\\textcolor{gray}{\\textsf{" )
                    .append( safeLatex( line ) )
                    .append( " }}$" );
               } else {
                  final String command = COMMAND_MARKS.getOrDefault( splits[ 0 ], splits[ 0 ] );
                  sb.append( command );
                  for ( int i=1; i<splits.length; i++ ) {
                     final String parmMarkup = getParmMarkup( splits[ 0 ], splits[ i ], i );
                     sb.append( ' ' ).append( parmMarkup );
                  }
               }
            }
         }
         sb.append( "<br>\n" );
      }
      sb.append( "</details>\n\n" );
      return sb.toString();
   }

   static private final String SH_LICENSE =
         "# Licensed to the Apache Software Foundation (ASF) under one\n" +
         "# or more contributor license agreements.  See the NOTICE file\n" +
         "# distributed with this work for additional information\n" +
         "# regarding copyright ownership.  The ASF licenses this file\n" +
         "# to you under the Apache License, Version 2.0 (the\n" +
         "# \"License\"); you may not use this file except in compliance\n" +
         "# with the License.  You may obtain a copy of the License at\n" +
         "#\n" +
         "#   http://www.apache.org/licenses/LICENSE-2.0\n" +
         "#\n" +
         "# Unless required by applicable law or agreed to in writing,\n" +
         "# software distributed under the License is distributed on an\n" +
         "# \"AS IS\" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY\n" +
         "# KIND, either express or implied.  See the License for the\n" +
         "# specific language governing permissions and limitations\n" +
         "# under the License.\n";

   static private final String BAT_LICENSE =
         "@REM Licensed to the Apache Software Foundation (ASF) under one\n" +
         "@REM or more contributor license agreements.  See the NOTICE file\n" +
         "@REM distributed with this work for additional information\n" +
         "@REM regarding copyright ownership.  The ASF licenses this file\n" +
         "@REM to you under the Apache License, Version 2.0 (the\n" +
         "@REM \"License\"); you may not use this file except in compliance\n" +
         "@REM with the License.  You may obtain a copy of the License at\n" +
         "@REM\n" +
         "@REM   http://www.apache.org/licenses/LICENSE-2.0\n" +
         "@REM\n" +
         "@REM Unless required by applicable law or agreed to in writing,\n" +
         "@REM software distributed under the License is distributed on an\n" +
         "@REM \"AS IS\" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY\n" +
         "@REM KIND, either express or implied.  See the License for the\n" +
         "@REM specific language governing permissions and limitations\n" +
         "@REM under the License.\n";

   static private final String BAT_LICENSE_2 =
           ":: Licensed to the Apache Software Foundation (ASF) under one\n" +
          ":: or more contributor license agreements.  See the NOTICE file\n" +
          ":: distributed with this work for additional information\n" +
          ":: regarding copyright ownership.  The ASF licenses this file\n" +
          ":: to you under the Apache License, Version 2.0 (the\n" +
          ":: \"License\"); you may not use this file except in compliance\n" +
          ":: with the License.  You may obtain a copy of the License at\n" +
          "::\n" +
          "::   http://www.apache.org/licenses/LICENSE-2.0\n" +
          "::\n" +
          ":: Unless required by applicable law or agreed to in writing,\n" +
          ":: software distributed under the License is distributed on an\n" +
          ":: \"AS IS\" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY\n" +
          ":: KIND, either express or implied.  See the License for the\n" +
          ":: specific language governing permissions and limitations\n" +
          ":: under the License.\n";

   static private String getScriptType( final String filename ) {
      if ( filename.endsWith( ".sh" ) || filename.endsWith( ".bash" ) ) {
         return " sh";
      }
      if ( filename.endsWith( ".bat" ) || filename.endsWith( ".cmd" ) ) {
         return " bat";
      }
      return "";
   }

   static private String getScriptType2( final String filename ) {
      if ( filename.endsWith( ".sh" ) || filename.endsWith( ".bash" ) ) {
         return "Linux / Mac shell script ";
      }
      if ( filename.endsWith( ".bat" ) || filename.endsWith( ".cmd" ) ) {
         return "Windows shell script ";
      }
      return "";
   }

   private Map<String,String> getBinScriptsMap() {
      final Map<String,String> binScripts = new HashMap<>();
      final File dir = new File( "ctakes-distribution/src/main/bin" );
      final File[] files = dir.listFiles();
      if ( files == null ) {
         return Collections.emptyMap();
      }
      for ( File file : files ) {
         final String filename = file.getName();
         if ( filename.startsWith( "OpenCmd" ) || filename.startsWith( "ant" ) ) {
            continue;
         }
         final String type = getScriptType( filename );
         if ( !file.canRead() || type.isEmpty() ) {
            continue;
         }
         final StringBuilder sb = new StringBuilder();
//         sb.append( "\n### " ).append( filename, 0, filename.lastIndexOf( '.' ) ).append( "\n" );
         sb.append( "\n### " ).append( filename ).append( "\n" );
         sb.append( "<details>\n<summary>" )
//           .append( getScriptType2( filename ) ).append( filename ).append( "</summary>\n\n" );
                 .append( getScriptType2( filename ) ).append( "</summary>\n\n" );
         sb.append( "```" ).append( type ).append( "\n" );
         try ( BufferedReader reader = new BufferedReader( new FileReader( file ) ) ) {
            String line = "";
            while ( line != null ) {
               sb.append( line ).append( "\n" );
               line = reader.readLine();
            }
         } catch ( IOException ioE ) {
            System.err.println( ioE.getMessage() );
         }
         sb.append( "```\n</details>\n\n" );
         final String script = sb.toString().replace( SH_LICENSE, "" )
                 .replace( BAT_LICENSE, "" )
                 .replace( BAT_LICENSE_2, "" );
         binScripts.put( filename, script );
      }
      return binScripts;
   }

   private void writeBinScripts( final String dir ) {
      _sidebar.append( "- [Scripts](Shell+Scripts)\n" );
      final Map<String,String> binScripts = getBinScriptsMap();
      try ( Writer writer = new BufferedWriter( new FileWriter( dir + "/Shell Scripts.md" ) ) ) {
         writer.write( "Shell Scripts included in the bin/ directory of a cTAKES installation.  \n" );
         writer.write( "These scripts can only be run in a cTAKES installation.  \n" );
         writer.write( "They cannot be used within a source code project.\n\n" );
         final List<String> names = binScripts.keySet().stream().sorted().collect( Collectors.toList() );
         for ( String name : names ) {
            writer.write( binScripts.get( name ) );
         }
      } catch ( IOException ioE ) {
         System.err.println( ioE.getMessage() );
         System.exit( 1 );
      }
   }

   private List<ModuleDesc> parseModules() {
      PipeBitFinder.getInstance().scan();
      final Map<String, ModuleDesc> moduleMap = new HashMap<>();
      final Collection<Class<?>> bitClasses = PipeBitFinder.getInstance().getPipeBits();
      for ( Class<?> bitClass : bitClasses ) {
         final PipeBitDesc bitDesc = new PipeBitDesc( bitClass );
         final String moduleName = bitDesc._module;
         final ModuleDesc moduleDesc = moduleMap.computeIfAbsent( moduleName, ModuleDesc::new );
         moduleDesc.addBitDesc( bitDesc );
      }
      parsePipers();
      for ( String moduleName : _pipers.keySet() ) {
         moduleMap.computeIfAbsent( moduleName, ModuleDesc::new );
      }
      return moduleMap.values().stream()
                      .sorted( Comparator.comparing( ModuleDesc::getName ) )
                      .collect( Collectors.toList() );
   }

   private void parsePipers() {
      final File resources = new File( "resources/" );
      getPipers( resources );
   }

   static private final Map<String,Collection<PiperDesc>> _pipers = new HashMap<>();

   private void getPipers( final File dir ) {
      final File[] files = dir.listFiles();
      if ( files == null ) {
         return;
      }
      for ( File file : files ) {
         if ( file.isFile() && file.getName().endsWith( ".piper" ) ) {
            final PiperDesc piper = new PiperDesc( file );
            final String module = piper._module;
            _pipers.computeIfAbsent( module, p -> new ArrayList<>() ).add( piper );
         } else if ( file.isDirectory() ) {
            getPipers( file );
         }
      }
   }


   private void startSidebar() {
      _sidebar.append( "[[/images/ctakes_logo.jpg|Apache cTAKES]]\n\n" )
              .append( "\n### [Home](Home)\n\n" )
              .append( "- [New in v5](Differences+between+4.0+and+5.0)\n" )
              .append( "- [Videos](Videos)\n" );
   }

   private void sidebarRunCtakes( final String dir ) {
      _sidebar.append( "\n### Running cTAKES\n\n" )
              .append( "- [Piper File Submitter GUI](Piper+File+Submitter)\n" );
      writeBinScripts( dir );
      _sidebar.append( "- [PiperFileRunner class](" ).append( REPO )
              .append( "ctakes-core/src/main/java/org/apache/ctakes/core/pipeline/PiperFileRunner.java)\n" );
      // TODO - Create a page for bin/runctakes
      //  TODO - Create a page for PiperFileRunner
   }

   private void sidebarPipelines() {
      _sidebar.append( "\n### Pipelines\n\n" )
              .append( "- [Pipeline Introduction](Default-Clinical-Pipeline)\n" )
              .append( "- [Default Clinical Pipelines](ctakes-clinical-pipeline)\n" )
              .append( "- [Piper File Introduction](Piper+Files)\n" )
              .append( "- [Piper File List](Piper+File+List)\n" );
   }

   private void sidebarGUIs() {
      _sidebar.append( "\n### GUI Tools\n\n- PLACEHOLDER" );
   }

   private void sidebarModules() {
      _sidebar.append( "\n### Code Modules\n\n" );
   }

   private void sidebarPipeBits() {
      _sidebar.append( "\n### Pipeline Components\n\n" );
   }

   private void sidebarPBJ() {
      _sidebar.append( "\n### Python Bridge to Java\n\n" )
              .append( "- [ctakes-pbj](ctakes-pbj)\n" )
               .append( "- Python COMPONENTS\n" )
              .append( "- [examples](ctakes+pbj+and+ctakes+cnlpt+examples)\n" );
      // TODO - Python Components.  Pages -exist- but should be aggregated.
      // TODO - edit that intro page so that each "first setup API here" is replaced with
      //  "install Artemis" https://activemq.apache.org/components/artemis/
   }

   static private final String EXAMPLE_RESOURCE = "ctakes-examples/src/user/resources/org/apache/ctakes/examples/";
   private void sidebarExamples() {
      _sidebar.append( "\n### Examples\n\n" )
              .append( "- [ctakes-examples](ctakes-examples)\n" )
              .append( "- [Notes](" ).append( REPO ).append( EXAMPLE_RESOURCE ).append( "notes/annotated)\n" )
              .append( "- [Annotations](" ).append( REPO ).append( EXAMPLE_RESOURCE ).append( "annotation)\n" );

      // TODO  Notes, annotations (link for anafora), pipelines, readers, annotators, writers ...
   }



   private void endSidebar( final String dir ) {
      _sidebar.append( "\n### General\n\n" )
              .append( "- [History](history)\n" )
              .append( "- [Citation](Citation)\n" )
              .append( "- [License](https://www.apache.org/licenses)\n" )
              .append( "- [Security](https://www.apache.org/security)\n" )
              .append( "- [Get Involved](Get-Involved)\n" )
              .append( "- [Apache™ Sponsorship](https://www.apache.org/foundation/sponsorship.html)\n" )
              .append( "- [Thank you](https://www.apache.org/foundation/thanks.html)\n" );
      try ( BufferedWriter writer = new BufferedWriter( new FileWriter( dir + "/_Sidebar.md" ) ) ) {
         writer.write( _sidebar.toString() );
      } catch ( IOException ioE ) {
         System.err.println( ioE.getMessage() );
      }
   }


   private void handleModules( final String dir ) {
      for ( ModuleDesc module : _modules ) {
         writeModule( dir, module );
         System.out.println( module._name + ".md" );
      }
   }

   final private List<ModuleDesc> _modules;

   private WikiPipeLister() {
      _modules = parseModules();
   }

   static public void main( final String... args ) {
//      final String dir = args[ 0 ];
      final String dir = "C:/temp/wiki_modules";
      new File( dir ).mkdirs();
      final WikiPipeLister tabler = new WikiPipeLister();
      tabler.startSidebar();
      tabler.sidebarRunCtakes( dir );
      tabler.sidebarPipelines();
      tabler.sidebarGUIs();
      tabler.sidebarModules();
      tabler.handleModules( dir );
      tabler.writeAllPipers( dir );
      tabler.sidebarPipeBits();
      tabler.writeAllBitTypes( dir );
      tabler.sidebarExamples();
      tabler.sidebarPBJ();
      tabler.endSidebar( dir );
   }


   // TODO Copy the PBJ python pages.  Plus a whole bunch of others like the gui pages.  Maybe put those in <details>

}
