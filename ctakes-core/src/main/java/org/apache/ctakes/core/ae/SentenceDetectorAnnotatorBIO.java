package org.apache.ctakes.core.ae;

import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.core.resource.FileLocator;
import org.apache.ctakes.typesystem.type.textspan.Segment;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.ctakes.utils.struct.CounterMap;
import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.cleartk.ml.CleartkAnnotator;
import org.cleartk.ml.DataWriter;
import org.cleartk.ml.Feature;
import org.cleartk.ml.Instance;
import org.cleartk.ml.feature.function.CharacterCategoryPatternFunction;
import org.cleartk.ml.feature.function.CharacterCategoryPatternFunction.PatternType;
import org.cleartk.ml.jar.DefaultDataWriterFactory;
import org.cleartk.ml.jar.DirectoryDataWriterFactory;
import org.cleartk.ml.jar.GenericJarClassifierFactory;
import org.cleartk.util.ViewUriUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;


/**
 * Prose sentence detector.
 *
 * <p>This sentence detector operates at the character level and splits a
 * document into sentences using a trained model.  It notably differs
 * from org.apache.ctakes.core.ae.SentenceDetector in that it allows
 * colon, semicolon, and newline characters mid-sentence.
 *
 * <p>This sentence detector does sometimes lump lines together if they
 * aren't prose, e.g. lines of text in a list.  See the cross-references
 * for annotators that can help correct this.
 *
 * @see org.apache.ctakes.core.ae.ListSentenceFixer
 * @see org.apache.ctakes.core.ae.ParagraphSentenceFixer
 * @see org.apache.ctakes.core.ae.SentenceDetector
 */
@PipeBitInfo(
      name = "Prose Sentence Detector",
      description = "Sentence detector that uses B I O for determination.  " +
                    "Useful for documents in which newlines may not indicate sentence boundaries.",
      role = PipeBitInfo.Role.ANNOTATOR,
      dependencies = PipeBitInfo.TypeProduct.SECTION,
      products = PipeBitInfo.TypeProduct.SENTENCE
)
public class SentenceDetectorAnnotatorBIO extends CleartkAnnotator<String>{

  private Logger logger = Logger.getLogger(SentenceDetectorAnnotatorBIO.class);
  private static final int WINDOW_SIZE = 3;
  
  public static enum FEAT_CONFIG {GILLICK, CHAR, SHAPE, LINE_POS, CHAR_SHAPE, CHAR_POS, CHAR_SHAPE_POS }
  public static final String PARAM_FEAT_CONFIG = "FeatureConfiguration";
  @ConfigurationParameter(name=PARAM_FEAT_CONFIG,mandatory=false)
  private FEAT_CONFIG featConfig=FEAT_CONFIG.CHAR;
  
  public static final String PARAM_TOKEN_FILE = "TokenFilename";
  @ConfigurationParameter(name=PARAM_TOKEN_FILE,mandatory=false)
  private String tokenCountFile = "org/apache/ctakes/core/models/sentdetect/tokenCounts.txt";
  CounterMap<String> tokenCounts = new CounterMap<>();

  private HashMap<Integer,Double> endCounts = null;
  private double maxLineStrength = -1;
  private int maxLineLength = -1;
  
  @Override
  public void initialize(UimaContext context)
      throws ResourceInitializationException {
    super.initialize(context);
    try{
      Scanner scanner = new Scanner(FileLocator.getAsStream(tokenCountFile));
      while(scanner.hasNextLine()){
        String[] pair = scanner.nextLine().trim().split(" : ");
        if(pair.length == 2){
          tokenCounts.put(pair[0], Integer.parseInt(pair[1]));
        }
      }
      scanner.close();
    }catch(FileNotFoundException e){
      throw new ResourceInitializationException(e);
    }
  }
  
