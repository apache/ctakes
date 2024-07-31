package org.apache.ctakes.smokingstatus.ae;

import libsvm.svm;
import libsvm.svm_model;
import libsvm.svm_node;
import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.core.resource.FileLocator;
import org.apache.ctakes.core.util.log.DotLogger;
import org.apache.ctakes.smokingstatus.type.libsvm.NominalAttributeValue;
import org.apache.ctakes.typesystem.type.syntax.WordToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.apache.ctakes.smokingstatus.Const.*;


/**
 * Update of original PcsClassifierAnnotator_libsvm to use UimaFit.
 *
 * @author SPF , chip-nlp
 * @since {6/3/2022}
 */
@PipeBitInfo(
      name = "PcsClassifier",
      description = "Uses SVM for smoking status classification.",
      role = PipeBitInfo.Role.ANNOTATOR
)
public class PcsClassifier extends JCasAnnotator_ImplBase {

   static private final Logger LOGGER = LoggerFactory.getLogger( "PcsClassifier" );

   static public final String CASED_PARAM = "CaseSensitive";
   static public final String CASED_DESC = "yes/no for case sensitivity.";
   @ConfigurationParameter(
         name = CASED_PARAM,
         description = CASED_DESC,
         mandatory = false,
         defaultValue = "yes"
   )
   private String _caseSensitive;

   static public final String STOP_WORDS_PARAM = "StopWordsPath";
   static public final String STOP_WORDS_DESC = "Path to file containing stop words.";
   @ConfigurationParameter(
         name = STOP_WORDS_PARAM,
         description = STOP_WORDS_DESC
   )
   private String _stopWordsPath;

   static public final String KEY_WORDS_PARAM = "KeyWordsPath";
   static public final String KEY_WORDS_DESC = "Path to file containing key words.";
   @ConfigurationParameter(
         name = KEY_WORDS_PARAM,
         description = KEY_WORDS_DESC
   )
   private String _keyWordsPath;

   static public final String MODEL_PARAM = "ModelPath";
   static public final String MODEL_DESC = "Path to file containing the model.";
   @ConfigurationParameter(
         name = MODEL_PARAM,
         description = MODEL_DESC
   )
   private String _modelPath;

   static private final Map<Integer, String> SMOKER_CODES = new HashMap<>();

   static private final Pattern SPACE_PATTERN = Pattern.compile( "\\s+" );
   static private final Pattern TEXT_CLEANER_PATTERN = Pattern.compile( "[.?!:;()',\"{}<>#+]" );
   static private final String[] DATE_REGEXES = {
         "19\\d\\d", "19\\d\\ds", "20\\d\\d", "20\\d\\ds", "[1-9]0s", "\\d{1,2}[/-]\\d{1,2}",
         "\\d{1,2}[/-]\\d{4}", "\\d{1,2}[/-]\\d{1,2}[/-]\\d{2}", "\\d{1,2}[/-]\\d{1,2}[/-]\\d{4}" };

   static private final Collection<Pattern> DATE_PATTERNS = new ArrayList<>();

   static {
      for ( String regex : DATE_REGEXES ) {
         DATE_PATTERNS.add( Pattern.compile( regex ) );
      }
      SMOKER_CODES.put( CLASS_CURR_SMOKER_INT, CLASS_CURR_SMOKER );
      SMOKER_CODES.put( CLASS_PAST_SMOKER_INT, CLASS_PAST_SMOKER );
      SMOKER_CODES.put( CLASS_SMOKER_INT, CLASS_SMOKER );
   }

   private boolean _isCaseSensitive = true;
   private final Collection<String> _stopWords = new HashSet<>();
   private final List<String> _keyWords = new ArrayList<>();
   // Trained lib_svm model.
   private svm_model _model;


