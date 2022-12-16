package org.apache.ctakes.coreference.ae;

import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.core.util.ListFactory;
import org.apache.ctakes.coreference.eval.EvaluationOfEventCoreference;
import org.apache.ctakes.dependency.parser.util.DependencyUtility;
import org.apache.ctakes.typesystem.type.constants.CONST;
import org.apache.ctakes.typesystem.type.relation.CollectionTextRelation;
import org.apache.ctakes.typesystem.type.relation.CoreferenceRelation;
import org.apache.ctakes.typesystem.type.relation.RelationArgument;
import org.apache.ctakes.typesystem.type.syntax.ConllDependencyNode;
import org.apache.ctakes.typesystem.type.textsem.Markable;
import org.apache.log4j.Logger;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CASException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSList;
import org.apache.uima.jcas.cas.NonEmptyFSList;

import java.util.*;

/**
 * Created by tmill on 4/18/18.
 */
@PipeBitInfo(
        name = "Coreference Copier",
        description = "Sets Modality based upon context.",
        role = PipeBitInfo.Role.SPECIAL,
        dependencies = { PipeBitInfo.TypeProduct.MARKABLE, PipeBitInfo.TypeProduct.COREFERENCE_RELATION, PipeBitInfo.TypeProduct.DEPENDENCY_NODE }
)
public class CopyCoreferenceRelations extends org.apache.uima.fit.component.JCasAnnotator_ImplBase {
    private static Logger logger = Logger.getLogger(EvaluationOfEventCoreference.class);
    private static final double DROPOUT_RATE = 0.1;

    // TODO - make document aware for mention-cluster coreference? Not as easy as relation remover because this should work for
    // non-document-aware annotators.
    public static final String PARAM_GOLD_VIEW = "GoldViewName";
    @ConfigurationParameter(name=PARAM_GOLD_VIEW, mandatory=false, description="View containing gold standard annotations")
    private String goldViewName=EvaluationOfEventCoreference.GOLD_VIEW_NAME;

    public static final String PARAM_DROP_ELEMENTS = "Dropout";
    @ConfigurationParameter(name = PARAM_DROP_ELEMENTS, mandatory=false)
    private boolean dropout = false;

    @SuppressWarnings("synthetic-access")
    @Override
    public void process(JCas jcas) throws AnalysisEngineProcessException {
        JCas goldView = null;
        try {
            goldView = jcas.getView(goldViewName);
        } catch (CASException e) {
            e.printStackTrace();
            throw new AnalysisEngineProcessException(e);
        }
        copyRelations(jcas, goldView, dropout);
    }

