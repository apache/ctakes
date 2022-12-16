package org.apache.ctakes.coreference.util;

import org.apache.ctakes.typesystem.type.syntax.ConllDependencyNode;
import org.apache.ctakes.typesystem.type.textsem.Markable;

import java.util.Map;

/**
 * Created by tmill on 11/2/17.
 */
public interface MarkableCacheRelationExtractor {
    public void setCache(Map<Markable, ConllDependencyNode> cache);
}
