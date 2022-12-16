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

/**
 * A useful key for hash collections based upon start and end indices.
 * This is faster than using String as {@link String#hashCode()}
 * iterates over the internal character array of a new string (new(..), .substring(..), .lowercase(..), ...).
 */
@Immutable
final public class DefaultTextSpan implements TextSpan {

   final private int _start;
   final private int _end;
   final private int _hashCode;

   /**
    * Given span indices should be ordered start < end, but it is not an absolute requirement.
    *
    * @param start start index of a span, be it of a string or other
    * @param end   end index of a span,  be it of a  string or other
    */
   public DefaultTextSpan( final int start, final int end ) {
      _start = start;
      _end = end;
      _hashCode = 1000 * _end + _start;
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
    * {@inheritDoc}
    */
   @Override
   public int getLength() {
      return _end - _start + 1;
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
      return object instanceof DefaultTextSpan
             && _start == ((DefaultTextSpan)object)._start
             && _end == ((DefaultTextSpan)object)._end;
   }

   /**
    * {@inheritDoc}
    *
    * @return "TextSpan for span [start index] to [end index]"
    */
   @Override
   public String toString() {
      return "TextSpan for span " + _start + " to " + _end;
   }
}
