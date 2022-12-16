package org.apache.ctakes.coreference.ae;

import java.util.ArrayList;
import java.util.List;

import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.dependency.parser.util.DependencyUtility;
import org.apache.ctakes.typesystem.type.relation.CollectionTextRelation;
import org.apache.ctakes.typesystem.type.syntax.ConllDependencyNode;
import org.apache.ctakes.typesystem.type.syntax.WordToken;
import org.apache.ctakes.typesystem.type.textsem.Markable;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.EmptyFSList;
import org.apache.uima.jcas.cas.FSList;
import org.apache.uima.jcas.cas.NonEmptyFSList;
import org.apache.uima.resource.ResourceInitializationException;

@PipeBitInfo(
      name = "Person Coreference Annotator",
      description = "Annotates coreferences between person mentions.",
      dependencies = { PipeBitInfo.TypeProduct.BASE_TOKEN },
      products = { PipeBitInfo.TypeProduct.MARKABLE, PipeBitInfo.TypeProduct.COREFERENCE_RELATION }
)
public class PersonChainAnnotator extends JCasAnnotator_ImplBase {

  @Override
  public void process(JCas jcas) throws AnalysisEngineProcessException {
    NonEmptyFSList ptList = new NonEmptyFSList(jcas);
    ptList.setHead(null);
    NonEmptyFSList weList = new NonEmptyFSList(jcas);
    weList.setHead(null);
    NonEmptyFSList drList = new NonEmptyFSList(jcas);
    drList.setHead(null);
    List<NonEmptyFSList> otherDrs = new ArrayList<>();
    
    List<WordToken> words = new ArrayList<>(JCasUtil.select(jcas, WordToken.class));
    for(int i = 0; i < words.size(); i++){
      WordToken word = words.get(i);
      String text = word.getCoveredText();
      if(word.getPartOfSpeech().startsWith("PRP")){
        if(text.equalsIgnoreCase("I") || text.equalsIgnoreCase("me") || text.equalsIgnoreCase("my")){
          Markable drMention = new Markable(jcas, word.getBegin(), word.getEnd());
          addToList(jcas, drList, drMention);
        }else if(text.equalsIgnoreCase("we") || text.equalsIgnoreCase("us") || text.equalsIgnoreCase("our")){
          Markable weMention = new Markable(jcas, word.getBegin(), word.getEnd());
          addToList(jcas, weList, weMention);
        }else if(text.equalsIgnoreCase("it")){
          // do nothing
        }else{
          Markable ptMention = new Markable(jcas, word.getBegin(), word.getEnd());
          addToList(jcas, ptList, ptMention);
        }
      }else if(text.equalsIgnoreCase("dr.")){      
        Markable drMention = getDoctorMarkable(jcas, word); //new Markable(jcas, word.getBegin(), words.get(i+1).getEnd());
        addToList(jcas, getCorrectDoctor(jcas, drMention, otherDrs), drMention);
      }else if(text.equalsIgnoreCase("mrs.") || text.equalsIgnoreCase("mr.") || text.equalsIgnoreCase("ms.")){
        // TODO - smarter logic for Dr. Firstname Lastname
        Markable ptMention = new Markable(jcas, word.getBegin(), words.get(i+1).getEnd());
        addToList(jcas, ptList, ptMention);
      }else if(text.equalsIgnoreCase("patient") || text.equalsIgnoreCase("pt")){
        Markable ptMention = new Markable(jcas, word.getBegin(), word.getEnd());
        addToList(jcas, ptList, ptMention);
      }
    }
    
    for(NonEmptyFSList otherDr : otherDrs){
      if(otherDr.getHead() != null){
        if(otherDr.getTail() != null){
          endList(jcas, otherDr);
          CollectionTextRelation drChain = new CollectionTextRelation(jcas);
          drChain.setMembers(otherDr);
          drChain.addToIndexes();
        }
      }
    }
    
    if(drList.getHead() != null && drList.getTail() != null){
      endList(jcas, drList);
      CollectionTextRelation drChain = new CollectionTextRelation(jcas);
      drChain.setMembers(drList);
      drChain.addToIndexes();
    }
    if(ptList.getHead() != null && ptList.getTail() != null){
      endList(jcas, ptList);
      CollectionTextRelation ptChain = new CollectionTextRelation(jcas);
      ptChain.setMembers(ptList);
      ptChain.addToIndexes();
    }
    if(weList.getHead() != null && weList.getTail() != null){
      endList(jcas, weList);
      CollectionTextRelation weChain = new CollectionTextRelation(jcas);
      weChain.setMembers(weList);
      weChain.addToIndexes();
    }
  }

  public static AnalysisEngineDescription createAnnotatorDescription() throws ResourceInitializationException {
    return AnalysisEngineFactory.createEngineDescription(PersonChainAnnotator.class);
  }

  private static void addToList(JCas jcas, NonEmptyFSList list, Markable arg){
    arg.addToIndexes();
    if(list.getHead() == null){
      // first list element:
      list.setHead(arg);
    }else{
      // subsequent list elements:
      NonEmptyFSList cur = list;
      while(cur.getTail() != null){
        cur = (NonEmptyFSList)cur.getTail();
      }
      NonEmptyFSList tail = new NonEmptyFSList(jcas);
      tail.setHead(arg);
      cur.setTail(tail);
      tail.addToIndexes();
    }
  }
  
  private static void endList(JCas jcas, NonEmptyFSList list){
    NonEmptyFSList cur = list;
    while(cur.getTail() != null){
      cur = (NonEmptyFSList)cur.getTail();
    }
    EmptyFSList tail = new EmptyFSList(jcas);
    cur.setTail(tail);
    tail.addToIndexes();
  }
  
  private static NonEmptyFSList getCorrectDoctor(JCas jcas, Markable mention, List<NonEmptyFSList> drLists){
    NonEmptyFSList correctDr = null;
    if(mention.getCoveredText().length() < 5){
      if(drLists.size() > 0){
        correctDr = drLists.get(0);
      }
    }else{
      String nameText = mention.getCoveredText().substring(4);
      for(NonEmptyFSList drList : drLists){
        FSList curNode = drList;
        do{
          String otherName = ((Markable)((NonEmptyFSList)curNode).getHead()).getCoveredText();
          if(otherName.length() >= 5){
            otherName = otherName.substring(4);
            if(otherName.contains(nameText) || nameText.contains(otherName)){
              correctDr = drList;
            }
          }
          curNode = ((NonEmptyFSList)curNode).getTail();
        }while(curNode instanceof NonEmptyFSList);
        if(correctDr != null) break;
      }
    }
    if(correctDr == null){
      correctDr = new NonEmptyFSList(jcas);
      correctDr.setHead(null);
      drLists.add(correctDr);
    }
    return correctDr;
  }
  
  private static Markable getDoctorMarkable(JCas jcas, WordToken drToken){
    Markable markable = null;
    
    ConllDependencyNode nnpHead = DependencyUtility.getDependencyNode(jcas, drToken);
    try{
    	while(nnpHead != null && nnpHead.getHead() != null && nnpHead.getHead().getId() != 0 && nnpHead.getHead().getPostag().equals("NNP")){
    		nnpHead = nnpHead.getHead();
    	}
    }catch(NullPointerException e){
      System.err.print(".");
    }
    
    int start = drToken.getBegin();
    int end = nnpHead.getEnd();
    if(end < start) end = drToken.getEnd();
    
    markable = new Markable(jcas, start, end);    
    return markable;
  }
}
