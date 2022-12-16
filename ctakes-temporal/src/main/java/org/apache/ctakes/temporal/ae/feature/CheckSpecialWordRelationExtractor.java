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
package org.apache.ctakes.temporal.ae.feature;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.regex.Pattern;

import org.apache.ctakes.core.resource.FileLocator;
import org.apache.ctakes.core.util.StringUtil;
import org.apache.ctakes.relationextractor.ae.features.RelationFeaturesExtractor;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.cleartk.ml.Feature;
import org.springframework.util.StringUtils;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

public class CheckSpecialWordRelationExtractor implements RelationFeaturesExtractor<IdentifiedAnnotation,IdentifiedAnnotation>{

	//final static List<String> specialWd = Arrays.asList("before","prior","previous","previously","ago","soon","earlier","early","after","later","subsequent","follow","following","followed","post","since","back","start","started","by","past","starting");
	private static final String LOOKUP_PATH = "org/apache/ctakes/temporal/TimeLexicon.csv";
	static private final Pattern EOL_PATTERN = Pattern.compile( "[\r\n]" );

//	private Multimap<String, String> specialWd;
	private final Map<String, Collection<String>> _specialWd;
	public CheckSpecialWordRelationExtractor() throws ResourceInitializationException {
		// TODO Use a plain old java HashMap<String,Collection<String>>.
//		this.specialWd = ArrayListMultimap.create();
		_specialWd = new HashMap<>();
		try ( final BufferedReader reader
						= new BufferedReader( new InputStreamReader( FileLocator.getAsStream( LOOKUP_PATH ) ) ) ) {
			reader.lines().forEachOrdered( this::loadWordTypes );
		} catch ( IOException | IllegalArgumentException multE ) {
			throw new ResourceInitializationException( multE );
		}
	}

	private void loadWordTypes( final String line ) throws IllegalArgumentException {
		final String[] wordAndType = StringUtil.fastSplit( line, ',' );
		if (wordAndType.length != 2) {
			throw new IllegalArgumentException("Expected '<word>,<type>', found: " + line);
		}
//		specialWd.put(wordAndType[0], wordAndType[1]);
		_specialWd.computeIfAbsent( wordAndType[0], t -> new HashSet<>() ).add( wordAndType[1] );
	}


