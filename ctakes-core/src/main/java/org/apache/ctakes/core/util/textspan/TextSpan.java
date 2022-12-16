package org.apache.ctakes.core.util.textspan;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 7/6/2015
 */
public interface TextSpan {

   /**
    * @return begin offset
    */
   int getBegin();

   /**
    * @return end offset
    */
   int getEnd();

   /**
    * @return width of the text span
    */
   int getWidth();

   /**
    * @param textSpan another text span
    * @return true if the text spans overlap
    */
   boolean overlaps( TextSpan textSpan );

   /**
    * @param textSpan another text span
    * @return true if this text span contains the other
    */
   boolean contains( TextSpan textSpan );

}
