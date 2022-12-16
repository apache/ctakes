package org.apache.ctakes.temporal.ae.feature;

import java.util.ArrayList;
import java.util.List;

import org.apache.ctakes.relationextractor.ae.features.RelationFeaturesExtractor;
import org.apache.ctakes.typesystem.type.syntax.BaseToken;
import org.apache.ctakes.typesystem.type.textsem.AnatomicalSiteMention;
import org.apache.ctakes.typesystem.type.textsem.DiseaseDisorderMention;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textsem.MedicationMention;
import org.apache.ctakes.typesystem.type.textsem.ProcedureMention;
import org.apache.ctakes.typesystem.type.textsem.SignSymptomMention;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.cleartk.ml.Feature;
import org.apache.uima.fit.util.JCasUtil;

public class TokenPropertyFeaturesExtractor implements
RelationFeaturesExtractor<IdentifiedAnnotation,IdentifiedAnnotation> {

	@SuppressWarnings("null")
	@Override
	public List<Feature> extract(JCas jCas, IdentifiedAnnotation arg1,
			IdentifiedAnnotation arg2) throws AnalysisEngineProcessException {
		ArrayList<Feature> feats = new ArrayList<>();

		List<BaseToken> arg1Tokens = JCasUtil.selectCovered(jCas, BaseToken.class, arg1);
		List<BaseToken> arg2Tokens = JCasUtil.selectCovered(jCas, BaseToken.class, arg2);

		int arg1Length = arg1Tokens == null ? 0 : arg1Tokens.size();
		int arg2Length = arg2Tokens == null ? 0 : arg2Tokens.size();

		//get token size feature:
		feats.add(new Feature("arg1_tokenSize", arg1Length));
		feats.add(new Feature("arg2_tokenSize", arg2Length));

		for(BaseToken bt : arg1Tokens){
			if(bt.getPartOfSpeech().startsWith("VB")){
				feats.add(new Feature("arg1_contains", "VB"));
				break;
			}
		}
		for(BaseToken bt : arg2Tokens){
			if(bt.getPartOfSpeech().startsWith("VB")){
				feats.add(new Feature("arg2_contains", "VB"));
				break;
			}
		}

		if(arg1Length == 0 || arg2Length == 0){
			return feats;
		}

		//check if the last token match:
		String arg1last = arg1Tokens.get(arg1Length-1).getCoveredText().toLowerCase();
		String arg2last = arg2Tokens.get(arg2Length-1).getCoveredText().toLowerCase();
		if(arg1last.equals(arg2last)){
			feats.add(new Feature("contain_matching_last_token", true));
			feats.add(new Feature("matching_last_token_", arg1last));
		}

		//check if contains the same anatomical sites:
		List<AnatomicalSiteMention> arg1anaSites = JCasUtil.selectCovered(jCas, AnatomicalSiteMention.class, arg1);
		List<AnatomicalSiteMention> arg2anaSites = JCasUtil.selectCovered(jCas, AnatomicalSiteMention.class, arg2);
		for(AnatomicalSiteMention siteA : arg1anaSites){
			for(AnatomicalSiteMention siteB : arg2anaSites){
				if(siteA.getCoveredText().equalsIgnoreCase(siteB.getCoveredText())){
					feats.add(new Feature("contain_matching_anatomicalSite", true));
					feats.add(new Feature("matching_anatomicalSite_", siteA.getCoveredText().toLowerCase()));
				}
			}
		}

		//check if contains the same procedure:
		List<ProcedureMention> arg1procedure = JCasUtil.selectCovered(jCas, ProcedureMention.class, arg1);
		List<ProcedureMention> arg2procedure = JCasUtil.selectCovered(jCas, ProcedureMention.class, arg2);
		for(ProcedureMention proA : arg1procedure){
			for(ProcedureMention proB : arg2procedure){
				if(proA.getCoveredText().equalsIgnoreCase(proB.getCoveredText())){
					feats.add(new Feature("contain_matching_Procedure", true));
					feats.add(new Feature("matching_Procedure_", proA.getCoveredText().toLowerCase()));
				}
			}
		}

		//check if contains the same Sign and Symptom:
		List<SignSymptomMention> arg1ss = JCasUtil.selectCovered(jCas, SignSymptomMention.class, arg1);
		List<SignSymptomMention> arg2ss = JCasUtil.selectCovered(jCas, SignSymptomMention.class, arg2);
		for(SignSymptomMention ssA : arg1ss){
			for(SignSymptomMention ssB : arg2ss){
				if(ssA.getCoveredText().equalsIgnoreCase(ssB.getCoveredText())){
					feats.add(new Feature("contain_matching_SignSymptom", true));
					feats.add(new Feature("matching_SignSymptom_", ssA.getCoveredText().toLowerCase()));
				}
			}
		}

		//check if contains the same Disease Disorder:
		List<DiseaseDisorderMention> arg1dd = JCasUtil.selectCovered(jCas, DiseaseDisorderMention.class, arg1);
		List<DiseaseDisorderMention> arg2dd = JCasUtil.selectCovered(jCas, DiseaseDisorderMention.class, arg2);
		for(DiseaseDisorderMention ddA : arg1dd){
			for(DiseaseDisorderMention ddB : arg2dd){
				if(ddA.getCoveredText().equalsIgnoreCase(ddB.getCoveredText())){
					feats.add(new Feature("contain_matching_DiseaseDisorder", true));
					feats.add(new Feature("matching_DiseaseDisorder_", ddA.getCoveredText().toLowerCase()));
				}
			}
		}

		//check if contains the same Medication:
		List<MedicationMention> arg1med = JCasUtil.selectCovered(jCas, MedicationMention.class, arg1);
		List<MedicationMention> arg2med = JCasUtil.selectCovered(jCas, MedicationMention.class, arg2);
		for(MedicationMention medA : arg1med){
			for(MedicationMention medB : arg2med){
				if(medA.getCoveredText().equalsIgnoreCase(medB.getCoveredText())){
					feats.add(new Feature("contain_matching_Medication", true));
					feats.add(new Feature("matching_Medication_", medA.getCoveredText().toLowerCase()));
				}
			}
		}

		return feats;
	}

}
