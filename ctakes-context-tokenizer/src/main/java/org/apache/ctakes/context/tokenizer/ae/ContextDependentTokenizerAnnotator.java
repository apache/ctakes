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
package org.apache.ctakes.context.tokenizer.ae;

import org.apache.ctakes.core.ae.TokenizerAnnotator;
import org.apache.ctakes.core.fsm.adapters.*;
import org.apache.ctakes.core.fsm.machine.*;
import org.apache.ctakes.core.fsm.output.*;
import org.apache.ctakes.core.fsm.token.BaseToken;
import org.apache.ctakes.core.fsm.token.EolToken;
import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.typesystem.type.syntax.*;
import org.apache.ctakes.typesystem.type.textsem.*;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import java.util.*;

/**
 * Finds tokens based on context.
 * 
 * @author Mayo Clinic
 */
@PipeBitInfo(
      name = "Context Dependent Annotator",
      description = "Finds tokens based upon context.  Time, Date, Roman numeral, Fraction, Range, Measurement, Person title.",
      dependencies = { PipeBitInfo.TypeProduct.SENTENCE, PipeBitInfo.TypeProduct.BASE_TOKEN }
)
public class ContextDependentTokenizerAnnotator extends JCasAnnotator_ImplBase {
	// LOG4J logger based on class name
	private Logger LOGGER = LoggerFactory.getLogger(getClass().getName());

	private DateFSM iv_dateFSM;
	private TimeFSM iv_timeFSM;
	private FractionFSM iv_fractionFSM;
	private RomanNumeralFSM iv_romanNumeralFSM;
	private RangeFSM iv_rangeFSM;
	private MeasurementFSM iv_measurementFSM;
	private PersonTitleFSM iv_personTitleFSM;

	@Override
  public void initialize(UimaContext annotCtx) throws ResourceInitializationException {
		super.initialize(annotCtx);

		iv_dateFSM = new DateFSM();
		iv_timeFSM = new TimeFSM();
		iv_fractionFSM = new FractionFSM();
		iv_romanNumeralFSM = new RomanNumeralFSM();
		iv_rangeFSM = new RangeFSM();
		iv_measurementFSM = new MeasurementFSM();
		iv_personTitleFSM = new PersonTitleFSM();
		LOGGER.info("Finite state machines loaded.");
	}

	@Override
  public void process(JCas jcas) throws AnalysisEngineProcessException {

		try {
			
		  LOGGER.info("process(JCas)");

			Collection<Sentence> sents = JCasUtil.select(jcas, Sentence.class);
			
			for(Sentence sentAnnot : sents){
			  List<org.apache.ctakes.typesystem.type.syntax.BaseToken> tokens = 
			      JCasUtil.selectCovered(org.apache.ctakes.typesystem.type.syntax.BaseToken.class, sentAnnot);
			  // adapt JCas objects into objects expected by the Finite state
			  // machines
				List<BaseToken> baseTokenList = new ArrayList<>();
				for(org.apache.ctakes.typesystem.type.syntax.BaseToken bta : tokens){
					// ignore newlines, avoid null tokens
					BaseToken bt = adaptToBaseToken(bta);
					if(bt != null && !(bt instanceof EolToken))
						baseTokenList.add(bt);
				}

				// execute FSM logic
				executeFSMs(jcas, baseTokenList);
			}
		} catch (Exception e) {
			throw new AnalysisEngineProcessException(e);
		}
	}

