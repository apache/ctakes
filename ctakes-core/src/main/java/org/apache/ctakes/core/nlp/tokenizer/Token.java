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
package org.apache.ctakes.core.nlp.tokenizer;

/**
 * Object that represents a generic token.  The token is related back to the 
 * original text via a start and end offset.  These are character positions
 * that relate directly to the original text.
 * 
 * A token can be one of many different types.  Please see the javadoc for the
 * TYPE fields to see a description of each.
 * 
 * @author Mayo Clinic
 */
public class Token 
{
	/**
	 * The type is unknown.
	 */
    public static final byte TYPE_UNKNOWN = 0;
    
    /**
     * A word token is defined as a consecutive series of word characters.
     * Word characters are defined as A-Z and a-z.  A word token may contain
     * hypens if the hyphen has a word character on each side.  A word token
     * may contain an apostrophe if the apostrophe has a word character on each
     * side.
     */
    public static final byte TYPE_WORD = 1;
    
    /**
     * A number token is defined as a consecutive series of digits.
     */
    public static final byte TYPE_NUMBER = 2;
	
	/**
	 * A punctuation token is defined as one character that can be either a
	 * period, double quote, single quote, question mark, exclamation point,
	 * hyphen (if not surrounded by word characters), etc...
	 */
    public static final byte TYPE_PUNCT = 3;

	/**
	 * A EOL token is defined as a line feed or carriage return character.
	 */
	public static final byte TYPE_EOL = 4;

	/**
	 * Contains contractions and possessives (since they cannot be
	 * differentiated without context).
	 */
	public static final byte TYPE_CONTRACTION = 5;

	/**
	 * Characters @!#$%^&*?
	 */
	public static final byte TYPE_SYMBOL = 6;
	
	public static final byte CAPS_UNKNOWN = 0;
	public static final byte CAPS_NONE = 1;
	public static final byte CAPS_MIXED = 2;
	public static final byte CAPS_FIRST_ONLY = 3;
	public static final byte CAPS_ALL = 4;

	public static final byte NUM_NONE = 0;
	public static final byte NUM_FIRST = 1;
	public static final byte NUM_MIDDLE = 2;
	public static final byte NUM_LAST = 3;

    private byte iv_type = TYPE_UNKNOWN;
    private byte iv_caps = CAPS_UNKNOWN; 
    private byte iv_numPosition = NUM_NONE;
    private int iv_startOffset = 0;
    private int iv_endOffset = 0;
	private String iv_text;
	private boolean iv_isInteger;

    /**
     * Constructor
     * @param startOffset The token's start offset.
     * @param endOffset The token's end offset.
     */
    public Token(int startOffset, int endOffset)
    {
        iv_startOffset = startOffset;
        iv_endOffset = endOffset;
    }

    /**
     * Gets the end offset.  This is the position directly after the last letter.
     */
    public int getEndOffset()
    {
        return iv_endOffset;
    }

	/**
	 * Sets the end offset.  This is the position directly after the last letter.
	 */
	public void setEndOffset(int i)
	{
		iv_endOffset = i;
	}

    /**
     * Gets the start offset.  This is the position of the first letter.
     */
    public int getStartOffset()
    {
        return iv_startOffset;
    }

	/**
	 * Sets the start offset.  This is the position of the first letter.
	 */
	public void setStartOffset(int i)
	{
		iv_startOffset = i;
	}

    /**
     * Gets the type of the token.  Please see the javadoc for the TYPE fields.
     */
    public byte getType()
    {
        return iv_type;
    }

	/**
	 * Sets the type of the token.  Please see the javadoc for the TYPE fields.
	 */
	public void setType(byte b)
	{
		iv_type = b;
	}

    /**
     * Gets the caps state of the token.
     */
    public byte getCaps()
    {
        return iv_caps;
    }

    /**
     * Sets the caps state of the token.
     */
    public void setCaps(byte b)
    {
        iv_caps = b;
    }

    /**
     * Gets the position of a number inside a Token.
     */
    public byte getNumPosition()
    {
        return iv_numPosition;
    }

    /**
     * Sets the position of a number inside a Token.
     */
    public void setNumPosition(byte b)
    {
        iv_numPosition = b;
    }

    public String getText()
    {
        return iv_text;
    }

    public void setText(String s)
    {
        iv_text = s;
    }

    public boolean isInteger()
    {
        return iv_isInteger;
    }

    public void setIsInteger(boolean isInteger)
    {
        iv_isInteger = isInteger;
    }

    public String toString()
    {
    	return "\""+iv_text+"\" ("+iv_startOffset+","+iv_endOffset+") type="+typeDescription(iv_type);
    }
    
    public static String typeDescription(byte type)
    {
        if(type == TYPE_UNKNOWN)
        	return "TYPE_UNKNOWN";
        else if(type == TYPE_WORD)
        	return "TYPE_WORD";
        else if(type == TYPE_NUMBER)
        	return "TYPE_NUMBER";
        else if(type == TYPE_PUNCT)
        	return "TYPE_PUNCT";
        else if(type == TYPE_EOL)
        	return "TYPE_EOL";
        else if(type == TYPE_CONTRACTION)
        	return "TYPE_CONTRACTION";
        else if(type == TYPE_SYMBOL)
        	return "TYPE_SYMBOL";
        return "not a valid type";
    }
}


