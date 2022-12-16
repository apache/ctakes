package org.apache.ctakes.coreference.ae.pairing.cluster;

import org.apache.ctakes.coreference.util.ClusterMentionFetcher;
import org.apache.ctakes.typesystem.type.textsem.Markable;
import org.apache.uima.jcas.JCas;

import java.util.List;

/**
 * Created by tmill on 3/22/18.
 */
public abstract class CrossDocumentPairer_ImplBase extends ClusterMentionPairer_ImplBase{
    public abstract List<ClusterMentionFetcher.CollectionTextRelationIdentifiedAnnotationPair> getPairs(JCas jcas, Markable m, JCas prevCas);

    @Override
    public List<ClusterMentionFetcher.CollectionTextRelationIdentifiedAnnotationPair> getPairs(JCas jcas, Markable m){
        return getPairs(jcas, m, null);
    }
}
