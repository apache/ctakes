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
package org.apache.ctakes.ytex.tools;

import gov.nih.nlm.nls.lvg.Api.LvgCmdApi;
import org.apache.ctakes.core.nlp.tokenizer.Token;
import org.apache.ctakes.core.nlp.tokenizer.TokenizerPTB;
import org.apache.ctakes.ytex.kernel.KernelContextHolder;
import org.apache.ctakes.ytex.umls.dao.UMLSDao;
import org.apache.ctakes.ytex.umls.model.UmlsAuiFirstWord;
import org.apache.log4j.Logger;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.net.URL;
import java.util.*;

/**
 * setup umls_aui_fword table
 * 
 * @author vijay
 * 
 */
public class SetupAuiFirstWord {

	static private final Logger LOGGER = Logger.getLogger(SetupAuiFirstWord.class);
	
	// private static final Pattern nonWord = Pattern.compile("\\W");
	private TokenizerPTB tokenizer;
	private LvgCmdApi lvgCmd;
	private Set<String> exclusionSet = null;

	/**
	 * Initialize tokenizer using the hyphen map from
	 * "tokenizer/hyphenated.txt". Use freqCutoff of 0. If this is changed in
	 * the TokenizerAnnotator.xml uima config, then the tokenization here will
	 * not match the tokenization done during document processing.
	 * <p/>
	 * Initialize exclusionSet from LvgAnnotator.xml. The exclusion set should
	 * be case insensitive, but it isn't that way in the LvgAnnotator so we
	 * retain the same functionality.
	 * <p/>
	 * Initialize LVG. copied from
	 * edu.mayo.bmi.uima.lvg.resource.LvgCmdApiResourceImpl.
	 * 
	 * @throws Exception
	 */
	public SetupAuiFirstWord() throws Exception {
		initTokenizer();
		// initialize exclusion set
		initExclusionSet();
		initLvg();
	}

	/**
	 * initialize lvgCmd
	 */
	private void initLvg() {
		// See
		// http://lexsrv2.nlm.nih.gov/SPECIALIST/Projects/lvg/2008/docs/userDoc/index.html
		// See
		// http://lexsrv3.nlm.nih.gov/SPECIALIST/Projects/lvg/2008/docs/designDoc/UDF/flow/index.html
		// Lower-case the terms and then uninflect
		// f = using flow components (in this order)
		// l = lower case
		// b = uninflect a term
		try {
			URL uri = this.getClass().getClassLoader()
					.getResource("org/apache/ctakes/lvg/data/config/lvg.properties");
			LOGGER.info("loading lvg.properties from:" + uri.getPath());
			File f = new File(uri.getPath());
			LOGGER.info(f.getAbsolutePath());
			String configDir = f.getParentFile().getAbsolutePath();
			String lvgDir = configDir.substring(0, configDir.length()
					- "data/config".length());
			System.setProperty("user.dir", lvgDir);
			lvgCmd = new LvgCmdApi("-f:l:b", f.getAbsolutePath());
		} catch (Exception e) {
			LOGGER.warn("could not initialize lvg - will not create a stemmed dictionary.", e);
		}
	}

	/**
	 * initialize lvg exclusion set
	 * 
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws IOException
	 */
	private void initExclusionSet() throws ParserConfigurationException,
			SAXException, IOException {
		this.exclusionSet = new HashSet<String>();
		InputStream isLvgAnno = null;
		try {
			isLvgAnno = this
					.getClass()
					.getClassLoader()
					.getResourceAsStream(
							"ctakes-lvg/desc/analysis_engine/LvgAnnotator.xml");
			if(isLvgAnno == null) {
				LOGGER.warn("classpath:ctakes-lvg/desc/analysis_engine/LvgAnnotator.xml not available, attempting to load from file system");
				File f = new File("../ctakes-lvg/desc/analysis_engine/LvgAnnotator.xml");
				if(f.exists())
					isLvgAnno = new BufferedInputStream(new FileInputStream(f));
			} 
			if (isLvgAnno == null) {
				LOGGER.warn("ctakes-lvg/desc/analysis_engine/LvgAnnotator.xml not available, using empty exclusion set");
			} else {
				DocumentBuilderFactory dbFactory = DocumentBuilderFactory
						.newInstance();
				DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
				Document doc = dBuilder.parse(isLvgAnno);
				NodeList nList = doc.getElementsByTagName("nameValuePair");
				for (int i = 0; i < nList.getLength(); i++) {
					Element e = (Element) nList.item(i);
					String name = e.getElementsByTagName("name").item(0)
							.getChildNodes().item(0).getNodeValue();
					if ("ExclusionSet".equals(name)) {
						NodeList nListEx = e.getElementsByTagName("string");
						for (int j = 0; j < nListEx.getLength(); j++) {
							exclusionSet.add(nListEx.item(j).getChildNodes()
									.item(0).getNodeValue());
						}
					}
				}
			}
		} finally {
			if (isLvgAnno != null)
				isLvgAnno.close();
		}
	}

