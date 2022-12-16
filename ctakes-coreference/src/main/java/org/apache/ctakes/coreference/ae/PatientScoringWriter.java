package org.apache.ctakes.coreference.ae;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import org.apache.ctakes.constituency.parser.util.TreeUtils;
import org.apache.ctakes.core.config.ConfigParameterConstants;
import org.apache.ctakes.core.patient.AbstractPatientConsumer;
import org.apache.ctakes.core.patient.PatientNoteStore;
import org.apache.ctakes.core.patient.PatientViewUtil;
import org.apache.ctakes.coreference.util.ThymeCasOrderer;
import org.apache.ctakes.temporal.eval.Evaluation_ImplBase;
import org.apache.ctakes.typesystem.type.relation.CollectionTextRelation;
import org.apache.ctakes.typesystem.type.syntax.BaseToken;
import org.apache.ctakes.typesystem.type.syntax.NewlineToken;
import org.apache.ctakes.typesystem.type.textsem.Markable;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.factory.AggregateBuilder;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

/**
 * Created by tmill on 7/12/18.
 */
public class PatientScoringWriter extends AbstractPatientConsumer {

  @ConfigurationParameter(
          name = ConfigParameterConstants.PARAM_OUTPUTDIR,
          description = "Name of chain file in CoNLL format"
  )
  private String outputDir = null;

  public static final String PARAM_CONFIG = "Config";
  @ConfigurationParameter(
          name = PARAM_CONFIG,
          description = "Descriptive string representing configuration of this run"
  )
  private String configName = null;

  private PrintWriter sysOut = null;
  private PrintWriter goldOut = null;

  private PatientNoteStore notes = PatientNoteStore.INSTANCE;

  public PatientScoringWriter() {
    super("PatientScoringWriter", "Writes conll output that can be used in standard scoring scripts.");
  }

  @Override
  protected void processPatientCas(JCas patientJcas) throws AnalysisEngineProcessException {

    AggregateBuilder agg = new AggregateBuilder();
    AnalysisEngineDescription aed = null;

    // First we need to build a cross-document mapping from entities to chain indices:
    HashMap<Annotation, Integer> sysEntChainMap = new HashMap<>();
    HashMap<Annotation, Integer> goldEntChainMap = new HashMap<>();

    try {
      for (JCas docView : ThymeCasOrderer.getOrderedCases(patientJcas)) {
        if (docView.getViewName().equals(CAS.NAME_DEFAULT_SOFA) || !docView.getViewName().contains("Initial")) {
          continue;
        }

        JCas goldView = PatientViewUtil.getAlignedGoldCas(patientJcas, docView);
        int goldChainNum = 1;
        System.out.println("Gold chains:");
        for (CollectionTextRelation rel : JCasUtil.select(goldView, CollectionTextRelation.class)) {
          for (Markable member : JCasUtil.select(rel.getMembers(), Markable.class)) {
            System.out.print("Mention: " + member.getCoveredText().replace("\n", "<CR>"));
            goldEntChainMap.put(member, goldChainNum);
            System.out.print("  ----->    ");
          }
          System.out.println();
          goldChainNum++;
        }
        System.out.println();
        int sysChainNum = 1;
        System.out.println("System chains:");
        for (CollectionTextRelation rel : JCasUtil.select(docView, CollectionTextRelation.class)) {
          for (Markable member : JCasUtil.select(rel.getMembers(), Markable.class)) {
            System.out.print("Mention: " + member.getCoveredText().replace("\n", "<CR>"));
            sysEntChainMap.put(member, sysChainNum);
            if(!member.getView().getViewName().equals(docView.getViewName())){
              System.out.print("[DOC:" + member.getView().getViewName() + "]");
            }
            System.out.print("  ----->    ");
          }
          System.out.println();
          sysChainNum++;
        }
        System.out.println();
      }
    } catch (CASException e) {
      e.printStackTrace();
      throw new AnalysisEngineProcessException(e);
    }

    // Now we use those cross-document mappings to write the file:
    try {
      String pid = PatientNoteStore.getDefaultPatientId(patientJcas);
      sysOut.println("#begin document (" + pid + "); part 000");
      goldOut.println("#begin document (" + pid + "); part 000");

      for (JCas docView : ThymeCasOrderer.getOrderedCases(patientJcas)) {
        String noteId = PatientNoteStore.getDefaultDocumentId(docView);
        writeOneDocument(docView, docView, sysEntChainMap, sysOut, noteId);

        JCas goldView = PatientViewUtil.getAlignedGoldCas(patientJcas, docView);
        writeOneDocument(docView, goldView, goldEntChainMap, goldOut, noteId);
      }
      sysOut.println("#end document " + pid);
      goldOut.println("#end document " + pid);
    } catch (CASException e) {
      e.printStackTrace();
      throw new AnalysisEngineProcessException(e);
    }
    sysOut.flush();
    goldOut.flush();
  }