  @Override
  public void process(JCas jcas) throws AnalysisEngineProcessException {
    String uri=null;
    try{
      uri = ViewUriUtil.getURI(jcas).toString();
      logger.info(String.format("Processing file with uri %s", uri));
    }catch(CASRuntimeException e){
      logger.debug("No uri found, probably not a big deal unless this is an evaluation.");
    }
    
    if(featConfig == FEAT_CONFIG.LINE_POS || featConfig == FEAT_CONFIG.CHAR_POS || featConfig == FEAT_CONFIG.CHAR_SHAPE_POS){
      buildDocEndlineModel(jcas);
    }
    
    for(Segment seg : JCasUtil.select(jcas, Segment.class)){
      // keep track of next sentence during training
      List<Sentence> sents = JCasUtil.selectCovered(jcas, Sentence.class, seg);
      int sentInd = 0;
      Sentence nextSent = sents.size() > 0 ? sents.get(sentInd++) : null;
      int startInd=0;
      
      // Iterate over every character in the Segment and classify it as Begin, Inside, or Outside a Sentence
      String prevOutcome = "O";
      String segText = seg.getCoveredText();
      for(int ind = 0; ind < segText.length(); ind++){
        List<Feature> feats = new ArrayList<>();
        
        char curChar = segText.charAt(ind);
        
        // Start collecting features:
        feats.add(new Feature("PrevOutcome", prevOutcome));
        
        // all systems get to know about the current char they're classifying (i.e. is this a period)
        feats.addAll(getCharFeatures(curChar, "Character"));

        if(featConfig == FEAT_CONFIG.CHAR || featConfig == FEAT_CONFIG.CHAR_POS || featConfig == FEAT_CONFIG.CHAR_SHAPE || featConfig == FEAT_CONFIG.CHAR_SHAPE_POS){
          for(int window = -WINDOW_SIZE; window <= WINDOW_SIZE; window++){
            if(ind+window >= 0 && ind+window < segText.length()){
              char conChar = segText.charAt(ind+window);
              feats.addAll(getCharFeatures(conChar, "CharOffset_"+window));
            }
          }
        }
        
        
        String nextToken = getNextToken(segText, ind);
        String prevToken = getPrevToken(segText, ind);
        feats.addAll(getTokenFeatures(prevToken, nextToken, "Token")); 

        if(featConfig == FEAT_CONFIG.LINE_POS || featConfig == FEAT_CONFIG.CHAR_POS || featConfig == FEAT_CONFIG.CHAR_SHAPE_POS){
          feats.addAll(getPositionFeatures(curChar, ind, segText, nextToken));
        }

        String outcome;
        int casInd = seg.getBegin() + ind;
        if(this.isTraining()){
          // if ind pointer has passed nextSent pointer advance nextSent
          while(nextSent != null && nextSent.getEnd() < casInd && sentInd < sents.size()){
            nextSent = sents.get(sentInd++);
          }
          if(nextSent == null){
            outcome = "O";
          }else if(casInd < nextSent.getBegin()){
            // current index is prior to next sentence
            outcome = "O";
          }else if(prevOutcome.equals("O")){
            // current index is in sentence but just after a character that was out of the sentence
            outcome = "B";
          }else{
            // current index is in the middle of a sentence
            outcome = "I";
          }
          this.dataWriter.write(new Instance<String>(outcome, feats));
        }else{
          if(!prevOutcome.equals("O") && Character.isLetterOrDigit(curChar)){
            outcome = "I";
          }else{
            outcome = this.classifier.classify(feats);
            // This shouldn't be necessary, but if the learning algorithm fails, we need to correct it so
            // that our accounting works. Only a B or O can follow an O, if classifier predicts "I", switch
            // it to a "B".
            if(outcome.equals("I") && prevOutcome.equals("O")){
              outcome = "B";
            }
            if(outcome.equals("B")) startInd = casInd;
            else if(outcome.equals("O") && 
                (prevOutcome.equals("I") || prevOutcome.equals("B"))){
              // just ended a sentence
              int endInd = casInd;
              while(endInd > startInd && Character.isWhitespace(segText.charAt(endInd-seg.getBegin()-1))){
                endInd--;
              }

              if(endInd > startInd){
                makeSentence(jcas, startInd, endInd);
              }
            }
          }
        }
        prevOutcome = outcome;
      }
      // One final sentence at the end of the segment if we were in the middle of one when we ran out of characters.
      if(!this.isTraining() && !prevOutcome.equals("O")){
        // segment ended with a sentence
        makeSentence(jcas, startInd, seg.getEnd());
      }
    }
  }


