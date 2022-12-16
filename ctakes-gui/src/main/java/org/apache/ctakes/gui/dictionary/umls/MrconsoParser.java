package org.apache.ctakes.gui.dictionary.umls;


import org.apache.ctakes.gui.dictionary.util.FileUtil;
import org.apache.ctakes.gui.dictionary.util.TextTokenizer;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static org.apache.ctakes.gui.dictionary.umls.MrconsoIndex.*;

/**
 * Author: SPF
 * Affiliation: CHIP-NLP
 * Date: 1/17/14
 */
final public class MrconsoParser {

   static private final Logger LOGGER = Logger.getLogger( "MrConsoParser" );

   static private final String MR_CONSO_SUB_PATH = "/META/MRCONSO.RRF";

   // TODO - put all exclusions in a data file, display for user, allow changes and save, etc.

   //  https://www.nlm.nih.gov/research/umls/sourcereleasedocs
   //   https://www.nlm.nih.gov/research/umls/sourcereleasedocs/current/SNOMEDCT_US/stats.html
   //   https://www.nlm.nih.gov/research/umls/sourcereleasedocs/current/RXNORM/stats.html
   static private final String[] DEFAULT_EXCLUSIONS = { "FN", "CCS", "CA2", "CA3", "PSN", "TMSY",
                                                        "SBD", "SBDC", "SBDF", "SBDG",
                                                        "SCD", "SCDC", "SCDF", "SCDG", "BPCK", "GPCK", "XM" };

   static private final String[] SNOMED_OBSOLETES = { "OF", "MTH_OF", "OAP", "MTH_OAP", "OAF", "MTH_OAF",
                                                      "IS", "MTH_IS", "OAS", "MTH_OAS",
                                                      "OP", "MTH_OP" };
   // Snomed OF  = Obsolete Fully Specified Name      MTH_OF
   // Snomed OAP = Obsolete Active Preferred Term     MTH_OAP
   // Snomed OAF = Obsolete Active Full Name          MTH_OAF
   // Snomed IS  = Obsolete Synonym                   MTH_IS
   // Snomed OAS = Obsolete Active Synonym            MTH_OAS
   // Snomed OP  = Obsolete Preferred Name            MTH_OP
   // Snomed PT  = Preferred Term , but we don't need that for valid cuis ...  or do we want only those with preferred terms?
   // Snomed PTGB = British Preferred Term

   // GO has same snomed obsoletes +
   // GO EOT = Obsolete Entry Term
   // HPO has same snomed obsoletes

   // MTHSPL - DP is Drug Product  as is MTH_RXN_DP      MTHSPL SU is active substance
   // VANDF AB  is abbreviation for drug  VANDF CD is Clinical Drug.  Both are dosed.
   //  NDFRT AB?  Looks like ingredient.  NDFRT PT can be dosed

   static private final String[] GO_OBSOLETES = { "EOT" };

   static private final String[] LOINC_OBSOLETES = { "LO", "OLC", "MTH_LO", "OOSN" };

   static private final String[] MEDRA_OBSOLETES = { "OL", "MTH_OL" };

   static private final String[] MESH_EXCLUSIONS = { "N1", "EN", "PEN" };

   static private final String[] RXNORM_EXCLUSIONS = { "SY" };   // What is IN ?  Ingredient?

   static private final String[] NCI_EXCLUSIONS = { "CSN" };

   // Related to, but not synonymous
   static private final String[] UMDNS_EXCLUSIONS = { "RT" };

   private MrconsoParser() {
   }

   static public String[] getDefaultExclusions() {
      return DEFAULT_EXCLUSIONS;
   }

   static public String[] getSnomedExclusions() {
      final String[] defaults = getDefaultExclusions();
      final String[] exclusionTypes = Arrays.copyOf( defaults,
            defaults.length + SNOMED_OBSOLETES.length );
      System.arraycopy( SNOMED_OBSOLETES, 0, exclusionTypes, defaults.length, SNOMED_OBSOLETES.length );
      return exclusionTypes;
   }

   static public String[] getNonRxnormExclusions() {
      final String[] snomeds = getSnomedExclusions();
      final String[] exclusionTypes = Arrays.copyOf( snomeds,
            snomeds.length
            + GO_OBSOLETES.length
            + LOINC_OBSOLETES.length
            + MEDRA_OBSOLETES.length
            + MESH_EXCLUSIONS.length
            + NCI_EXCLUSIONS.length
            + UMDNS_EXCLUSIONS.length );
      int start = snomeds.length;
      System.arraycopy( GO_OBSOLETES, 0, exclusionTypes, start, GO_OBSOLETES.length );
      start += GO_OBSOLETES.length;
      System.arraycopy( LOINC_OBSOLETES, 0, exclusionTypes, start, LOINC_OBSOLETES.length );
      start += LOINC_OBSOLETES.length;
      System.arraycopy( MEDRA_OBSOLETES, 0, exclusionTypes, start, MEDRA_OBSOLETES.length );
      start += MEDRA_OBSOLETES.length;
      System.arraycopy( MESH_EXCLUSIONS, 0, exclusionTypes, start, MESH_EXCLUSIONS.length );
      start += MESH_EXCLUSIONS.length;
      System.arraycopy( NCI_EXCLUSIONS, 0, exclusionTypes, start, NCI_EXCLUSIONS.length );
      start += NCI_EXCLUSIONS.length;
      System.arraycopy( UMDNS_EXCLUSIONS, 0, exclusionTypes, start, UMDNS_EXCLUSIONS.length );
      return exclusionTypes;
   }

