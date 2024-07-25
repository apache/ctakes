/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.mitre.medfacts.zoner;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author MCOARR
 */
public class LineTokenToCharacterOffsetConverter
{
  public static final Logger LOGGER = LogManager.getLogger( "LineTokenToCharacterOffsetConverter" );

  public static final Pattern endOfLinePattern = Pattern.compile("\\r?\\n");
  public static final Pattern eolOrSpacePattern = Pattern.compile("( +)|(\\r?\\n)");
  public static final Pattern spacePattern = Pattern.compile(" +");
  protected TreeSet<Integer> eolPositionSet = new TreeSet<Integer>();
  protected TreeMap<Integer,WhitespaceType> eolOrSpacePositionMap = new TreeMap<Integer,WhitespaceType>();

  protected ArrayList<ArrayList<BeginAndEndCharacterOffsetPair>> offsets = null;
  protected Map<Integer, LineAndTokenPosition> characterOffsetToLineAndTokenMap;

  public LineTokenToCharacterOffsetConverter(String inputString)
  {
    ArrayList<ArrayList<BeginAndEndCharacterOffsetPair>> all = new ArrayList<ArrayList<BeginAndEndCharacterOffsetPair>>();

//    ArrayList<Integer> currentLine = new ArrayList<Integer>();
//    all.add(currentLine);

    /////

    Matcher eolMatcher = endOfLinePattern.matcher(inputString);
    //ArrayList<String> lineList = new ArrayList<String>();
    //ArrayList<Integer> lineBeginOffsetList = new ArrayList<Integer>();

    ArrayList<ArrayList<BeginAndEndCharacterOffsetPair>> offsets = new ArrayList<ArrayList<BeginAndEndCharacterOffsetPair>>();

    int i = 0;
    while (eolMatcher.find())
    {
      int eolStart = eolMatcher.start();
      int eolEnd = eolMatcher.end();

      ArrayList<BeginAndEndCharacterOffsetPair> lineOffsets = new ArrayList<BeginAndEndCharacterOffsetPair>();
      offsets.add(lineOffsets);

      String line = inputString.substring(i, eolStart);

      LOGGER.debug(String.format("LINE [%d-%d] \"%s\"", i, eolStart - 1, line));
      parseLine(line, lineOffsets, i);

      //lineList.add(line);
      //lineBeginOffsetList.add(i);
      i = eolEnd;
    }
    if (i < inputString.length())
    {
      String line = inputString.substring(i);
      ArrayList<BeginAndEndCharacterOffsetPair> lineOffsets = new ArrayList<BeginAndEndCharacterOffsetPair>();
      LOGGER.debug(String.format("LINE (before eof) [%d-%d] \"%s\"", i, inputString.length() - 1, line));
      offsets.add(lineOffsets);

      parseLine(line, lineOffsets, i);
    }

    Map<Integer, LineAndTokenPosition> characterOffsetToLineAndTokenMap =
        new TreeMap<Integer, LineAndTokenPosition>();
    for (int line=0; line < offsets.size(); line++)
    {
      ArrayList<BeginAndEndCharacterOffsetPair> currentLine = offsets.get(line);

      for (int token=0; token < currentLine.size(); token++)
      {
        BeginAndEndCharacterOffsetPair currentTokenOffsets = currentLine.get(token);
        Integer begin = currentTokenOffsets.getBegin();
        Integer end = currentTokenOffsets.getEnd();

        LineAndTokenPosition position = new LineAndTokenPosition();
        position.setLine(line + 1);
        position.setTokenOffset(token);

        characterOffsetToLineAndTokenMap.put(begin, position);
        characterOffsetToLineAndTokenMap.put(end, position);
      }
    }
    
    this.offsets = offsets;
    this.characterOffsetToLineAndTokenMap = characterOffsetToLineAndTokenMap;
  }

  private void parseLine(String line, ArrayList<BeginAndEndCharacterOffsetPair> lineOffsets, int startOfLineOffset)
  {
    Matcher spaceMatcher = spacePattern.matcher(line);
    int j = 0;
    while (spaceMatcher.find())
    {
      int spaceBegin = spaceMatcher.start();
      int spaceEnd = spaceMatcher.end();

      int wordBegin = j;;
      int wordEnd = spaceBegin - 1;

      int wordBeginOverall = startOfLineOffset + wordBegin;
      int wordEndOverall = startOfLineOffset + wordEnd;

      String token = line.substring(j, spaceBegin);

      LOGGER.debug(String.format("    TOKEN [%d-%d] [%d-%d] \"%s\"", wordBegin, wordEnd, wordBeginOverall, wordEndOverall, token));
      BeginAndEndCharacterOffsetPair current = new BeginAndEndCharacterOffsetPair();
      current.setBegin(wordBeginOverall);
      current.setEnd(wordEndOverall);
      lineOffsets.add(current);
      j = spaceEnd;
    }
    if (j < line.length())
    {
      int wordBegin = j;
      int wordEnd = line.length() - 1;

      int wordBeginOverall = startOfLineOffset + wordBegin;
      int wordEndOverall = startOfLineOffset + wordEnd;

      String token = line.substring(j);
      LOGGER.debug(String.format("    TOKEN (before eol) [%d-%d] [%d-%d] \"%s\"", wordBegin, wordEnd, wordBeginOverall, wordEndOverall, token));

      BeginAndEndCharacterOffsetPair current = new BeginAndEndCharacterOffsetPair();
      current.setBegin(wordBeginOverall);
      current.setEnd(wordEndOverall);
      lineOffsets.add(current);
    }
  }

