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
package org.apache.ctakes.sideeffect.util;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Regular expression pattern match class used in SideEffectAnnotaor:
 * Constructor: 
 * PatternMatch(String expr, String in, List<String> kw):
 *   "expr" is an regular expression. Note that "KW" will be replaced by the list of kw argument;
 *   "in" is a string to match;
 *   "kw" is a list of keywords;
 *   
 * PatternMatch(String expr, String in): 
 *   "expr" is an regular expression;
 *   "in" is a string to match.	
 *   
 * @author Mayo Clinic
 *
 */
public class PatternMatch {
	private List<String> keywords; 
	private String regex;
	private String input;    
    public Pattern pat;
    public Matcher mat;
    
    //expr: (PSE).*(KW).*(DRUG)
    public PatternMatch(String expr, String in, List<String> kw) {
    	StringBuffer sb = new StringBuffer();
    	for(String s : kw) 
    		sb.append(s+"|");
    	
    	regex = expr.replaceAll("KW", sb.substring(0, sb.length()-1)); 
    	input = in;
    	keywords = kw;  
    	pat = Pattern.compile(regex);
    	mat = pat.matcher(input);
    }
    
    public PatternMatch(String expr, String in) {   	
    	regex = expr; 
    	input = in;
    	pat = Pattern.compile(regex);
    	mat = pat.matcher(input);
    }
        
    /**
     * Return true if finds "DRUG" in input.substring(begin, end)
     * 
     * @param begin
     * @param end
     */
    public boolean isDrugBetween(int begin, int end) {
    	return input.substring(begin, end).matches(".*<DRUG>.*");
    }
    
    /**
     * Return true if finds "DRUG" in input.substring(begin, end)
     * and assign offsets of DRUG to span
     * 
     * @param begin
     * @param end
     * @param span offset of the first DRUG in "input"
     * @return
     */
    public boolean isDrugBetween(int begin, int end, int[] span) {
       	Pattern p = Pattern.compile("(<DRUG>)");
    	Matcher m = p.matcher(input.substring(begin, end));
    	while(m.find()) {
    		span[0] = m.start()+begin;
    		span[1] = m.end()+begin;
    		return true;
    	}
    	
    	return false;
    }
    
    /**
     * Return true if finds "PSE" in input.substring(begin, end)
     * 
     * @param begin
     * @param end
     */
    public boolean isPseBetween(int begin, int end) {
    	return input.substring(begin, end).matches(".*<PSE>.*");
    }
    
    /**
     * Return true if finds "PSE" in input.substring(begin, end)
     * and assign offsets of PSE to span
     * 
     * @param begin
     * @param end
     * @param span offset of the first PSE in "input"
     * @return
     */
    public boolean isPseBetween(int begin, int end, int[] span) {
       	Pattern p = Pattern.compile("(<PSE>)");
    	Matcher m = p.matcher(input.substring(begin, end));
    	while(m.find()) {
    		span[0] = m.start()+begin;
    		span[1] = m.end()+begin;
    		return true;
    	}
    	
    	return false;
    }
    
    /**
     * Return true if exist other than "and" "or" "," (ignoring DRUG and PSE) 
     * in input.substring(begin, end)
     * 
     * @param begin
     * @param end
     * @return
     */
    public boolean isDistantBetween(int begin, int end) {
    	String s = input.substring(begin, end).replaceAll("<DRUG>|<PSE>", "");
    	return !s.matches("(\\sand\\s)|(\\sor\\s)|(,)|(,\\s)|(\\s,\\s)");
    }       
}
