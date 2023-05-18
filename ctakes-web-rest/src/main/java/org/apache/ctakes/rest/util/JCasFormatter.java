/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.ctakes.rest.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ctakes.drugner.type.*;
import org.apache.ctakes.rest.service.CuiResponse;
import org.apache.ctakes.typesystem.type.textsem.*;
import org.apache.uima.UIMAException;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.impl.XmiCasSerializer;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.factory.TypeSystemDescriptionFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.json.JsonCasSerializer;
import org.apache.uima.resource.metadata.TypeDescription;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.resource.metadata.impl.TypeSystemDescription_impl;
import org.xml.sax.SAXException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Extracts desired Annotations from a jcas, returning information as a map containing {@link CuiResponse}.
 *
 * Created by tmill on 12/20/18.
 */
final public class JCasFormatter {

   final private static List<Class<? extends Annotation>> semClasses = Arrays.asList(

           // CUI types:
           DiseaseDisorderMention.class,
           SignSymptomMention.class,
           ProcedureMention.class,
           AnatomicalSiteMention.class,
           MedicationMention.class,
           EventMention.class,
           EntityMention.class,
           IdentifiedAnnotation.class,

           // Temporal types:
           TimeMention.class,
           DateAnnotation.class,

           // Drug-related types:
           FractionStrengthAnnotation.class,
           DrugChangeStatusAnnotation.class,
           StrengthUnitAnnotation.class,
           StrengthAnnotation.class,
           RouteAnnotation.class,
           FrequencyUnitAnnotation.class,
           MeasurementAnnotation.class
   );

   /* There are a lot of types in cTAKES that are populated by "linguistic" modules, then these types are added to
      the CAS for use in multiple downstream AEs. But most end users won't care about these types. We can filter them
      out to send back a smaller return package. The way to do that is create a new typesystem that has all the same
      types except the ones with "syntax" in the package name. This is a blunt weapon but I couldn't find a "real" way
      to do this using UIMA library calls.
    */
   static TypeSystem filteredTypeSystem;
   static {
      try {
         TypeSystemDescription tsd = TypeSystemDescriptionFactory.createTypeSystemDescription();
         TypeSystemDescription filteredTypeSystemDescription = new TypeSystemDescription_impl();
         for(TypeDescription td : tsd.getTypes()){
            if(!td.getName().contains(("syntax"))) {
               TypeDescription newType = filteredTypeSystemDescription.addType(td.getName(), td.getDescription(), td.getSupertypeName());
               newType.setFeatures(td.getFeatures());
            }
         }
         JCas tsCas = JCasFactory.createJCas(filteredTypeSystemDescription);
         filteredTypeSystem = tsCas.getTypeSystem();
      } catch (UIMAException e) {
         e.printStackTrace();
         throw new RuntimeException(e);
      }
   }

   /**
    * @param jcas ye olde ...
    * @return A Map, key is annotation class name (type), value is a list of {@link CuiResponse},
    * one for each annotation of interest.
    */
   public static String getJsonSummaryFormat( final JCas jcas ) { //throws Exception {

      Map<String, List<CuiResponse>> struct = JCasUtil.select( jcas, Annotation.class ).stream()
                     .filter( a -> semClasses.contains( a.getClass() ) )
                     .map( CuiResponse::new )
                     .collect( Collectors.groupingBy( CuiResponse::getType ) );
      ObjectMapper mapper = new ObjectMapper();
      try {
         return mapper.writeValueAsString(struct);
      } catch (JsonProcessingException e) {
         return "{\"Status\": \"Error caused during mapping of summary object to json.\"}";
      }
   }

   /**
    * @param jcas ye olde ...
    * @return The UIMA-generated JSON string representation of the CAS
    */
   public static String getJsonFullFormat( final JCas jcas ) { //throws Exception {
      StringWriter writer = new StringWriter();
      try {
         JsonCasSerializer.jsonSerialize(jcas.getCas(), jcas.getTypeSystem(), writer);
         return writer.toString();
      }catch(IOException e){
         return "{\"Status\": \"Error caused during JSON serialization of JCas on REST server.\"}";
      }
   }

   /**
    * @param jcas ye olde ...
    * @return The UIMA-generated JSON string representation of the CAS
    */
   public static String getJsonFilteredFormat( final JCas jcas ) { //throws Exception {
      StringWriter writer = new StringWriter();
      try {
         JsonCasSerializer.jsonSerialize(jcas.getCas(), filteredTypeSystem, writer);
         return writer.toString();
      }catch(IOException e){
         return "{\"Status\": \"Error caused during JSON serialization of JCas on REST server.\"}";
      }
   }

   /**
    * @param jcas ye olde ...
    * @return The XML generated by UIMA (specifically. their xmi format).
    */
   public static String getXmiFormat( final JCas jcas ) {
      ObjectMapper mapper = new ObjectMapper();
      ByteArrayOutputStream baos = new ByteArrayOutputStream();

      try {
         XmiCasSerializer.serialize(jcas.getCas(), baos);
         String xmiString = baos.toString();
         Map<String,String> jsonDict = new HashMap<>();
         jsonDict.put("xml", xmiString);
         return mapper.writeValueAsString(jsonDict);
      }catch(SAXException | JsonProcessingException e){
         return "{\"Status\": \"Error caused during XMI serialization of JCas on REST server.\"}";
      }
   }
}
