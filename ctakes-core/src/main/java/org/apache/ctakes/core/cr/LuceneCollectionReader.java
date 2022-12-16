package org.apache.ctakes.core.cr;

import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.typesystem.type.structured.DocumentID;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.uima.UimaContext;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.fit.component.CasCollectionReader_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.internal.util.XMLUtils;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Progress;
import org.apache.uima.util.ProgressImpl;

import java.io.File;
import java.io.IOException;

@PipeBitInfo(
      name = "Lucene Field Reader",
      description = "Reads document texts from Lucene text fields.",
      role = PipeBitInfo.Role.READER,
      products = { PipeBitInfo.TypeProduct.DOCUMENT_ID }
)
public class LuceneCollectionReader extends CasCollectionReader_ImplBase {

  public static final String PARAM_INDEX_DIR = "IndexDirectory";
  @ConfigurationParameter(
      name = PARAM_INDEX_DIR,
      description = "Location of lucene index",
      mandatory = true
      )
  private String indexDir;
  
  public static final String PARAM_FIELD_NAME = "FieldName";
  @ConfigurationParameter(
      name = PARAM_FIELD_NAME,
      description = "Field to look in for document text",
      mandatory = false
      )
  private String fieldName = "text";
  
  public static final String PARAM_MAX_WORDS = "MaxWords";
  @ConfigurationParameter(
      name = PARAM_MAX_WORDS,
      description = "Maximum number of words to process (approximate -- actually based on characters)",
      mandatory = false
      )
  private int maxWords = -1;
  
  private int docNum = 0;
  private DirectoryReader ireader = null;
  private int wordNum = 0;
  public static final int CHARS_PER_WORD = 6;
  
  @Override
  public void initialize(UimaContext context)
      throws ResourceInitializationException {
    super.initialize(context);
    
    Directory dir;
    try {
      dir = FSDirectory.open(new File(indexDir));
      ireader = DirectoryReader.open(dir);
    } catch (IOException e) {
      e.printStackTrace();
      throw new ResourceInitializationException(e);
    }
 }

  @Override
  public void getNext(CAS cas) throws IOException, CollectionException {
    JCas jcas = null;
    try {
      jcas = cas.getJCas();
    } catch (CASException e) {
      e.printStackTrace();
      throw new IOException(e);
    }
    
    Document doc = ireader.document(docNum++);
    IndexableField textField = doc.getField(fieldName);
    while(textField == null){
      doc = ireader.document(docNum++);
      textField = doc.getField(fieldName);
    }
    StringBuffer text = new StringBuffer(textField.stringValue());
    int pos;
    while((pos = XMLUtils.checkForNonXmlCharacters(text.toString())) != -1){
      text.setCharAt(pos, ' ');
    }
    jcas.setDocumentText(text.toString().replaceAll("__+", " "));
    DocumentID docId = new DocumentID(jcas);
    docId.setDocumentID("doc" + docNum);
    docId.addToIndexes();
    
    wordNum += text.length() / CHARS_PER_WORD;
  }

  @Override
  public Progress[] getProgress() {
    return new Progress[]{ (maxWords < 0 ? new ProgressImpl(docNum, ireader.numDocs(), "Documents") : new ProgressImpl(wordNum, maxWords, "Words"))};
  }

  @Override
  public boolean hasNext() throws IOException, CollectionException {
    return (docNum < ireader.numDocs()) &&
        (maxWords < 0 || wordNum < maxWords);
  }

}