  public BeginAndEndCharacterOffsetPair convert(LineAndTokenPosition lineAndTokenPosition)
  {
    int line = lineAndTokenPosition.getLine();
    int token = lineAndTokenPosition.getTokenOffset();

    ArrayList<BeginAndEndCharacterOffsetPair> lineArray = offsets.get(line - 1);
    if ((lineArray == null) || (lineArray.size() < 1)) { return null; }
    BeginAndEndCharacterOffsetPair offsetsForToken = lineArray.get(token);
    
    return offsetsForToken;
  }

  public LineAndTokenPosition convertReverse(Integer characterOffset)
  {
    LineAndTokenPosition lineAndTokenPosition =
      characterOffsetToLineAndTokenMap.get(characterOffset);
    return lineAndTokenPosition;
  }

  public Integer convertOld(LineAndTokenPosition lineAndTokenPosition)
  {
    int line = lineAndTokenPosition.getLine();
    int token = lineAndTokenPosition.getTokenOffset();

    Set<Entry<Integer, WhitespaceType>> fullEntrySet = eolOrSpacePositionMap.entrySet();
    Iterator<Entry<Integer, WhitespaceType>> iterator = fullEntrySet.iterator();

    Entry<Integer, WhitespaceType> currentEntry = null;

    LOGGER.info( "before line loop");
    boolean lineFound = false;
    int lineNumber = 1;
    if (line == 1)
    {
      LOGGER.info( "searching for first line, not going into line loop.");
      lineFound = true;
    } else
    while(!lineFound && iterator.hasNext())
    {
      currentEntry = iterator.next();
      LOGGER.info( String.format("::currentEntry (%d, %s) [line loop]%n", currentEntry.getKey(), currentEntry.getValue()));
      if (WhitespaceType.EOL == currentEntry.getValue())
      {
        lineNumber++;
        LOGGER.info( String.format("processed line %d%n", lineNumber));
      }
      if (lineNumber == line)
      {
        LOGGER.info( String.format("found requested line!%n"));
        lineFound = true;
      }
    }
    LOGGER.info( "after line loop");

    if (token == 0)
    {
      LOGGER.info( String.format("token 0 was requested on line %d, so not going through second loop%n", line));
      return (currentEntry == null) ? 0 : currentEntry.getKey() + 2;
    }

    boolean tokenFound = false;
    int tokenCount = 0;
    LOGGER.info( "before token loop");
    if (token == 0)
    {
      LOGGER.info( "searching for first token, not going into token loop.");
      tokenFound = true;
    }
    while(!tokenFound && iterator.hasNext())
    {
      LOGGER.info( "inside token loop...");
      currentEntry = iterator.next();
      LOGGER.info( String.format("::currentEntry (%d, %s) [token loop]%n", currentEntry.getKey(), currentEntry.getValue()));
      if (WhitespaceType.EOL == currentEntry.getValue())
      {
        System.err.println("ERROR: found EOL before finding token!");
        return null;
      }
      if (WhitespaceType.SPACE == currentEntry.getValue())
      {
        tokenCount++;
        LOGGER.info( String.format("processed token %d%n", tokenCount));
      }

      if (tokenCount == token)
      {
        tokenFound = true;
        LOGGER.info( String.format(("found requested token!%n")));
        Entry<Integer, WhitespaceType> whatWouldHaveBeenNext = iterator.next();
        LOGGER.info( String.format("::currentEntry (%d, %s) [token loop]%n", whatWouldHaveBeenNext.getKey(), whatWouldHaveBeenNext.getValue()));
      }
    }
    LOGGER.info( "after line loop");

    if (lineFound && tokenFound)
    {
      LOGGER.info( String.format("token 0 was requested on line %d, so not going through second loop%n", line));
      return (currentEntry == null) ? null : currentEntry.getKey() + 2;
    } else
    {
      return null;
    }
  }

  public enum WhitespaceType
  {
    SPACE,
    EOL
  }

  public class CharacterOffsetAndWhitespaceType
  {
    protected int characterOffset;
    protected WhitespaceType whitespaceType;

    public int getCharacterOffset()
    {
      return characterOffset;
    }

    public void setCharacterOffset(int characterOffset)
    {
      this.characterOffset = characterOffset;
    }

    public WhitespaceType getWhitespaceType()
    {
      return whitespaceType;
    }

    public void setWhitespaceType(WhitespaceType whitespaceType)
    {
      this.whitespaceType = whitespaceType;
    }
  }

  public class BeginAndEndCharacterOffsetPair
  {
    protected int begin;
    protected int end;

    public int getBegin()
    {
      return begin;
    }

    public void setBegin(int begin)
    {
      this.begin = begin;
    }

    public int getEnd()
    {
      return end;
    }

    public void setEnd(int end)
    {
      this.end = end;
    }
  }
}