	@Override
	public List<Feature> extract(JCas jcas, IdentifiedAnnotation arg1,
			IdentifiedAnnotation arg2) throws AnalysisEngineProcessException {
		List<Feature> feats = new ArrayList<>();

		// swap the order if necessary:
		if(isBefore(arg2,arg1)){
			IdentifiedAnnotation temp = arg1;
			arg1 = arg2;
			arg2 = temp;
		}else if(isBefore(arg1,arg2)){
			//keep the order of arg1 arg2
		}else{
			return feats; //don't do anything if arg1 overlap arg2
		}

		//1 get covering sentence:
//		Map<IdentifiedAnnotation, Collection<Sentence>> coveringMap =
//				JCasUtil.indexCovering(jcas, IdentifiedAnnotation.class, Sentence.class);
//
		int begin = arg1.getEnd();
		int end = arg2.getBegin();
//		int window = 30;
//
//		//get two covering sentences for arg1 and arg2, two arguments could come from different sentences.
//		List<Sentence> sentList = new ArrayList<>();
//		sentList.addAll(coveringMap.get(arg1));
//		if(sentList.isEmpty()) return feats;
//		Sentence arg1Sent = sentList.get(0);
//
//		sentList = new ArrayList<>();
//		sentList.addAll(coveringMap.get(arg2));
//		if(sentList.isEmpty()) return feats;
//		Sentence arg2Sent = sentList.get(0);


		String textInBetween = null;
		//		String textAfterArg1 = null;
		//		String textBeforeArg2 = null;
		//		if(end-begin <= 2* window){
		textInBetween = jcas.getDocumentText()
								  .substring(begin, end)
//								  .replaceAll("[\r\n]", " ")
								  .toLowerCase();
		textInBetween = EOL_PATTERN.matcher( textInBetween ).replaceAll( " " );

		//		}else{
		//			int arg1tail = Math.min(begin + window, arg1Sent.getEnd());
		//			textAfterArg1 = jcas.getDocumentText().substring(begin, arg1tail).replaceAll("[\r\n]", " ").toLowerCase();
		//			int arg2head = Math.max(end - window, arg2Sent.getBegin());
		//			textBeforeArg2 = jcas.getDocumentText().substring(arg2head, end).replaceAll("[\r\n]", " ").toLowerCase();
		//		}
//		int arg1head = Math.max(arg1.getBegin()-window, arg1Sent.getBegin());
//		String textBeforeArg1 = jcas.getDocumentText().substring(arg1head, arg1.getBegin()).replaceAll("[\r\n]", " ").toLowerCase();
//		int arg2tail = Math.min(arg2.getEnd()+window, arg2Sent.getEnd());
//		String textAfterArg2 = jcas.getDocumentText().substring(arg2.getEnd(), arg2tail).replaceAll("[\r\n]", " ").toLowerCase();
		String textInArg1 = jcas.getDocumentText()
										.substring(arg1.getBegin(), arg1.getEnd())
//										.replaceAll("[\r\n]", " ")
										.toLowerCase();
		textInArg1 = EOL_PATTERN.matcher( textInArg1 ).replaceAll( " " );
		String textInArg2 = jcas.getDocumentText()
										.substring(arg2.getBegin(), arg2.getEnd())
//										.replaceAll("[\r\n]", " ")
										.toLowerCase();
		textInArg2 = EOL_PATTERN.matcher( textInArg2 ).replaceAll( " " );


//		for(String lexicon : specialWd.keySet()){
//			if( textInBetween != null && textInBetween.matches(".*\\b"+lexicon+"\\b.*")){
//				String type = StringUtils.collectionToCommaDelimitedString(specialWd.get(lexicon));
//				Feature feature = new Feature("SpecialWd_InBetween", type);
//				feats.add(feature);
//			}
////			if( textBeforeArg1.matches(".*\\b"+lexicon+"\\b.*")){
////				String type = StringUtils.collectionToCommaDelimitedString(specialWd.get(lexicon));
////				Feature feature = new Feature("SpecialWd_BeforeArg1", type);
////				feats.add(feature);
////			}
//			if( textInArg1.matches(".*\\b"+lexicon+"\\b.*")){
//				String type = StringUtils.collectionToCommaDelimitedString(specialWd.get(lexicon));
//				Feature feature = new Feature("SpecialWd_InArg1", type);
//				feats.add(feature);
//			}
//			//			if( textAfterArg1 != null && textAfterArg1.matches(".*\\b"+lexicon+"\\b.*")){
//			//				String type = StringUtils.collectionToCommaDelimitedString(specialWd.get(lexicon));
//			//				Feature feature = new Feature("SpecialWd_AfterArg1", type);
//			//				feats.add(feature);
//			//			}
//			//			if( textBeforeArg2 != null && textBeforeArg2.matches(".*\\b"+lexicon+"\\b.*")){
//			//				String type = StringUtils.collectionToCommaDelimitedString(specialWd.get(lexicon));
//			//				Feature feature = new Feature("SpecialWd_BeforeArg2", type);
//			//				feats.add(feature);
//			//			}
//			if( textInArg2.matches(".*\\b"+lexicon+"\\b.*")){
//				String type = StringUtils.collectionToCommaDelimitedString(specialWd.get(lexicon));
//				Feature feature = new Feature("SpecialWd_InArg2", type);
//				feats.add(feature);
//			}
////			if( textAfterArg2.matches(".*\\b"+lexicon+"\\b.*")){
////				String type = StringUtils.collectionToCommaDelimitedString(specialWd.get(lexicon));
////				Feature feature = new Feature("SpecialWd_AfterArg2", type);
////				feats.add(feature);
////			}
//		}

		for( Map.Entry<String,Collection<String>> lexiconType : _specialWd.entrySet()){
			final Pattern lexiconPattern = Pattern.compile( ".*\\b"+lexiconType.getKey()+"\\b.*" );
			if( textInBetween != null && lexiconPattern.matcher( textInBetween ).matches() ){
				final String type = String.join( ",", lexiconType.getValue() );
				final Feature feature = new Feature("SpecialWd_InBetween", type);
				feats.add(feature);
			}
//			if( textBeforeArg1.matches(".*\\b"+lexicon+"\\b.*")){
//				String type = StringUtils.collectionToCommaDelimitedString(specialWd.get(lexicon));
//				Feature feature = new Feature("SpecialWd_BeforeArg1", type);
//				feats.add(feature);
//			}
			if( lexiconPattern.matcher( textInArg1 ).matches() ){
				final String type = String.join( ",", lexiconType.getValue() );
				final Feature feature = new Feature("SpecialWd_InArg1", type);
				feats.add(feature);
			}
			//			if( textAfterArg1 != null && textAfterArg1.matches(".*\\b"+lexicon+"\\b.*")){
			//				String type = StringUtils.collectionToCommaDelimitedString(specialWd.get(lexicon));
			//				Feature feature = new Feature("SpecialWd_AfterArg1", type);
			//				feats.add(feature);
			//			}
			//			if( textBeforeArg2 != null && textBeforeArg2.matches(".*\\b"+lexicon+"\\b.*")){
			//				String type = StringUtils.collectionToCommaDelimitedString(specialWd.get(lexicon));
			//				Feature feature = new Feature("SpecialWd_BeforeArg2", type);
			//				feats.add(feature);
			//			}
			if( lexiconPattern.matcher( textInArg2 ).matches() ){
				final String type = String.join( ",", lexiconType.getValue() );
				Feature feature = new Feature("SpecialWd_InArg2", type);
				feats.add(feature);
			}
//			if( textAfterArg2.matches(".*\\b"+lexicon+"\\b.*")){
//				String type = StringUtils.collectionToCommaDelimitedString(specialWd.get(lexicon));
//				Feature feature = new Feature("SpecialWd_AfterArg2", type);
//				feats.add(feature);
//			}
		}

		//logger.info("found nearby verb's pos tag: "+ verbTP);
		return feats;
	}

	private static boolean isBefore(IdentifiedAnnotation arg1,
			IdentifiedAnnotation arg2) {
		if(arg1.getBegin()<arg2.getBegin()){
			if(arg1.getEnd()<arg2.getBegin()){
				return true;
			}
		}
		return false;
	}


}
