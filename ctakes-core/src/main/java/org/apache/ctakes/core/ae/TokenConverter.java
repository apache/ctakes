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
package org.apache.ctakes.core.ae;

import org.apache.uima.jcas.JCas;

import org.apache.ctakes.core.nlp.tokenizer.Token;
import org.apache.ctakes.typesystem.type.syntax.BaseToken;
import org.apache.ctakes.typesystem.type.syntax.ContractionToken;
import org.apache.ctakes.typesystem.type.syntax.NewlineToken;
import org.apache.ctakes.typesystem.type.syntax.NumToken;
import org.apache.ctakes.typesystem.type.syntax.PunctuationToken;
import org.apache.ctakes.typesystem.type.syntax.SymbolToken;
import org.apache.ctakes.typesystem.type.syntax.WordToken;

/**
 * Utilities methods for converting between Java Tokenizer objects and their
 * equivalent JCas objects.
 * 
 * @author Mayo Clinic
 */
public class TokenConverter
{
    /**
     * Converts from Java Tokenizer object into a JCas object.
     */
    public static BaseToken convert(Token t, JCas jcas, int beginPos)
    {
        BaseToken bta = null;

        int begin = beginPos + t.getStartOffset();
        int end = beginPos + t.getEndOffset();

        switch (t.getType())
        {
        case Token.TYPE_WORD:
            WordToken wta = new WordToken(jcas);
            wta.setBegin(begin);
            wta.setEnd(end);
            int cap = -1;
            switch (t.getCaps())
            {
            case Token.CAPS_ALL:
                cap = TokenizerAnnotator.TOKEN_CAP_ALL;
                break;
            case Token.CAPS_FIRST_ONLY:
                cap = TokenizerAnnotator.TOKEN_CAP_FIRST_ONLY;
                break;
            case Token.CAPS_MIXED:
                cap = TokenizerAnnotator.TOKEN_CAP_MIXED;
                break;
            case Token.CAPS_NONE:
                cap = TokenizerAnnotator.TOKEN_CAP_NONE;
                break;
            }

            int numPos = -1;
            switch (t.getNumPosition())
            {
            case Token.NUM_FIRST:
                numPos = TokenizerAnnotator.TOKEN_NUM_POS_FIRST;
                break;
            case Token.NUM_MIDDLE:
                numPos = TokenizerAnnotator.TOKEN_NUM_POS_MIDDLE;
                break;
            case Token.NUM_LAST:
                numPos = TokenizerAnnotator.TOKEN_NUM_POS_LAST;
                break;
            case Token.NUM_NONE:
                numPos = TokenizerAnnotator.TOKEN_NUM_POS_NONE;
                break;
            }
            wta.setCapitalization(cap);
            wta.setNumPosition(numPos);
            bta = wta;
            break;
        case Token.TYPE_NUMBER:
            NumToken nta = new NumToken(jcas);
            nta.setBegin(begin);
            nta.setEnd(end);
            if (t.isInteger())
            {
                nta.setNumType(TokenizerAnnotator.TOKEN_NUM_TYPE_INTEGER);
            }
            else
            {
                nta.setNumType(TokenizerAnnotator.TOKEN_NUM_TYPE_DECIMAL);
            }
            bta = nta;
            break;
        case Token.TYPE_PUNCT:
            PunctuationToken pta = new PunctuationToken(jcas);
            pta.setBegin(begin);
            pta.setEnd(end);
            bta = pta;
            break;
        case Token.TYPE_EOL:
            NewlineToken nlta = new NewlineToken(jcas);
            nlta.setBegin(begin);
            nlta.setEnd(end);
            bta = nlta;
            break;
        case Token.TYPE_CONTRACTION:
            ContractionToken cta = new ContractionToken(
                    jcas);
            cta.setBegin(begin);
            cta.setEnd(end);
            bta = cta;
            break;
        case Token.TYPE_SYMBOL:
            SymbolToken sta = new SymbolToken(jcas);
            sta.setBegin(begin);
            sta.setEnd(end);
            bta = sta;
            break;
        default:
        }

        return bta;
    }

    /**
     * Convert from a JCas object into Java Tokenizer object.
     */
    public static Token convert(BaseToken bta)
    {
        Token token = new Token(bta.getBegin(), bta.getEnd());
        token.setText(bta.getCoveredText());

        if (bta instanceof WordToken)
        {
            WordToken wta = (WordToken) bta;
            token.setType(Token.TYPE_WORD);

            switch (wta.getCapitalization())
            {
            case TokenizerAnnotator.TOKEN_CAP_ALL:
                token.setCaps(Token.CAPS_ALL);
                break;
            case TokenizerAnnotator.TOKEN_CAP_FIRST_ONLY:
                token.setCaps(Token.CAPS_FIRST_ONLY);
                break;
            case TokenizerAnnotator.TOKEN_CAP_MIXED:
                token.setCaps(Token.CAPS_MIXED);
                break;
            case TokenizerAnnotator.TOKEN_CAP_NONE:
                token.setCaps(Token.CAPS_NONE);
                break;
            }

            switch (wta.getNumPosition())
            {
            case TokenizerAnnotator.TOKEN_NUM_POS_FIRST:
                token.setNumPosition(Token.NUM_FIRST);
                break;
            case TokenizerAnnotator.TOKEN_NUM_POS_MIDDLE:
                token.setNumPosition(Token.NUM_MIDDLE);
                break;
            case TokenizerAnnotator.TOKEN_NUM_POS_LAST:
                token.setNumPosition(Token.NUM_LAST);
                break;
            case TokenizerAnnotator.TOKEN_NUM_POS_NONE:
                token.setNumPosition(Token.NUM_NONE);
                break;
            }
        }
        else if (bta instanceof NumToken)
        {
            NumToken nta = (NumToken) bta;
            token.setType(Token.TYPE_NUMBER);

            if (nta.getNumType() == TokenizerAnnotator.TOKEN_NUM_TYPE_INTEGER)
            {
                token.setIsInteger(true);
            }
            else
            {
                token.setIsInteger(false);
            }
        }
        else if (bta instanceof PunctuationToken)
        {
            token.setType(Token.TYPE_PUNCT);
        }
        else if (bta instanceof NewlineToken)
        {
            token.setType(Token.TYPE_EOL);
        }
        else if (bta instanceof ContractionToken)
        {
            token.setType(Token.TYPE_CONTRACTION);
        }
        else if (bta instanceof SymbolToken)
        {
            token.setType(Token.TYPE_SYMBOL);
        }

        return token;
    }    
}