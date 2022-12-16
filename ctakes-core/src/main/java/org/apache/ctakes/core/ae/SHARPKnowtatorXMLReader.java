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
package org.apache.ctakes.core.ae;

import com.google.common.base.Charsets;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.io.Files;
import org.apache.ctakes.core.knowtator.KnowtatorAnnotation;
import org.apache.ctakes.core.knowtator.KnowtatorXMLParser;
import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.core.util.SHARPKnowtatorXMLDefaults;
import org.apache.ctakes.typesystem.type.constants.CONST;
import org.apache.ctakes.typesystem.type.refsem.*;
import org.apache.ctakes.typesystem.type.refsem.Date;
import org.apache.ctakes.typesystem.type.relation.*;
import org.apache.ctakes.typesystem.type.structured.DocumentID;
import org.apache.ctakes.typesystem.type.textsem.*;
import org.apache.log4j.Logger;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.cas.Feature;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.util.CasIOUtil;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.cas.TOP;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.util.UriUtils;
import org.jdom2.JDOMException;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

@PipeBitInfo(
      name = "Knowtator XML Reader (SHARP)",
      description = "Reads annotations from SHARP schema Knowtator XML files in a directory.",
      role = PipeBitInfo.Role.SPECIAL,
      products = { PipeBitInfo.TypeProduct.IDENTIFIED_ANNOTATION, PipeBitInfo.TypeProduct.EVENT,
                   PipeBitInfo.TypeProduct.TIMEX, PipeBitInfo.TypeProduct.LOCATION_RELATION,
                   PipeBitInfo.TypeProduct.DEGREE_RELATION, PipeBitInfo.TypeProduct.TEMPORAL_RELATION }
)
public class SHARPKnowtatorXMLReader extends JCasAnnotator_ImplBase {
  static Logger LOGGER = Logger.getLogger(SHARPKnowtatorXMLReader.class);
  
  public static final String PARAM_TEXT_DIRECTORY = "TextDirectory";
  @ConfigurationParameter(
      name = PARAM_TEXT_DIRECTORY,
      mandatory = false,
      description = "directory containing the text files (if DocumentIDs are just filenames); "
          + "defaults to assuming that DocumentIDs are full file paths")
  private File textDirectory=null;
  
  public static final String PARAM_SET_DEFAULTS = "SetDefaults";
  @ConfigurationParameter(
      name = PARAM_SET_DEFAULTS,
      description = "whether or not to set default attribute values if no annotation is present")
  private boolean setDefaults;