  private void buildDocEndlineModel(JCas jcas) {
    int window = 5;
    HashMap<Integer,Double> rawCounts = new HashMap<>();
    endCounts = new HashMap<>();
    maxLineStrength = -1;
    maxLineLength = -1;
    for(Segment seg : JCasUtil.select(jcas, Segment.class)){
      String[] lines = seg.getCoveredText().split("\n+");

      // fill map with line lengths
      for(String line : lines){
        if(line.contains("[**") || line.contains("**]")) continue; // mimic PHI-replacement artificially extends lines
        if(!rawCounts.containsKey(line.length())){
          rawCounts.put(line.length(), 0.0);
        }
        rawCounts.put(line.length(), rawCounts.get(line.length()) + 1.0);
        int adjustedLength = line.replaceAll("\\s+$","").length();
        if(adjustedLength > maxLineLength){
          maxLineLength = adjustedLength;
        }
      }
    }

    // smooth with triangle filter
    for(int len : rawCounts.keySet()){
      double count = rawCounts.get(len);
      for(int i = Math.max(0,len-window+1); i < len+window; i++){
        if(!endCounts.containsKey(i)){
          endCounts.put(i, 0.0);
        }
        double partialMass = (window - Math.abs(i - len)) * count / window;
        endCounts.put(i, endCounts.get(i) + partialMass);
      }
    }
    
    // normalize to probabilities
    for(double count : endCounts.values()){
      if(count > maxLineStrength){
        maxLineStrength = count;
      }
    }
  }

  // Create UIMA annotation after cleaning up begin and end of sentence.
  public static void makeSentence(JCas jcas, int begin, int end){
    String docText = jcas.getDocumentText();
    while(begin < docText.length() && Character.isWhitespace(docText.charAt(begin))){
      begin++;
    }
    while(end > 0 && Character.isWhitespace(docText.charAt(end-1))){
      end--;
    }
    if(begin < end){
      Sentence sent = new Sentence(jcas, begin, end);
      sent.addToIndexes();
    }
  }
  
  private static String getNextToken(String segText, int ind) {
    int startInd = ind;
    
    // move startInd right if it's whitespace and left if it's not.
    while(startInd < segText.length() && Character.isWhitespace(segText.charAt(startInd))){
      startInd++;
    }
    while(startInd > 0 && !Character.isWhitespace(segText.charAt(startInd-1))){
      startInd--;
    }
    
    int endInd = startInd;
    while(endInd < segText.length() && !Character.isWhitespace(segText.charAt(endInd))){
      endInd++;
    }
    
    return segText.substring(startInd, endInd);    
  }
  
  private static String getPrevToken(String segText, int ind){
    int endInd = ind;
    
    // move endInd left until we hit whitespace:
    while(endInd > 0 && !Character.isWhitespace(segText.charAt(endInd))){
      endInd--;
    }
    // then move until the character to the left is whitespace
    while(endInd > 0 && Character.isWhitespace(segText.charAt(endInd))){
      endInd--;
    }
    
    int startInd = endInd;
    while(startInd > 0 && !Character.isWhitespace(segText.charAt(startInd)) && !Character.isWhitespace(segText.charAt(startInd-1))){
      startInd--;
    }
    
    return segText.substring(startInd, endInd+1);
  }

  static CharacterCategoryPatternFunction<Annotation> shapeFun = new CharacterCategoryPatternFunction<>(PatternType.REPEATS_AS_KLEENE_PLUS);
  
  private Collection<? extends Feature> getTokenFeatures(String prevToken, String nextToken, String prefix) {
    List<Feature> feats = new ArrayList<>();
    
    // identity features (1 & 2 in Table 1, Gillick 2009)
    Feature prevTokenFeat = new Feature(prefix + "PrevIdentity", prevToken);
    feats.add(prevTokenFeat);
    Feature nextTokenFeat = new Feature(prefix + "NextIdentity", nextToken);
    feats.add(nextTokenFeat);
    
    // length features (3 in Gillick but only for the left token to approximately model abbreviations)
    if(featConfig != FEAT_CONFIG.GILLICK){
      feats.add(new Feature(prefix+"NextLength="+nextToken.length(), true));
    }
    feats.add(new Feature(prefix+"PrevLength="+prevToken.length(), true));
    
    // capitalzation of right word (4 in gillick)
    feats.add(new Feature(prefix+"cap", nextToken.length() > 0 && Character.isUpperCase(nextToken.charAt(0))));
    
    // shape features for word identity
    if(featConfig == FEAT_CONFIG.CHAR_SHAPE_POS || featConfig == FEAT_CONFIG.CHAR_SHAPE || featConfig == FEAT_CONFIG.SHAPE){
      feats.addAll(shapeFun.apply(prevTokenFeat));
      feats.addAll(shapeFun.apply(nextTokenFeat));
    }
    
    // token count features (5 & 6 in gillick)
    int rightLower = (int) Math.round(Math.log(tokenCounts.get(nextToken.toLowerCase())));
    feats.add(new Feature(prefix + "_RightLower_"+ rightLower, true));

    String prevDotless = prevToken;
    if(prevToken.endsWith(".")){
      prevDotless = prevToken.substring(0, prevToken.length()-1);
    }
    int leftDotless = (int) Math.round(Math.log(tokenCounts.get(prevDotless)));
    feats.add(new Feature(prefix + "_LeftDotless_" + leftDotless, true));
    
    // token joint features: identity pair (7 in gillick) and left_identity-right_is_capitalized (8 in gillick)
    feats.add(new Feature("TokenContextCat_" + prevToken + "_" + nextToken));
    feats.add(new Feature("LeftWordRightCap", prevToken + "_" + (nextToken.length() > 0 && Character.isUpperCase(nextToken.charAt(0)))));

    return feats;
  }