	/**
	 * initialize the tokenizer. loads the hypenated word list.
	 * 
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	private void initTokenizer() throws FileNotFoundException, IOException {
		this.tokenizer = new TokenizerPTB();
	}

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		SetupAuiFirstWord setupFword = new SetupAuiFirstWord();
		setupFword.setupAuiFirstWord();
	}

	public void setupAuiFirstWord() {
		UMLSDao umlsDao = KernelContextHolder.getApplicationContext().getBean(
				UMLSDao.class);
		TransactionTemplate t = new TransactionTemplate(KernelContextHolder
				.getApplicationContext().getBean(
						PlatformTransactionManager.class));
		t.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);
		// delete all records
		// umlsDao.deleteAuiFirstWord();

		// get all auis and their strings
		// restart processing after the last aui we processed.
		// if this is null, then just process everything
		String lastAui = umlsDao.getLastAui();
		List<Object[]> listAuiStr = null;
		do {
			// get the next 10k auis
			listAuiStr = umlsDao.getAllAuiStr(lastAui);
			// put the aui - fword pairs in a list
			List<UmlsAuiFirstWord> listFword = new ArrayList<UmlsAuiFirstWord>(
					1000);
			for (Object[] auiStr : listAuiStr) {
				String aui = (String) auiStr[0];
				String str = (String) auiStr[1];
				lastAui = aui;
				if (str.length() < 200) {
					try {
						UmlsAuiFirstWord fw = this.tokenizeStr(aui, str);
						if (fw == null)
							LOGGER.error("Error tokenizing aui=" + aui + ", str="
									+ str);
						else if (fw.getFword().length() > 70)
							LOGGER.debug("fword too long: aui=" + aui + ", str="
									+ fw.getFword());
						else if (fw.getTokenizedStr().length() > 250)
							LOGGER.debug("string too long: aui=" + aui + ", str="
									+ str);
						else {
							LOGGER.debug("aui=" + aui + ", fw=" + fw);
							listFword.add(fw);
						}
					} catch (Exception e) {
						LOGGER.error("Error tokenizing aui=" + aui + ", str=" + str, e);
					}
				} else {
					LOGGER.debug("Skipping aui because str to long: aui=" + aui + ", str=" + str);
				}
			}
			// batch insert
			if (listFword.size() > 0) {
				umlsDao.insertAuiFirstWord(listFword);
				LOGGER.info("inserted " + listFword.size() + " rows");
			}
		} while (listAuiStr.size() > 0);
	}

	/**
	 * tokenize the umls concept. copied from
	 * edu\mayo\bmi\dictionarytools\CreateLuceneIndexFromDelimitedFile.java.
	 * 
	 * Stem the concept. Stemming performed analogous to LvgAnnotator.
	 * 
	 * @param aui
	 * @param str
	 * @return
	 * @throws Exception
	 */
	public UmlsAuiFirstWord tokenizeStr(String aui, String str)
			throws Exception {
		List<?> list = tokenizer.tokenize(str);
		Iterator<?> tokenItr = list.iterator();
		int tCount = 0;
		String firstTokenText = "";
		StringBuilder tokenizedDesc = new StringBuilder();
		String firstTokenStem = "";
		StringBuilder stemmedDesc = new StringBuilder();

		// get first word token and
		while (tokenItr.hasNext()) {
			tCount++;
			Token t = (Token) tokenItr.next();
			if (tCount == 1) {
				firstTokenText = t.getText(); // first token (aka "first word")
				tokenizedDesc.append(firstTokenText);
				if (this.lvgCmd != null) {
					firstTokenStem = stemToken(t);
					stemmedDesc.append(firstTokenStem);
				}
			} else { // use blank to separate tokens
				tokenizedDesc.append(" ").append(t.getText());
				// stem the next token, add it to the stemmed desc only if there
				// is a valid first word
				if (this.lvgCmd != null && firstTokenStem != null) {
					String stemmedWord = stemToken(t);
					stemmedDesc.append(" ").append(stemmedWord);
				}
			}
		}
		UmlsAuiFirstWord fw = new UmlsAuiFirstWord();
		fw.setAui(aui);
		fw.setFword(firstTokenText.toLowerCase(Locale.ENGLISH));
		fw.setTokenizedStr(tokenizedDesc.toString());
		if (this.lvgCmd != null) {
			fw.setFstem(firstTokenStem.toLowerCase(Locale.ENGLISH));
			fw.setStemmedStr(stemmedDesc.toString());
		}
		return fw;
	}

	/**
	 * 
	 * @param t
	 *            token
	 * @return stemmed text if token is a word and stemmed text is non-empty.
	 *         else raw token text.
	 * @throws Exception
	 */
	private String stemToken(Token t) throws Exception {
		String stemmedWord = t.getText();
		if (Token.TYPE_WORD == t.getType() || Token.TYPE_UNKNOWN == t.getType()) {
			stemmedWord = this.getCanonicalForm(t.getText());
			if (stemmedWord == null || stemmedWord.length() == 0) {
				stemmedWord = t.getText();
			}
		}
		return stemmedWord;
	}

	/**
	 * copied from edu.mayo.bmi.uima.lvg.ae.LvgAnnotator
	 * 
	 * @param word
	 * @return
	 * @throws Exception
	 */
	private String getCanonicalForm(String word) throws Exception {
		if (lvgCmd == null || this.exclusionSet.contains(word))
			return null;
		String canonicalForm = null;
		String out = lvgCmd.MutateToString(word);
		// vng null check
		String[] output = null;
		if (out != null)
			output = out.split("\\|");
		else {
			LOGGER.warn("mutateToString returned null for: " + word);
		}

		if ((output != null) && (output.length >= 2)
				&& (!output[1].matches("No Output"))) {
			canonicalForm = output[1];
		}
		return canonicalForm;
	}
}
