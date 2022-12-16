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
/*
 * Created on May 23, 2005
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package org.apache.ctakes.core.ci;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.ctakes.core.nlp.tokenizer.Token;
import org.apache.ctakes.core.nlp.tokenizer.Tokenizer;
import org.apache.ctakes.core.resource.FileLocator;


/**
 * @author Mayo Clinic
 * 
 */
public class HyphenTextModifierImpl implements TextModifier {

	private Map<String, Integer> iv_shouldbeHyphenMap = null;
	private int iv_windowSize = 3; // default lookahead window
	private Tokenizer iv_tokenizer = null;

	/*
	 * DECPRECATED: Use InputSteam instead of filename
	 */
	public HyphenTextModifierImpl(String hyphenfilename, int windowSize) {
		iv_windowSize = windowSize;
		iv_tokenizer = new Tokenizer();
		BufferedReader br;
		try {
			br = new BufferedReader(new InputStreamReader(
				      FileLocator.getAsStream(hyphenfilename)));
			String line = "";

			iv_shouldbeHyphenMap = new HashMap<String, Integer>();
			while ((line = br.readLine()) != null) {
				String[] toks = line.split("\\|");
				String[] unh = toks[0].split("\\-");
				String shouldbehyphen = "";
				for (int i = 0; i < unh.length; i++) {
					shouldbehyphen += " " + unh[i];
				}
				shouldbehyphen = shouldbehyphen.trim().toLowerCase();
				iv_shouldbeHyphenMap.put(shouldbehyphen, new Integer(1));
			}
		} catch (FileNotFoundException e) {
			System.err.println("Cannot find the hyphenation file:" + hyphenfilename);
			e.printStackTrace();
		} catch (IOException e) {
			System.err.println("IOException accessing the hyphenation file:" + hyphenfilename);
			e.printStackTrace();
		}

	}
	/**
	 * Default constructor takes a name of the file containing hyphenated
	 * phrases, with their frequency.
	 * Currently the frequency is unused.<br>
	 * The case of the words in the file is unimportant - we lowercase
	 * everything when doing compares.<br>
	 * The file is delimited with "|" and has two fields:<br>
	 * hyphen-term|frequency
	 */
	public HyphenTextModifierImpl(InputStream hyphenfilename, int windowSize) {
		iv_windowSize = windowSize;
		iv_tokenizer = new Tokenizer();
		BufferedReader br;
		try {
			br = new BufferedReader(new InputStreamReader(hyphenfilename));
			String line = "";

			iv_shouldbeHyphenMap = new HashMap<String, Integer>();
			while ((line = br.readLine()) != null) {
				String[] toks = line.split("\\|");
				String[] unh = toks[0].split("\\-");
				String shouldbehyphen = "";
				for (int i = 0; i < unh.length; i++) {
					shouldbehyphen += " " + unh[i];
				}
				shouldbehyphen = shouldbehyphen.trim().toLowerCase();
				iv_shouldbeHyphenMap.put(shouldbehyphen, new Integer(1));
			}
		} catch (FileNotFoundException e) {
			System.err.println("Cannot find the hyphenation file:" + hyphenfilename);
			e.printStackTrace();
		} catch (IOException e) {
			System.err.println("IOException accessing the hyphenation file:" + hyphenfilename);
			e.printStackTrace();
		}

	}	