	private void executeFSMs(JCas jcas, List<? extends BaseToken> baseTokenList) throws AnalysisEngineProcessException {
		try {
			Set<DateToken> dateTokenSet = iv_dateFSM.execute(baseTokenList);
			Iterator<DateToken> dateTokenItr = dateTokenSet.iterator();
			while (dateTokenItr.hasNext()) {
				DateToken dt = dateTokenItr.next();
				DateAnnotation dta = new DateAnnotation(jcas, dt.getStartOffset(), dt.getEndOffset());
				dta.addToIndexes();
			}

			Set<TimeToken> timeTokenSet = iv_timeFSM.execute(baseTokenList);
			Iterator<TimeToken> timeTokenItr = timeTokenSet.iterator();
			while (timeTokenItr.hasNext()) {
				TimeToken tt = timeTokenItr.next();
				TimeAnnotation ta = new TimeAnnotation(jcas, tt.getStartOffset(), tt.getEndOffset());
				ta.addToIndexes();
			}

			Set<RomanNumeralToken> romanNumeralTokenSet = iv_romanNumeralFSM.execute(baseTokenList);
			Iterator<RomanNumeralToken> romanNumeralTokenItr = romanNumeralTokenSet.iterator();
			while (romanNumeralTokenItr.hasNext()) {
				RomanNumeralToken rnt = romanNumeralTokenItr.next();
				RomanNumeralAnnotation rna = new RomanNumeralAnnotation(jcas, rnt.getStartOffset(), rnt.getEndOffset());
				rna.addToIndexes();
			}

			Set<FractionToken> fractionTokenSet = iv_fractionFSM.execute(baseTokenList);
			Iterator<FractionToken> fractionTokenItr = fractionTokenSet.iterator();
			while (fractionTokenItr.hasNext()) {
				FractionToken ft = fractionTokenItr.next();
				FractionAnnotation fa = new FractionAnnotation(jcas, ft.getStartOffset(), ft.getEndOffset());
				fa.addToIndexes();
			}

			Set<RangeToken> rangeTokenSet = iv_rangeFSM.execute(baseTokenList, romanNumeralTokenSet);
			Iterator<RangeToken> rangeTokenItr = rangeTokenSet.iterator();
			while (rangeTokenItr.hasNext()) {
				RangeToken rt = rangeTokenItr.next();
				RangeAnnotation ra = new RangeAnnotation(jcas, rt.getStartOffset(), rt.getEndOffset());
				ra.addToIndexes();
			}

			Set<MeasurementToken> measurementTokenSet = iv_measurementFSM.execute(baseTokenList, rangeTokenSet);
			Iterator<MeasurementToken> measurementTokenItr = measurementTokenSet.iterator();
			while (measurementTokenItr.hasNext()) {
				MeasurementToken mt = measurementTokenItr.next();
				MeasurementAnnotation ma = new MeasurementAnnotation(jcas, mt.getStartOffset(), mt.getEndOffset());
				ma.addToIndexes();
			}

			Set<PersonTitleToken> personTitleTokenSet = iv_personTitleFSM.execute(baseTokenList);
			Iterator<PersonTitleToken> personTitleTokenItr = personTitleTokenSet.iterator();
			while (personTitleTokenItr.hasNext()) {
				PersonTitleToken ptt = personTitleTokenItr.next();
				PersonTitleAnnotation pta = new PersonTitleAnnotation(jcas, ptt.getStartOffset(), ptt.getEndOffset());
				pta.addToIndexes();
			}
		} catch (Exception e) {
			throw new AnalysisEngineProcessException(e);
		}
	}

	/**
	 * Adapts JCas objects to BaseToken interfaces expected by the Finite State
	 * Machines.
	 * 
	 * @param obj
	 * @return
	 */
	private static BaseToken adaptToBaseToken(org.apache.ctakes.typesystem.type.syntax.BaseToken obj) throws Exception {
		if (obj instanceof WordToken) {
			WordToken wta = (WordToken) obj;
			return new WordTokenAdapter(wta);
		} else if (obj instanceof NumToken) {
			NumToken nta = (NumToken) obj;
			if (nta.getNumType() == TokenizerAnnotator.TOKEN_NUM_TYPE_INTEGER) {
				return new IntegerTokenAdapter(nta);
			}
      return new DecimalTokenAdapter(nta);
		} else if (obj instanceof PunctuationToken) {
			PunctuationToken pta = (PunctuationToken) obj;
			return new PunctuationTokenAdapter(pta);
		} else if (obj instanceof NewlineToken) {
			NewlineToken nta = (NewlineToken) obj;
			return new NewlineTokenAdapter(nta);
		} else if (obj instanceof ContractionToken) {
			ContractionToken cta = (ContractionToken) obj;
			return new ContractionTokenAdapter(cta);
		} else if (obj instanceof SymbolToken) {
			SymbolToken sta = (SymbolToken) obj;
			return new SymbolTokenAdapter(sta);
		}

		throw new Exception("No Context Dependent Tokenizer adapter for class: " + obj.getClass());
	}
	
	public static AnalysisEngineDescription createAnnotatorDescription() throws ResourceInitializationException{
	  return AnalysisEngineFactory.createEngineDescription(ContextDependentTokenizerAnnotator.class);
	}
}
