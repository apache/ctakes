/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.mitre.medfacts.zoner;

import java.util.NavigableMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author MCOARR
 */
public class CharacterOffsetToLineTokenConverterDefaultImpl implements CharacterOffsetToLineTokenConverter
{
  protected static final Pattern endOfLinePattern = Pattern.compile("\\r?\\n");
  protected static final Pattern eolOrSpacePattern = Pattern.compile("( +)|(\\r?\\n)");
  protected TreeSet<Integer> eolPositionSet = new TreeSet<Integer>();
  protected TreeMap<Integer,WhitespaceType> eolOrSpacePositionMap = new TreeMap<Integer,WhitespaceType>();

  public CharacterOffsetToLineTokenConverterDefaultImpl(String inputString)
  {
    Matcher eolMatcher = endOfLinePattern.matcher(inputString);
    while (eolMatcher.find())
    {
      int begin = eolMatcher.start();
      eolPositionSet.add(begin);
    }

    Matcher eolOrSpaceMatcher = eolOrSpacePattern.matcher(inputString);
    while (eolOrSpaceMatcher.find())
    {
      int begin = eolOrSpaceMatcher.start();
      WhitespaceType type=null;
      if ("".equals(eolOrSpaceMatcher.group(0)))
      {
        type = WhitespaceType.SPACE;
      } else
      {
        type = WhitespaceType.EOL;
      }
      eolOrSpacePositionMap.put(begin, type);
    }
  }

  /* (non-Javadoc)
   * @see org.mitre.medfacts.zoner.CharacterOffsetToLineTokenConverter#convert(int)
   */

  public LineAndTokenPosition convert(int characterOffset)
  {
    LineAndTokenPosition returnedPosition = new LineAndTokenPosition();
    System.out.format("input (character offset): %d%n", characterOffset);
    SortedSet<Integer> eolHeadSet = eolPositionSet.headSet(characterOffset);
    int numberEolsBefore = eolHeadSet.size();
    int lineNumber = numberEolsBefore + 1;
    
    eolOrSpacePositionMap.floorKey(characterOffset);

    int tokenOffset = 0;
    if (eolHeadSet == null || eolHeadSet.isEmpty())
    {
      tokenOffset = 0;
    } else
    {
      int lastEolCharacterOffset = eolHeadSet.last();
      NavigableMap<Integer, WhitespaceType> spacesOnCurrentLineSubMap =
        eolOrSpacePositionMap.subMap(lastEolCharacterOffset, false, characterOffset, false);
      tokenOffset = spacesOnCurrentLineSubMap.size();
    }

    returnedPosition.setLine(lineNumber);
    returnedPosition.setTokenOffset(tokenOffset);

    return returnedPosition;
  }

  public enum WhitespaceType
  {
    SPACE,
    EOL
  }
}
