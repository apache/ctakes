/**
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
package org.apache.ctakes.assertion.cr;

import org.apache.ctakes.assertion.util.AssertionConst;
import org.apache.ctakes.core.cc.FileTreeXmiWriter;
import org.apache.ctakes.core.config.ConfigParameterConstants;
import org.apache.ctakes.core.cr.FileTreeReader;
import org.apache.ctakes.core.knowtator.KnowtatorAnnotation;
import org.apache.ctakes.core.knowtator.KnowtatorXMLParser;
import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.core.util.SHARPKnowtatorXMLDefaults;
import org.apache.ctakes.typesystem.type.constants.CONST;
import org.apache.ctakes.typesystem.type.refsem.Date;
import org.apache.ctakes.typesystem.type.refsem.*;
import org.apache.ctakes.typesystem.type.relation.*;
import org.apache.ctakes.typesystem.type.structured.DocumentID;
import org.apache.ctakes.typesystem.type.textsem.*;
import org.apache.log4j.Logger;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.Feature;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.cas.TOP;
import org.apache.uima.jcas.tcas.Annotation;
import org.jdom2.JDOMException;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;


/**
 * assumes knowtator xml files are in "exported-xml" subdirectory w/ train/dev/test subsubdirs
 * and the original plaintext files are in "text" subdirectory w/ train/dev/test subsubdirs
 *
 */
@PipeBitInfo(
      name = "Knowtator XML Reader (MiPACQ)",
      description = "Reads annotations from MiPACQ schema Knowtator XML files in a directory.",
      role = PipeBitInfo.Role.SPECIAL,
      products = { PipeBitInfo.TypeProduct.IDENTIFIED_ANNOTATION, PipeBitInfo.TypeProduct.GENERIC_RELATION }
)
public class MiPACQKnowtatorXMLReader extends JCasAnnotator_ImplBase {
  static Logger LOGGER = Logger.getLogger(MiPACQKnowtatorXMLReader.class);
  
  public static final String PARAM_TEXT_DIRECTORY = "TextDirectory";
  @ConfigurationParameter(
      name = PARAM_TEXT_DIRECTORY,
      description = "directory containing the text files (if DocumentIDs are just filenames); "
          + "defaults to assuming that DocumentIDs are full file paths")
  private File textDirectory;
  
  public static final String PARAM_SET_DEFAULTS = "SetDefaults";
  @ConfigurationParameter(
      name = PARAM_SET_DEFAULTS,
      description = "whether or not to set default attribute values if no annotation is present",
      mandatory=false)
  private boolean setDefaults=true;

  private static final Map<String, String> SUBJECT_KNOWTATOR_TO_UIMA_MAP;
  static {
//    SUBJECT_KNOWTATOR_TO_UIMA_MAP = Maps.newHashMap();
    SUBJECT_KNOWTATOR_TO_UIMA_MAP = new HashMap<>();
    SUBJECT_KNOWTATOR_TO_UIMA_MAP.put("C0030705", CONST.ATTR_SUBJECT_PATIENT);
    SUBJECT_KNOWTATOR_TO_UIMA_MAP.put("patient", CONST.ATTR_SUBJECT_PATIENT);
    SUBJECT_KNOWTATOR_TO_UIMA_MAP.put("family_member", CONST.ATTR_SUBJECT_FAMILY_MEMBER);
    SUBJECT_KNOWTATOR_TO_UIMA_MAP.put("donor_family_member", CONST.ATTR_SUBJECT_DONOR_FAMILY_MEMBER);
    SUBJECT_KNOWTATOR_TO_UIMA_MAP.put("donor_other", CONST.ATTR_SUBJECT_DONOR_OTHER);
    SUBJECT_KNOWTATOR_TO_UIMA_MAP.put("other", CONST.ATTR_SUBJECT_OTHER);
  }
  
  /**
   * Get the URI that the text in this class was loaded from
   */
  protected URI getTextURI(JCas jCas) throws AnalysisEngineProcessException {

	  String textPath = JCasUtil.selectSingle(jCas, DocumentID.class).getDocumentID();
	  if (this.textDirectory != null) {
		  textPath = this.textDirectory + File.separator +  textPath;
	  }

	  URI uri;
	  try {
		  uri = new URI("file://"+textPath);
	  } catch (URISyntaxException e) {
		  throw new AnalysisEngineProcessException(e);
	  }

	  //LOGGER.info("textPath = " + textPath);
	  //LOGGER.info("uri = " + uri);
	  
	  
	  
	  
	  //File tmpFile = new File(textPath); // Note this does not work with something like "file:/C:/usr/data/MiPACQ/1/xml/0054074073-0.xml"
	  //LOGGER.info("tmpFile = " + tmpFile);
	  //URI answer = tmpFile.toURI();
	  //LOGGER.info("answer = " + answer);

	  return uri;

  }
  
  /**
   * Get the URI for the Knowtator XML file that should be loaded
   */
  protected URI getKnowtatorURI(JCas jCas) throws AnalysisEngineProcessException {

	  String replace = "/text/";
	  String path = this.getTextURI(jCas).toString();
	  //LOGGER.info("path = " + path);
	  String s = path.replace(replace,  "/exported-xml/");
	  String newPath = stripSuffix(s) + ".xml";
	  //LOGGER.info("newPath = " + newPath);
	  try {
//		  URI uri = new URI(textURI);
//		  String path = uri.getRawPath();
//		  LOGGER.info("path = " + path);
//		  File f = new File(path); // This does not work well if path is of form like "file:/BLAH"
//		  File dir = f.getParentFile();
//		  File parent = dir.getParentFile();
//		  File siblingDir = new File(parent, "exported-xml");
//		  String fn = f.getName();
//		  fn = stripSuffix(fn);
//		  fn = fn + ".source";
//		  String newPath = new File(siblingDir, fn).getAbsolutePath();
//		  LOGGER.info("newPath = " + newPath);
//		  URI newUri =new URI(newPath); 
//		  LOGGER.info("newUri = " + newUri);
//		  String[] textPath = this.getTextURI(jCas).toString().split("/");
//		  String lastDir = "";
//		  String file = "";
//		  if (textPath.length>1) {
//			  lastDir = textPath[textPath.length-2];
//			  file = textPath[textPath.length-1];
//		  }
//		  URI relUri = new URI("../../exported-xml/"+lastDir+"/"+file); // relative to text directory
//		  URI newUri = this.getTextURI(jCas).resolve(relUri);
       return new URI( newPath);
	  } catch (URISyntaxException e) {
		  throw new AnalysisEngineProcessException(e);
	  }

  }


  private static String stripSuffix(String fn) {
	int i = fn.lastIndexOf('.');
	if (i<0) return fn;
	if (i==0) return fn;
	return fn.substring(0, i);
}

/**
   * Returns the names of the annotators in the Knowtator files that represent the gold standard
   */
  protected static String[] getAnnotatorNames() {
    return new String[] { "cTAKES , Mayo Clinic", "CU annotator ,", "consensus set annotator team" , "cons annotator team", "cons team", "team" }; // these three are what are used by MiPACQ gold standard
  }
  

  private static List<String> getDiseaseDisorderKnowtatorClasses() {
	  return Collections.singletonList( "Disorders" );
  }
  
  
  private static List<String> getSignSymptomKnowtatorClasses() {
	  return Arrays.asList( "Sign_Symptom", "Finding" );
  }
  