	/**
	 * Filters out unwanted tokens - newlines.
	 * 
	 * @param tokenList
	 */
	private void filterTokens(List<Token> tokenList) {

		List<Token> removalList = new ArrayList<Token>();
		Iterator<Token> tokenItr = tokenList.iterator();

		while (tokenItr.hasNext()) {
			Token token = tokenItr.next();
			if (token.getType() == Token.TYPE_EOL) {
				removalList.add(token);
			}
		}

		tokenList.removeAll(removalList);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.mayo.bmi.uima.util.ci.TextModifier#modify(java.lang.String)
	 */
	public TextModification[] modify(String in) throws Exception {

		// intermediate data structure to use for easy adding of new
		// TextModification objects
		ArrayList<TextModification> textmods = new ArrayList<TextModification>();

		// Tokenize the input to get offset information
		List<Token> inputtoks = iv_tokenizer.tokenizeAndSort(in);

		filterTokens(inputtoks);

		int orig_startOffset = 0;
		int orig_endOffset = 0;
		int new_startOffset = 0;
		int new_endOffset = 0;

		int i = 0;
		int j = 0;
		int end_offset_adj = 0;
		int start_offset_adj = 0;

		while (i < inputtoks.size()) {

			if (inputtoks.size() - (i + 1) < iv_windowSize) {
				j = inputtoks.size() - 1;
			} else {
				j = i + iv_windowSize;
			}

			while (j > i) {

				StringBuffer candSB = new StringBuffer();
				for (int k = i; k <= j; k++) {
					Token currtok = (Token) inputtoks.get(k);
					candSB.append(" ");
					candSB.append(currtok.getText());
				}
				String cand = candSB.toString().trim();

				// Attempt to look up the candidate in the hyphen map
				if (iv_shouldbeHyphenMap.containsKey(cand.toLowerCase())) {

					// set the initial offsets
					orig_startOffset = ((Token) inputtoks.get(i)).getStartOffset();
					orig_endOffset = ((Token) inputtoks.get(j)).getEndOffset();
					new_startOffset = orig_startOffset;
					new_endOffset = orig_endOffset;

					// compile new text
					String newText = "";
					for (int k = i; k <= j; k++) {
						Token currtok = (Token) inputtoks.get(k);
						newText += currtok.getText() + "-";
					}
					newText = newText.substring(0, newText.length() - 1);

					// Get the new and old lengths of hyphenated spans
					int new_Length = newText.length();
					int orig_Length = orig_endOffset - orig_startOffset;

					// Pad the end offset adjuster by the new amount
					end_offset_adj += orig_Length - new_Length;

					// Create a new modification object
					TextModification tm = new TextModification(orig_startOffset, orig_endOffset, new_startOffset
							- start_offset_adj, new_endOffset - end_offset_adj, newText);

					// Adjust the start offset on the next Text Modification
					// object
					start_offset_adj += orig_Length - new_Length;

					// Put the newly created TextMod object into a temporary
					// holder
					textmods.add(tm);

					i = j;
				}
				j--;
			}

			i++;
		}

		// generate the expected return as an array of TextModification objects
		TextModification[] tma = new TextModification[textmods.size()];
		for (int y = 0; y < tma.length; y++) {
			tma[y] = (TextModification) textmods.get(y);
		}

		return tma;
	}

	
    /**
     * Apply text modifier to the text <br>
     * TODO - move this to <code>TextModifier</code> and take a <code>Logger</code>
     * 		See <code>HyphenTextModifierImpl</code>
	 * @param tm TextModifier to apply
	 * @param text Original text
	 * @param sb Buffer containing text to apply modifier to
     * @return unableToModifyText true if modifier would require offset changes, which is not supported by this method 
	 * @throws Exception
     */
    private static boolean applyTextModifier(TextModifier tm, String text, StringBuffer sb) throws Exception {
    	boolean unableToModifyText = false;
        TextModification[] textModArr = tm.modify(text);
        for (int i = 0; i < textModArr.length; i++) {

        	TextModification textMod = textModArr[i];
            
            if ((textMod.getOrigStartOffset() != textMod.getNewStartOffset())
                    || (textMod.getOrigEndOffset() != textMod.getNewEndOffset())) {
                System.err.println("UNSUPPORTED: TextModification with offset changes.");
                unableToModifyText = true;
            }
            else {
            	sb.replace(textMod.getOrigStartOffset(), 
        				textMod.getOrigEndOffset(), 
        				textMod.getNewText());
            }
        }  
        return unableToModifyText;
    }
	
    public static ArrayList<String> test(HyphenTextModifierImpl tm, String text) {
    	ArrayList<String> messages = new ArrayList<String>();
    	try {
			TextModification[] tma = tm.modify(text);
			StringBuffer sb = new StringBuffer(text);
			boolean errorModifyingText = applyTextModifier(tm,text,sb);
			messages.add("Orig: " + text);
			if (!errorModifyingText) {
				messages.add("New:  " + sb);
			}
			else {
				System.err.println("New:  (new text not generated, see previous messages)");				
			}
			// Regardless of whether was able to modify the text
			// without
			// (_apply_ the TextModifier), output the  
			// the 
			for (int u = 0; u < tma.length; u++) {
				TextModification tmo = (TextModification) tma[u];
				messages.add(tmo.getNewText() + " Orig: " + tmo.getOrigStartOffset() + "-"
						+ tmo.getOrigEndOffset() + " New: " + tmo.getNewStartOffset() + "-" + tmo.getNewEndOffset());
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return messages;
    	
    }
	/**
	 * Simple tests of <code>TextModification</code>
	 * <br>
	 * Output expected:<br>
	 * 		UNSUPPORTED: TextModification with offset changes.<br>
	 * 		UNSUPPORTED: TextModification with offset changes.<br>
	 * 		UNSUPPORTED: TextModification with offset changes.<br>
	 *      Orig: Non  Hodgkin's the x  ray without any non small  cell complications.<br>
	 *      New:  (new text not generated, see previous messages)
	 * 		Non-Hodgkin Orig: 0-12 New: 0-11<br>
	 * 		x-ray Orig: 19-25 New: 18-23<br>
	 * 		non-small-cell Orig: 38-53 New: 36-50<br>
	 * 
	 * 		Orig: Non Hodgkin's the x ray without any non small cell complications.<br>
	 * 		New:  Non-Hodgkin's the x-ray without any non-small-cell complications.<br>
	 * 		Non-Hodgkin Orig: 0-11 New: 0-11<br>
	 * 		x-ray Orig: 18-23 New: 18-23<br>
	 * 		non-small-cell Orig: 36-50 New: 36-50<br>
	 * Note the case of the words doesn't matter. 
	 * @param args hyphen text filename (each line: hyphenated-word|freq)
	 */
	public static void main(String[] args) {
		ArrayList<String> messages;
		HyphenTextModifierImpl tm = new HyphenTextModifierImpl(args[0], 7);

		String t = "Non  Hodgkin's the x  ray without any non small  cell complications.";
		messages = test(tm, t); // extra blanks
		for (String s : messages) {	System.out.println(s); }

		t = t.replace("  ", " "); // change text to only have single blanks between words
		messages = test(tm, t); // single blanks
		for (String s : messages) {	System.out.println(s); }
	}

}
