/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.ctakes.relationextractor.eval;

import com.google.common.collect.Sets;
import org.apache.ctakes.chunker.ae.Chunker;
import org.apache.ctakes.chunker.ae.adjuster.ChunkAdjuster;
import org.apache.ctakes.constituency.parser.ae.ConstituencyParser;
import org.apache.ctakes.context.tokenizer.ae.ContextDependentTokenizerAnnotator;
import org.apache.ctakes.core.ae.SentenceDetectorAnnotatorBIO;
import org.apache.ctakes.core.ae.SimpleSegmentAnnotator;
import org.apache.ctakes.core.ae.TokenizerAnnotatorPTB;
import org.apache.ctakes.core.util.doc.DocIdUtil;
import org.apache.ctakes.dependency.parser.ae.ClearNLPDependencyParserAE;
import org.apache.ctakes.dictionary.lookup2.ae.DefaultJCasTermAnnotator;
import org.apache.ctakes.lvg.ae.LvgAnnotator;
import org.apache.ctakes.postagger.POSTagger;
import org.apache.ctakes.typesystem.type.constants.CONST;
import org.apache.ctakes.typesystem.type.relation.LocationOfTextRelation;
import org.apache.ctakes.typesystem.type.relation.RelationArgument;
import org.apache.ctakes.typesystem.type.textsem.*;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.impl.XmiCasSerializer;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.component.ViewCreatorAnnotator;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.factory.AggregateBuilder;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.pipeline.JCasIterator;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.XMLSerializer;
import org.cleartk.util.ViewUriUtil;
import org.cleartk.util.ae.UriToDocumentTextAnnotator;
import org.cleartk.util.cr.UriCollectionReader;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.xml.sax.ContentHandler;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by tmill on 1/24/17.
 */
public class DeepPheXMI extends CorpusXMI {

    private static Pattern dirPatt = Pattern.compile("patient(\\d+)_report(\\d+)_(.*)");
    private static Matcher matcher = null;

    // These are the splits for the breast cancer patient set.
    // See here: https://healthnlp.hms.harvard.edu/cancer/wiki/index.php/Main_Page#DeepPhe_Gold_Set
    public final static Set<Integer> trainPatients = Sets.newHashSet(3, 11, 92, 93);
    public final static Set<Integer> devPatients = Sets.newHashSet(2, 21);
    public final static Set<Integer> testPatients = Sets.newHashSet(1, 16);

    // TODO - much of this can be encapsulated in the parent class and just pass it the description for the corpus reader.
    public static void generateXMI(File xmiDirectory, File anaforaInputDirectory) throws Exception {
        // if necessary, write the XMIs first
        if ( !xmiDirectory.exists() ) {
            xmiDirectory.mkdirs();
        }

        List<File> files = new ArrayList<>();
        files.addAll(getTrainTextFiles(anaforaInputDirectory));
        files.addAll(getDevTextFiles(anaforaInputDirectory));
        files.addAll(getTestTextFiles(anaforaInputDirectory));

        CollectionReader reader = UriCollectionReader.getCollectionReaderFromFiles(files);
        AggregateBuilder builder = new AggregateBuilder();
        builder.add( UriToDocumentTextAnnotator.getDescription() );

        builder.add( getDeepPhePreprocessingPipeline() );
        builder.add( AnalysisEngineFactory.createEngineDescription(
                ViewCreatorAnnotator.class,
                ViewCreatorAnnotator.PARAM_VIEW_NAME,
                GOLD_VIEW_NAME ) );
        builder.add( AnalysisEngineFactory.createEngineDescription( CopyDocumentTextToGoldView.class ) );
        builder.add(
                AnalysisEngineFactory.createEngineDescription( DocumentIDAnnotator.class ),
                CAS.NAME_DEFAULT_SOFA,
                GOLD_VIEW_NAME );
        builder.add(
                AnalysisEngineFactory.createEngineDescription( DeepPheAnaforaXMLReader.getDescription(anaforaInputDirectory) ),
                CAS.NAME_DEFAULT_SOFA,
                GOLD_VIEW_NAME );

        // write out an XMI for each file
        for (Iterator<JCas> casIter = new JCasIterator( reader, builder.createAggregate() ); casIter.hasNext(); ) {
            JCas jCas = casIter.next();
            JCas goldView = jCas.getView(GOLD_VIEW_NAME);
            String documentID = DocIdUtil.getDocumentID(goldView);
            if (documentID == null) {//|| documentID.equals( DocumentIDAnnotationUtil.NO_DOCUMENT_ID ) ) {
                throw new IllegalArgumentException("No documentID for CAS:\n" + jCas);
            }
            File outFile = toXMIFile(xmiDirectory, new File(documentID));
            FileOutputStream stream = new FileOutputStream(outFile);
            ContentHandler handler = new XMLSerializer(stream).getContentHandler();
            new XmiCasSerializer(jCas.getTypeSystem()).serialize(jCas.getCas(), handler);
            stream.close();
        }
    }

