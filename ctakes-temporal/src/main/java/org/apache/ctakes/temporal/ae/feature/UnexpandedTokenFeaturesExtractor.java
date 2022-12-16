package org.apache.ctakes.temporal.ae.feature;

import java.util.ArrayList;
import java.util.List;

import org.apache.ctakes.relationextractor.ae.features.TokenFeaturesExtractor;
import org.apache.ctakes.typesystem.type.syntax.BaseToken;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.cleartk.ml.Feature;
import org.cleartk.ml.feature.extractor.CleartkExtractor;
import org.cleartk.ml.feature.extractor.CleartkExtractor.Bag;
import org.cleartk.ml.feature.extractor.CleartkExtractor.Covered;
import org.cleartk.ml.feature.extractor.CleartkExtractor.FirstCovered;
import org.cleartk.ml.feature.extractor.CleartkExtractor.Following;
import org.cleartk.ml.feature.extractor.CleartkExtractor.LastCovered;
import org.cleartk.ml.feature.extractor.CleartkExtractor.Preceding;
import org.cleartk.ml.feature.extractor.DistanceExtractor;
import org.cleartk.ml.feature.extractor.CoveredTextExtractor;
import org.cleartk.ml.feature.extractor.NamingExtractor1;
import org.cleartk.ml.feature.extractor.FeatureExtractor1;
import org.cleartk.ml.feature.extractor.CombinedExtractor1;

public class UnexpandedTokenFeaturesExtractor extends TokenFeaturesExtractor {

	private FeatureExtractor1 coveredText = new CoveredTextExtractor();

	/**
	 * First word of the mention, last word of the mention, all words of the mention as a bag, the
	 * preceding 3 words, the following 3 words
	 */
	private FeatureExtractor1 tokenContext = new CleartkExtractor(
			BaseToken.class,
			coveredText,
			new FirstCovered(1),
			new LastCovered(1),
			new Bag(new Covered()),
			new Preceding(3),
			new Following(3));

	/**
	 * All extractors for mention 1, with features named to distinguish them from mention 2
	 */
	private FeatureExtractor1 mention1FeaturesExtractor = new NamingExtractor1(
			"mention1",
			new CombinedExtractor1(coveredText, tokenContext));

	/**
	 * All extractors for mention 2, with features named to distinguish them from mention 1
	 */
	private FeatureExtractor1 mention2FeaturesExtractor = new NamingExtractor1(
			"mention2",
			new CombinedExtractor1(coveredText, tokenContext));

	/**
	 * First word, last word, and all words between the mentions
	 */
	private CleartkExtractor tokensBetween = new CleartkExtractor(
			BaseToken.class,
			new NamingExtractor1("BetweenMentions", coveredText),
			new FirstCovered(1),
			new LastCovered(1),
			new Bag(new Covered()));

	/**
	 * Number of words between the mentions
	 */
	private DistanceExtractor nTokensBetween = new DistanceExtractor(null, BaseToken.class);

	@Override
	public List<Feature> extract(JCas jCas, IdentifiedAnnotation mention1, IdentifiedAnnotation mention2)
			throws AnalysisEngineProcessException {
		List<Feature> features = new ArrayList<>();
		Annotation arg1 = mention1;
		Annotation arg2 = mention2;

		//no expansion to NP for each event

		features.addAll(this.mention1FeaturesExtractor.extract(jCas, arg1));
		features.addAll(this.mention2FeaturesExtractor.extract(jCas, arg2));
		features.addAll(this.tokensBetween.extractBetween(jCas, arg1, arg2));
		features.addAll(this.nTokensBetween.extract(jCas, arg1, arg2));
		return features;
	}

}