    public static void copyRelations(JCas jcas, JCas goldView, boolean dropout){

        HashMap<Markable,Markable> gold2sys = new HashMap<>();
        Map<ConllDependencyNode,Collection<Markable>> depIndex = JCasUtil.indexCovering(jcas, ConllDependencyNode.class, Markable.class);

        for(CollectionTextRelation goldChain : JCasUtil.select(goldView, CollectionTextRelation.class)){
            FSList head = goldChain.getMembers();
            List<List<Markable>> systemLists = new ArrayList<>(); // the gold list can be split up into many lists if we allow dropout.
            boolean removeChain = false;

            // first one is guaranteed to be nonempty otherwise it would not be in cas
            do{
                NonEmptyFSList element = (NonEmptyFSList) head;
                Markable goldMarkable = (Markable) element.getHead();
                if(goldMarkable == null){
                    logger.error(String.format("Found an unexpected null gold markable"));
                }
                boolean mapped = mapGoldMarkable(jcas, goldMarkable, gold2sys, depIndex);

                // if we can't align the gold markable with one in the system cas then don't add it:
                if(!mapped){
                    String text = "<Out of bounds>";
                    if(!(goldMarkable.getBegin() < 0 || goldMarkable.getEnd() >= jcas.getDocumentText().length())){
                        text = goldMarkable.getCoveredText();
                    }
                    logger.warn(String.format("There is a gold markable %s [%d, %d] which could not map to a system markable.",
                            text, goldMarkable.getBegin(), goldMarkable.getEnd()));
                    removeChain = true;
                    break;
                }

                Markable sysMarkable = gold2sys.get(goldMarkable);
                if(!dropout || systemLists.size() == 0){
                    if(systemLists.size() == 0) systemLists.add(new ArrayList<>());
                    systemLists.get(0).add(sysMarkable);
                }else{
                    // 3 options: Do correctly (append to same list as last element), ii) Start its own list, iii) Randomly join another list
                    if(Math.random() > DROPOUT_RATE){
                        // most of the time do the right thing:
                        systemLists.get(0).add(sysMarkable);
                    }else{
                        int listIndex = (int) Math.ceil(Math.random() * systemLists.size());
                        if(listIndex == systemLists.size()){
                            systemLists.add(new ArrayList<>());
                        }
                        systemLists.get(listIndex).add(sysMarkable);
                    }
                }
                head = element.getTail();
            }while(head instanceof NonEmptyFSList);

            // don't bother copying over -- the gold chain was of person mentions
            if(!removeChain){
                for(List<Markable> chain : systemLists){
                    if(chain.size() > 1){
                        CollectionTextRelation sysRel = new CollectionTextRelation(jcas);
                        sysRel.setMembers(ListFactory.buildList(jcas, chain));
                        sysRel.addToIndexes();
                    }
                }
            }
        }

        for(CoreferenceRelation goldRel : JCasUtil.select(goldView, CoreferenceRelation.class)){
            if((gold2sys.containsKey(goldRel.getArg1().getArgument()) && gold2sys.containsKey(goldRel.getArg2().getArgument()))){
                CoreferenceRelation sysRel = new CoreferenceRelation(jcas);
                sysRel.setCategory(goldRel.getCategory());
                sysRel.setDiscoveryTechnique(CONST.REL_DISCOVERY_TECH_GOLD_ANNOTATION);

                RelationArgument arg1 = new RelationArgument(jcas);
                arg1.setArgument(gold2sys.get(goldRel.getArg1().getArgument()));
                sysRel.setArg1(arg1);
                arg1.addToIndexes();

                RelationArgument arg2 = new RelationArgument(jcas);
                arg2.setArgument(gold2sys.get(goldRel.getArg2().getArgument()));
                sysRel.setArg2(arg2);
                arg2.addToIndexes();

                sysRel.addToIndexes();
            }
        }
    }

    /* Fills in entries in a map for the gold markable passed in to the system markable.
        Algorithm:
         * Find dependency head for gold algorithm
         * Iterate over the markables that span that head
         * Check if any of those markables has the same head
         * if so add it to the map and return true
     */
    public static boolean mapGoldMarkable(JCas jcas, Markable goldMarkable, Map<Markable,Markable> gold2sys, Map<ConllDependencyNode, Collection<Markable>> depIndex){
        if(!(goldMarkable.getBegin() < 0 || goldMarkable.getEnd() >= jcas.getDocumentText().length())){
            ConllDependencyNode headNode = DependencyUtility.getNominalHeadNode(jcas, goldMarkable);

            for(Markable sysMarkable : depIndex.get(headNode)){
                ConllDependencyNode markNode = DependencyUtility.getNominalHeadNode(jcas, sysMarkable);
                if(markNode == headNode){
                    gold2sys.put(goldMarkable, sysMarkable);
                    return true;
                }
            }
        }else{
            // Have seen some instances where anafora writes a span that is not possible, log them
            // so they can be found and fixed:
            logger.warn(String.format("There is a markable with span [%d, %d] in a document with length %d\n",
                    goldMarkable.getBegin(), goldMarkable.getEnd(), jcas.getDocumentText().length()));
            return false;
        }
        return false;
    }
}
