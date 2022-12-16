package org.apache.ctakes.constituency.parser.util;

import org.apache.ctakes.constituency.parser.ae.ConstituencyParser;
import org.apache.ctakes.core.ae.SentenceDetector;
import org.apache.ctakes.core.ae.SentenceDetectorAnnotatorBIO;
import org.apache.ctakes.core.ae.SimpleSegmentAnnotator;
import org.apache.ctakes.core.ae.TokenizerAnnotatorPTB;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.fit.factory.AggregateBuilder;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.xml.sax.SAXException;
//import sun.java2d.pipe.SpanShapeRenderer;

import java.io.FileWriter;
import java.io.IOException;

/**
 * Created by miller on 7/4/17.
 */
public class GenerateDescriptors {
    public static void main(String[] args) throws UIMAException, IOException, SAXException {
        AggregateBuilder builder = new AggregateBuilder();
        builder.add(AnalysisEngineFactory.createEngineDescription(SimpleSegmentAnnotator.class));
        builder.add(AnalysisEngineFactory.createEngineDescription(
                SentenceDetector.class,
                SentenceDetector.SD_MODEL_FILE_PARAM,
                "org/apache/ctakes/core/models/sentdetect/sd-med-model.zip"));
        builder.add(AnalysisEngineFactory.createEngineDescription(TokenizerAnnotatorPTB.class));
        builder.add(AnalysisEngineFactory.createEngineDescription(ConstituencyParser.class));
        AnalysisEngineDescription defaultDescriptor = builder.createAggregateDescription();
        defaultDescriptor.toXML(new FileWriter("desc/analysis_engine/DefaultAggregateParsingProcessor.xml"));

        builder = new AggregateBuilder();
        builder.add(SimpleSegmentAnnotator.createAnnotatorDescription());
        builder.add(SentenceDetectorAnnotatorBIO.getDescription());
        builder.add(TokenizerAnnotatorPTB.createAnnotatorDescription());
        builder.add(ConstituencyParser.createAnnotatorDescription());
        AnalysisEngineDescription mimicDescriptor = builder.createAggregateDescription();
        mimicDescriptor.toXML(new FileWriter("desc/analysis_engine/MimicAggregateParsingProcessor.xml"));
    }
}