  private static List<String> getProcedureKnowtatorClasses() {
	  return Arrays.asList( "Diagnostic_procedure",
                           "Laboratory_procedure",
                           "Procedures",
                           "Therapeutic_or_preventive_procedure",
                           "Intervention",
                           "Health_care_activity",
                           "Research_activity" );
  }
  
  private static List<String> getMedicationKnowtatorClasses() {
	  return Arrays.asList( "Chemicals_and_drugs", "Pharmacologic_substance" );
  }
  
  private static List<String> getAnatomyKnowtatorClasses() {
	  return Collections.singletonList( "Anatomy" );
  }
  
  @Override
  public void process(JCas jCas) throws AnalysisEngineProcessException {
    String text = jCas.getDocumentText();
    URI textURI = this.getTextURI(jCas);
    LOGGER.info("processing: " + textURI);

    // determine Knowtator XML file from the CAS
    URI knowtatorURI = this.getKnowtatorURI(jCas);
    if (!new File(knowtatorURI).exists()) {
      LOGGER.warn("near-FATAL: no such Knowtator XML file " + knowtatorURI);
      return;
    }

    // parse the Knowtator XML file into annotation objects
    KnowtatorXMLParser parser = new KnowtatorXMLParser(getAnnotatorNames());
    Collection<KnowtatorAnnotation> annotations;
    try {
      annotations = parser.parse(knowtatorURI);
    } catch (JDOMException e) {
      throw new AnalysisEngineProcessException(e);
    } catch (IOException e) {
      throw new AnalysisEngineProcessException(e);
    }

    
//    Set<String> nonAnnotationTypes = Sets.newHashSet(); // those expected not to have spans
    Set<String> nonAnnotationTypes = new HashSet<>(); // those expected not to have spans

    // create a CAS object for each annotation
    Map<String, TOP> idAnnotationMap = new HashMap<>();
    List<DelayedFeature> delayedFeatures = new ArrayList<>();
    LOGGER.info("Processing " + annotations.size() + " annotations for " + knowtatorURI);
    for (final KnowtatorAnnotation annotation : annotations) {

      // copy the slots so we can remove them as we use them
      Map<String, String> stringSlots = new HashMap<>(annotation.stringSlots);
      Map<String, Boolean> booleanSlots = new HashMap<>(annotation.booleanSlots);
      Map<String, KnowtatorAnnotation> annotationSlots = new HashMap<>(
          annotation.annotationSlots);
      KnowtatorAnnotation.Span coveringSpan = annotation.getCoveringSpan();
      
      if (nonAnnotationTypes.contains(annotation.type)) {
        if (coveringSpan.begin != Integer.MAX_VALUE || coveringSpan.end != Integer.MIN_VALUE) {
          LOGGER.warn(String.format(
              "expected no span but found %s for '%s' with id '%s' in %s'",
              annotation.spans,
              annotation.type,
              annotation.id,
              knowtatorURI));
        }
      } else {
        if (coveringSpan.begin == Integer.MAX_VALUE || coveringSpan.end == Integer.MIN_VALUE) {
          LOGGER.warn(String.format(
              "expected span but found none for '%s' with id '%s' in %s'",
              annotation.type,
              annotation.id,
              knowtatorURI));
        }
      }

      if (getAnatomyKnowtatorClasses().contains(annotation.type)) {
        AnatomicalSiteMention mention = new AnatomicalSiteMention(jCas, coveringSpan.begin, coveringSpan.end);
        addIdentifiedAnnotationFeatures(
            annotation,
            mention,
            jCas,
            CONST.NE_TYPE_ID_ANATOMICAL_SITE,
            stringSlots,
            booleanSlots,
            annotationSlots,
            idAnnotationMap,
            delayedFeatures);
        KnowtatorAnnotation bodyLaterality = annotationSlots.remove("body_laterality");
        delayedFeatures.add(new DelayedFeature(mention, "bodyLaterality", bodyLaterality));
        KnowtatorAnnotation bodySide = annotationSlots.remove("body_side");
        delayedFeatures.add(new DelayedFeature(mention, "bodySide", bodySide));

      } else if ("Clinical_attribute".equals(annotation.type)) {
        EventMention mention = new EventMention(jCas, coveringSpan.begin, coveringSpan.end);
        addIdentifiedAnnotationFeatures(
            annotation,
            mention,
            jCas,
            CONST.NE_TYPE_ID_CLINICAL_ATTRIBUTE,
            stringSlots,
            booleanSlots,
            annotationSlots,
            idAnnotationMap,
            delayedFeatures);

      } else if ("Devices".equals(annotation.type)) {
        EntityMention mention = new EntityMention(jCas, coveringSpan.begin, coveringSpan.end);
        addIdentifiedAnnotationFeatures(
            annotation,
            mention,
            jCas,
            CONST.NE_TYPE_ID_DEVICE,
            stringSlots,
            booleanSlots,
            annotationSlots,
            idAnnotationMap,
            delayedFeatures);

      } else if (getDiseaseDisorderKnowtatorClasses().contains(annotation.type)) {
        DiseaseDisorderMention mention = new DiseaseDisorderMention(jCas, coveringSpan.begin, coveringSpan.end);
        addIdentifiedAnnotationFeatures(
            annotation,
            mention,
            jCas,
            CONST.NE_TYPE_ID_DISORDER,
            stringSlots,
            booleanSlots,
            annotationSlots,
            idAnnotationMap,
            delayedFeatures);
        KnowtatorAnnotation alleviatingFactor = annotationSlots.remove("alleviating_factor");
        delayedFeatures.add(DelayedRelationFeature.forArg2(
            mention,
            "alleviatingFactor",
            alleviatingFactor,
            ManagesTreatsTextRelation.class,
            EventMention.class));
        KnowtatorAnnotation signOrSymptom = annotationSlots.remove("associated_sign_or_symptom");
        delayedFeatures.add(DelayedRelationFeature.forArg1(
            mention,
            "associatedSignSymptom",
            signOrSymptom,
            ManifestationOfTextRelation.class,
            EventMention.class));
        KnowtatorAnnotation bodyLaterality = annotationSlots.remove("body_laterality");
        delayedFeatures.add(new DelayedFeature(mention, "bodyLaterality", bodyLaterality));
        KnowtatorAnnotation bodyLocation = annotationSlots.remove("body_location");
        delayedFeatures.add(DelayedRelationFeature.forArg1(
            mention,
            "bodyLocation",
            bodyLocation,
            LocationOfTextRelation.class,
            AnatomicalSiteMention.class));
        KnowtatorAnnotation bodySide = annotationSlots.remove("body_side");
        delayedFeatures.add(new DelayedFeature(mention, "bodySide", bodySide));
        KnowtatorAnnotation course = annotationSlots.remove("course");
        delayedFeatures.add(DelayedRelationFeature.forArg1(
            mention,
            "course",
            course,
            DegreeOfTextRelation.class,
            CourseModifier.class));
        KnowtatorAnnotation exacerbatingFactor = annotationSlots.remove("exacerbating_factor");
        delayedFeatures.add(DelayedRelationFeature.forArg2(
            mention,
            "exacerbatingFactor",
            exacerbatingFactor,
            ComplicatesDisruptsTextRelation.class,
            EventMention.class));
        KnowtatorAnnotation severity = annotationSlots.remove("severity");
        delayedFeatures.add(DelayedRelationFeature.forArg1(
            mention,
            "severity",
            severity,
            DegreeOfTextRelation.class,
            SeverityModifier.class));

      } else if ("Lab".equals(annotation.type)) {
        LabMention mention = new LabMention(jCas, coveringSpan.begin, coveringSpan.end);
        addIdentifiedAnnotationFeatures(
            annotation,
            mention,
            jCas,
            CONST.NE_TYPE_ID_LAB,
            stringSlots,
            booleanSlots,
            annotationSlots,
            idAnnotationMap,
            delayedFeatures);
        KnowtatorAnnotation ordinal = annotationSlots.remove("ordinal_interpretation");
        delayedFeatures.add(DelayedRelationFeature.forArg1(
            mention,
            "ordinalInterpretation",
            ordinal,
            DegreeOfTextRelation.class,
            LabInterpretationModifier.class));
        KnowtatorAnnotation referenceRange = annotationSlots.remove("reference_range_narrative");
        delayedFeatures.add(new DelayedFeature(mention, "referenceRangeNarrative", referenceRange));
        KnowtatorAnnotation labValue = annotationSlots.remove("lab_value");
        delayedFeatures.add(DelayedRelationFeature.forArg1(
            mention,
            "labValue",
            labValue,
            ResultOfTextRelation.class,
            LabValueModifier.class));

      } else if (getMedicationKnowtatorClasses().contains(annotation.type)) {
        MedicationMention mention = new MedicationMention(jCas, coveringSpan.begin, coveringSpan.end);
        addIdentifiedAnnotationFeatures(
            annotation,
            mention,
            jCas,
            CONST.NE_TYPE_ID_DRUG,
            stringSlots,
            booleanSlots,
            annotationSlots,
            idAnnotationMap,
            delayedFeatures);
        KnowtatorAnnotation allergy = annotationSlots.remove("allergy_indicator");
        delayedFeatures.add(new DelayedFeature(mention, "medicationAllergy", allergy));
        KnowtatorAnnotation changeStatus = annotationSlots.remove("change_status_model");
        delayedFeatures.add(new DelayedFeature(mention, "medicationStatusChange", changeStatus));
        KnowtatorAnnotation dosage = annotationSlots.remove("dosage_model");
        delayedFeatures.add(new DelayedFeature(mention, "medicationDosage", dosage));
        KnowtatorAnnotation duration = annotationSlots.remove("duration_model");
        delayedFeatures.add(new DelayedFeature(mention, "medicationDuration", duration));
        KnowtatorAnnotation form = annotationSlots.remove("form_model");
        delayedFeatures.add(new DelayedFeature(mention, "medicationForm", form));
        KnowtatorAnnotation frequency = annotationSlots.remove("frequency_model");
        delayedFeatures.add(new DelayedFeature(mention, "medicationFrequency", frequency));
        KnowtatorAnnotation route = annotationSlots.remove("route_model");
        delayedFeatures.add(new DelayedFeature(mention, "medicationRoute", route));
        KnowtatorAnnotation startDate = annotationSlots.remove("start_date");
        delayedFeatures.add(new DelayedFeature(mention, "startDate", startDate));
        KnowtatorAnnotation strength = annotationSlots.remove("strength_model");
        delayedFeatures.add(new DelayedFeature(mention, "medicationStrength", strength));

      } else if ("Phenomena".equals(annotation.type)) {
        EventMention mention = new EventMention(jCas, coveringSpan.begin, coveringSpan.end);
        addIdentifiedAnnotationFeatures(
            annotation,
            mention,
            jCas,
            CONST.NE_TYPE_ID_PHENOMENA,
            stringSlots,
            booleanSlots,
            annotationSlots,
            idAnnotationMap,
            delayedFeatures);

      } else if (getProcedureKnowtatorClasses().contains(annotation.type)) {
        ProcedureMention mention = new ProcedureMention(jCas, coveringSpan.begin, coveringSpan.end);
        addIdentifiedAnnotationFeatures(
            annotation,
            mention,
            jCas,
            CONST.NE_TYPE_ID_PROCEDURE,
            stringSlots,
            booleanSlots,
            annotationSlots,
            idAnnotationMap,
            delayedFeatures);
        KnowtatorAnnotation bodyLaterality = annotationSlots.remove("body_laterality");
        delayedFeatures.add(new DelayedFeature(mention, "bodyLaterality", bodyLaterality));
        KnowtatorAnnotation bodyLocation = annotationSlots.remove("body_location");
        delayedFeatures.add(DelayedRelationFeature.forArg1(
            mention,
            "bodyLocation",
            bodyLocation,
            LocationOfTextRelation.class,
            AnatomicalSiteMention.class));
        KnowtatorAnnotation bodySide = annotationSlots.remove("body_side");
        delayedFeatures.add(new DelayedFeature(mention, "bodySide", bodySide));
        KnowtatorAnnotation device = annotationSlots.remove("device");
        delayedFeatures.add(new DelayedFeature(mention, "procedureDevice", device));
        KnowtatorAnnotation method = annotationSlots.remove("method");
        delayedFeatures.add(new DelayedFeature(mention, "method", method));

      } else if (getSignSymptomKnowtatorClasses().contains(annotation.type)) {
        SignSymptomMention mention = new SignSymptomMention(jCas, coveringSpan.begin, coveringSpan.end);
        addIdentifiedAnnotationFeatures(
            annotation,
            mention,
            jCas,
            CONST.NE_TYPE_ID_FINDING,
            stringSlots,
            booleanSlots,
            annotationSlots,
            idAnnotationMap,
            delayedFeatures);
        KnowtatorAnnotation alleviatingFactor = annotationSlots.remove("alleviating_factor");
        delayedFeatures.add(DelayedRelationFeature.forArg2(
            mention,
            "alleviatingFactor",
            alleviatingFactor,
            ManagesTreatsTextRelation.class,
            ProcedureMention.class));
        KnowtatorAnnotation bodyLaterality = annotationSlots.remove("body_laterality");
        delayedFeatures.add(new DelayedFeature(mention, "bodyLaterality", bodyLaterality));
        KnowtatorAnnotation bodyLocation = annotationSlots.remove("body_location");
        delayedFeatures.add(DelayedRelationFeature.forArg1(
            mention,
            "bodyLocation",
            bodyLocation,
            LocationOfTextRelation.class,
            AnatomicalSiteMention.class));
        KnowtatorAnnotation bodySide = annotationSlots.remove("body_side");
        delayedFeatures.add(new DelayedFeature(mention, "bodySide", bodySide));
        KnowtatorAnnotation course = annotationSlots.remove("course");
        delayedFeatures.add(DelayedRelationFeature.forArg1(
            mention,
            "course",
            course,
            DegreeOfTextRelation.class,
            CourseModifier.class));
        KnowtatorAnnotation exacerbatingFactor = annotationSlots.remove("exacerbating_factor");
        delayedFeatures.add(DelayedRelationFeature.forArg2(
            mention,
            "exacerbatingFactor",
            exacerbatingFactor,
            ComplicatesDisruptsTextRelation.class,
            EventMention.class));
        KnowtatorAnnotation severity = annotationSlots.remove("severity");
        delayedFeatures.add(DelayedRelationFeature.forArg1(
            mention,
            "severity",
            severity,
            DegreeOfTextRelation.class,
            SeverityModifier.class));

      } else if ("EVENT".equals(annotation.type)) {

        // collect the event properties (setting defaults as necessary)
        EventProperties eventProperties = new EventProperties(jCas);
        eventProperties.setCategory(stringSlots.remove("type"));
        if (eventProperties.getCategory() == null) {
          eventProperties.setCategory("N/A");
        }
        eventProperties.setContextualModality(stringSlots.remove("contextualmoduality"));
        if (eventProperties.getContextualModality() == null) {
          eventProperties.setContextualModality("ACTUAL");
        }
        eventProperties.setContextualAspect(stringSlots.remove("contextualaspect"));
        if (eventProperties.getContextualAspect() == null) {
          eventProperties.setContextualAspect("N/A");
        }
        eventProperties.setDegree(stringSlots.remove("degree"));
        if (eventProperties.getDegree() == null) {
          eventProperties.setDegree("N/A");
        }
        eventProperties.setDocTimeRel(stringSlots.remove("DocTimeRel"));
        if (eventProperties.getDocTimeRel() == null) {
          LOGGER.warn(String.format(
              "assuming docTimeRel=OVERLAP for annotation with id \"%s\"",
              annotation.id));
          eventProperties.setDocTimeRel("OVERLAP");
        }
        eventProperties.setPermanence(stringSlots.remove("permanence"));
        if (eventProperties.getPermanence() == null) {
          eventProperties.setPermanence("UNDETERMINED");
        }
        String polarityStr = stringSlots.remove("polarity");
        int polarity;
        if (polarityStr == null || polarityStr.equals("POS")) {
          polarity = CONST.NE_POLARITY_NEGATION_ABSENT;
        } else if (polarityStr.equals("NEG")) {
          polarity = CONST.NE_POLARITY_NEGATION_PRESENT;
        } else {
          throw new IllegalArgumentException("Invalid polarity: " + polarityStr);
        }
        eventProperties.setPolarity(polarity);

        // create the event object
        Event event = new Event(jCas);
        event.setConfidence(1.0f);
        event.setDiscoveryTechnique(CONST.NE_DISCOVERY_TECH_GOLD_ANNOTATION);

        // create the event mention
        EventMention eventMention = new EventMention(jCas, coveringSpan.begin, coveringSpan.end);
        eventMention.setConfidence(1.0f);
        eventMention.setDiscoveryTechnique(CONST.NE_DISCOVERY_TECH_GOLD_ANNOTATION);

        // add the links between event, mention and properties
        event.setProperties(eventProperties);
        event.setMentions(new FSArray(jCas, 1));
        event.setMentions(0, eventMention);
        eventMention.setEvent(event);

        // add the annotations to the indexes
        eventProperties.addToIndexes();
        event.addToIndexes();
        eventMention.addToIndexes();
        idAnnotationMap.put(annotation.id, eventMention);

      } else if ("DOCTIME".equals(annotation.type)) {
        TimeMention timeMention = new TimeMention(jCas, coveringSpan.begin, coveringSpan.end);
        timeMention.setTimeClass(annotation.type);
        timeMention.addToIndexes();
        idAnnotationMap.put(annotation.id, timeMention);

      } else if ("SECTIONTIME".equals(annotation.type)) {
        TimeMention timeMention = new TimeMention(jCas, coveringSpan.begin, coveringSpan.end);
        timeMention.setTimeClass(annotation.type);
        timeMention.addToIndexes();
        idAnnotationMap.put(annotation.id, timeMention);

      } else if ("TIMEX3".equals(annotation.type)) {
        String timexClass = stringSlots.remove("class");
        TimeMention timeMention = new TimeMention(jCas, coveringSpan.begin, coveringSpan.end);
        timeMention.setTimeClass(timexClass);
        timeMention.addToIndexes();
        idAnnotationMap.put(annotation.id, timeMention);
        
      } else if ("conditional_class".equals(annotation.type)) {
        Boolean value = booleanSlots.remove("conditional_normalization");
        ConditionalModifier modifier = new ConditionalModifier(jCas, coveringSpan.begin, coveringSpan.end);
        modifier.setConditional( value != null && value );
        modifier.addToIndexes();
        idAnnotationMap.put(annotation.id, modifier);

      } else if ("generic_class".equals(annotation.type)) {
        Boolean value = booleanSlots.remove("generic_normalization");
        GenericModifier modifier = new GenericModifier(jCas, coveringSpan.begin, coveringSpan.end);
        modifier.setGeneric( value != null && value );
        modifier.addToIndexes();
        idAnnotationMap.put(annotation.id, modifier);

      } else if ("negation_indicator_class".equals(annotation.type)) {
        String value = stringSlots.remove("negation_indicator_normalization");
        PolarityModifier modifier = new PolarityModifier(jCas, coveringSpan.begin, coveringSpan.end);
        if (value == null) {
          LOGGER.warn(String.format(
              "assuming NE_POLARITY_NEGATION_PRESENT for %s with id \"%s\"",
              format(modifier),
              annotation.id));
          modifier.setPolarity(CONST.NE_POLARITY_NEGATION_PRESENT);
        } else if (value.equals("negation_absent")) {
          modifier.setPolarity(CONST.NE_POLARITY_NEGATION_ABSENT);
        } else if (value.equals("negation_present")) {
          modifier.setPolarity(CONST.NE_POLARITY_NEGATION_PRESENT);
        } else {
          throw new UnsupportedOperationException("Invalid negation: " + value);
        }
        modifier.addToIndexes();
        idAnnotationMap.put(annotation.id, modifier);

      } else if ("uncertainty_indicator_class".equals(annotation.type)) {
        String value = stringSlots.remove("uncertainty_indicator_normalization");
        UncertaintyModifier modifier = new UncertaintyModifier(jCas, coveringSpan.begin, coveringSpan.end);
        if (value == null) {
          LOGGER.warn(String.format(
              "assuming NE_UNCERTAINTY_PRESENT for %s with id \"%s\"",
              format(modifier),
              annotation.id));
          modifier.setUncertainty(CONST.NE_UNCERTAINTY_PRESENT);
        } else if (value.equals("indicator_absent")) {
          modifier.setUncertainty(CONST.NE_UNCERTAINTY_ABSENT);
        } else if (value.equals("indicator_present")) {
          modifier.setUncertainty(CONST.NE_UNCERTAINTY_PRESENT);
        } else {
          throw new UnsupportedOperationException("Invalid uncertainty: " + value);
        }
        modifier.addToIndexes();
        idAnnotationMap.put(annotation.id, modifier);

      } else if ("Person".equals(annotation.type)) {
        String value = stringSlots.remove("subject_normalization_CU");
        String uimaValue = SUBJECT_KNOWTATOR_TO_UIMA_MAP.get(value);
        String code = stringSlots.remove("associatedCode");
        String uimaCode = SUBJECT_KNOWTATOR_TO_UIMA_MAP.get(code);
        if (value != null && uimaValue == null) {
          LOGGER.error(String.format(
              "unrecognized subject value \"%s\" for annotation with id \"%s\"",
              value,
              annotation.id));
        }
        if (code != null && uimaCode == null) {
          LOGGER.error(String.format(
              "unrecognized subject code \"%s\" for annotation with id \"%s\"",
              code,
              annotation.id));
        }
        if (uimaValue != null && uimaCode != null && !uimaValue.equals(uimaCode)) {
          LOGGER.error(String.format(
              "subject value \"%s\" and code \"%s\" are inconsistent for annotation with id \"%s\"",
              value,
              code,
              annotation.id));
        }
        String subject = uimaValue != null ? uimaValue : uimaCode;
        if (subject == null && this.setDefaults) {
          subject = SHARPKnowtatorXMLDefaults.getSubject();
        }
        SubjectModifier modifier = new SubjectModifier(jCas, coveringSpan.begin, coveringSpan.end);
        modifier.setSubject(subject);
        modifier.addToIndexes();
        idAnnotationMap.put(annotation.id, modifier);

      } else if ("historyOf_indicator_class".equals(annotation.type)) {
        String value = stringSlots.remove("historyOf_normalization");
        HistoryOfModifier modifier = new HistoryOfModifier(jCas, coveringSpan.begin, coveringSpan.end);
        if (null == value) {
          if (this.setDefaults) {
            modifier.setHistoryOf(SHARPKnowtatorXMLDefaults.getHistoryOf());
          }
        } else if ("historyOf_present".equals(value)) {
          modifier.setHistoryOf(CONST.NE_HISTORY_OF_PRESENT);
        } else if ("historyOf_absent".equals(value)) {
          modifier.setHistoryOf(CONST.NE_HISTORY_OF_ABSENT);
        } else {
          LOGGER.error(String.format(
              "unrecognized history-of value \"%s\" on annotation with id \"%s\"",
              value,
              annotation.id));
        }
        modifier.addToIndexes();
        idAnnotationMap.put(annotation.id, modifier);

      } else if ("distal_or_proximal".equals(annotation.type)) {
        String value = stringSlots.remove("distal_or_proximal_normalization");
        BodyLateralityModifier modifier = new BodyLateralityModifier(jCas, coveringSpan.begin, coveringSpan.end);
        BodyLaterality attribute = new BodyLaterality(jCas);
        if (value == null) {
          LOGGER.warn(String.format(
              "assuming \"%s\" for %s with id \"%s\"",
              CONST.ATTR_BODYLATERALITY_UNMARKED,
              format(modifier),
              annotation.id));
          value = CONST.ATTR_BODYLATERALITY_UNMARKED;
        } else if (!value.equals(CONST.ATTR_BODYLATERALITY_DISTAL) &&
            !value.equals(CONST.ATTR_BODYLATERALITY_PROXIMAL) &&
            !value.equals(CONST.ATTR_BODYLATERALITY_UNMARKED)) {
          throw new UnsupportedOperationException("Invalid BodyLaterality: " + value);
        }
        attribute.setValue(value);
        attribute.addToIndexes();
        modifier.setNormalizedForm(attribute);
        modifier.addToIndexes();
        idAnnotationMap.put(annotation.id, modifier);

      } else if ("superior_or_inferior".equals(annotation.type)) {
        String value = stringSlots.remove("superior_or_inferior_normalization");
        BodyLateralityModifier modifier = new BodyLateralityModifier(jCas, coveringSpan.begin, coveringSpan.end);
        BodyLaterality attribute = new BodyLaterality(jCas);
        if (value == null) {
          LOGGER.warn(String.format(
              "assuming \"%s\" for %s with id \"%s\"",
              CONST.ATTR_BODYLATERALITY_UNMARKED,
              format(modifier),
              annotation.id));
          value = CONST.ATTR_BODYLATERALITY_UNMARKED;
        } else if (!value.equals(CONST.ATTR_BODYLATERALITY_DISTAL) &&
            !value.equals(CONST.ATTR_BODYLATERALITY_SUPERIOR) &&
            !value.equals(CONST.ATTR_BODYLATERALITY_INFERIOR)) {
          throw new UnsupportedOperationException("Invalid BodyLaterality: " + value);
        }
        attribute.setValue(value);
        attribute.addToIndexes();
        modifier.setNormalizedForm(attribute);
        modifier.addToIndexes();
        idAnnotationMap.put(annotation.id, modifier);

      } else if ("medial_or_lateral".equals(annotation.type)) {
        String value = stringSlots.remove("medial_or_lateral_normalization");
        
        BodyLateralityModifier modifier = new BodyLateralityModifier(jCas, coveringSpan.begin, coveringSpan.end);
        BodyLaterality attribute = new BodyLaterality(jCas);
        if (value == null) {
          LOGGER.warn(String.format(
              "assuming \"%s\" for %s with id \"%s\"",
              CONST.ATTR_BODYLATERALITY_UNMARKED,
              format(modifier),
              annotation.id));
          value = CONST.ATTR_BODYLATERALITY_UNMARKED;
        } else if (!value.equals(CONST.ATTR_BODYLATERALITY_DISTAL) &&
            !value.equals(CONST.ATTR_BODYLATERALITY_MEDIAL) &&
            !value.equals(CONST.ATTR_BODYLATERALITY_LATERAL)) {
          throw new UnsupportedOperationException("Invalid BodyLaterality: " + value);
        }
        attribute.setValue(value);
        attribute.addToIndexes();
        modifier.setNormalizedForm(attribute);
        modifier.addToIndexes();
        idAnnotationMap.put(annotation.id, modifier);

      } else if ("dorsal_or_ventral".equals(annotation.type)) {
        String value = stringSlots.remove("dorsal_or_ventral_normalization");
        
        BodyLateralityModifier modifier = new BodyLateralityModifier(jCas, coveringSpan.begin, coveringSpan.end);
        BodyLaterality attribute = new BodyLaterality(jCas);
        if (value == null) {
          LOGGER.warn(String.format(
              "assuming \"%s\" for %s with id \"%s\"",
              CONST.ATTR_BODYLATERALITY_UNMARKED,
              format(modifier),
              annotation.id));
          value = CONST.ATTR_BODYLATERALITY_UNMARKED;
        } else if (!value.equals(CONST.ATTR_BODYLATERALITY_DISTAL) &&
            !value.equals(CONST.ATTR_BODYLATERALITY_DORSAL) &&
            !value.equals(CONST.ATTR_BODYLATERALITY_VENTRAL)) {
          throw new UnsupportedOperationException("Invalid BodyLaterality: " + value);
        }
        attribute.setValue(value);
        attribute.addToIndexes();
        modifier.setNormalizedForm(attribute);
        modifier.addToIndexes();
        idAnnotationMap.put(annotation.id, modifier);

      } else if ("body_side_class".equals(annotation.type)) {
        BodySide attribute = new BodySide(jCas);
        attribute.setValue(stringSlots.remove("body_side_normalization"));
        attribute.addToIndexes();
        BodySideModifier modifier = new BodySideModifier(jCas, coveringSpan.begin, coveringSpan.end);
        modifier.setNormalizedForm(attribute);
        modifier.addToIndexes();
        idAnnotationMap.put(annotation.id, modifier);

      } else if ("course_class".equals(annotation.type)) {
        Course attribute = new Course(jCas);
        attribute.setValue(stringSlots.remove("course_normalization"));
        attribute.addToIndexes();
        CourseModifier modifier = new CourseModifier(jCas, coveringSpan.begin, coveringSpan.end);
        modifier.setTypeID(CONST.MODIFIER_TYPE_ID_COURSE_CLASS);
        modifier.setNormalizedForm(attribute);
        modifier.addToIndexes();
        idAnnotationMap.put(annotation.id, modifier);

      } else if ("estimated_flag_indicator".equals(annotation.type)) {
        boolean value = booleanSlots.remove("estimated_normalization");
        LabEstimatedModifier modifier = new LabEstimatedModifier(jCas, coveringSpan.begin, coveringSpan.end);
        modifier.setIndicated(value);
        modifier.addToIndexes();
        idAnnotationMap.put(annotation.id, modifier);

      } else if ("lab_interpretation_indicator".equals(annotation.type)) {
        String value = stringSlots.remove("lab_interpretation_normalization");
        LabInterpretationModifier modifier = new LabInterpretationModifier(jCas, coveringSpan.begin, coveringSpan.end);
        modifier.setTypeID(CONST.MODIFIER_TYPE_ID_LAB_INTERPRETATION_INDICATOR);
        modifier.setValue(value);
        modifier.addToIndexes();
        idAnnotationMap.put(annotation.id, modifier);

      } else if ("reference_range".equals(annotation.type)) {
        LabReferenceRangeModifier modifier = new LabReferenceRangeModifier(jCas, coveringSpan.begin, coveringSpan.end);
        LabReferenceRange attribute = new LabReferenceRange(jCas);
        attribute.setValue(modifier.getCoveredText());
        attribute.addToIndexes();
        modifier.setNormalizedForm(attribute);
        modifier.addToIndexes();
        idAnnotationMap.put(annotation.id, modifier);

      } else if ("Value".equals(annotation.type)) {
        KnowtatorAnnotation unit = annotationSlots.remove("value_unit");
        KnowtatorAnnotation number = annotationSlots.remove("value_number");
        LabValue attribute = new LabValue(jCas);
        if (unit != null) {
          KnowtatorAnnotation.Span unitSpan = unit.getCoveringSpan();
          String unitString = text.substring(unitSpan.begin, unitSpan.end);
          attribute.setUnit(unitString);
        }
        if (number != null) {
          KnowtatorAnnotation.Span numberSpan = number.getCoveringSpan();
          String numberString = text.substring(numberSpan.begin, numberSpan.end);
          attribute.setNumber(numberString);
        }
        attribute.addToIndexes();
        LabValueModifier modifier = new LabValueModifier(jCas, coveringSpan.begin, coveringSpan.end);
        modifier.setNormalizedForm(attribute);
        modifier.addToIndexes();
        idAnnotationMap.put(annotation.id, modifier);

      } else if ("Value number".equals(annotation.type)) {
        // already handled in "Value" above

      } else if ("Value unit".equals(annotation.type)) {
        // already handled in "Value" above

      } else if ("allergy_indicator_class".equals(annotation.type)) {
        String value = stringSlots.remove("allergy_indicator_normalization");
        MedicationAllergyModifier modifier = new MedicationAllergyModifier(jCas, coveringSpan.begin, coveringSpan.end);
        if (null == value) {
          modifier.setIndicated(false);
        } else if ("indicator_present".equals(value)) {
          modifier.setIndicated(true);
        } else if ("indicator_absent".equals(value)) {
          modifier.setIndicated(false);
        } else {
          LOGGER.error(String.format(
              "unrecognized allergy-indicator value \"%s\" on annotation with id \"%s\"",
              value,
              annotation.id));
        }
        modifier.addToIndexes();
        idAnnotationMap.put(annotation.id, modifier);

      } else if ("Dosage".equals(annotation.type)) {
        String value = stringSlots.remove("dosage_values");
        MedicationDosage attribute = new MedicationDosage(jCas);
        attribute.setValue(value);
        attribute.addToIndexes();
        MedicationDosageModifier modifier = new MedicationDosageModifier(jCas, coveringSpan.begin, coveringSpan.end);
        modifier.setNormalizedForm(attribute);
        modifier.addToIndexes();
        idAnnotationMap.put(annotation.id, modifier);

      } else if ("Duration".equals(annotation.type)) {
        String value = stringSlots.remove("duration_values");
        MedicationDuration attribute = new MedicationDuration(jCas);
        attribute.setValue(value);
        attribute.addToIndexes();
        MedicationDurationModifier modifier = new MedicationDurationModifier(jCas, coveringSpan.begin, coveringSpan.end);
        modifier.setNormalizedForm(attribute);
        modifier.addToIndexes();
        idAnnotationMap.put(annotation.id, modifier);

      } else if ("Form".equals(annotation.type)) {
        String value = stringSlots.remove("form_values");
        MedicationForm attribute = new MedicationForm(jCas);
        attribute.setValue(value);
        attribute.addToIndexes();
        MedicationFormModifier modifier = new MedicationFormModifier(jCas, coveringSpan.begin, coveringSpan.end);
        modifier.setNormalizedForm(attribute);
        modifier.addToIndexes();
        idAnnotationMap.put(annotation.id, modifier);
        
      } else if ("Frequency".equals(annotation.type)) {
        KnowtatorAnnotation unit = annotationSlots.remove("frequency_unit");
        KnowtatorAnnotation number = annotationSlots.remove("frequency_number");
        MedicationFrequency attribute = new MedicationFrequency(jCas);
        if (unit != null) {
          String unitString = unit.stringSlots.get("frequency_unit_values");
          attribute.setUnit(unitString);
        }
        if (number != null) {
          String numberString = number.stringSlots.get("frequency_number_normalization");
          attribute.setNumber(numberString);
        }
        attribute.addToIndexes();
        MedicationFrequencyModifier modifier = new MedicationFrequencyModifier(jCas, coveringSpan.begin, coveringSpan.end);
        modifier.setNormalizedForm(attribute);
        modifier.addToIndexes();
        idAnnotationMap.put(annotation.id, modifier);

      } else if ("Frequency number".equals(annotation.type)) {
        // already handled in "Frequency" above
        stringSlots.remove("frequency_number_normalization");

      } else if ("Frequency unit".equals(annotation.type)) {
        // already handled in "Frequency" above
        stringSlots.remove("frequency_unit_values");

      } else if ("Route".equals(annotation.type)) {
        String value = stringSlots.remove("route_values");
        MedicationRoute attribute = new MedicationRoute(jCas);
        attribute.setValue(value);
        attribute.addToIndexes();
        MedicationRouteModifier modifier = new MedicationRouteModifier(jCas, coveringSpan.begin, coveringSpan.end);
        modifier.setNormalizedForm(attribute);
        modifier.addToIndexes();
        idAnnotationMap.put(annotation.id, modifier);
        
      } else if ("Status change".equals(annotation.type)) {
        String value = stringSlots.remove("change_status_value");
        MedicationStatusChange attribute = new MedicationStatusChange(jCas);
        attribute.setValue(value);
        attribute.addToIndexes();
        MedicationStatusChangeModifier modifier = new MedicationStatusChangeModifier(jCas, coveringSpan.begin, coveringSpan.end);
        modifier.setNormalizedForm(attribute);
        modifier.addToIndexes();
        idAnnotationMap.put(annotation.id, modifier);

      } else if ("Strength".equals(annotation.type)) {
        KnowtatorAnnotation unit = annotationSlots.remove("strength_unit");
        KnowtatorAnnotation number = annotationSlots.remove("strength_number");
        MedicationStrength attribute = new MedicationStrength(jCas);
        if (unit != null) {
          KnowtatorAnnotation.Span unitSpan = unit.getCoveringSpan();
          String unitString = text.substring(unitSpan.begin, unitSpan.end);
          attribute.setUnit(unitString);
        }
        if (number != null) {
          KnowtatorAnnotation.Span numberSpan = number.getCoveringSpan();
          String numberString = text.substring(numberSpan.begin, numberSpan.end);
          attribute.setNumber(numberString);
        }
        attribute.addToIndexes();
        MedicationStrengthModifier modifier = new MedicationStrengthModifier(jCas, coveringSpan.begin, coveringSpan.end);
        modifier.setNormalizedForm(attribute);
        modifier.addToIndexes();
        idAnnotationMap.put(annotation.id, modifier);

      } else if ("Strength number".equals(annotation.type)) {
        // already handled in "Strength" above

      } else if ("Strength unit".equals(annotation.type)) {
        // already handled in "Strength" above

      } else if ("device_class".equals(annotation.type)) {
        String code = stringSlots.remove("associatedCode");
        ProcedureDevice attribute = new ProcedureDevice(jCas);
        attribute.setValue(code);
        ProcedureDeviceModifier modifier = new ProcedureDeviceModifier(jCas, coveringSpan.begin, coveringSpan.end);
        modifier.setNormalizedForm(attribute);
        modifier.addToIndexes();
        idAnnotationMap.put(annotation.id, modifier);

      } else if ("method_class".equals(annotation.type)) {
        String code = stringSlots.remove("associatedCode");
        ProcedureMethod attribute = new ProcedureMethod(jCas);
        attribute.setValue(code);
        ProcedureMethodModifier modifier = new ProcedureMethodModifier(jCas, coveringSpan.begin, coveringSpan.end);
        modifier.setNormalizedForm(attribute);
        modifier.addToIndexes();
        idAnnotationMap.put(annotation.id, modifier);

      } else if ("severity_class".equals(annotation.type)) {
        Severity attribute = new Severity(jCas);
        attribute.setValue(stringSlots.remove("severity_normalization"));
        attribute.addToIndexes();
        SeverityModifier modifier = new SeverityModifier(jCas, coveringSpan.begin, coveringSpan.end);
        modifier.setTypeID(CONST.MODIFIER_TYPE_ID_SEVERITY_CLASS);
        modifier.setNormalizedForm(attribute);
        modifier.addToIndexes();
        idAnnotationMap.put(annotation.id, modifier);

      } else if ("Date".equals(annotation.type)) {
        String month = stringSlots.remove("month");
        String day = stringSlots.remove("day");
        Date date = new Date(jCas);
        date.setMonth(month);
        date.setDay(day);
        date.addToIndexes();
        TimeMention mention = new TimeMention(jCas, coveringSpan.begin, coveringSpan.end);
        mention.setDate(date);
        mention.addToIndexes();
        idAnnotationMap.put(annotation.id, mention);

      } else {
        LOGGER.info(String.format(
            "unrecognized type '%s' for annotation with id \"%s\"",
            annotation.type,
            annotation.id));
      }

      // make sure all slots have been consumed
      Map<String, Set<String>> slotGroups = new HashMap<>();
      slotGroups.put("stringSlots", stringSlots.keySet());
      slotGroups.put("booleanSlots", booleanSlots.keySet());
      slotGroups.put("annotationSlots", annotationSlots.keySet());
      for (Map.Entry<String, Set<String>> entry : slotGroups.entrySet()) {
        Set<String> remainingSlots = entry.getValue();
        if (!remainingSlots.isEmpty()) {
          Exception e = new UnsupportedOperationException(String.format(
              "%s has unprocessed %s %s in %s",
              annotation.type,
              entry.getKey(),
              remainingSlots,
              knowtatorURI));
          LOGGER.warn(e.getLocalizedMessage());
        }
      }
    }


    // all mentions should be added, so add features that required other annotations
    for (DelayedFeature delayedFeature : delayedFeatures) {
      delayedFeature.setValueFrom(idAnnotationMap);
    }
  }
  