   static public Map<Long, Concept> parseAllConcepts( final String umlsDirPath,
                                                      final Map<Long, Concept> conceptMap,
                                                      final Collection<String> wantedSources,
                                                      final Collection<String> wantedTargets,
                                                      final UmlsTermUtil umlsTermUtil,
                                                      final Collection<String> languages,
                                                      final boolean extractAbbreviations,
                                                      final int minCharLength,
                                                      final int maxCharLength,
                                                      final int maxWordCount,
                                                      final int maxSymCount ) {
      final String mrconsoPath = umlsDirPath + MR_CONSO_SUB_PATH;
      final Collection<String> invalidTypeSet = new HashSet<>( Arrays.asList( getNonRxnormExclusions() ) );
      LOGGER.info( "Compiling map of Concepts from " + mrconsoPath );
      long lineCount = 0;
      long textCount = 0;
      try ( final BufferedReader reader = FileUtil.createReader( mrconsoPath ) ) {
         List<String> tokens = FileUtil.readBsvTokens( reader, mrconsoPath );
         while ( tokens != null ) {
            lineCount++;
            if ( lineCount % 100000 == 0 ) {
               LOGGER.info( "File Line " + lineCount + "   Texts " + textCount );
            }
            if ( !isRowLengthOk( tokens )
                 || !isLanguageOk( tokens, languages )
                 || !isTermTypeOk( tokens, invalidTypeSet ) ) {
               tokens = FileUtil.readBsvTokens( reader, mrconsoPath );
               continue;
            }
            final Long cuiCode = CuiCodeUtil.getInstance().getCuiCode( getToken( tokens, CUI ) );
            final Concept concept = conceptMap.get( cuiCode );
            if ( concept == null ) {
               // cui for current row is unwanted
               tokens = FileUtil.readBsvTokens( reader, mrconsoPath );
               continue;
            }
            final String text = getToken( tokens, TEXT );
            if ( !umlsTermUtil.isTextValid( text.toLowerCase() ) ) {
               tokens = FileUtil.readBsvTokens( reader, mrconsoPath );
               continue;
            }
            if ( isPreferredTerm( tokens ) ) {
               concept.setPreferredText( text );
            }
            // Get tokenized text
            final String tokenizedText = TextTokenizer.getTokenizedText( text.toLowerCase() );
            if ( tokenizedText == null || tokenizedText.isEmpty()
                 || !umlsTermUtil.isTextValid( tokenizedText )
                 || DoseUtil.hasUnit( tokenizedText ) ) {
               tokens = FileUtil.readBsvTokens( reader, mrconsoPath );
               continue;
            }
            // Remove unwanted prefixes and suffixes
            final String strippedText = umlsTermUtil.getStrippedText( tokenizedText );
            if ( strippedText == null || strippedText.isEmpty()
                 || UmlsTermUtil.isTextTooShort( strippedText, minCharLength )
                 || UmlsTermUtil.isTextTooLong( strippedText, maxCharLength, maxWordCount, maxSymCount ) ) {
               tokens = FileUtil.readBsvTokens( reader, mrconsoPath );
               continue;
            }
            final Collection<String> formattedTexts
                  = umlsTermUtil.getFormattedTexts( strippedText, extractAbbreviations, minCharLength,
                  maxCharLength, maxWordCount, maxSymCount );
            if ( formattedTexts != null && !formattedTexts.isEmpty() ) {
               textCount += concept.addTexts( formattedTexts );
               // Add secondary codes
               final String source = getToken( tokens, SOURCE );
               final String code = getToken( tokens, SOURCE_CODE );
               if ( wantedTargets.contains( source ) && !code.equals( "NOCODE" ) ) {
                  concept.addCode( source, code );
               }
            }
            tokens = FileUtil.readBsvTokens( reader, mrconsoPath );
         }
      } catch ( IOException ioE ) {
         LOGGER.error( ioE.getMessage() );
      }
      final Collection<Long> empties = conceptMap.entrySet().stream()
            .filter( e -> e.getValue().isEmpty() )
            .map( Map.Entry::getKey )
            .collect( Collectors.toList() );
      conceptMap.keySet().removeAll( empties );
      LOGGER.info( "File Lines: " + lineCount + " Concepts: " + conceptMap.size() + "  Texts: " + textCount );
      return conceptMap;
   }

   static private boolean isRowLengthOk( final List<String> tokens ) {
      return tokens.size() >= TEXT._index;
   }

   static private boolean isLanguageOk( final List<String> tokens,
                                        final Collection<String> languages ) {
      return languages.contains( getToken( tokens, LANGUAGE ) );
   }