  /*
   * The CoNLL format has one line per token, tab-delimited with different pieces of information. Most of this is not
   * used by the coreference eval, so it is blank or default values, but the tokens are at least useful for debugging.
   * The algorithm is:
   * 1) Tokenize
   * 2) For each token:
   *     a) Find all markables covering this token
   *     b) For each covering markable that is also part of a coref chain (in ent2chain)
   *        i) Check if this token starts the markable, ends it, or is in the middle
   *        ii) Build lists for each condition
   *     c) Write basic information (docId, token index, token, POS tag)
   *     d) Create a dummy parse tree and write
   *     e) Write empty values for some filler columns
   *     f) Write the markable indices using the lists built above in order: ending, starting&ending, starting
   */
  private void writeOneDocument(JCas tokensCas, JCas chainsCas, Map<Annotation,Integer> ent2chain, PrintWriter out, String docId){
    List<BaseToken> tokens = new ArrayList<>(JCasUtil.select(tokensCas, BaseToken.class));
    Multiset<Integer> endSet = HashMultiset.create();

    int tokenId = 0;
    int sentId = 0;
    BaseToken nextToken = tokens.get(0);
    for(int i = 0; i < tokens.size(); i++){
      boolean endSentToken = false;
      BaseToken token = nextToken;
      if(i+1 < tokens.size()){
        nextToken = tokens.get(i+1);
        if(nextToken instanceof NewlineToken || (token.getCoveredText().equals(".") && !(endSet.size() > 0))){
          endSentToken = true;
        }
      }

      // if we see a newline token at the end of a sentence break the sentence
      // only print out if we are not at the start of the sentence:
      if(token instanceof NewlineToken){
        continue;
      }

      int lastInd = token.getEnd();
      // fix for some bad tokenization
      if(token.getCoveredText().length() > 1 && token.getCoveredText().endsWith(".")){
        lastInd = token.getEnd()-1;
      }
      List<Markable> markables = new ArrayList<>(JCasUtil.selectCovering(chainsCas, Markable.class, token.getBegin(), lastInd));

      // Write all the boilerplate stuff not used for eval but necessary to match the CoNLL Format:
      out.print(docId); out.print('\t');
      out.print(0); out.print('\t');
      out.print(tokenId++); out.print('\t');
      out.print(token instanceof NewlineToken ? "Newline" : TreeUtils.escapePunct(token.getCoveredText())); out.print('\t');
      out.print(token.getPartOfSpeech()); out.print('\t');

      // create a dummy parse -- assume flat parse
      if(tokenId == 1){
        out.print("(NOPARSE*");
        // special case for one word sentences:
        if(endSentToken){
          out.print(")");
        }
      }else if(endSentToken){
        out.print("*)");
      }else{
        out.print("*");
      }
      out.print('\t');
      // predicate lemma -- can ignore
      out.print('-'); out.print('\t');
      // predicate frameset id -- can ignore
      out.print('-'); out.print('\t');
      // word sense
      out.print('-'); out.print('\t');
      // speaker/author
      out.print('-'); out.print('\t');
      // named entities
      out.print('*'); out.print('\t');

      // Use the lists of markables that end, start, or start&end to create an output string:
      String markableString = createMarkableString(token, markables, ent2chain, endSet, endSentToken);
      out.println(markableString);


      if(endSentToken){
        out.println();
        tokenId = 0;
        sentId++;
      }
    }
  }