  static String format(Annotation ann) {
    String result;
    if (ann.getEnd() == Integer.MIN_VALUE || ann.getBegin() == Integer.MAX_VALUE) {
      result = "<no-spanned-text>";
    } else {
      result = String.format("\"%s\"[%d,%d]", ann.getCoveredText(), ann.getBegin(), ann.getEnd());
    }
    return String.format("%s(%s)", ann.getClass().getSimpleName(), result);
  }
  
  private static void addIdentifiedAnnotationFeatures(
      KnowtatorAnnotation annotation,
      final IdentifiedAnnotation mention,
      JCas jCas,
      int typeID,
      Map<String, String> stringSlots,
      Map<String, Boolean> booleanSlots,
      Map<String, KnowtatorAnnotation> annotationSlots,
      Map<String, TOP> idAnnotationMap,
      List<DelayedFeature> delayedFeatures) {
    mention.setTypeID(typeID);
    mention.setConfidence(1.0f);
    mention.setDiscoveryTechnique(CONST.NE_DISCOVERY_TECH_GOLD_ANNOTATION);

    // convert negation to an integer
    Boolean negation = booleanSlots.remove("negation"); 
    mention.setPolarity(negation == null
        ? CONST.NE_POLARITY_NEGATION_ABSENT
        : negation ? CONST.NE_POLARITY_NEGATION_PRESENT : CONST.NE_POLARITY_NEGATION_ABSENT );

    // add features for conditional, generic, etc.
    KnowtatorAnnotation conditional = annotationSlots.remove("conditional_CU");
    delayedFeatures.add(new DelayedFeatureFromFeature(mention, "conditional", conditional));
    KnowtatorAnnotation generic = annotationSlots.remove("generic_CU");
    delayedFeatures.add(new DelayedFeatureFromFeature(mention, "generic", generic));
    KnowtatorAnnotation historyOf = annotationSlots.remove("historyOf_CU");
    delayedFeatures.add(new DelayedFeatureFromFeature(mention, "historyOf", historyOf));
    KnowtatorAnnotation negationIndicator = annotationSlots.remove("negation_indicator_CU");
    delayedFeatures.add(new DelayedFeatureFromFeature(mention, "polarity", negationIndicator));
    KnowtatorAnnotation subject = annotationSlots.remove("subject_CU");
    delayedFeatures.add(new DelayedFeatureFromFeature(mention, "subject", subject) {
      @Override
      public void setValueFrom(Map<String, ? extends TOP> annotationMap) {
        super.setValueFrom(annotationMap);
        if (mention.getSubject() == null) {
          mention.setSubject(CONST.ATTR_SUBJECT_PATIENT);
        }
      }
    });
    KnowtatorAnnotation uncertainty = annotationSlots.remove("uncertainty_indicator_CU");
    delayedFeatures.add(new DelayedFeatureFromFeature(mention, "uncertainty", uncertainty));

    // convert status as necessary
    String status = stringSlots.remove("Status");
    if (status != null) {
      switch ( status ) {
        case "HistoryOf":
          mention.setHistoryOf( CONST.NE_HISTORY_OF_PRESENT );
          break;
        case "FamilyHistoryOf":
          mention.setHistoryOf( CONST.NE_HISTORY_OF_PRESENT );
          mention.setSubject( CONST.ATTR_SUBJECT_FAMILY_MEMBER );
          break;
        case "Possible":
          mention.setUncertainty( CONST.NE_CERTAINTY_NEGATED );
          break;
        default:
          throw new UnsupportedOperationException( "Unknown status: " + status );
      }
    }

    // convert code to ontology concept or CUI
    String code = stringSlots.remove("AssociateCode");
    if (code == null) {
      code = stringSlots.remove("associatedCode");
    }
    OntologyConcept ontologyConcept;
    if (mention.getTypeID() == CONST.NE_TYPE_ID_DRUG) {
      ontologyConcept = new OntologyConcept(jCas);
      ontologyConcept.setCode(code);
    } else {
      UmlsConcept umlsConcept = new UmlsConcept(jCas);
      umlsConcept.setCui(code);
      ontologyConcept = umlsConcept;
    }
    ontologyConcept.addToIndexes();
    mention.setOntologyConceptArr(new FSArray(jCas, 1));
    mention.setOntologyConceptArr(0, ontologyConcept);

    // add entity mention to CAS
    mention.addToIndexes();
    idAnnotationMap.put(annotation.id, mention);
  }

  
  private static class DelayedFeature {
    protected Annotation annotation;
    protected String featureName;
    protected Feature feature;
    protected KnowtatorAnnotation featureValue;

