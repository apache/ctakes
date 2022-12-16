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

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.log4j.Logger;

import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.XMLOutputter;

public class KnowtatorXMLParser {

  private static final Logger LOGGER = Logger.getLogger(KnowtatorXMLParser.class.getName());

  private XMLOutputter xmlOutputter = new XMLOutputter();

  private Set<String> annotatorNames;

  public KnowtatorXMLParser(String... annotatorNames) {
    this(new HashSet<String>(Arrays.asList(annotatorNames)));
  }

  public KnowtatorXMLParser(Set<String> annotatorNames) {
    this.annotatorNames = annotatorNames;
  }

  public Collection<KnowtatorAnnotation> parse(URI knowtatorXML) throws JDOMException, IOException {

    Element annotationsElem = new SAXBuilder().build(knowtatorXML.toURL()).getRootElement();

    // parse <annotation> elements
    Set<String> ignoredAnnotators = new HashSet<String>();
    Map<String, KnowtatorAnnotation> annotations = new HashMap<String, KnowtatorAnnotation>();
    for (Element annotationElem : annotationsElem.getChildren("annotation")) {
      for (Element annotatorElem : this.getChild(annotationElem, "annotator")) {
        String annotatorName = annotatorElem.getText();
        if (!this.annotatorNames.contains(annotatorName)) {
          ignoredAnnotators.add(annotatorName);
        } else {
          for (Element mentionElem : this.getChild(annotationElem, "mention")) {
            for (String id : this.getAttributeValue(mentionElem, "id")) {
              KnowtatorAnnotation annotation = new KnowtatorAnnotation();
              annotation.id = id;
              annotations.put(id, annotation);
              List<Element> spanElems = annotationElem.getChildren("span");
              if (!spanElems.isEmpty()) {
                for (Element spannedTextElem : this.getChild(annotationElem, "spannedText")) {
                  annotation.spannedText = spannedTextElem.getText();
                }
                for (Element spanElem : spanElems) {
                  for (String startStr : this.getAttributeValue(spanElem, "start")) {
                    for (String endStr : this.getAttributeValue(spanElem, "end")) {
                      annotation.addSpan(Integer.parseInt(startStr), Integer.parseInt(endStr));
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
    LOGGER.debug(String.format("Ignored annotators %s in %s", ignoredAnnotators, knowtatorXML));

    // parse <stringSlotMention> elements
    Map<String, Slot<String>> stringSlots = new HashMap<String, Slot<String>>();
    for (Element slotMentionElem : annotationsElem.getChildren("stringSlotMention")) {
      for (IdAndSlot<String> idAndSlot : this.parseSlotMention(
          slotMentionElem,
          "stringSlotMentionValue")) {
        stringSlots.put(idAndSlot.id, idAndSlot.slot);
      }
    }

    // parse <booleanSlotMention> elements
    Map<String, Slot<Boolean>> booleanSlots = new HashMap<String, Slot<Boolean>>();
    for (Element slotMentionElem : annotationsElem.getChildren("booleanSlotMention")) {
      for (IdAndSlot<String> idAndSlot : this.parseSlotMention(
          slotMentionElem,
          "booleanSlotMentionValue")) {
        Slot<String> slot = idAndSlot.slot;
        Boolean value = Boolean.parseBoolean(slot.value);
        booleanSlots.put(idAndSlot.id, new Slot<Boolean>(slot.name, value));
      }
    }

    // parse <complexSlotMention> elements
    Map<String, Slot<KnowtatorAnnotation>> mentionSlots = new HashMap<String, Slot<KnowtatorAnnotation>>();
    for (Element slotMentionElem : annotationsElem.getChildren("complexSlotMention")) {
      for (IdAndSlot<String> idAndSlot : this.parseSlotMention(
          slotMentionElem,
          "complexSlotMentionValue")) {
        Slot<String> slot = idAndSlot.slot;
        KnowtatorAnnotation mention = annotations.get(slot.value);
        if (mention != null) {
          mentionSlots.put(idAndSlot.id, new Slot<KnowtatorAnnotation>(slot.name, mention));
        }
      }
    }

    // parse <classMention> elements
    for (Element classMentionElem : annotationsElem.getChildren("classMention")) {
      for (String id : this.getAttributeValue(classMentionElem, "id")) {
        KnowtatorAnnotation annotation = annotations.get(id);
        if (annotation == null) {
          continue;
        }
        annotation.type = classMentionElem.getChildText("mentionClass");
        for (Element hasSlotMentionElem : classMentionElem.getChildren("hasSlotMention")) {
          for (String slotId : this.getAttributeValue(hasSlotMentionElem, "id")) {
            Slot<String> stringSlot = stringSlots.get(slotId);
            if (stringSlot != null) {
              annotation.stringSlots.put(stringSlot.name, stringSlot.value);
            } else {
              Slot<Boolean> booleanSlot = booleanSlots.get(slotId);
              if (booleanSlot != null) {
                annotation.booleanSlots.put(booleanSlot.name, booleanSlot.value);
              } else {
                Slot<KnowtatorAnnotation> mentionSlot = mentionSlots.get(slotId);
                if (mentionSlot != null) {
                  annotation.annotationSlots.put(mentionSlot.name, mentionSlot.value);
                } else {
                  LOGGER.warn("no simple slot for " + slotId);
                }
              }
            }
          }
        }
      }
    }

    return annotations.values();
  }

  private Option<Element> getChild(final Element element, final String cname) {
    final Element child = element.getChild(cname);
    if (child == null) {
      String xml = this.xmlOutputter.outputString(element);
      LOGGER.debug(String.format("no child <%s> for %s", cname, xml));
    }
    return new Option<Element>(child);
  }

  private Option<String> getAttributeValue(final Element element, final String attname) {
    final String value = element.getAttributeValue(attname);
    if (value == null) {
      String xml = this.xmlOutputter.outputString(element);
      LOGGER.debug(String.format("no attribute %s for %s", attname, xml));
    }
    return new Option<String>(value);
  }

  private Option<IdAndSlot<String>> parseSlotMention(
      Element slotMentionElem,
      String slotMentionValueElemName) {
    IdAndSlot<String> result = null;
    for (String slotId : this.getAttributeValue(slotMentionElem, "id")) {
      for (Element mentionSlotElem : this.getChild(slotMentionElem, "mentionSlot")) {
        for (String slotName : this.getAttributeValue(mentionSlotElem, "id")) {
          for (Element slotMentionValueElem : this.getChild(
              slotMentionElem,
              slotMentionValueElemName)) {
            for (String slotValue : this.getAttributeValue(slotMentionValueElem, "value")) {
              result = new IdAndSlot<String>(slotId, new Slot<String>(slotName, slotValue));
            }
          }
        }
      }
    }
    return new Option<IdAndSlot<String>>(result);
  }

  private static class Option<T> implements Iterable<T> {
    T value;

    public Option(T value) {
      this.value = value;
    }

    @Override
    public Iterator<T> iterator() {
      return new Iterator<T>() {
        private T next = value;

        @Override
        public boolean hasNext() {
          return this.next != null;
        }

        @Override
        public T next() {
          T result = this.next;
          this.next = null;
          return result;
        }

        @Override
        public void remove() {
          throw new UnsupportedOperationException();
        }
      };
    }
  }

  private static class Slot<T> {
    public String name;

    public T value;

    public Slot(String name, T value) {
      this.name = name;
      this.value = value;
    }
  }

  private static class IdAndSlot<T> {
    public String id;

    public Slot<T> slot;

    public IdAndSlot(String id, Slot<T> slot) {
      this.id = id;
      this.slot = slot;
    }
  }
}
