package org.apache.ctakes.gui.pipeline.bit.user;

import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.gui.pipeline.bit.parameter.ParameterHolder;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 1/20/2017
 */
public interface UserBit extends ParameterHolder {

   /**
    * @return Human-readable name of the Reader, Annotator, or Writer
    */
   String getBitName();

   /**
    * @param name Human-readable name of the Reader, Annotator, or Writer
    */
   void setBitName( String name );

   /**
    * @return PipeBitInfo associated with this UserBit
    */
   PipeBitInfo getPipeBitInfo();

   /**
    * @return Reader, AE, Writer associated with this UserBit
    */
   Class<?> getPipeBitClass();

   /**
    * @param index  -
    * @param values User Values for Configuration parameter at index
    */
   void setParameterValue( int index, String... values );

}
