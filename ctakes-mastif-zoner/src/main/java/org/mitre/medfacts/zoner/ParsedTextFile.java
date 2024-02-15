package org.mitre.medfacts.zoner;

public class ParsedTextFile
{
  protected String[][] tokens;
  protected String everything;

  public String[][] getTokens()
  {
    return tokens;
  }

  public void setTokens(String[][] tokens)
  {
    this.tokens = tokens;
  }

  public String getEverything()
  {
    return everything;
  }

  public void setEverything(String everything)
  {
    this.everything = everything;
  }
}
