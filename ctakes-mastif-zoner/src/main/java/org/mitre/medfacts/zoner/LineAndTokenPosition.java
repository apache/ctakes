/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.mitre.medfacts.zoner;

/**
 *
 * @author MCOARR
 */
public class LineAndTokenPosition
{
  protected int line;
  protected int tokenOffset;

  public int getLine()
  {
    return line;
  }

  public void setLine(int line)
  {
    this.line = line;
  }

  public int getTokenOffset()
  {
    return tokenOffset;
  }

  public void setTokenOffset(int tokenOffset)
  {
    this.tokenOffset = tokenOffset;
  }

  @Override
  public String toString()
  {
    return String.format("[%d:%d]", line, tokenOffset);
  }

}
