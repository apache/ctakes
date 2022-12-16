package org.apache.ctakes.coreference.ae;

import com.google.common.collect.Maps;
import org.apache.ctakes.core.patient.AbstractPatientConsumer;
import org.apache.ctakes.core.patient.PatientNoteCollector;
import org.apache.ctakes.core.patient.PatientNoteStore;
import org.apache.ctakes.core.patient.PatientViewUtil;
import org.apache.ctakes.core.util.ListFactory;
import org.apache.ctakes.core.util.doc.SourceMetadataUtil;
import org.apache.ctakes.coreference.eval.EvaluationOfEventCoreference;
import org.apache.ctakes.coreference.util.ThymeCasOrderer;
import org.apache.ctakes.temporal.eval.Evaluation_ImplBase;
import org.apache.ctakes.temporal.eval.THYMEData;
import org.apache.ctakes.typesystem.type.relation.CollectionTextRelation;
import org.apache.ctakes.typesystem.type.relation.CoreferenceRelation;
import org.apache.ctakes.typesystem.type.relation.RelationArgument;
import org.apache.ctakes.typesystem.type.textsem.EventMention;
import org.apache.ctakes.typesystem.type.textsem.Markable;
import org.apache.ctakes.utils.struct.CounterMap;
import org.apache.log4j.Logger;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.component.ViewCreatorAnnotator;
import org.apache.uima.fit.component.ViewTextCopierAnnotator;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.factory.AggregateBuilder;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.pipeline.JCasIterator;
import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSList;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.cleartk.util.cr.UriCollectionReader;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

/**
 * Created by tmill on 2/22/18.
 */
public class ThymeAnaforaCrossDocCorefXmlReader extends AbstractPatientConsumer {

    public static final String PARAM_XML_DIRECTORY = "XmlDirectory";
    @ConfigurationParameter(
            name = PARAM_XML_DIRECTORY,
            description = "Directory containing cross-document coreference annotations"
    )String xmlDir;

    public static final String PARAM_IS_TRAINING = "IsTraining";
    @ConfigurationParameter(
            name = PARAM_IS_TRAINING,
            description = "Whether this reader is being called at training or test time, and thus whether gold annotations should be put in document or gold CAS"
    )boolean isTraining;

    private static final String NAME = ThymeAnaforaCrossDocCorefXmlReader.class.getSimpleName();
    private static final Logger LOGGER = Logger.getLogger(ThymeAnaforaCrossDocCorefXmlReader.class);

    public ThymeAnaforaCrossDocCorefXmlReader(){
        super(NAME,
                "Reads gold standard cross-document coreference annotations in the format created for the THYME project, using the Anafora tool.");
    }

    public static AnalysisEngineDescription getDescription(String xmlDir, boolean training) throws ResourceInitializationException {
        return AnalysisEngineFactory.createEngineDescription(ThymeAnaforaCrossDocCorefXmlReader.class,
                ThymeAnaforaCrossDocCorefXmlReader.PARAM_XML_DIRECTORY,
                xmlDir,
                ThymeAnaforaCrossDocCorefXmlReader.PARAM_IS_TRAINING,
                training);
    }

    @Override
    public String getEngineName() {
        return NAME + (isTraining?"_training":"_test");
    }

    @Override
    protected void processPatientCas(JCas patientJcas) throws AnalysisEngineProcessException {
        String patientName = SourceMetadataUtil.getPatientIdentifier( patientJcas );
        String xmlFilename = String.format("%s.Thyme2v1-PostProc.gold.completed.xml", patientName);
        File annotationDir = null;
        for(String subdir : new String[]{"Train", "Dev", "Test"}){
            annotationDir = new File(new File(this.xmlDir, subdir), patientName);
            if(annotationDir.exists()) break;
        }
        if(annotationDir == null){
            System.err.println("Could not find a cross-doc coreference file for patient: " + patientName + " in the specified directory: " + this.xmlDir);
            throw new AnalysisEngineProcessException();
        }
        File annotationFile = new File(annotationDir, xmlFilename);
        if(!annotationFile.exists()){
            LOGGER.error("No *Correction.gold.completed.xml file exists for this patient either... please remove from dataset");
            throw new AnalysisEngineProcessException();
        }
        Map<String,String> notes = new HashMap<>();

        for(File file : annotationDir.listFiles()){
            if(file.isDirectory()){
                String fileContents = null;
                File noteFile = new File(file, file.getName());
                try {
                    fileContents = new String(Files.readAllBytes(Paths.get(noteFile.toURI())));
                } catch (IOException e) {
                    e.printStackTrace();
                    throw new AnalysisEngineProcessException(e);
                }
                notes.put(file.getName(), fileContents);
            }
        }
        processXmlfile(patientJcas, annotationFile, notes);
    }

