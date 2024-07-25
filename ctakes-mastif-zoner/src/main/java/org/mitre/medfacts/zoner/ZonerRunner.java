package org.mitre.medfacts.zoner;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.List;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.mitre.medfacts.zoner.ZonerCli.Range;

/**
 *
 * @author MCOARR
 */
public class ZonerRunner
{
  public static final Logger LOGGER = LogManager.getLogger(ZonerRunner.class.getName());

  protected String inputDirectoryString;

  public static void main(String args[])
  {
    LOGGER.info("ZonerRunner starting...");

    ZonerRunner runner = new ZonerRunner();
    
    String inputDirectoryString = null;
    if (args.length > 0)
    {
      inputDirectoryString = args[0];
    } else
    {
      LOGGER.error( "ZonerRunner requires an input parameter specifying the input directory." );
      System.exit( 1 );
    }

    runner.setInputDirectoryString(inputDirectoryString);
    runner.execute();

    LOGGER.info("ZonerRunner finished");
  }

  public void execute()
  {
    LOGGER.info("ZonerRunner.execute() begin");

    File inputDirectory = new File(inputDirectoryString);
    File[] textFiles = inputDirectory.listFiles(new FilenameFilter()
    {
      public boolean accept(File dir, String name)
      {
        return name.endsWith(".txt");
      }
    });
    System.out.println("=== TEXT FILE LIST BEGIN ===");
    for (File currentTextFile : textFiles)
    {
      LOGGER.info(String.format("currentTextFile: %s", currentTextFile.getAbsolutePath()));
      
      try
      {

        ParsedTextFile parsedTextFile =
            ZonerCli.processTextFile(currentTextFile);

        findZones(parsedTextFile.getEverything(), parsedTextFile.getTokens());

      } catch (IOException e)
      {
        String message = String.format("IOException while running zoner on file %s", currentTextFile.getAbsolutePath());
        LOGGER.warn(message, e);
        throw new RuntimeException(message, e);
      }
    }

    LOGGER.info("ZonerRunner.execute() end");
  }

  public static void findZones(String entireContents, String textLookup[][])
  {
    ZonerCli zoner = new ZonerCli();
    zoner.setEntireContents(entireContents);
    zoner.findHeadings();
    List<Range> zonerRangeList = zoner.getRangeList();

//    ArrayList<ZoneAnnotation> tempZoneAnnotationList =
//        new ArrayList<ZoneAnnotation>();

    for (Range currentRange : zonerRangeList)
    {
      LineAndTokenPosition rangeBegin = currentRange.getBeginLineAndToken();
      LineAndTokenPosition rangeEnd = currentRange.getEndLineAndToken();
      String rangeLabel = currentRange.getLabel();
      LOGGER.info(String.format("FOUND ZONE \"%s\"", rangeLabel));

//      int firstLine = rangeBegin.getLine();
//      int lastLine = rangeEnd.getLine();
//
//      for (int i=firstLine; i <= lastLine; i++)
//      {
//        boolean isFirstLine = (i == firstLine);
//        boolean isLastLine  = (i == lastLine);
//
//        int beginToken;
//        if (isFirstLine)
//        {
//          beginToken = rangeBegin.getTokenOffset();
//        } else
//        {
//          beginToken = 0;
//        }
//
//        int endToken;
//        if (isLastLine)
//        {
//          endToken = rangeEnd.getTokenOffset();
//        } else
//        {
//          // todo fix logic here
//          if (i >= textLookup.length)
//          {
//            System.out.println("This should not be happening, fix me... (can be ignored for now)");
//            continue;
//          }
//          endToken = textLookup[i].length - 1;
//        }
//
//        ZoneAnnotation zone = new ZoneAnnotation();
//        zone.setZoneName(currentRange.getLabel());
//        Location begin = new Location();
//        begin.setLine(i);
//        begin.setTokenOffset(beginToken);
//        zone.setBegin(begin);
//        Location end = new Location();
//        end.setLine(i);
//        end.setTokenOffset(endToken);
//        zone.setEnd(end);
//
////        allAnnotationList.add(zone);
////        List<Annotation> zoneAnnotationList = annotationsByType.get(AnnotationType.ZONE);
////        if (zoneAnnotationList == null)
////        {
////          zoneAnnotationList = new ArrayList<Annotation>();
////          annotationsByType.put(AnnotationType.ZONE, zoneAnnotationList);
////        }
//        tempZoneAnnotationList.add(zone);
//      }
    }

//    return tempZoneAnnotationList;
  }



  public String getInputDirectoryString()
  {
    return inputDirectoryString;
  }

  public void setInputDirectoryString(String inputDirectoryString)
  {
    this.inputDirectoryString = inputDirectoryString;
  }

}