    public DelayedFeature(
        Annotation annotation,
        String featureName,
        KnowtatorAnnotation featureValue) {
      this.annotation = annotation;
      this.featureName = featureName;
      this.feature = this.getFeature(this.annotation);
      this.featureValue = featureValue;
    }

    public void setValueFrom(Map<String, ? extends TOP> idAnnotationMap) {
      if (this.featureValue != null) {
        TOP valueAnnotation = idAnnotationMap.get(this.featureValue.id);
        if (valueAnnotation == null) {
          LOGGER.warn(String.format(
              "unable to set feature; found no annotation for %s",
              this.featureValue.id));
        } else {
          this.setValue(valueAnnotation);
        }
      }
    }
    
    protected void setValue(TOP value) {
      this.annotation.setFeatureValue(this.feature, value);
    }
    
    protected Feature getFeature(TOP top) {
      Feature result = top.getType().getFeatureByBaseName(this.featureName);
      if (result == null) {
        throw new IllegalArgumentException(String.format(
            "no feature %s on %s",
            featureName,
            top.getClass()));
      }
      return result;
    }
  }
  
  private static class DelayedFeatureFromFeature extends DelayedFeature {

    public DelayedFeatureFromFeature(
        Annotation annotation,
        String featureName,
        KnowtatorAnnotation featureValue) {
      super(annotation, featureName, featureValue);
    }
    