    private void processXmlfile(JCas patientJcas, File xmlFile, Map<String,String> notes) throws AnalysisEngineProcessException {
        // load the XML
        Element dataElem;
        try {
            dataElem = new SAXBuilder().build(xmlFile.toURI().toURL()).getRootElement();
        } catch (MalformedURLException e) {
            throw new AnalysisEngineProcessException(e);
        } catch (JDOMException e) {
            throw new AnalysisEngineProcessException(e);
        } catch (IOException e) {
            throw new AnalysisEngineProcessException(e);
        }
        HashMap<String,Integer> docLens = new HashMap<>();
        notes.forEach((k,v) -> docLens.put(k, v.length()));
        HashMap<String,JCas> docCases = new HashMap<>();
        HashMap<String,JCas> goldCases = new HashMap<>();
        for(String docName : notes.keySet()) {
            for (JCas docView : PatientViewUtil.getAllViews(patientJcas)) {
                if (docView.getViewName().contains(docName) && docView.getViewName().contains(CAS.NAME_DEFAULT_SOFA)) {
                    docCases.put(docName, docView);
                    break;
                }
            }
            for(JCas goldView : PatientViewUtil.getAllViews(patientJcas)){
                if(goldView.getViewName().contains(docName) && goldView.getViewName().contains(PatientViewUtil.GOLD_PREFIX)) {
                    goldCases.put(docName, goldView);
                }
            }
        }
        for (Element annotationsElem : dataElem.getChildren("annotations")) {
            // keep track of entity ids as we read entities so that we can find them from the map annotations later:
            Map<String, Annotation> idToAnnotation = Maps.newHashMap();

            for (Element entityElem : annotationsElem.getChildren("entity")) {
                String id = removeSingleChildText(entityElem, "id", null);
                String[] parts = id.split("@");
                String entNum = parts[0];   // note-specific id for this entity
                String entNoteName = parts[2];  // which note is this entity in: e.g., ID001_clinic_001
                String entAnnot = parts[3]; // should be "gold" for gold
                String entNote = notes.get(entNoteName);
                JCas entCas = goldCases.get(entNoteName);
                int docLen = entNote.length();
                Element spanElem = removeSingleChild(entityElem, "span", id);
                String type = removeSingleChildText(entityElem, "type", id);
                Element propertiesElem = removeSingleChild(entityElem, "properties", id);

                // UIMA doesn't support disjoint spans, so take the span enclosing
                // everything
                int begin = Integer.MAX_VALUE;
                int end = Integer.MIN_VALUE;
                for (String spanString : spanElem.getText().split(";")) {
                    String[] beginEndStrings = spanString.split(",");
                    if (beginEndStrings.length != 2) {
                        error("span not of the format 'number,number'", id);
                    }
                    int spanBegin = Integer.parseInt(beginEndStrings[0]);
                    int spanEnd = Integer.parseInt(beginEndStrings[1]);
                    if (spanBegin < begin && spanBegin >= 0) {
                        begin = spanBegin;
                    }
                    if (spanEnd > end && spanEnd <= docLen) {
                        end = spanEnd;
                    }
                }
                if (begin < 0 || end > docLen || end < 0) {
                    error("Illegal begin or end boundary", id);
                    continue;
                }

                Annotation annotation = null;
                if (type.equals("Markable")) {
                    while (end >= begin && (entNote.charAt(end - 1) == '\n' || entNote.charAt(end - 1) == '\r')) {
                        end--;
                    }
                    if (begin < 0 || end < 0) {
                        error("Illegal negative span", id);
                    }
                    Markable markable = new Markable(entCas, begin, end);
                    markable.addToIndexes();
                    annotation = markable;
                } else if (type.equals("EVENT")) {
                    while (end >= begin && (entNote.charAt(end - 1) == '\n' || entNote.charAt(end - 1) == '\r')) {
                        end--;
                    }
                    if (begin < 0 || end < 0) {
                        error("Illegal negative span", id);
                    }
                    EventMention event = new EventMention(entCas, begin, end);
                    event.addToIndexes();

                    // use the docCas here since we need the dependency parses.
                    Markable markable = new Markable(entCas, begin, end);
                    markable.addToIndexes();
                    annotation = markable;
                } else {
                    LOGGER.warn(String.format("Skipping entity type %s because the handler hasn't been written.", type));
                }
                if (annotation != null) idToAnnotation.put(id, annotation);
            }

            Map<Markable, CollectionTextRelation> markable2chain = new HashMap<>();
            List<List<Markable>> xDocLists = new ArrayList<>();
            for (Element relationElem : annotationsElem.getChildren("relation")) {
                String id = removeSingleChildText(relationElem, "id", null);
                String[] parts = id.split("@");
                String relNum = parts[0];   // note-specific id for this entity
                String relNoteName = parts[2];  // which note is this entity in: e.g., ID001_clinic_001
                String relAnnot = parts[3]; // should be "gold" for gold
                String relNote = notes.get(relNoteName);
                JCas relCas = goldCases.get(relNoteName);
                String type = removeSingleChildText(relationElem, "type", id);
                Element propertiesElem = removeSingleChild(relationElem, "properties", id);

                if (type.equals("Identical")) {
                    boolean crossDoc = false;
                    // Build list of Markables from FirstInstance and Coreferring_String annotations:
                    Set<String> chainNotes = new HashSet<>();
                    String mention = removeSingleChildText(propertiesElem, "FirstInstance", id);
                    if(mention != null) {
                        String[] mentionParts = mention.split("@");
                        String mentionNote = mentionParts[2];
                        chainNotes.add(mentionNote);
                    }
                    List<Markable> markables = new ArrayList<>();
                    Markable antecedent, anaphor;
                    Annotation chainElement = idToAnnotation.get(mention);

                    if (chainElement != null && chainElement instanceof Markable) {
                        antecedent = (Markable) chainElement;
                        markables.add(antecedent);
                    } else {
                        error("Null markable as FirstInstance", id);
                    }

                    List<Element> corefs = propertiesElem.getChildren("Coreferring_String");
                    for(Element coref : corefs){
                        mention = coref.getText();
                        if(mention != null && mention.length() > 0) {
                            String[] mentionParts = mention.split("@");
                            String mentionNote = mentionParts[2];
                            chainNotes.add(mentionNote);
                        }
                        chainElement = idToAnnotation.get(mention);
                        if(chainElement != null && chainElement instanceof Markable){
                            anaphor = (Markable) chainElement;
                            markables.add(anaphor);
                        }else{
                            error("Null markable as Coreferring_String", id);
                        }
                    }
                    if(chainNotes.size() > 1){
                        // if this list of markables has more than one note reference in it, save the  list of markables for later
                        xDocLists.add(markables);
                    }else {
                        // this is a within-document coref chain, so build it and add to indexes.
                        // Iterate over markable list creating binary coref relations:
                        for (int antInd = 0; antInd < markables.size() - 1; antInd++) {
                            int anaInd = antInd + 1;
                            // create set of binary relations from chain elements:
                            CoreferenceRelation pair = new CoreferenceRelation(relCas);
                            pair.setCategory("Identity");
                            RelationArgument arg1 = new RelationArgument(relCas);
                            arg1.setArgument(markables.get(antInd));
                            arg1.setRole("antecedent");
                            pair.setArg1(arg1);
                            RelationArgument arg2 = new RelationArgument(relCas);
                            arg2.setArgument(markables.get(anaInd));
                            arg2.setRole("anaphor");
                            pair.setArg2(arg2);
                            pair.addToIndexes();
                        }
                        // Create FSList from markable list and add to collection text relation:
                        if (markables.size() > 1) {
                            CollectionTextRelation chain = new CollectionTextRelation(relCas);
                            FSList list = ListFactory.buildList(relCas, markables);
                            list.addToIndexes();
                            chain.setMembers(list);
                            chain.addToIndexes();
                            System.out.println("Creating new chain in thyme anafora reader:");
                            System.out.print("W/in doc chain ");
                            for (Markable m : markables) {
                                System.out.print(" -> " + m.getCoveredText());
                                markable2chain.put(m, chain);
                            }
                            System.out.println();
                        } else {
                            error("Coreference chain of length <= 1", id);
                        }
                    }
                    propertiesElem.removeChildren("Coreferring_String");
                }else{
                    LOGGER.warn(String.format("This script cannot process relations of type %s yet.", type));
                }
            }
            // after processing all relations, go back to the queued cross-doc markable lists and use them to create cross-doc chains
            for(List<Markable> mlist : xDocLists){
                // the first markable is from the first document, we'll use that as the basis for the chain
                // and then add the markables from the subsequent doc chains to the end.
                List<Markable> ptMarkableList = new ArrayList<>();
                JCas chainCas = null;
                for(int i = 0; i < mlist.size(); i++) {
                    Markable child = mlist.get(i);
                    if( i == 0 ){
                        try {
                            chainCas = child.getCAS().getJCas();
                        }catch(CASException e){
                            throw new AnalysisEngineProcessException(e);
                        }
                    }
                    // dump the markables from this chain into a java list and remove the
                    // uima structures for the chain and its list of markables:
                    CollectionTextRelation inDocChain = markable2chain.get(child);
                    if(inDocChain != null) {
                        FSList members = inDocChain.getMembers();
                        ptMarkableList.addAll(JCasUtil.select(members, Markable.class));
                        members.removeFromIndexes();
                        inDocChain.removeFromIndexes();
                    }else{
                        // if we didn't see this markable in any other chains (i.e. it wasn't in a coref chain in its
                        // own document, then the markable2chain map will have an empty entry for that markable,
                        // and so we have to add the markable itself to the list of all markables in this chain.
                        ptMarkableList.add(child);
                    }
                }
                // put the big java list of markables into a coref chain structure and add to indexes:
                FSList newMembers = FSCollectionFactory.createFSList(chainCas, ptMarkableList);
                newMembers.addToIndexes();
                CollectionTextRelation ptChain = new CollectionTextRelation(chainCas);
                ptChain.setMembers(newMembers);
                ptChain.addToIndexes();
            }
        }
    }

