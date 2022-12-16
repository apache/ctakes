/**
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
package data.pos.training;

import static org.junit.Assert.assertEquals;

import org.jdom.JDOMException;
import org.junit.Test;

import data.pos.training.GeniaPosTrainingDataExtractor.TaggedAbstract;
import data.pos.training.GeniaPosTrainingDataExtractor.TaggedSentence;
import data.pos.training.GeniaPosTrainingDataExtractor.TaggedWord;

public class GeniaPosTrainingDataExtractorTests {

	@Test
    public void test() throws JDOMException {
		GeniaPosTrainingDataExtractor gptde = new GeniaPosTrainingDataExtractor("test/data/GENIAcorpus3.02.pos.test.xml");  

		TaggedAbstract taggedAbstract = gptde.next();
		TaggedSentence taggedSentence;
		TaggedWord taggedWord;
		
		//test one full sentence from title
		taggedSentence = taggedAbstract.getTaggedSentences().get(0);
		taggedWord = taggedSentence.getTaggedWords().get(0);
		assertEquals("Pancreatic", taggedWord.getWord());
		assertEquals("JJ", taggedWord.getTag());
		taggedWord = taggedSentence.getTaggedWords().get(1);
		assertEquals("development", taggedWord.getWord());
		assertEquals("NN", taggedWord.getTag());
		taggedWord = taggedSentence.getTaggedWords().get(2);
		assertEquals("and", taggedWord.getWord());
		assertEquals("CC", taggedWord.getTag());
		taggedWord = taggedSentence.getTaggedWords().get(3);
		assertEquals("maturation", taggedWord.getWord());
		assertEquals("NN", taggedWord.getTag());
		taggedWord = taggedSentence.getTaggedWords().get(4);
		assertEquals("of", taggedWord.getWord());
		assertEquals("IN", taggedWord.getTag());
		taggedWord = taggedSentence.getTaggedWords().get(5);
		assertEquals("the", taggedWord.getWord());
		assertEquals("DT", taggedWord.getTag());
		taggedWord = taggedSentence.getTaggedWords().get(6);
		assertEquals("islet", taggedWord.getWord());
		assertEquals("NN", taggedWord.getTag());
		taggedWord = taggedSentence.getTaggedWords().get(7);
		assertEquals("B", taggedWord.getWord());
		assertEquals("NN", taggedWord.getTag());
		taggedWord = taggedSentence.getTaggedWords().get(8);
		assertEquals("cell", taggedWord.getWord());
		assertEquals("NN", taggedWord.getTag());
		taggedWord = taggedSentence.getTaggedWords().get(9);
		assertEquals(".", taggedWord.getWord());
		assertEquals(".", taggedWord.getTag());

		//test one full sentence from abstract
		//<sentence><w c="DT">The</w> <w c="CD">three</w> <w c="NNS">compartments</w> <w c="VBP">are</w> <w c="VBN">thought</w> <w c="TO">to</w> <w c="VB">be</w> <w c="IN">of</w> <w c="JJ">common</w> <w c="JJ">endodermal</w> <w c="NN">origin</w><w c=":">;</w> <w c="IN">in</w> <w c="NN">contrast</w> <w c="TO">to</w> <w c="JJR">earlier</w> <w c="NNS">hypotheses</w><w c=",">,</w> <w c="WDT">which</w> <w c="VBD">suggested</w> <w c="IN">that</w> <w c="DT">the</w> <w c="JJ">endocrine</w> <w c="NN">compartment</w> <w c="VBD">was</w> <w c="IN">of</w> <w c="JJ">neuroectodermal</w> <w c="NN">origin</w><w c=".">.</w></sentence>
		taggedSentence = taggedAbstract.getTaggedSentences().get(5);
		taggedWord = taggedSentence.getTaggedWords().get(0);
		assertEquals("The", taggedWord.getWord());
		assertEquals("DT", taggedWord.getTag());

		
		taggedSentence = taggedAbstract.getTaggedSentences().get(1);
		assertEquals(6, taggedSentence.getTaggedWords().size());

		taggedSentence = taggedAbstract.getTaggedSentences().get(2);
		taggedWord = taggedSentence.getTaggedWords().get(0);
		assertEquals("Pancreas", taggedWord.getWord());
		assertEquals("NN", taggedWord.getTag());
		taggedWord = taggedSentence.getTaggedWords().get(11);
		assertEquals("anlage", taggedWord.getWord());
		assertEquals("NNS", taggedWord.getTag());
		taggedWord = taggedSentence.getTaggedWords().get(17);
		assertEquals(".", taggedWord.getWord());
		assertEquals(".", taggedWord.getTag());


		
		taggedAbstract = gptde.next();
		taggedSentence = taggedAbstract.getTaggedSentences().get(4);
		taggedWord = taggedSentence.getTaggedWords().get(0);
		assertEquals("We", taggedWord.getWord());
		assertEquals("PRP", taggedWord.getTag());
		taggedWord = taggedSentence.getTaggedWords().get(37);
		assertEquals("non-octamer", taggedWord.getWord());
		assertEquals("JJ", taggedWord.getTag());
		
	}
}


