package org.apache.ctakes.temporal.ae.feature;

import org.apache.ctakes.relationextractor.ae.features.RelationFeaturesExtractor;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.cleartk.ml.Feature;

import java.util.List;

public class EmptyFeaturesExtractor implements RelationFeaturesExtractor<IdentifiedAnnotation,IdentifiedAnnotation> {

	public List<Feature> extract(JCas jCas, IdentifiedAnnotation arg1,
			IdentifiedAnnotation arg2) throws AnalysisEngineProcessException {
		// TODO Auto-generated method stub
		return null;
	}
 //do noting
}