  /**
   * Call initialize() on super and the delegate
   * {@inheritDoc}
   */
  @Override
  public void initialize(final UimaContext context) throws ResourceInitializationException {
    super.initialize(context);
    String sysOutputFilename = new File(this.outputDir, "system." + configName + ".conll").getAbsolutePath();
    String goldOutputFilename = new File(this.outputDir, "gold." + configName + ".conll").getAbsolutePath();
    try {
      sysOut = new PrintWriter(new FileWriter(sysOutputFilename));
      goldOut = new PrintWriter(new FileWriter(goldOutputFilename));
    } catch (IOException e) {
      e.printStackTrace();
      throw new ResourceInitializationException(e);
    }
  }

  @Override
  public void collectionProcessComplete() throws AnalysisEngineProcessException {
    // FIXME - this is never called by the parent
    super.collectionProcessComplete();
    sysOut.close();
    goldOut.close();
  }

  @Override
  protected void finalize() throws Throwable {
    super.finalize();
    sysOut.close();
    goldOut.close();
  }

  private static String createMarkableString(BaseToken token, Collection<Markable> markables, Map<Annotation,Integer> ent2chain, Multiset<Integer> endSet, boolean endSentToken){
    List<Annotation> startMention = new ArrayList<>();
    Multiset<Integer> endMention = HashMultiset.create();
    List<Integer> wholeMention = new ArrayList<>();

    for(Annotation markable : markables){
      if(ent2chain.containsKey(markable)){
        if(markable.getBegin() == token.getBegin()){
          if(markable.getEnd() == token.getEnd()){
            wholeMention.add(ent2chain.get(markable));
          }else{
            startMention.add(markable);
          }
        }else if(markable.getEnd() <= token.getEnd()){
          if(endMention.contains(ent2chain.get(markable))){
            System.err.println("There is a duplicate element -- should be handled by multiset");
          }
          if(markable.getEnd() < token.getEnd()){
            System.err.println("There is a markable that ends in the middle of a token!");
          }
          endMention.add(ent2chain.get(markable));
        }
      }
    }

    StringBuffer buff = new StringBuffer();
    for(int ind : endMention){
      if(endSet.contains(ind)){
        buff.append(ind);
        buff.append(')');
        buff.append('|');
      }
      endSet.remove(ind);
    }
    for(int ind : wholeMention){
      buff.append('(');
      buff.append(ind);
      buff.append(')');
      buff.append('|');
    }
    // sort start mentions by ordering of ending
    while(startMention.size() > 0){
      int ind;
      Annotation latestEnd = null;
      for(int j = 0; j < startMention.size(); j++){
        if(latestEnd == null || startMention.get(j).getEnd() > latestEnd.getEnd()){
          latestEnd = startMention.get(j);
        }
      }
      startMention.remove(latestEnd);
      ind = ent2chain.get(latestEnd);
      buff.append('(');
      buff.append(ind);
      buff.append('|');
      endSet.add(ind);
//        endStack.push(ind);
    }

    // In some datasets markables end in the middle of a token -- this is a problem because our check above is for all markables that cover the
    // current token. In this case the markable end will still be unused when we get to the end of the sentence. We'll just hack it by throwing
    // them on the last token of the sentence.
    if(endSentToken && endSet.size() > 0){
      System.err.println("Error! There are opened markables that never closed! Putting them on the end of the sentence.");
      for(int ind : endSet){
        buff.append(ind);
        buff.append(')');
        buff.append('|');
      }
      endSet.clear();
    }
    if(buff.length() > 0){
      return buff.substring(0,  buff.length()-1);
    }else{
      return "_";
    }
  }
}
