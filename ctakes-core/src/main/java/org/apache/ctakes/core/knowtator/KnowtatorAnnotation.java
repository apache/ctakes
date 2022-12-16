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
package org.apache.ctakes.core.knowtator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.base.Objects;
import com.google.common.base.Objects.ToStringHelper;

/**
 * Represents a Knowtator annotation.
 */
public class KnowtatorAnnotation {
  /**
   * The unique identifier assigned to this annotation by Knowtator
   */
  public String id;

  /**
   * The character offsets of this annotation (empty if not associated with a span of text).
   */
  public List<Span> spans = new ArrayList<Span>();

  /**
   * Get a span that approximates {@link #spans}, giving the earliest begin offset and the latest
   * end offset.
   */
  public Span getCoveringSpan() {
    int begin = Integer.MAX_VALUE;
    int end = Integer.MIN_VALUE;
    for (KnowtatorAnnotation.Span span : this.spans) {
      if (span.begin < begin) {
        begin = span.begin;
      }
      if (span.end > end) {
        end = span.end;
      }
    }
    return new Span(begin, end);
  }

  /**
   * Create a new span and add it to the list (not publicly available)
   */
  void addSpan(int begin, int end) {
    this.spans.add(new Span(begin, end));
  }

  /**
   * The text spanned by this annnotation (<code>null</code> if not associated with a span of text).
   */
  public String spannedText;

  /**
   * The type (or "class") of annotation
   */
  public String type;

  /**
   * The string-valued annotation attributes
   */
  public Map<String, String> stringSlots = new HashMap<String, String>();

  /**
   * The boolean-valued annotation attributes
   */
  public Map<String, Boolean> booleanSlots = new HashMap<String, Boolean>();

  /**
   * The annotation-valued annotation attributes (i.e. links between annotations)
   */
  public Map<String, KnowtatorAnnotation> annotationSlots = new HashMap<String, KnowtatorAnnotation>();

  /**
   * Construct a new KnowtatorAnnotation. (Not publicly available.)
   */
  KnowtatorAnnotation() {
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(
        this.id,
        this.spans,
        this.spannedText,
        this.type,
        this.stringSlots,
        this.booleanSlots,
        this.annotationSlots);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null || obj.getClass() != this.getClass()) {
      return false;
    }
    KnowtatorAnnotation that = (KnowtatorAnnotation) obj;
    return Objects.equal(this.id, that.id) && Objects.equal(this.spans, that.spans)
        && Objects.equal(this.spannedText, that.spannedText) && Objects.equal(this.type, that.type)
        && Objects.equal(this.stringSlots, that.stringSlots)
        && Objects.equal(this.booleanSlots, that.booleanSlots)
        && Objects.equal(this.annotationSlots, that.annotationSlots);
  }

  @Override
  public String toString() {
    ToStringHelper builder = Objects.toStringHelper(this);
    builder.add("id", this.id);
    builder.add("spans", this.spans);
    builder.add("spannedText", this.spannedText);
    builder.add("type", this.type);
    builder.add("stringSlots", this.stringSlots);
    builder.add("booleanSlots", this.booleanSlots);
    builder.add("mentionSlots", this.annotationSlots);
    return builder.toString();
  }

  /**
   * Represents the character offsets of a Knowtator annotation.
   */
  public static class Span {
    /**
     * The offset of the first character in the text span.
     */
    public int begin;

    /**
     * The offset immediately after the last character in the text span.
     */
    public int end;

    /**
     * Construct a new Span. (Not publicly available.)
     */
    Span(int begin, int end) {
      this.begin = begin;
      this.end = end;
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(this.begin, this.end);
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == null || obj.getClass() != this.getClass()) {
        return false;
      }
      Span that = (Span) obj;
      return this.begin == that.begin && this.end == that.end;
    }

    @Override
    public String toString() {
      ToStringHelper builder = Objects.toStringHelper(this);
      builder.add("begin", this.begin);
      builder.add("end", this.end);
      return builder.toString();
    }

  }

}