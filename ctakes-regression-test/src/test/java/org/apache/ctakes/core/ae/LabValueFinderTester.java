package org.apache.ctakes.core.ae;

import static org.apache.ctakes.core.ae.LabValueFinder.PARAM_ALL_SECTIONS;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.List;

import org.apache.ctakes.contexttokenizer.ae.ContextDependentTokenizerAnnotator;
import org.apache.ctakes.core.ae.LabValueFinder;
import org.apache.ctakes.core.ae.SentenceDetector;
import org.apache.ctakes.core.ae.SimpleSegmentAnnotator;
import org.apache.ctakes.core.ae.TokenizerAnnotatorPTB;
import org.apache.ctakes.dictionary.lookup2.ae.DefaultJCasTermAnnotator;
import org.apache.ctakes.dictionary.lookup2.util.UmlsUserApprover;
import org.apache.ctakes.postagger.POSTagger;
import org.apache.ctakes.typesystem.type.textsem.LabMention;
import org.apache.ctakes.typesystem.type.textspan.Segment;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.fit.factory.AggregateBuilder;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 11/14/2017
 * 
 * Updated 12/30/2020 pabramowitsch
 * no longer indicates failure, but matches many more
 * values to entities and more talkative output demonstrates 
 * full capability of LVF 
 */

public class LabValueFinderTester {
	static private final Logger LOGGER = Logger
			.getLogger("LabValueFinderTester");

	static private final String LAB_RESULTS_OID = "2.16.840.1.113883.10.20.22.2.3.1";
	static private final String DICT_DESC_PATH = "org/apache/ctakes/examples/dictionary/lookup/fast/tinyDictSpec.xml";
	//If you want to run against a UMLS dictionary, comment out the above line and put the dict-spec-xml path here and 
	//-Dctakes.umls_apikey="your key"  in the VM args.
	// for example:
	// static private final String DICT_DESC_PATH = "org/apache/ctakes/dictionary/lookup/fast/sno_rx_16ab.xml";
	static private AnalysisEngineDescription simpleSegmentator;
	static private AnalysisEngineDescription labSegmentator;
	static private AnalysisEngineDescription midPipeline;
	static private AnalysisEngineDescription defaultLabAnnotator;
	static private AnalysisEngineDescription sameLineLabAnnotator;

