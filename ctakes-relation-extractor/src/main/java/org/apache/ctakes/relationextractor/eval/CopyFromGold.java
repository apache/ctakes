package org.apache.ctakes.relationextractor.eval;

import com.google.common.collect.Lists;
import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.Feature;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.TOP;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.CasCopier;

@PipeBitInfo(
        name = "Gold Annotation Copier",
        description = "Copies an annotation type from the Gold view to the System view.",
        role = PipeBitInfo.Role.SPECIAL
)
public class CopyFromGold extends JCasAnnotator_ImplBase {
    public static AnalysisEngineDescription getDescription(String goldViewName, Class<?>... classes )
            throws ResourceInitializationException {
        return AnalysisEngineFactory.createEngineDescription(
                CopyFromGold.class,
                CopyFromGold.PARAM_GOLD_VIEW_NAME,
                goldViewName,
                CopyFromGold.PARAM_ANNOTATION_CLASSES,
                classes );
    }

    public static final String PARAM_ANNOTATION_CLASSES = "AnnotationClasses";

    @ConfigurationParameter( name = PARAM_ANNOTATION_CLASSES, mandatory = true )
    private Class<? extends TOP>[] annotationClasses;

    public static final String PARAM_GOLD_VIEW_NAME = "GoldViewName";
    @ConfigurationParameter( name = PARAM_GOLD_VIEW_NAME, mandatory = true )
    private String goldViewName;

    @Override
    public void process( JCas jCas ) throws AnalysisEngineProcessException {
        JCas goldView, systemView;
        try {
            goldView = jCas.getView( goldViewName );
            systemView = jCas.getView( CAS.NAME_DEFAULT_SOFA );
        } catch ( CASException e ) {
            throw new AnalysisEngineProcessException( e );
        }
        for ( Class<? extends TOP> annotationClass : this.annotationClasses ) {
            for ( TOP annotation : Lists.<TOP>newArrayList(JCasUtil.select(systemView, annotationClass))) {
                if ( annotation.getClass().equals( annotationClass ) ) {
                    annotation.removeFromIndexes();
                }
            }
        }
        CasCopier copier = new CasCopier( goldView.getCas(), systemView.getCas() );
        Feature sofaFeature = jCas.getTypeSystem().getFeatureByFullName( CAS.FEATURE_FULL_NAME_SOFA );
        for ( Class<? extends TOP> annotationClass : this.annotationClasses ) {
            for ( TOP annotation : JCasUtil.select( goldView, annotationClass ) ) {
                TOP copy = (TOP)copier.copyFs( annotation );
                if ( copy instanceof Annotation) {
                    copy.setFeatureValue( sofaFeature, systemView.getSofa() );
                }
                copy.addToIndexes( systemView );
            }
        }
    }
}
