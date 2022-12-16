package org.apache.ctakes.gui.dictionary.util;


import org.apache.ctakes.core.resource.FileLocator;
import org.apache.log4j.Logger;

import javax.swing.filechooser.FileSystemView;
import java.io.*;
import java.util.*;

/**
 * Author: SPF
 * Affiliation: CHIP-NLP
 * Date: 1/15/14
 */
final public class FileUtil {

   private FileUtil() {
   }

   static private final Logger LOGGER = Logger.getLogger( "FileUtil" );

   static public String parseDirText( final String dirPath ) {
      if ( dirPath == null || dirPath.isEmpty() ) {
         return parseDirText( "." );
      } else if ( dirPath.startsWith( "~" ) ) {
         return parseDirText( dirPath.replaceAll( "~", System.getProperty( "user.home" ) ) );
      } else if ( dirPath.equals( "." ) ) {
         final String userDir = System.getProperty( "user.dir" );
         if ( userDir == null || userDir.isEmpty() ) {
            return FileSystemView.getFileSystemView().getDefaultDirectory().getPath();
         }
         return userDir;
      } else if ( dirPath.startsWith( ".." ) ) {
         final String userDirPath = parseDirText( "." );
         File cwd = new File( userDirPath );
         String cwdPath = dirPath;
         while ( cwdPath.startsWith( ".." ) ) {
            if ( !cwd.isDirectory() ) {
               LOGGER.error( "Invalid directory " + dirPath );
               System.exit( 1 );
            }
            cwd = cwd.getParentFile();
            if ( cwdPath.equals( ".." ) ) {
               return cwd.getPath();
            }
            cwdPath = cwdPath.substring( 3 );
         }
         return cwd.getPath();
      }
      return dirPath;
   }


   static public BufferedReader createReader( final String filePath ) {
//      final String formattedPath = parseDirText( filePath );
//      final File file = new File( formattedPath );
      try {
         final File file = FileLocator.getFile( filePath );
         if ( !file.canRead() ) {
            LOGGER.error( "Cannot read file " + filePath );
            System.exit( 1 );
         }
         return new BufferedReader( new FileReader( file ) );
      } catch ( IOException ioE ) {
         LOGGER.error( "Cannot create Reader for " + filePath );
         LOGGER.error( ioE.getMessage() );
         System.exit( 1 );
      }
      return null;
   }

   static private BufferedWriter createWriter( final String filePath ) {
//      final String formattedPath = parseDirText( filePath );
//      final File file = new File( formattedPath );
      try {
         final File file = FileLocator.getFile( filePath );
         if ( file.getParentFile() != null && !file.getParentFile().isDirectory() ) {
            file.getParentFile().mkdirs();
         }
         return new BufferedWriter( new FileWriter( file, true ) );
      } catch ( IOException ioE ) {
         LOGGER.error( "Cannot create Writer for " + filePath );
         LOGGER.error( ioE.getMessage() );
         System.exit( 1 );
      }
      return null;
   }

   static private String readLine( final BufferedReader reader, final String filePath ) {
      try {
         String line = reader.readLine();
         while ( line != null ) {
            if ( !line.trim().isEmpty() && !line.trim().startsWith( "//" ) ) {
               return line;
            }
            line = reader.readLine();
         }
      } catch ( IOException ioE ) {
         LOGGER.error( "Error reading from file " + filePath );
      }
      return null;
   }

   static public List<String> readBsvTokens( final BufferedReader reader, final String filePath ) {
      final String line = readLine( reader, filePath );
      if ( line == null ) {
         return null;
      }
      return TokenUtil.getBsvItems( line );
   }

   static public List<String> readCsvTokens( final BufferedReader reader, final String filePath ) {
      final String line = readLine( reader, filePath );
      if ( line == null ) {
         return null;
      }
      return TokenUtil.getCsvItems( line );
   }

   static public List<String> readTildeTokens( final BufferedReader reader, final String filePath ) {
      final String line = readLine( reader, filePath );
      if ( line == null ) {
         return null;
      }
      return TokenUtil.getTildeItems( line );
   }

   static public void writeOneColumn( final String filePath, final String description,
                                      final Collection<String> list ) {
      LOGGER.info( "Writing " + description + " to " + filePath );
      long lineCount = 0;
      try ( BufferedWriter writer = createWriter( filePath ) ) {
         for ( String item : list ) {
            lineCount++;
            writer.write( item );
            writer.newLine();
            if ( lineCount % 100000 == 0 ) {
               LOGGER.info( "File Line " + lineCount );
            }
         }
      } catch ( IOException ioE ) {
         LOGGER.error( "Error writing " + description + " on line " + lineCount + " in file " + filePath );
      }
      LOGGER.info( "Wrote " + lineCount + " " + description + " to " + filePath );
   }