	/**
	 * If you choose to test with the default ctakes dictionary, use the second
	 * DefaultJCasTermAnnotator with UMLS credential as described above. Also, uncomment the
	 * PARAM_USE_DRUGS lines in descriptor creation as many a "Lab" is
	 * represented by a medication in traditional ctakes.
	 *
	 * @throws UIMAException
	 *             -
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws UIMAException {
		simpleSegmentator = SimpleSegmentAnnotator.createAnnotatorDescription();
		labSegmentator = SimpleSegmentAnnotator
				.createAnnotatorDescription(LAB_RESULTS_OID);

		final AggregateBuilder builder = new AggregateBuilder();
		builder.add(SentenceDetector.createAnnotatorDescription());
		builder.add(TokenizerAnnotatorPTB.createAnnotatorDescription());
		builder.add(AnalysisEngineFactory
				.createEngineDescription(ContextDependentTokenizerAnnotator.class));
		builder.add(AnalysisEngineFactory
				.createEngineDescription(POSTagger.class));
		builder.add(DefaultJCasTermAnnotator
				.createAnnotatorDescription(DICT_DESC_PATH));
		// builder.add( AnalysisEngineFactory.createEngineDescription(
		// DefaultJCasTermAnnotator.class) );
		midPipeline = builder.createAggregateDescription();

		defaultLabAnnotator = LabValueFinder.createAnnotatorDescription(
				// LabValueFinder.PARAM_USE_DRUGS, "true", 
				PARAM_ALL_SECTIONS, "false");
		sameLineLabAnnotator = LabValueFinder.createAnnotatorDescription(
				// LabValueFinder.PARAM_USE_DRUGS, "true",
				LabValueFinder.PARAM_MAX_NEWLINES, 1, 
				PARAM_ALL_SECTIONS, "false");
		Logger LOGGER = Logger.getLogger("LabValueFinder");
		LOGGER.setLevel(Level.INFO);
	}

	/**
	 * @throws UIMAException
	 *             -
	 */
	@Test
	public void testTable() throws UIMAException {
		String text = "Recent Results (from the past 24 hour(s))\n"
				+ "HEPATIC FUNCTION PANEL\n"
				+ "Collection Time: 12/04/15 5:40 PM\n"
				+ "    Result Value Ref Range\n"
				+ " Albumin 2.2 (*) 3.7 - 5.1 g/dL\n"
				+ " Total Protein 5.5 (*) 5.8 - 8.0 g/dL\n"
				+ " Alkaline Phosphatase 844 (*) 42 - 121 IU/L\n"
				+ " ALT 30  10 - 60 Unit/L\n"
				+ " AST 130 (*) 10 - 42 Unit/L\n"
				+ " Total Bilirubin 1.3  0.4 - 1.3 mg/dL\n"
				+ " Bilirubin, Direct 0.4 (*) 0.0 - 0.2 mg/dL\n"
				+ " Bilirubin, Indirect 0.9  0.0 - 1.0 mg/dL\n"
				+ "LIPASE\n"
				+ "Collection Time:  12/04/15 7 PM\n"
				+ // "7 PM" is covered by a TimeAnnotation
				"    Result Value Ref Range\n"
				+ " Lipase 19 (*) 22 - 51 Unit/L\n"
				+ "PROTIME-INR\n"
				+ " Collection Time: 12/04/15 7:45 PM\n"
				+ // "7:45 PM" isn't covered by a TimeAnnotation
				"    Result Value Ref Range\n"
				+ " Protime 18.0 (*) 9.0 - 11.5 sec\n" + " INR 1.9\n"
				+ "COMPREHENSIVE METABOLIC PANEL\n"
				+ "Collection Time:  12/04/15 7:45 AM\n"
				+ "Result Value Ref Range\n"
				+ "GFR Calc , Female N-Blk 73 >60 mL/min\n"
				+ "Osmolality Calc 281 266 - 309 mOsm/K\n"
				+ "A/G Ratio 0.7 (*) 1.1 - 2.2\n" + "RBC, UA 1 0 - 2 /HPF\n"
				+ "WBC, UA 5 (*) 0 - 4 /HPF\n" + "CK TOTAL AND CKMB\n"
				+ "Collection Time:  12/04/15 10:00 AM\n"
				+ // "10:00 AM" isn't covered by a TimeAnnotation
				"Result Value Ref Range\n" + "Total CK 125 30 - 240 Unit/L\n"
				+ "CK-MB 1.3 0.0 - 9.0 ng/mL\n";
		JCas jCas = processLabText(text);
		assertLabMentions(jCas, "Albumin", "2.2", "Protein", "5.5",
				"Alkaline Phosphatase", "844", "ALT", "30", "AST", "130",
				"Bilirubin", "1.3", "Bilirubin, Direct",
				"0.4",
				"Bilirubin",
				"", // We are not using term subsumption, so bilubrin shows up
					// twice
				"Bilirubin, Indirect", "0.9",
				"Bilirubin",
				"", // We are not using term subsumption, so bilubrin shows up
					// twice
				"LIPASE", "", "Lipase", "19", "PROTIME", "",
				"INR",
				"7", // wrong, but time not annotated
				"Protime", "18.0", "INR", "1.9", "GFR", "73", "Osmolality",
				"281", "A/G Ratio", "0.7", "RBC, UA", "1", "WBC, UA", "5",
				"CKMB", "10", // wrong, but time not annotated
				"Total CK", "125", "CK-MB", "1.3");

		// no lab mentions except in lab sections
		jCas = processNonLabText(text);
		assertLabMentions(jCas);
	}

	@Test
	public void testRanges() throws UIMAException {
		final String text = "Sodium Latest Range: 135-145 mmol/L 138.\n"
				+ "Anion Gap Latest Range: 13-16 mmol/L.\n"
				+ "Potassium Latest Range: 3.5-5.3 mmol/L 3.8.\n"
				+ // range not annotated
				"TSH, High Sensitivity Latest Range: 0.450-5.100 uIU/mL 1.939.\n"
				+ // range not annotated
				"LDL/HDL Ratio No range found 2.6.\n";
		final JCas jCas = processLabText(text);
		assertLabMentions(jCas, "Sodium", "138", "Anion Gap", "13-16", // nothing
																		// but
																		// range
																		// available,
																		// so we
																		// use
																		// that
				"Potassium", "3.5", // should be "3.8", but range not annotated
				"TSH", "0.450", // should be "1.939", but range not annotated
				"LDL/HDL", "2.6");
	}