  private static final Map<String, String> SUBJECT_KNOWTATOR_TO_UIMA_MAP;
  static {
    SUBJECT_KNOWTATOR_TO_UIMA_MAP = Maps.newHashMap();
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
  protected URI getTextURI(JCas jCas) {
    String textPath = JCasUtil.selectSingle(jCas, DocumentID.class).getDocumentID();
    if (this.textDirectory != null) {
      textPath = this.textDirectory + File.separator + textPath;
    }

    File tmpFile = new File(textPath);
    URI answer = tmpFile.toURI();
    return answer;
  }
  
  /**
   * Get the URI for the Knowtator XML file that should be loaded
   * @throws URISyntaxException 
   */
  protected URI getKnowtatorURI(JCas jCas) {
    File textURI = new File(this.getTextURI(jCas));
    String filename = textURI.getName().replace(".txt", "");
    File xmlPath = new File(textURI.getParentFile().getParentFile().getParentFile().getParentFile().getParentFile(), "by-document/" + filename + "/" + filename + ".umls.knowtator.xml");
    return UriUtils.create("file:" + xmlPath.getAbsolutePath());
  }

  /**
   * Returns the names of the annotators in the Knowtator files that represent the gold standard
   */
  protected String[] getAnnotatorNames() {
    return new String[] { "consensus set annotator team" };
  }

  @Override
  public void process(JCas jCas) throws AnalysisEngineProcessException {
    String text = jCas.getDocumentText();
    URI textURI = this.getTextURI(jCas);
    LOGGER.info("processing " + textURI);

    // determine Knowtator XML file from the CAS
    URI knowtatorURI = this.getKnowtatorURI(jCas);
    if (!new File(knowtatorURI).exists()) {
      LOGGER.fatal("no such Knowtator XML file " + knowtatorURI);
      return;
    }

    // parse the Knowtator XML file into annotation objects
    KnowtatorXMLParser parser = new KnowtatorXMLParser(this.getAnnotatorNames());
    Collection<KnowtatorAnnotation> annotations;
    try {
      annotations = parser.parse(knowtatorURI);
    } catch (JDOMException e) {
      throw new AnalysisEngineProcessException(e);
    } catch (IOException e) {
      throw new AnalysisEngineProcessException(e);
    }

    // the relation types
    Set<String> entityRelationTypes = new HashSet<String>();
    entityRelationTypes.add("affects");
    entityRelationTypes.add("causes/brings_about");
    entityRelationTypes.add("complicates/disrupts");
    entityRelationTypes.add("contraindicates");
    entityRelationTypes.add("degree_of");
    entityRelationTypes.add("diagnoses");
    entityRelationTypes.add("indicates");
    entityRelationTypes.add("is_indicated_for");
    entityRelationTypes.add("location_of");
    entityRelationTypes.add("manages/treats");
    entityRelationTypes.add("manifestation_of"); // note the misspelling
    entityRelationTypes.add("prevents");
    entityRelationTypes.add("result_of");
    Set<String> eventRelationTypes = new HashSet<String>();
    eventRelationTypes.add("TLINK");
    eventRelationTypes.add("ALINK");
    
    Set<String> nonAnnotationTypes = Sets.newHashSet("Strength", "Frequency", "Value");
    nonAnnotationTypes.addAll(entityRelationTypes);
    nonAnnotationTypes.addAll(eventRelationTypes);

    // create a CAS object for each annotation
    Map<String, TOP> idAnnotationMap = new HashMap<String, TOP>();
    List<DelayedRelation> delayedRelations = new ArrayList<DelayedRelation>();
    List<DelayedFeature> delayedFeatures = new ArrayList<DelayedFeature>();
    for (final KnowtatorAnnotation annotation : annotations) {

      // copy the slots so we can remove them as we use them
      Map<String, String> stringSlots = new HashMap<String, String>(annotation.stringSlots);
      Map<String, Boolean> booleanSlots = new HashMap<String, Boolean>(annotation.booleanSlots);
      Map<String, KnowtatorAnnotation> annotationSlots = new HashMap<String, KnowtatorAnnotation>(
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

      if ("Anatomical_site".equals(annotation.type)) {
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

      } else if ("Disease_Disorder".equals(annotation.type)) {
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
        KnowtatorAnnotation deltaFlag = annotationSlots.remove("delta_flag");
        delayedFeatures.add(new DelayedFeature(mention, "deltaFlag", deltaFlag));
      } else if ("Medications/Drugs".equals(annotation.type)) {
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
        KnowtatorAnnotation endDate = annotationSlots.remove("end_date");
        delayedFeatures.add(new DelayedFeature(mention, "endDate", endDate));
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

      } else if ("Procedure".equals(annotation.type)) {
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

      } else if ("Sign_symptom".equals(annotation.type)) {
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
        modifier.setConditional(value == null ? false : value);
        modifier.addToIndexes();
        idAnnotationMap.put(annotation.id, modifier);

      } else if ("generic_class".equals(annotation.type)) {
        Boolean value = booleanSlots.remove("generic_normalization");
        GenericModifier modifier = new GenericModifier(jCas, coveringSpan.begin, coveringSpan.end);
        modifier.setGeneric(value == null ? false : value);
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
      } else if ("delta_flag_indicator".equals(annotation.type)) {
        String value = stringSlots.remove("delta_flag_normalization");
        LabDeltaFlagModifier modifier = new LabDeltaFlagModifier(jCas, coveringSpan.begin, coveringSpan.end);
        LabDeltaFlag attribute = new LabDeltaFlag(jCas);
        attribute.setValue(value);
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
        int spanStart=text.length()-1,spanEnd=0;  // the strength annotation is spanless so we get the modifier span by its components
        if (unit != null) {
          KnowtatorAnnotation.Span unitSpan = unit.getCoveringSpan();
          String unitString = text.substring(unitSpan.begin, unitSpan.end);
          attribute.setUnit(unitString);
          if(unitSpan.begin < spanStart) spanStart = unitSpan.begin;
          if(unitSpan.end > spanEnd) spanEnd = unitSpan.end;
        }
        if (number != null) {
          KnowtatorAnnotation.Span numberSpan = number.getCoveringSpan();
          String numberString = text.substring(numberSpan.begin, numberSpan.end);
          attribute.setNumber(numberString);
          if(numberSpan.begin < spanStart) spanStart = numberSpan.begin;
          if(numberSpan.end > spanEnd) spanEnd = numberSpan.end;
        }
        attribute.addToIndexes();
        MedicationStrengthModifier modifier = new MedicationStrengthModifier(jCas, spanStart, spanEnd);
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
        String year = stringSlots.remove("year");
        Date date = new Date(jCas);
        date.setMonth(month);
        date.setDay(day);
        date.setYear(year);
        date.addToIndexes();
        TimeMention mention = new TimeMention(jCas, coveringSpan.begin, coveringSpan.end);
        mention.setDate(date);
        mention.addToIndexes();
        idAnnotationMap.put(annotation.id, mention);

      } else if (eventRelationTypes.contains(annotation.type)) {
        // store the ALINK information for later, once all annotations are in the CAS
        DelayedRelation relation = new DelayedRelation();
        relation.sourceFile = knowtatorURI;
        relation.annotation = annotation;
        relation.source = annotationSlots.remove("Event");
        relation.target = annotationSlots.remove("related_to");
        relation.type = stringSlots.remove("Relationtype");
        delayedRelations.add(relation);

      } else if (entityRelationTypes.contains(annotation.type)) {
        // store the relation information for later, once all annotations are in the CAS
        DelayedRelation relation = new DelayedRelation();
        relation.sourceFile = knowtatorURI;
        relation.annotation = annotation;
        relation.source = annotationSlots.remove("Argument_CU");
        relation.target = annotationSlots.remove("Related_to_CU");
        relation.conditional = annotationSlots.remove("conditional_CU");
        relation.negation = annotationSlots.remove("negation_indicator_CU");
        relation.uncertainty = annotationSlots.remove("uncertainty_indicator_CU");
        delayedRelations.add(relation);

      } else {
        LOGGER.error(String.format(
            "unrecognized type '%s' for annotation with id \"%s\"",
            annotation.type,
            annotation.id));
      }

      // make sure all slots have been consumed
      Map<String, Set<String>> slotGroups = new HashMap<String, Set<String>>();
      slotGroups.put("stringSlots", stringSlots.keySet());
      slotGroups.put("booleanSlots", booleanSlots.keySet());
      slotGroups.put("annotationSlots", annotationSlots.keySet());
      for (Map.Entry<String, Set<String>> entry : slotGroups.entrySet()) {
        Set<String> remainingSlots = entry.getValue();
        if (!remainingSlots.isEmpty()) {
          throw new UnsupportedOperationException(String.format(
              "%s has unprocessed %s %s in %s",
              annotation.type,
              entry.getKey(),
              remainingSlots,
              knowtatorURI));
        }
      }
    }

    // all mentions should be added, so add relations between annotations
    for (DelayedRelation delayedRelation : delayedRelations) {
      delayedRelation.addToIndexes(jCas, idAnnotationMap);
    }

    // all mentions should be added, so add features that required other annotations
    for (DelayedFeature delayedFeature : delayedFeatures) {
      try{
        delayedFeature.setValueFrom(idAnnotationMap);
      }catch(CASRuntimeException e){
        LOGGER.error(String.format("Unable to set delayed feature value with message %s", e.getMessage()));
      }
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
    Boolean negation = booleanSlots.remove("Negation");
    mention.setPolarity(negation == null
        ? CONST.NE_POLARITY_NEGATION_ABSENT
        : negation == true ? CONST.NE_POLARITY_NEGATION_PRESENT : CONST.NE_POLARITY_NEGATION_ABSENT);

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
      if ("HistoryOf".equals(status)) {
        mention.setHistoryOf(CONST.NE_HISTORY_OF_PRESENT);
      } else if ("FamilyHistoryOf".equals(status)) {
        mention.setHistoryOf(CONST.NE_HISTORY_OF_PRESENT);
        mention.setSubject(CONST.ATTR_SUBJECT_FAMILY_MEMBER);
      } else if ("Possible".equals(status)) {
        mention.setUncertainty(CONST.NE_CERTAINTY_NEGATED);
      } else {
        throw new UnsupportedOperationException("Unknown status: " + status);
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

  private static class DelayedRelation {
    public URI sourceFile;

    public KnowtatorAnnotation annotation;

    public KnowtatorAnnotation source;

    public KnowtatorAnnotation target;

    public String type;

    public KnowtatorAnnotation conditional;
    
    public KnowtatorAnnotation negation;
    
    public KnowtatorAnnotation uncertainty;
    
    public DelayedRelation() {
    }

    public void addToIndexes(JCas jCas, Map<String, TOP> idAnnotationMap) {
      if (this.source == null) {
        // throw new UnsupportedOperationException(String.format(
        LOGGER.error(String.format(
            "no source for '%s' with id '%s' and annotationSlots %s in %s",
            this.annotation.type,
            this.annotation.id,
            this.annotation.annotationSlots.keySet(),
            this.sourceFile));
        return;
      }
      if (this.target == null) {
        // throw new UnsupportedOperationException(String.format(
        LOGGER.error(String.format(
            "no target for '%s' with id '%s' and annotationSlots %s in %s",
            this.annotation.type,
            this.annotation.id,
            this.annotation.annotationSlots.keySet(),
            this.sourceFile));
        return;
      }

      // look up the relations in the map and issue an error if they're missing or an invalid type
      Annotation sourceMention, targetMention;
      try {
        sourceMention = (Annotation)idAnnotationMap.get(this.source.id);
      } catch (ClassCastException e) {
        LOGGER.error(String.format("invalid source %s: %s", this.source.id, e.getMessage()));
        return;
      }
      try {
        targetMention = (Annotation)idAnnotationMap.get(this.target.id);
      } catch (ClassCastException e) {
        LOGGER.error(String.format("invalid target %s: %s", this.target.id, e.getMessage()));
        return;
      }
      if (sourceMention == null) {
        LOGGER.error(String.format(
            "no Annotation for source id '%s' in %s",
            this.source.id,
            this.sourceFile));
        return;
      } else if (targetMention == null) {
        LOGGER.error(String.format(
            "no Annotation for target id '%s' in %s",
            this.target.id,
            this.sourceFile));
        return;
      }

      // get the conditional
      if (this.conditional != null) {
        Annotation conditionalAnnotation = (Annotation)idAnnotationMap.get(this.conditional.id);
        if (conditionalAnnotation == null) {
          throw new UnsupportedOperationException(String.format(
              "no annotation with id '%s' in %s",
              this.conditional.id,
              this.sourceFile));
        }
      }

      // get the negation
      if (this.negation != null) {
        Annotation negationAnnotation = (Annotation)idAnnotationMap.get(this.negation.id);
        if (negationAnnotation == null) {
          throw new UnsupportedOperationException(String.format(
              "no annotation with id '%s' in %s",
              this.negation.id,
              this.sourceFile));
        }
      }

      // get the uncertainty
      if (this.uncertainty != null) {
        Annotation uncertaintyAnnotation = (Annotation)idAnnotationMap.get(this.uncertainty.id);
        if (uncertaintyAnnotation == null) {
          throw new UnsupportedOperationException(String.format(
              "no annotation with id '%s' in %s",
              this.uncertainty.id,
              this.sourceFile));
        }
      }
      
      // add the relation to the CAS
      BinaryTextRelation relation = null;
      if ("affects".equals(this.annotation.type)) {
        this.assertTypes(sourceMention, EventMention.class, targetMention, IdentifiedAnnotation.class);
        relation = new AffectsTextRelation(jCas);
      } else if ("complicates/disrupts".equals(this.annotation.type)) {
        this.assertTypes(sourceMention, EventMention.class, targetMention, EventMention.class);
        relation = new ComplicatesDisruptsTextRelation(jCas);
      } else if ("causes/brings_about".equals(this.annotation.type)) {
        this.assertTypes(sourceMention, EventMention.class, targetMention, EventMention.class);
        relation = new CausesBringsAboutTextRelation(jCas);
      } else if ("indicates".equals(this.annotation.type)) {
        this.assertTypes(sourceMention, EventMention.class, targetMention, EventMention.class);
        relation = new IndicatesTextRelation(jCas);
      } else if ("degree_of".equals(this.annotation.type)) {
        this.assertTypes(sourceMention, EventMention.class, targetMention, Modifier.class);
        relation = new DegreeOfTextRelation(jCas);
      } else if ("location_of".equals(this.annotation.type)) {
        if (!(targetMention instanceof AnatomicalSiteMention) && (sourceMention instanceof AnatomicalSiteMention)) {
          // fix reversed arguments in manual annotations
          Annotation temp = sourceMention;
          sourceMention = targetMention;
          targetMention = temp;
//          LOGGER.warn(String.format(
//              "relation arguments in reverse order for \"%s\" annotation with id \"%s\"",
//              annotation.type,
//              annotation.id));
        }
        this.assertTypes(sourceMention, IdentifiedAnnotation.class, targetMention, AnatomicalSiteMention.class);
        relation = new LocationOfTextRelation(jCas);
      } else if ("manages/treats".equals(this.annotation.type)) {
        this.assertTypes(sourceMention, EventMention.class, targetMention, EventMention.class);
        relation = new ManagesTreatsTextRelation(jCas);
      } else if ("manifestation_of".equals(this.annotation.type)) {
        this.assertTypes(sourceMention, EventMention.class, targetMention, EventMention.class);
        relation = new ManifestationOfTextRelation(jCas);
        relation.setCategory("manifestation_of"); // fix typo in Knowtator type system
      } else if ("prevents".equals(this.annotation.type)) {
        this.assertTypes(sourceMention, EventMention.class, targetMention, EventMention.class);
        relation = new PreventsTextRelation(jCas);
      } else if ("result_of".equals(this.annotation.type)) {
        this.assertTypes(sourceMention, EventMention.class, targetMention, IdentifiedAnnotation.class);
        relation = new ResultOfTextRelation(jCas);
      } else if ("TLINK".equals(this.annotation.type)) {
        relation = new TemporalTextRelation(jCas);
        relation.setCategory(this.type);
      } else if ("ALINK".equals(this.annotation.type)) {
        relation = new AspectualTextRelation(jCas);
        relation.setCategory(this.type);
      } else {
        relation = new BinaryTextRelation(jCas);
      }
      
      // set the relation cateory (if not already set)
      if (relation.getCategory() == null) {
        relation.setCategory(this.annotation.type);
      }
      
      // link the relation to its arguments and add it to the CAS
      RelationArgument sourceRA = new RelationArgument(jCas);
      sourceRA.setArgument(sourceMention);
      sourceRA.addToIndexes();
      RelationArgument targetRA = new RelationArgument(jCas);
      targetRA.setArgument(targetMention);
      targetRA.addToIndexes();
      relation.setArg1(sourceRA);
      relation.setArg2(targetRA);
      relation.addToIndexes();
    
      // add the relation to the map so it can be used in features of other annotations
      idAnnotationMap.put(this.annotation.id, relation);
    }
    
    private void assertTypes(Annotation sourceMention, Class<? extends Annotation> expectedSourceClass, Annotation targetMention, Class<? extends Annotation> expectedTargetClass) {
      if (!expectedSourceClass.isInstance(sourceMention) || !expectedTargetClass.isInstance(targetMention)) {
        LOGGER.warn(String.format(
            "wrong relation argument types: expected %s(%s(...), %s(...)) but found %s(%s, %s) for ids %s(%s, %s) in %s",
            this.annotation.type,
            expectedSourceClass.getSimpleName(),
            expectedTargetClass.getSimpleName(),
            this.annotation.type,
            format(sourceMention),
            format(targetMention),
            this.annotation.id,
            this.source.id,
            this.target.id,
            this.sourceFile));
      }
    }
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
    
    private Class<? extends BinaryTextRelation> relationClass;
    private Annotation arg1, arg2;
    private Class<? extends Annotation> arg1Class, arg2Class;
    
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
   * Expects directory named "Knowtator" and a sibling directory "Knowtator_XML".
   * "Knowtator" should have a subdirectory called "text" containing plaintext files
   * "Knowtator_XML" should have files that end with .knowtator.xml
   * @see #getKnowtatorURI
   */
  public static void main(String[] args) throws Exception {
    if (args.length == 0) {
      throw new IllegalArgumentException(String.format(
          "usage: java %s path/to/Knowtator/text [path/to/Knowtator/text ...]",
          SHARPKnowtatorXMLReader.class.getName()));
    }
    AnalysisEngine engine = AnalysisEngineFactory.createEngine(SHARPKnowtatorXMLReader.class);
    
    for (String knowtatorTextDirectoryPath : args) {
      File knowtatorTextDirectory = new File(knowtatorTextDirectoryPath);
      for (File textFile : knowtatorTextDirectory.listFiles()) {
        JCas jCas = engine.newJCas();
        jCas.setDocumentText(Files.toString(textFile, Charsets.US_ASCII));
        DocumentID documentID = new DocumentID(jCas);
        documentID.setDocumentID(textFile.toURI().toString());
        documentID.addToIndexes();
        engine.process(jCas);
        documentID.setDocumentID(textFile.getName());
        CasIOUtil.writeXmi(jCas, new File("/tmp", textFile.toURI().toString()));
      }
    }

  }
}