    @Override
    protected void setValue(TOP value) {
      String featureValueToCopy = value.getFeatureValueAsString(this.getFeature(value));
      this.annotation.setFeatureValueFromString(this.feature, featureValueToCopy);
    }
  }
  
  private static class DelayedRelationFeature extends DelayedFeature {
    
    private final Class<? extends BinaryTextRelation> relationClass;
    private final Annotation arg1;
    private final Annotation arg2;
    private final Class<? extends Annotation> arg1Class;
    private final Class<? extends Annotation> arg2Class;
    
    public DelayedRelationFeature(
        Annotation annotation,
        String featureName,
        KnowtatorAnnotation featureValue,
        Class<? extends BinaryTextRelation> relationClass,
        Annotation arg1,
        Class<? extends Annotation> arg1Class,
        Annotation arg2,
        Class<? extends Annotation> arg2Class) {
      super(annotation, featureName, featureValue);
      this.relationClass = relationClass;
      this.arg1 = arg1;
      this.arg1Class = arg1Class;
      this.arg2 = arg2;
      this.arg2Class = arg2Class;
    }

    public static DelayedRelationFeature forArg1(
        Annotation arg1,
        String featureName,
        KnowtatorAnnotation featureValue,
        Class<? extends BinaryTextRelation> relationClass,
        Class<? extends Annotation> arg2Class) {
      return new DelayedRelationFeature(
          arg1,
          featureName,
          featureValue,
          relationClass,
          arg1,
          arg1.getClass(),
          null,
          arg2Class);
    }