	@Test
	public void testFreeText() throws UIMAException {
		String text = "Weight / BMI:  Recent weight (as of 05/05/16) is\n"
				+ "45.36 kg (100 lb).\n "
				+ "Hemoglobin is 13.9, hematocrit 47.0, and platelet count\n"
				+ "366,000. CRP was 36.77.  Procalcitonin was 1.32.  Lactate was\n"
				+ "3.9. Free T4 was 1.3.  TSH was 2.82.  Point of care cardiac enzymes\n"
				+ "were normal. CMS was normal except for an elevated potassium of\n"
				+ "6, elevated anion gap of 27, elevated glucose of 153, elevated BUN\n"
				+ "of 80, elevated creatinine of 1.9.  Low GFR 25.\n"
				+ "\n"
				+ "Urinalysis: Specific gravity 1.015, white count was elevated\n"
				+ "29,100, with 69 segs, 20 bands, 5 lymphocytes, and\n"
				+ "6 monos.\n";
		JCas jCas = processLabText(text);
		LOGGER.info("TEXT SPANNING ANALYSIS");
		assertLabMentions(jCas, "Weight", "", "BMI", "", "weight", "45.36 kg",
				"Hemoglobin", "13.9", "hematocrit", "47.0", "platelet count",
				"366,000", "CRP", "36.77", "Procalcitonin", "1.32", "Lactate",
				"3.9", "Free T4", "1.3", "TSH", "2.82", "cardiac enzymes",
				"normal", "potassium", "6", "anion gap", "27", "glucose",
				"153", "BUN", "80", "creatinine", "1.9", "GFR", "25",
				"Specific gravity", "1.015", "white count", "29,100",
				"lymphocytes", "6" // Should be "5", but LabsAnnotator doesn't
									// handle values before words
		);
		jCas = processWithoutSpanningNewlines(text);
		LOGGER.info("NO TEXT SPANNING ANALYSIS");
		assertLabMentions(jCas, "Weight", "", "BMI", "", "weight", "",
				"Hemoglobin", "13.9", "hematocrit", "47.0", "platelet count",
				"", "CRP", "36.77", "Procalcitonin", "1.32", "Lactate", "",
				"Free T4", "1.3", "TSH", "2.82", "cardiac enzymes", "",
				"potassium", "", "anion gap", "27", "glucose", "153", "BUN",
				"", "creatinine", "1.9", "GFR", "25", "Specific gravity",
				"1.015", "white count", "elevated", // , // number on next line,
													// so went for the word
				"lymphocytes", "");
	}

	private JCas processLabText(final String text) throws UIMAException {
		return processText(text, true, true);
	}

	private JCas processNonLabText(final String text) throws UIMAException {
		return processText(text, false, true);
	}

	private JCas processWithoutSpanningNewlines(final String text)
			throws UIMAException {
		return processText(text, true, false);
	}

	private JCas processText(final String text, final boolean isLabText,
			final boolean spanNewlines) throws UIMAException {
		final JCas jCas = JCasFactory.createJCas();
		jCas.setDocumentText(text);
		SimplePipeline.runPipeline(jCas, (isLabText) ? labSegmentator
				: simpleSegmentator, midPipeline,
				(spanNewlines) ? defaultLabAnnotator : sameLineLabAnnotator);
		return jCas;
	}

	private void assertLabMentions(final JCas jCas, final String... expected) {
		final List<LabMention> labs = new ArrayList<>(JCasUtil.select(jCas,
				LabMention.class));
		Hashtable<String, LabMention> tbtable = new Hashtable<String, LabMention>();
		for (LabMention l : labs) {
			tbtable.put(l.getCoveredText(), l);
		}
		printLabMentions(jCas);
		int expectedLength = expected.length;
		assertEquals("Number of labs ", 0, expectedLength % 2);
		assertEquals("Number of labs ", expectedLength / 2, labs.size());
		for (int i = 0; i < expectedLength; i += 2) {
			final LabMention lab = tbtable.get(expected[i]); // expected
																// coveredText
			if (lab == null) {
				System.out.println("FAIL the labMention for " + expected[i]
						+ " was not found \n");
			} else {
				assertEquals("Param", expected[i], lab.getCoveredText());
				String lmValue = getLabMentionValue(lab);
				if (lmValue != null) {
					this.assertEquals("Value " + expected[i], expected[i + 1],
							lmValue);
				} else {
					this.assertEquals("Param " + lab.getCoveredText()
							+ " without value", expected[i + 1], "");
				}
			}
		}
	}

	private String getLabMentionValue(LabMention lab) {
		if (lab.getLabValue() != null && lab.getLabValue().getArg2() != null
				&& lab.getLabValue().getArg2().getArgument() != null) {
			return lab.getLabValue().getArg2().getArgument().getCoveredText();
		}
		return null;
	}

	private void assertEquals(String string, Object i, Object j) {
		if (!i.equals(j)) {
			System.out.println(string + " FAIL " + i + "!=" + j + "\n");
		} else {
			System.out.println(string + " OK " + i + "==" + j + "\n");
		}
	}

	private void printLabMentions(final JCas jCas) {
		for (Segment segment : JCasUtil.select(jCas, Segment.class)) {
			final Collection<LabMention> labs = JCasUtil.selectCovered(jCas,
					LabMention.class, segment);
			LOGGER.info("Section " + segment.getPreferredText() + " ("
					+ segment.getId() + "): " + labs.size() + " lab(s)");
			for (LabMention lab : labs) {
				if (lab.getLabValue() != null
						&& lab.getLabValue().getArg2() != null
						&& lab.getLabValue().getArg2().getArgument() != null) {
					LOGGER.info("   "
							+ getDebugText(lab)
							+ " value: "
							+ getDebugText(lab.getLabValue().getArg2()
									.getArgument()));
				} else {
					LOGGER.info("   " + getDebugText(lab) + " no value");
				}
			}
		}
	}

	static private String getDebugText(final Annotation a) {
		return a.getType().getShortName() + "(" + a.getBegin() + "-"
				+ a.getEnd() + "): " + a.getCoveredText();
	}

}
