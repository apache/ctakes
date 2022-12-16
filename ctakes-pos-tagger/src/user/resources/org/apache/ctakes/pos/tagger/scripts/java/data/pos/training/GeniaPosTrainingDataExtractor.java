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

import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

/**
 * This class reads in the GENIA corpus and produces part-of-speech training
 * data. It reads in the corpus file GENIAcorpus3.02.pos.xml and writes out the
 * file found at data/pos/training/genia-pos-training.txt.
 * 
 * see also data/pos/training/README
 * 
 * @author Mayo Clinic
 * 
 */
public class GeniaPosTrainingDataExtractor implements Iterator<data.pos.training.GeniaPosTrainingDataExtractor.TaggedAbstract> {

	Iterator<?> articles;
	Element article;
	
	public GeniaPosTrainingDataExtractor(String geniaCorpusFileName) throws JDOMException{
		File geniaCorpusFile = new File(geniaCorpusFileName);
		SAXBuilder builder = new SAXBuilder();
		builder.setDTDHandler(null);
		Element root = builder.build(geniaCorpusFile).getRootElement();
		articles = root.getChildren("article").iterator();
	}

	
	public boolean hasNext() {
		if(article != null)
			return true;
		else {
			if (articles.hasNext()) {
				article = (Element) articles.next();
				return true;
			}
		}
		return false;
	}

	public TaggedAbstract next() {
		if(hasNext()) {
			TaggedAbstract taggedAbstract = parseArticle(article);
			article = null;
			return taggedAbstract;
		}
		return null;
	}

	public void remove() {}

	public TaggedAbstract parseArticle(Element article) {
		List<TaggedSentence> taggedSentences = new ArrayList<TaggedSentence>();
		
		Element title = article.getChild("title");
		if (title != null)
			taggedSentences.addAll(parseAbstract(title));
		Element abstractElement = article.getChild("abstract");
		if (abstractElement != null)
			taggedSentences.addAll(parseAbstract(abstractElement));
		return new TaggedAbstract(taggedSentences);
	}
	
	public List<TaggedSentence> parseAbstract(Element titleOrAbstract){
		List<TaggedSentence> taggedSentences = new ArrayList<TaggedSentence>();
		Iterator<?> sentences = titleOrAbstract.getChildren("sentence").iterator();
		while (sentences.hasNext()) {
			Element sentence = (Element) sentences.next();
			TaggedSentence taggedSentence = parseSentence(sentence);
			taggedSentences.add(taggedSentence);
		}
		return taggedSentences;
	}
	
	public TaggedSentence parseSentence(Element sentence){
		List<TaggedWord> wordTags = new ArrayList<TaggedWord>();
		Iterator<?> words = sentence.getChildren("w").iterator();
		while (words.hasNext()) {
			Element word = (Element) words.next();
			String wordText = word.getText();
			String posTag = word.getAttributeValue("c");
			/**
			 * If the posTag is an asterisk, then we want to find the next word that has a 
			 * an actual posTag.  
			 */
			while (posTag.equals("*")) {
				word = (Element) words.next();
				wordText = wordText + word.getText();
				posTag = word.getAttributeValue("c");
			}
			
			if(posTag.indexOf("|") != -1)
				System.out.println(wordText+":  "+posTag);
			posTag = posTag.split("\\|")[0];
			/**
			 * some of the tags in Genia have white space that messes things up.  Just remove
			 * the whitespace from these words.
			 */
			wordText = wordText.replaceAll("\\s", "");
			wordTags.add(new TaggedWord(wordText, posTag));
		}
		return new TaggedSentence(wordTags);
	}
	
	public class TaggedAbstract{
		List<TaggedSentence> taggedSentences;

		public TaggedAbstract(List<TaggedSentence> taggedSentences) {
			super();
			this.taggedSentences = taggedSentences;
		}

		public List<TaggedSentence> getTaggedSentences() {
			return taggedSentences;
		}

		public void setTaggedSentences(List<TaggedSentence> taggedSentences) {
			this.taggedSentences = taggedSentences;
		}
		
	}
	
	public class TaggedSentence{
		List<TaggedWord> taggedWords;

		public TaggedSentence(List<TaggedWord> taggedWords) {
			super();
			this.taggedWords = taggedWords;
		}

		public List<TaggedWord> getTaggedWords() {
			return taggedWords;
		}

		public void setTaggedWords(List<TaggedWord> taggedWords) {
			this.taggedWords = taggedWords;
		}
	}
	
	public class TaggedWord{
		String word;
		String tag;
		public String getWord() {
			return word;
		}
		public void setWord(String word) {
			this.word = word;
		}
		public String getTag() {
			return tag;
		}
		public void setTag(String tag) {
			this.tag = tag;
		}
		public TaggedWord(String word, String tag) {
			super();
			this.word = word;
			this.tag = tag;
		}
		
	}
	
	
	public static void main(String[] args) {
		try {
			System.out.println("Usage: java GeniaPosExtractor GENIAcorpus3.02.pos.xml data/pos/training/genia-pos-training.txt");
			String geniaCorpusFileName = args[0];
			String outputFileName = args[1];

			PrintStream out = new PrintStream(outputFileName);

			GeniaPosTrainingDataExtractor gptde = new GeniaPosTrainingDataExtractor(geniaCorpusFileName);
			while(gptde.hasNext()) {
				TaggedAbstract taggedAbstract = gptde.next();
				for(TaggedSentence taggedSentence : taggedAbstract.getTaggedSentences()) {
					for(TaggedWord taggedWord : taggedSentence.getTaggedWords()) {
						out.print(taggedWord.getWord()+"_"+taggedWord.getTag()+" ");
					}
					out.println();
				}
			}

			out.flush();
			out.close();

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
