package org.apache.ctakes.gui.pipeline.bit.parameter;

import org.apache.uima.fit.descriptor.ConfigurationParameter;


/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 3/20/2017
 */
public interface ParameterHolder {

   int getParameterCount();

   ConfigurationParameter getParameter( int index );

   String getParameterClass( int index );

   String getParameterName( int index );

   String getParameterDescription( int index );

   boolean isParameterMandatory( int index );

   String[] getParameterValue( int index );

}