    public static DelayedRelationFeature forArg2(
        Annotation arg2,
        String featureName,
        KnowtatorAnnotation featureValue,
        Class<? extends BinaryTextRelation> relationClass,
        Class<? extends Annotation> arg1Class) {
      return new DelayedRelationFeature(
          arg2,
          featureName,
          featureValue,
          relationClass,
          null,
          arg1Class,
          arg2,
          arg2.getClass());
    }
    
    @Override
    protected void setValue(TOP value) {
      BinaryTextRelation relation = (BinaryTextRelation) value;
      String message = null;
      if (!this.relationClass.isInstance(relation)) {
        message = "wrong relation type";
      } else if (this.arg1 != null && relation.getArg1().getArgument() != this.arg1) {
        message = "wrong relation arg1";
      } else if (this.arg2 != null && relation.getArg2().getArgument() != this.arg2) {
        message = "wrong relation arg2";
      } else if (!this.arg1Class.isInstance(relation.getArg1().getArgument())) {
        message = "wrong relation arg1 type";
      } else if (!this.arg2Class.isInstance(relation.getArg2().getArgument())) {
        message = "wrong relation arg2 type";
      }
      if (message != null) {
        LOGGER.warn(String.format(
            "%s: expected %s feature of %s to be %s(%s, %s) but found %s[%s](%s, %s) with id \"%s\"",
            message,
            this.featureName,
            format(this.annotation),
            this.relationClass.getSimpleName(),
            this.arg1 == null ? String.format("%s(...)", this.arg1Class.getSimpleName()) : format(this.arg1),
            this.arg2 == null ? String.format("%s(...)", this.arg2Class.getSimpleName()) : format(this.arg2),
            relation.getClass().getSimpleName(),
            relation.getCategory(),
            format(relation.getArg1().getArgument()),
            format(relation.getArg2().getArgument()),
            this.featureValue.id));
      } else {
        super.setValue(value);
      }
    }
  }
  