   static public Collection<String> readOneColumn( final String listFilePath, final String description ) {
      LOGGER.info( "Reading " + description + " from " + listFilePath );
      final Collection<String> listItems = new HashSet<>();
      long lineCount = 0;
      try ( BufferedReader reader = createReader( listFilePath ) ) {
         String line = readLine( reader, listFilePath );
         while ( line != null ) {
            lineCount++;
            listItems.add( line );
            if ( lineCount % 100000 == 0 ) {
               LOGGER.info( "File Line " + lineCount );
            }
            line = readLine( reader, listFilePath );
         }
      } catch ( IOException ioE ) {
         LOGGER.error( ioE.getMessage() );
      }
      LOGGER.info( "File Lines " + lineCount + "\t " + description + " " + listItems.size() );
      return listItems;
   }

//   static public void writeNamedSets( final String filePath, final String description,
//                                      final HashSetMap<String, String> namedSets ) {
//      LOGGER.info( "Writing " + description + " to " + filePath );
//      long lineCount = 0;
//      try {
//         final BufferedWriter writer = createWriter( filePath );
//         for ( Map.Entry<String, Set<String>> namedSet : namedSets.entrySet() ) {
//            lineCount++;
//            writer.write( TokenUtil.createBsvLine( namedSet.getKey(),
//                                                   TokenUtil.createCsvLine( namedSet.getParameterValue() ) ) );
//            writer.newLine();
//            if ( lineCount % 100000 == 0 ) {
//               LOGGER.info( "File Line " + lineCount );
//            }
//         }
//         writer.close();
//      } catch ( IOException ioE ) {
//         LOGGER.error( "Error writing " + description + " on line " + lineCount + " in file " + filePath );
//      }
//      LOGGER.info( "Wrote " + lineCount + " " + description + " to " + filePath );
//   }

   /**
    * @deprecated
    */
   static public void writeNamedSets( final String filePath, final String description,
                                      final Map<String, Collection<String>> namedSets ) {
      LOGGER.info( "Writing " + description + " to " + filePath );
      long lineCount = 0;
      try ( BufferedWriter writer = createWriter( filePath ) ) {
         for ( Map.Entry<String, Collection<String>> namedSet : namedSets.entrySet() ) {
            lineCount++;
            writer.write( TokenUtil.createBsvLine( namedSet.getKey(),
                  TokenUtil.createCsvLine( namedSet.getValue() ) ) );
            writer.newLine();
            if ( lineCount % 100000 == 0 ) {
               LOGGER.info( "File Line " + lineCount );
            }
         }
      } catch ( IOException ioE ) {
         LOGGER.error( "Error writing " + description + " on line " + lineCount + " in file " + filePath );
      }
      LOGGER.info( "Wrote " + lineCount + " " + description + " to " + filePath );
   }

   /**
    * @deprecated
    */
   @Deprecated
   static public Map<String, Collection<String>> readNamedSetsOld( final String filePath, final String description ) {
      final Collection<String> lines = readOneColumn( filePath, description );
      final Map<String, Collection<String>> namedSets = new HashMap<>( lines.size() );
      for ( String line : lines ) {
         final List<String> nameAndList = TokenUtil.getBsvItems( line );
         if ( nameAndList == null || nameAndList.size() != 2 ) {
            LOGGER.error( "Bad line " + line );
            continue;
         }
         namedSets.put( nameAndList.get( 0 ), TokenUtil.getCsvItems( nameAndList.get( 1 ) ) );
      }
      return namedSets;
   }

//   static public HashSetMap<String, String> readNamedSets( final String filePath, final String description ) {
//      final Collection<String> lines = readOneColumn( filePath, description );
//      final HashSetMap<String, String> namedSets = new HashSetMap<>( lines.size() );
//      for ( String line : lines ) {
//         final List<String> nameAndList = TokenUtil.getBsvItems( line );
//         if ( nameAndList == null || nameAndList.size() != 2 ) {
//            LOGGER.error( "Bad line " + line );
//            continue;
//         }
//         namedSets.addAll( nameAndList.get( 0 ), TokenUtil.getCsvItems( nameAndList.get( 1 ) ) );
//      }
//      return namedSets;
//   }

}
