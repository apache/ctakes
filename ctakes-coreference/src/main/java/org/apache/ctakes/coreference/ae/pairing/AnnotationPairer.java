package org.apache.ctakes.coreference.ae.pairing;

import org.apache.uima.jcas.JCas;

import java.util.List;

public interface AnnotationPairer<MENTION_TYPE,PAIR_TYPE> {
  public List<PAIR_TYPE> getPairs(JCas jcas, MENTION_TYPE mention);
  public void reset(JCas jcas);
}