  /**
   * This main method is only for testing purposes. It runs the reader on Knowtator directories.
   * 	args[0] = "/usr/data/MiPACQ/copies-of-just-clinical-knowtator-xml-and-text/";
   * should have a child directory called "text"
   * should have a child directory called "exported-xml"
   * files in knowtator xml directory should have files that end with .xml
   */
  public static void main(String[] args) throws Exception {

	  String [] dirs;
	  if (args.length != 0) {
		  dirs = args;
	  } else {
		  try {
			  throw new IllegalArgumentException(String.format(
					  "usage: java %s path/to/Knowtator/parent [path/to/Knowtator/parent  ...]",
					  MiPACQKnowtatorXMLReader.class.getName()));
		  } catch (IllegalArgumentException e) {
			  e.printStackTrace();
		  }
		  Exception e = new RuntimeException("Going to continue with default values");
		  e.printStackTrace();
		  dirs = new String[1];
		  dirs[0] = AssertionConst.MiPACQ_CORPUS;
	  }

	  AnalysisEngine mipacqReader = AnalysisEngineFactory.createEngine(MiPACQKnowtatorXMLReader.class);

	  AnalysisEngine xWriter = AnalysisEngineFactory.createEngine(
//			  XmiWriterCasConsumerCtakes.class,
//			  XmiWriterCasConsumerCtakes.PARAM_OUTPUTDIR,
           FileTreeXmiWriter.class,
           ConfigParameterConstants.PARAM_OUTPUTDIR,
			  "/usr/data/MiPACQ/cTAKES-xmi/");

	  int n = dirs.length;
	  LOGGER.info("Processing " + n + " directories of knowtator xml files.");
     final FileTreeReader reader = new FileTreeReader();
	  for (String knowtatorTextDirectoryPath : dirs) {
		  //File knowtatorXmlDirectory = new File(knowtatorTextDirectoryPath, "exported-xml");
		  File knowtatorTextSourceDirectory = new File(knowtatorTextDirectoryPath, "text");
		  File [] knowtatorTextSourceFiles = knowtatorTextSourceDirectory.listFiles();
       assert knowtatorTextSourceFiles != null;
       int i = knowtatorTextSourceFiles.length;
		  LOGGER.info("Processing " + i + " knowtator text source files for this directory.");
		  for (File textFile : knowtatorTextSourceFiles) {
			  JCas jCas = mipacqReader.newJCas();
//			  jCas.setDocumentText(Files.toString(textFile, Charsets.US_ASCII));
          jCas.setDocumentText( reader.readFile( textFile ) );
          DocumentID documentID = new DocumentID(jCas);
			  documentID.setDocumentID(textFile.toURI().toString());
			  documentID.addToIndexes();
			  mipacqReader.process(jCas);
			  documentID.setDocumentID(textFile.getName());
			  xWriter.process(jCas);
		  }
	  }

  }
}