   /**
    * {@inheritDoc}
    */
   @Override
   public void initialize( final UimaContext context ) throws ResourceInitializationException {
      super.initialize( context );
      LOGGER.info( "Initializing ..." );
      try ( DotLogger dotter = new DotLogger() ) {
         // run long initialization process.  Caught Exception may be of some other type.
         if ( _caseSensitive.equalsIgnoreCase( "no" )
              || _caseSensitive.equalsIgnoreCase( "false" ) ) {
            _isCaseSensitive = false;
         }
         parseFile( _stopWordsPath, _isCaseSensitive, _stopWords );
         parseFile( _keyWordsPath, _isCaseSensitive, _keyWords );
         _model = svm.svm_load_model( FileLocator.getFile( _modelPath )
                                                 .getPath() );
      } catch ( IOException ioE ) {
         throw new ResourceInitializationException( ioE );
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void process( final JCas jcas ) throws AnalysisEngineProcessException {
      LOGGER.info( "Processing ..." );
      try ( DotLogger dotter = new DotLogger() ) {
         final List<Double> features = createFeatures( jcas );
         // date information
         double dateInfo = 0.0;
         // Cannot access sentence by SentenceAnnotator or RecordSentence.  this is sentence!!
         String sentence = jcas.getDocumentText();
         sentence = TEXT_CLEANER_PATTERN.matcher( sentence )
                                        .replaceAll( " " )
                                        .trim();
         final String[] textTokens = SPACE_PATTERN.split( sentence );
         for ( String textToken : textTokens ) {
            if ( DATE_PATTERNS.stream()
                              .anyMatch( p -> p.matcher( textToken )
                                               .matches() ) ) {
               dateInfo = 1.0;
               LOGGER.info( "***dateInfo|" + textToken + "|" + dateInfo );
               break;
            }
         }
         features.add( dateInfo );
         // set the libSVM feature vector.
         final svm_node[] svm_nodes = new svm_node[ features.size() ];
         for ( int j = 0; j < features.size(); j++ ) {
            svm_nodes[ j ] = new svm_node();
            svm_nodes[ j ].index = j + 1;
            svm_nodes[ j ].value = features.get( j );
         }
         // 1:CURRENT_SMOKER, 2:PAST_SMOKER, 3:SMOKER
         final double classLabel = svm.svm_predict( _model, svm_nodes );
         // string value.
         // note that the original code would cast to integer, which is equivalent to floor but poor form.
         final int intClassLabel = Double.valueOf( classLabel )
                                         .intValue();
         final String classValue = SMOKER_CODES.get( intClassLabel );
         LOGGER.info( "classLabel=" + classLabel + " intClassLabel" + intClassLabel + " classValue=" + classValue );
         final NominalAttributeValue nominalAttributeValue = new NominalAttributeValue( jcas );
         nominalAttributeValue.setAttributeName( "smoking_status" );
         nominalAttributeValue.setNominalValue( classValue );
         nominalAttributeValue.addToIndexes();
      } catch ( IOException ioE ) {
         throw new AnalysisEngineProcessException( ioE );
      }
   }

   private List<Double> createFeatures( final JCas jcas ) {
      final List<Double> features = new ArrayList<>();
      final List<String> unigrams = createUnigrams( jcas );
      final List<String> bigrams = new ArrayList<>();
      for ( int i = 0; i < unigrams.size() - 1; i++ ) {
         bigrams.add( unigrams.get( i ) + "_" + unigrams.get( i + 1 ) );
      }
      // unigram & bigram keywords
      for ( String keyWord : _keyWords ) {
         double value = 0.0;
         if ( keyWord.contains( "_" ) ) {
            if ( bigrams.stream()
                        .anyMatch( keyWord::equalsIgnoreCase ) ) {
               value = 1.0;
               LOGGER.info( "keyWord=" + keyWord + " bigram=" + bigrams.stream()
                                                                       .filter( keyWord::equalsIgnoreCase )
                                                                       .collect(
                                                                             Collectors.joining( " ; " ) ) );
            }
         } else {
            if ( unigrams.stream()
                         .anyMatch( keyWord::equalsIgnoreCase ) ) {
               value = 1.0;
               LOGGER.info( "keyWord=" + keyWord + " unigram=" + unigrams.stream()
                                                                         .filter( keyWord::equalsIgnoreCase )
                                                                         .collect(
                                                                               Collectors.joining( " ; " ) ) );
            }
         }
         features.add( value );
      }
      return features;
   }

   private List<String> createUnigrams( final JCas jcas ) {
      final List<String> unigrams = new ArrayList<>();
      final Collection<WordToken> wordTokens = JCasUtil.select( jcas, WordToken.class );
      for ( WordToken token : wordTokens ) {
         String tokenText = token.getCoveredText();
         if ( tokenText == null || tokenText.isEmpty() ) {
            continue;
         }
         // TODO - The following code CONDITIONALLY turns tokenText to lowercase,
         //  while the subsequent code ALWAYS turns tokenText to lowercase.
//            if ( !_isCaseSensitive ) {
//               tokenText = tokenText.toLowerCase();
//            }
         // if(!stopWords.contains(tok)) unigrams.add(tok);
         // -- this is the replace of the above line
         // Since the model was trained on words without non-word characters
         tokenText = tokenText.toLowerCase()
                              .replaceAll( "-{2,}", " " )
                              .trim();
         // with
         // the
         // cases
         // like:
         // Tobacco--quit
         // in
         // 1980.
         Arrays.stream( SPACE_PATTERN.split( tokenText ) )
               .filter( t -> !_stopWords.contains( t ) )
               .forEach( unigrams::add );
      }
      return unigrams;
   }

   static private void parseFile( final String filePath,
                                  final boolean isCaseSensitive,
                                  final Collection<String> collection ) throws IOException {
      try ( BufferedReader reader
                  = new BufferedReader(
            new InputStreamReader(
                  FileLocator.getAsStream( filePath ) ) ) ) {
         String line = reader.readLine();
         while ( line != null ) {
            if ( !isCaseSensitive ) {
               line = line.toLowerCase();
            }
            collection.add( line );
            line = reader.readLine();
         }
      } catch ( IOException ioE ) {
         throw new IOException( "Couldn't read " + filePath + " " + ioE.getMessage() );
      }
   }


}
