package org.apache.ctakes.core.ae;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 2/3/2018
 */
public interface NamedEngine {

   default String getEngineName() {
      return getClass().getSimpleName();
   }

}
