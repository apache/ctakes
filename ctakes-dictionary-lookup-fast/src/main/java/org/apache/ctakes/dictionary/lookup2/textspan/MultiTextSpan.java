/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.ctakes.dictionary.lookup2.textspan;


import javax.annotation.concurrent.Immutable;
import java.util.Collection;

/**
 * A useful key for hash collections based upon start and end indices and missing internal spans.
 * This is faster than using String as {@link String#hashCode()}
 * iterates over the internal character array of a new string (new(..), .substring(..), .lowercase(..), ...).
 * <p/>
 * There is a much better version of this in org.chboston.chip.nlp.annotation but this will do for now.
 */
@Immutable
final public class MultiTextSpan implements TextSpan {

   final private int _start;
   final private int _end;
   final private Collection<TextSpan> _missingSpans;
   final private int _hashCode;

   /**
    * Given span indices should be ordered start < end, but it is not an absolute requirement.
    *
    * @param start start index of a span, be it of a string or other
    * @param end   end index of a span,  be it of a  string or other
    */
   public MultiTextSpan( final int start, final int end, final Collection<TextSpan> missingSpans ) {
      _start = start;
      _end = end;
      _missingSpans = missingSpans;
      _hashCode = 1000 * _end + _start + missingSpans.hashCode();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public int getStart() {
      return _start;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public int getEnd() {
      return _end;
   }

   /**
    * return the length of the full span minus the lengths of the missing spans
    * {@inheritDoc}
    */
   @Override
   public int getLength() {
      int length = _end - _start + 1;
      for ( TextSpan missingSpan : _missingSpans ) {
         length -= missingSpan.getLength();
      }
      return length;
   }


   public Collection<TextSpan> getMissingSpans() {
      return _missingSpans;
   }

   /**
    * {@inheritDoc}
    *
    * @return a hashcode based upon the start and end indices of this span key
    */
   @Override
   public int hashCode() {
      return _hashCode;
   }

   /**
    * {@inheritDoc}
    *
    * @return true iff the start keys are equal and the end keys are equal
    */
   @Override
   public boolean equals( final Object object ) {
      return object instanceof MultiTextSpan
             && _start == ((MultiTextSpan)object)._start
             && _end == ((MultiTextSpan)object)._end
             && _missingSpans.equals( ((MultiTextSpan)object)._missingSpans );
   }

   /**
    * {@inheritDoc}
    *
    * @return "Discontiguous TextSpan for span [start index] to [end index] but missing:\n[missing spans]"
    */
   @Override
   public String toString() {
      final StringBuilder stringBuilder = new StringBuilder();
      stringBuilder.append( "Discontiguous TextSpan for span " ).append( _start ).append( " to " ).append( _end );
      stringBuilder.append( " but missing:\n" );
      for ( TextSpan textSpan : _missingSpans ) {
         stringBuilder.append( "   " ).append( textSpan.toString() ).append( '\n' );
      }
      return stringBuilder.toString();
   }

}