    public static List<File> getTrainTextFiles(File anaforaDirectory) {
        return getSetTextFiles(anaforaDirectory, trainPatients);
    }

    public static List<File> getDevTextFiles(File anaforaDirectory){
        return getSetTextFiles(anaforaDirectory, devPatients);
    }

    public static List<File> getTestTextFiles(File anaforaDirectory){
        return getSetTextFiles(anaforaDirectory, testPatients);
    }

    private static List<File> getSetTextFiles(File anaforaDirectory, Set<Integer> setToUse){
        List<File> files = new ArrayList<>();

        for(File file : anaforaDirectory.listFiles()){
            if(file.isDirectory()){
                // Anafora files are organized into directories per annotation file.
                matcher = dirPatt.matcher(file.getName());
                if(matcher.matches()){
                    int patientId = Integer.parseInt(matcher.group(1));
                    if(setToUse.contains(patientId)){
                        // The text file just replicates the last level of the directory path:
                        files.add(new File(file, file.getName()));
                    }
                }
            }
        }
        return files;

    }

    private static AnalysisEngineDescription getDeepPhePreprocessingPipeline() throws ResourceInitializationException, MalformedURLException {
        AggregateBuilder builder = new AggregateBuilder();
        builder.add(SimpleSegmentAnnotator.createAnnotatorDescription());
        builder.add(SentenceDetectorAnnotatorBIO.getDescription());
        builder.add(TokenizerAnnotatorPTB.createAnnotatorDescription());
        builder.add(LvgAnnotator.createAnnotatorDescription());
        builder.add(ContextDependentTokenizerAnnotator.createAnnotatorDescription());
        builder.add(POSTagger.createAnnotatorDescription());
        builder.add(ConstituencyParser.createAnnotatorDescription());
        builder.add(ClearNLPDependencyParserAE.createAnnotatorDescription());
        builder.add(Chunker.createAnnotatorDescription());
        builder.add(ChunkAdjuster.createAnnotatorDescription(new String[]{"NP", "NP"}, 1));
        builder.add(ChunkAdjuster.createAnnotatorDescription(new String[]{"NP", "PP", "NP"}, 2));
        builder.add(DefaultJCasTermAnnotator.createAnnotatorDescription());

        return builder.createAggregateDescription();
    }



    /**
     * Created by tmill on 2/7/17.
     */
    public static class DeepPheAnaforaXMLReader extends JCasAnnotator_ImplBase {
        private static Logger LOGGER = LogManager.getLogger(DeepPheAnaforaXMLReader.class);

        public static final String PARAM_ANAFORA_DIRECTORY = "anaforaDirectory";

        @ConfigurationParameter(
                name = PARAM_ANAFORA_DIRECTORY,
                description = "root directory of the Anafora-annotated files, with one subdirectory for "
                        + "each annotated file")
        private File anaforaDirectory;

        public static final String PARAM_ANAFORA_XML_SUFFIXES = "anaforaSuffixes";
        @ConfigurationParameter(
                name = PARAM_ANAFORA_XML_SUFFIXES,
                mandatory = false,
                description = "list of suffixes that might be added to a file name to identify the Anafora "
                        + "XML annotations file; only the first suffix corresponding to a file will be used")
        private String[] anaforaXMLSuffixes = new String[]{".UmlsDeepPhe.dave.completed.xml"};

        public static AnalysisEngineDescription getDescription() throws ResourceInitializationException {
            return AnalysisEngineFactory.createEngineDescription(DeepPheAnaforaXMLReader.class);
        }

        public static AnalysisEngineDescription getDescription(File anaforaDirectory)
                throws ResourceInitializationException {
            return AnalysisEngineFactory.createEngineDescription(
                    DeepPheAnaforaXMLReader.class,
                    DeepPheAnaforaXMLReader.PARAM_ANAFORA_DIRECTORY,
                    anaforaDirectory);
        }