    private static Element getSingleChild(Element elem, String elemName, String causeID) {
        List<Element> children = elem.getChildren(elemName);
        if (children.size() != 1) {
            error(String.format("not exactly one '%s' child", elemName), causeID);
        }
        return children.size() > 0 ? children.get(0) : null;
    }

    private static Element removeSingleChild(Element elem, String elemName, String causeID) {
        Element child = getSingleChild(elem, elemName, causeID);
        elem.removeChildren(elemName);
        return child;
    }

    private static String removeSingleChildText(Element elem, String elemName, String causeID) {
        Element child = getSingleChild(elem, elemName, causeID);
        String text = child.getText();
        if (text.isEmpty()) {
            error(String.format("an empty '%s' child", elemName), causeID);
            text = null;
        }
        elem.removeChildren(elemName);
        return text;
    }

    private static void error(String found, String id) {
        LOGGER.error(String.format("found %s in annotation with ID %s", found, id));
    }

    private static CollectionReader getCollectionReader(File xmlDirectory, List<Integer> patientSets ) throws Exception {
        List<File> collectedFiles = getFilesFor( xmlDirectory, patientSets );
        Collections.sort(collectedFiles);

        CounterMap<String> docCounts = new CounterMap<>();
        for(File f : collectedFiles){
            String ptidPrefix = null;
            ptidPrefix = f.getName().split("_")[0];
            docCounts.add(ptidPrefix);
        }
        docCounts.forEach( PatientNoteStore.getInstance()::setWantedDocCount );

        return UriCollectionReader.getCollectionReaderFromFiles( collectedFiles );
    }

