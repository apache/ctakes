package org.apache.ctakes.temporal.utils;
import org.apache.ctakes.typesystem.type.syntax.BaseToken;

public class TokenPreprocForWord2Vec {

  /**
   * Determine what to print based on the token's type.
   */
  public static String tokenToString(BaseToken token) {

    String stringValue;
    String tokenType = token.getClass().getSimpleName();
    String tokenText = token.getCoveredText().toLowerCase();

    switch(tokenType) {
    case "ContractionToken":
      stringValue = tokenText;
      break;
    case "NewlineToken":
      stringValue = " newline ";//changed by Chen on 1/10/2019
      break;
    case "NumToken":
      stringValue = tokenText; //"number_token"; changed by Chen on 2/21/2019
      break;
    case "PunctuationToken":
      stringValue = tokenText;
      break;
    case "SymbolToken":
      stringValue = tokenText;
      break;
    case "WordToken":
      stringValue = tokenText;
      break;
    default:
      throw new IllegalArgumentException("Invalid token type: " + tokenType);
    }

    return stringValue;
  }
}