        @Override
        public void process(JCas jCas) throws AnalysisEngineProcessException {
            File textFile = new File(ViewUriUtil.getURI(jCas));
            LOGGER.info("processing " + textFile);

            List<File> possibleXMLFiles = new ArrayList<>();
            for (String anaforaXMLSuffix : this.anaforaXMLSuffixes) {
                if (this.anaforaDirectory == null) {
                    possibleXMLFiles.add(new File(textFile + anaforaXMLSuffix));
                } else {
                    possibleXMLFiles.add(new File(textFile.getPath() + anaforaXMLSuffix));
                }
            }

            // find an Anafora XML file that actually exists
            File xmlFile = null;
            for (File possibleXMLFile : possibleXMLFiles) {
                if (possibleXMLFile.exists()) {
                    xmlFile = possibleXMLFile;
                    break;
                }
            }
            if (this.anaforaXMLSuffixes.length > 0 && xmlFile == null) {
                throw new IllegalArgumentException("no Anafora XML file found from " + possibleXMLFiles);
            }

            if (xmlFile != null) {
                processXmlFile(jCas, xmlFile);
            }

        }

        private static void processXmlFile(JCas jCas, File xmlFile) throws AnalysisEngineProcessException {
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

            List<String[]> delayedLocationRelations = new ArrayList<>();
            int docLen = jCas.getDocumentText().length();

            for (Element annotationsElem : dataElem.getChildren("annotations")) {

                Map<String, Annotation> idToAnnotation = new HashMap<>();
                for (Element entityElem : annotationsElem.getChildren("entity")) {
                    String id = removeSingleChildText(entityElem, "id", null);
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
                        if (spanBegin < begin) {
                            begin = spanBegin;
                        }
                        if (spanEnd > end) {
                            end = spanEnd;
                        }
                    }
                    if (begin < 0 || end >= docLen) {
                        error("Illegal begin or end boundary", id);
                        continue;
                    }

                    Annotation annotation = null;
                    if (type.equals("Disease_Disorder")) {
                        DiseaseDisorderMention dd = new DiseaseDisorderMention(jCas, begin, end);

                        String bodyLocation = removeSingleChildText(propertiesElem, "body_location", id);
                        if (bodyLocation != null && !bodyLocation.equals("")) {
                            delayedLocationRelations.add(new String[]{id, bodyLocation});
                        }
                        annotation = dd;
                    } else if (type.equals("Procedure")) {
                        ProcedureMention proc = new ProcedureMention(jCas, begin, end);
                        String bodyLocation = removeSingleChildText(propertiesElem, "body_location", id);
                        if (bodyLocation != null && !bodyLocation.equals("")) {
                            delayedLocationRelations.add(new String[]{id, bodyLocation});
                        }
                        annotation = proc;
                    } else if (type.equals("Sign_symptom")) {
                        SignSymptomMention ss = new SignSymptomMention(jCas, begin, end);
                        String bodyLocation = removeSingleChildText(propertiesElem, "body_location", id);
                        if (bodyLocation != null && !bodyLocation.equals("")) {
                            delayedLocationRelations.add(new String[]{id, bodyLocation});
                        }
                        annotation = ss;
                    } else if (type.equals("Metastasis")) {
                        EventMention meta = new EventMention(jCas, begin, end);
                        String bodyLocation = removeSingleChildText(propertiesElem, "body_location", id);
                        if (bodyLocation != null && !bodyLocation.equals("")) {
                            delayedLocationRelations.add(new String[]{id, bodyLocation});
                        }
                        annotation = meta;
                    } else if (type.equals("Anatomical_site")) {
                        AnatomicalSiteMention as = new AnatomicalSiteMention(jCas, begin, end);
                        String code = removeSingleChildText(propertiesElem, "associatedCode", id);
                        extractAttributeValues(propertiesElem, as, id);
                        annotation = as;
                    } else {
                        LOGGER.info("This entity type is not being extracted yet!");
                    }

                    // match the annotation to it's ID for later use
                    if (annotation != null) {
                        annotation.addToIndexes();
                        idToAnnotation.put(id, annotation);
                    }
                }

                for (String[] args : delayedLocationRelations) {
                    LocationOfTextRelation rel = new LocationOfTextRelation(jCas);
                    rel.setCategory("location_of");
                    RelationArgument arg1 = new RelationArgument(jCas);
                    arg1.setArgument(idToAnnotation.get(args[0]));
                    rel.setArg1(arg1);
                    RelationArgument arg2 = new RelationArgument(jCas);
                    arg2.setArgument(idToAnnotation.get(args[1]));
                    rel.setArg2(arg2);
                    rel.setDiscoveryTechnique(CONST.REL_DISCOVERY_TECH_GOLD_ANNOTATION);
                    rel.addToIndexes();
                }
            }
        }

        private static void extractAttributeValues(Element propertiesElem, IdentifiedAnnotation annotation, String id) {

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
    }
}