    private static List<File> getFilesFor( File xmlDirectory, List<Integer> patientSets ){
        List<File> files = new ArrayList<>();
        Set<String> ids = new HashSet<>();
        for (Integer set : patientSets) {
            ids.add(String.format("ID%03d", set));
        }

        for (File dir : xmlDirectory.listFiles()) {
            // this gets us into train/dev/test subdirectory
            for (File ptDir : dir.listFiles()) {
                if (ids.contains(ptDir.getName())) {
                    for (File subDir : ptDir.listFiles()) {
                        if (subDir.isDirectory()) {
                            // for document 001 for patient 001, directory is ID001/ID001_clinic_001
                            // and text file within is ID001_clinic_001
                            files.add(new File(subDir, subDir.getName()));
                        }
                    }
                }
            }
        }
        return files;
    }

    public static void main(String[] args) throws Exception {
        List<Integer> ptNums = Arrays.asList(new Integer[]{1,2,3,9,10,11,16,17,18,19,25,26,27,32,33,34,35,40,41,42,43,48,50,51,56,57,58,65,67,74,81,146});

        File xmlDirectory = new File(args[0]);
        List<Integer> trainItems = THYMEData.getPatientSets( ptNums, THYMEData.TRAIN_REMAINDERS );

        AggregateBuilder builder = new AggregateBuilder();

        // Then run the preprocessing engine on all views
        builder.add(AnalysisEngineFactory.createEngineDescription( Evaluation_ImplBase.UriToDocumentTextAnnotatorCtakes.class ));
        builder.add( AnalysisEngineFactory.createEngineDescription(
                ViewCreatorAnnotator.class,
                ViewCreatorAnnotator.PARAM_VIEW_NAME,
                Evaluation_ImplBase.GOLD_VIEW_NAME ) );
        builder.add( AnalysisEngineFactory.createEngineDescription(
                ViewTextCopierAnnotator.class,
                ViewTextCopierAnnotator.PARAM_SOURCE_VIEW_NAME,
                CAS.NAME_DEFAULT_SOFA,
                ViewTextCopierAnnotator.PARAM_DESTINATION_VIEW_NAME,
                Evaluation_ImplBase.GOLD_VIEW_NAME ) );
        builder.add(AnalysisEngineFactory.createEngineDescription(EvaluationOfEventCoreference.DocumentIdFromURI.class));
        builder.add(AnalysisEngineFactory.createEngineDescription(PatientNoteCollector.class));
        builder.add(ThymeAnaforaCrossDocCorefXmlReader.getDescription(xmlDirectory.getAbsolutePath(), true ) );

        CollectionReader reader = getCollectionReader(xmlDirectory, trainItems);
        Map<Integer, Integer> chainSizes = new HashMap<>();

        for(JCasIterator casIter = new JCasIterator(reader, builder.createAggregate());  casIter.hasNext(); ){
            JCas ptCas = casIter.next();
            for(JCas docCas : ThymeCasOrderer.getOrderedCases(ptCas)) {
                for (CollectionTextRelation corefChain : JCasUtil.select(docCas, CollectionTextRelation.class)) {
                    Collection<Markable> members = JCasUtil.select(corefChain.getMembers(), Markable.class);
                    int chainSize = members.size();
                    if (!chainSizes.containsKey(chainSize)) {
                        chainSizes.put(chainSize, 0);
                    }
                    chainSizes.put(chainSize, chainSizes.get(chainSize) + 1);
                }
            }
        }
    }
}