  public static List<Feature> getCharFeatures(char ch, String prefix){
    List<Feature> feats = new ArrayList<>();
    feats.add(new Feature(prefix+"_Id", ch == '\n' ? "<LF>" : ch));
    feats.add(new Feature(prefix+"_Upper", Character.isUpperCase(ch)));
    feats.add(new Feature(prefix+"_Lower", Character.isLowerCase(ch)));
    feats.add(new Feature(prefix+"_Digit", Character.isDigit(ch)));
    feats.add(new Feature(prefix+"_Space", Character.isWhitespace(ch)));
    feats.add(new Feature(prefix+"_Type"+Character.getType(ch), true));
    return feats;
  }
  
  public List<Feature> getPositionFeatures(char curChar, int ind, String segText, String nextToken){
    List<Feature> feats = new ArrayList<>();
    if(curChar == '\n' && ind > 0){
      int prevNewlineInd = segText.lastIndexOf('\n', ind-1);
      int thisLineLength = ind - prevNewlineInd-1;
      int endInd = thisLineLength + nextToken.length();
      
      // simple rule-based feature: working:
      if(thisLineLength <= maxLineLength && thisLineLength + 1 + nextToken.length() > maxLineLength){
        feats.add(new Feature("NextWordWrapsLine", true));
      }
      double beginStrength = endCounts.containsKey(thisLineLength) ? endCounts.get(thisLineLength) : 0.0;
      double endStrength = endCounts.containsKey(endInd) ? endCounts.get(endInd) : 0.0;
      
      // not working:
      for(int intLens = thisLineLength; intLens < thisLineLength+1+nextToken.length(); intLens++){
        if(!endCounts.containsKey(intLens)) continue;
        double strength = endCounts.get(intLens);
        if(strength > endStrength){
          feats.add(new Feature("LinePosNextWrapsLocalMax", true));
          break;
        }
      }
      
      // working:
      if(endCounts.containsKey(thisLineLength)){
        feats.add(new Feature("LinePosStrength", endCounts.get(thisLineLength) / this.maxLineStrength));
      }
    }
    return feats;
  }
  
  public static AnalysisEngineDescription getDataWriter(File outputDirectory, Class<? extends DataWriter<?>> class1) throws ResourceInitializationException {
    return AnalysisEngineFactory.createEngineDescription(
        SentenceDetectorAnnotatorBIO.class,
        SentenceDetectorAnnotatorBIO.PARAM_IS_TRAINING,
        true,
        DirectoryDataWriterFactory.PARAM_OUTPUT_DIRECTORY,
        outputDirectory,
        DefaultDataWriterFactory.PARAM_DATA_WRITER_CLASS_NAME,
        class1,
        SentenceDetectorAnnotatorBIO.PARAM_FEAT_CONFIG,
        SentenceDetectorAnnotatorBIO.FEAT_CONFIG.CHAR);
  }

  public static AnalysisEngineDescription getDescription(String modelPath) throws ResourceInitializationException {
    return AnalysisEngineFactory.createEngineDescription(
        SentenceDetectorAnnotatorBIO.class,
        SentenceDetectorAnnotatorBIO.PARAM_IS_TRAINING,
        false,
        GenericJarClassifierFactory.PARAM_CLASSIFIER_JAR_PATH,
        modelPath,
        SentenceDetectorAnnotatorBIO.PARAM_FEAT_CONFIG,
        SentenceDetectorAnnotatorBIO.FEAT_CONFIG.CHAR);
  }
  
  public static AnalysisEngineDescription getDescription() throws ResourceInitializationException {
    return getDescription("/org/apache/ctakes/core/models/sentdetect/model.jar");
  }
}

