package org.apache.ctakes.relationextractor.metastasis;

import com.google.common.io.CharStreams;
import org.apache.ctakes.relationextractor.eval.CorpusXMI.CopyDocumentTextToGoldView;
import org.apache.ctakes.relationextractor.eval.CorpusXMI.DocumentIDAnnotator;
import org.apache.uima.UIMAFramework;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.impl.XmiCasSerializer;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.component.ViewCreatorAnnotator;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.factory.AggregateBuilder;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.XMLInputSource;
import org.apache.uima.util.XMLParser;
import org.apache.uima.util.XMLSerializer;
import org.cleartk.util.ViewUriUtil;
import org.cleartk.util.ae.UriToDocumentTextAnnotator;
import org.cleartk.util.cr.UriCollectionReader;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class MetastasisXmiGenerationPipeline {

   //  public static final File ANAFORA_ANNOTATIONS_DIR = new File("DeepPhe/Metastasis/Anafora/All/");
   public static final File ANAFORA_ANNOTATIONS_DIR
         = new File( "\\\\rc-fs.tch.harvard.edu\\chip-nlp\\Public\\DeepPhe\\Metastasis\\Anafora\\Test" );
   public static final String XMI_OUTPUT_DIR = "C:\\Spiffy\\prj_darth_phenome\\output\\temp\\metastatic\\Test";
  public static final String GOLD_VIEW_NAME = "GoldView";

  public static void main(String[] args) throws Exception {
    
    List<File> files = new ArrayList<>();
    // notes have the same names as the directories in which they exist
    for(File anaforaNoteDir : ANAFORA_ANNOTATIONS_DIR.listFiles()) {
      String noteFileName = anaforaNoteDir.getName(); 
      String noteFullPath = anaforaNoteDir.getAbsolutePath() + "/" + noteFileName;
      files.add(new File(noteFullPath));
    }
    
    CollectionReader reader = UriCollectionReader.getCollectionReaderFromFiles(files);
    AnalysisEngine engine = getXMIWritingPreprocessorAggregateBuilder().createAggregate();
    SimplePipeline.runPipeline(reader, engine);
  }

  protected static AggregateBuilder getXMIWritingPreprocessorAggregateBuilder() throws Exception {

    AggregateBuilder builder = new AggregateBuilder();
    builder.add(UriToDocumentTextAnnotator.getDescription());

     File preprocessDescFile
           = new File( "C:\\Spiffy\\ctakes_trunk_intellij\\dev\\apache\\ctakes-relation-extractor\\desc\\analysis_engine/RelationExtractorPreprocessor.xml" );
    XMLParser parser = UIMAFramework.getXMLParser();
    XMLInputSource source = new XMLInputSource(preprocessDescFile);
    builder.add(parser.parseAnalysisEngineDescription(source));
    
    builder.add(AnalysisEngineFactory.createEngineDescription(
        ViewCreatorAnnotator.class,
        ViewCreatorAnnotator.PARAM_VIEW_NAME,
        GOLD_VIEW_NAME));

    builder.add(AnalysisEngineFactory.createEngineDescription(CopyDocumentTextToGoldView.class));
    
    builder.add(
        AnalysisEngineFactory.createEngineDescription(DocumentIDAnnotator.class),
        CAS.NAME_DEFAULT_SOFA,
        GOLD_VIEW_NAME);
    
    builder.add(
        MetastasisAnaforaXMLReader.getDescription(), 
        CAS.NAME_DEFAULT_SOFA,
        GOLD_VIEW_NAME); // this tells it to create all annotation in gold view!

    // write out the CAS after all the above annotations
    builder.add(AnalysisEngineFactory.createEngineDescription(
        XMIWriter.class,
        XMIWriter.PARAM_XMI_DIRECTORY,
        XMI_OUTPUT_DIR));

    return builder;
  }

  /* 
   * The following class overrides a ClearTK utility annotator class for reading
   * a text file into a JCas. The code is copy/pasted so that one tiny modification
   * can be made for this corpus -- replace a single odd character (0xc) with a 
   * space since it trips up xml output.  
   */
  public static class UriToDocumentTextAnnotatorCtakes extends UriToDocumentTextAnnotator {

    @Override
    public void process(JCas jCas) throws AnalysisEngineProcessException {
      URI uri = ViewUriUtil.getURI(jCas);
      String content;

      try {
        content = CharStreams.toString(new InputStreamReader(uri.toURL().openStream()));
        content = content.replace((char) 0xc, ' ');
        jCas.setSofaDataString(content, "text/plain");
      } catch (MalformedURLException e) {
        throw new AnalysisEngineProcessException(e);
      } catch (IOException e) {
        throw new AnalysisEngineProcessException(e);
      }
    }  
  }

  public static class XMIWriter extends JCasAnnotator_ImplBase {

    public static final String PARAM_XMI_DIRECTORY = "XMIDirectory";

    @ConfigurationParameter(name = PARAM_XMI_DIRECTORY, mandatory = true)
    private File xmiDirectory;

    @Override
    public void initialize(UimaContext context) throws ResourceInitializationException {
      super.initialize(context);
      if (!this.xmiDirectory.exists()) {
        this.xmiDirectory.mkdirs();
      }
    }

    @Override
    public void process(JCas jCas) throws AnalysisEngineProcessException {
      File xmiFile = getXMIFile(this.xmiDirectory, jCas);
      try {
        FileOutputStream outputStream = new FileOutputStream(xmiFile);
        try {
          XmiCasSerializer serializer = new XmiCasSerializer(jCas.getTypeSystem());
          ContentHandler handler = new XMLSerializer(outputStream, false).getContentHandler();
          serializer.serialize(jCas.getCas(), handler);
        } finally {
          outputStream.close();
        }
      } catch (SAXException e) {
        throw new AnalysisEngineProcessException(e);
      } catch (IOException e) {
        throw new AnalysisEngineProcessException(e);
      }
    }
  }

  static File getXMIFile(File xmiDirectory, JCas jCas) throws AnalysisEngineProcessException {
    return getXMIFile(xmiDirectory, new File(ViewUriUtil.getURI(jCas).getPath()));
  }

  static File getXMIFile(File xmiDirectory, File textFile) {
    return new File(xmiDirectory, textFile.getName() + ".xmi");
  }
}