   static private boolean isTermTypeOk( final List<String> tokens,
                                        final Collection<String> invalidTypeSet ) {
      final String type = getToken( tokens, TERM_TYPE );
      if ( invalidTypeSet.contains( type ) ) {
         return false;
      }
      // "Synonyms" are actually undesirable in the rxnorm vocabulary
      final String source = getToken( tokens, SOURCE );
      return !(source.equals( "RXNORM" ) && type.equals( "SY" ));
   }

   static private boolean isSourceOk( final List<String> tokens, final Collection<String> wantedSources ) {
      return wantedSources.contains( getToken( tokens, SOURCE ) );
   }

   static private boolean isPreferredTerm( final List<String> tokens ) {
      return getToken( tokens, STATUS ).equals( "P" ) && getToken( tokens, FORM ).equals( "PF" );
   }


   /**
    * Can cull the given collection of cuis
    *
    * @param umlsDirPath        path to the UMLS_ROOT Meta/MRCONSO.RRF file
    * @param sourceVocabularies desired source type names as appear in rrf: RXNORM, SNOMEDCT, MSH, etc.
    * @return Subset of cuis that exist in in the given sources
    */
   static public Collection<Long> getValidVocabularyCuis( final String umlsDirPath,
                                                          final Collection<String> sourceVocabularies ) {
      return getValidVocabularyCuis( umlsDirPath, sourceVocabularies, getNonRxnormExclusions() );
   }

//   /**
//    * Can cull the given collection of cuis
//    *
//    * @param umlsDirPath     path to the UMLS_ROOT Meta/MRCONSO.RRF file
//    * @return Subset of cuis that exist in in the given sources
//    */
//   static public Collection<Long> getValidRxNormCuis( final String umlsDirPath ) {
//      return getValidVocabularyCuis( umlsDirPath, Collections.singletonList( "RXNORM" ), getRxnormExclusions() );
//   }

   /**
    * Can cull the given collection of cuis
    *
    * @param umlsDirPath        path to the UMLS_ROOT Meta/MRCONSO.RRF file
    * @param sourceVocabularies desired source type names as appear in rrf: RXNORM, SNOMEDCT, MSH, etc.
    * @param invalidTypes       term type names as appear in rrf: FN, CCS, etc. that are not valid
    * @return Subset of cuis that exist in in the given sources
    */
   static private Collection<Long> getValidVocabularyCuis( final String umlsDirPath,
                                                           final Collection<String> sourceVocabularies,
                                                           final String... invalidTypes ) {
      final String mrconsoPath = umlsDirPath + MR_CONSO_SUB_PATH;
      LOGGER.info( "Compiling list of Cuis with wanted Vocabularies using " + mrconsoPath );
      final Map<String, Long> sourceCuis = new HashMap<>( sourceVocabularies.size() );
      for ( String target : sourceVocabularies ) {
         sourceCuis.put( target, 0L );
      }
      final Collection<Long> validCuis = new HashSet<>();
      long lineCount = 0;
      try ( final BufferedReader reader = FileUtil.createReader( mrconsoPath ) ) {
         List<String> tokens = FileUtil.readBsvTokens( reader, mrconsoPath );
         while ( tokens != null ) {
            lineCount++;
            if ( lineCount % 100000 == 0 ) {
               final String cuis = sourceCuis.entrySet().stream().map( e -> e.getKey() + " " + e.getValue() )
                     .collect( Collectors.joining( ", " ) );
               LOGGER.info( "File Lines " + lineCount + "\t Cuis: " + cuis );
            }
            if ( tokens.size() > SOURCE._index
                 && sourceVocabularies.stream().anyMatch( getToken( tokens, SOURCE )::equals )
                 && Arrays.stream( invalidTypes ).noneMatch( getToken( tokens, TERM_TYPE )::equals ) ) {
               final Long cuiCode = CuiCodeUtil.getInstance().getCuiCode( getToken( tokens, CUI ) );
               if ( validCuis.add( cuiCode ) ) {
                  final String source = getToken( tokens, SOURCE );
                  final long cuis = sourceCuis.get( source );
                  sourceCuis.put( source, (cuis + 1) );
               }
            }
            tokens = FileUtil.readBsvTokens( reader, mrconsoPath );
         }
      } catch ( IOException ioE ) {
         LOGGER.error( ioE.getMessage() );
      }
      final String cuis = sourceCuis.entrySet().stream().map( e -> e.getKey() + " " + e.getValue() )
            .collect( Collectors.joining( ", " ) );
      LOGGER.info( "File Lines " + lineCount + "\t Cuis: " + cuis );
      LOGGER.info( "File Lines " + lineCount + "\t Valid Cuis " + validCuis.size() + "\t for wanted Vocabularies" );
      LOGGER.info( "   Any Difference is caused by overlap of sources." );
      return validCuis;
   }


   static private String getToken( final List<String> tokens, final MrconsoIndex mrconsoIndex ) {
      return tokens.get( mrconsoIndex._index );
   }


}
