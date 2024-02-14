package org.mitre.medfacts.zoner;

public interface CharacterOffsetToLineTokenConverter
{

  public LineAndTokenPosition convert(int characterOffset);

}