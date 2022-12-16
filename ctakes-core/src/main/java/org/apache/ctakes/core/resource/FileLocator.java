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
package org.apache.ctakes.core.resource;

import org.apache.log4j.Logger;

import java.io.*;
import java.net.*;
import java.nio.file.StandardCopyOption;
import java.util.*;

/**
 * Utility class that attempts to locate files.
 * 
 * @author Mayo Clinic
 */
final public class FileLocator {

   static private final Logger LOGGER = Logger.getLogger( "FileLocator" );

   /**
    * $CTAKES_HOME is an environment variable that may be set to indicate
    * the absolute directory path of the ctakes installation
    */
   static private final String CTAKES_HOME = "CTAKES_HOME";

   private enum TempFileHolder {
      INSTANCE;
      private final Map<String, File> __tempFiles = new HashMap<>();

      private void addFile( final String name, final File file ) {
         __tempFiles.putIfAbsent( name, file );
      }

      private File getFile( final String name ) {
         return __tempFiles.get( name );
      }
   }

   private FileLocator() {
   }

   /**
    *
    * @param location some absolute or relative resource location
    * @return a collection containing the location and the location with the prefix resources/
    */
   static private Collection<String> getUrlSearchPaths( final String location ) {
      String loci = location;
      if ( loci.startsWith( "\"" ) && loci.endsWith( "\"" ) ) {
         loci = loci.substring( 1, loci.length() - 1 );
      }
      final Collection<String> paths = new ArrayList<>( 2 );
      paths.add( loci );
      paths.add( "resources/" + loci );
      return paths;
   }

   /**
    * @param location some absolute or relative file location
    * @return a collection containing the location with prefixes created using the working directory, CTAKES_HOME, ctakes, and resources.
    * The working directory is traversed backwards to help developers that create projects at upper levels from ctakes.
    */
   static private Collection<String> getFileSearchPaths( final String location ) {
      String loci = location;
      if ( loci.startsWith( "\"" ) && loci.endsWith( "\"" ) ) {
         loci = loci.substring( 1, loci.length() - 1 );
      }
      final Collection<String> paths = new ArrayList<>();
      final String dir = System.getProperty( "user.dir" );
      if ( dir != null && !dir.isEmpty() ) {
         paths.add( dir + "/" + loci );
         paths.add( dir + "/resources/" + loci );
      }
      final String cTakesHome = System.getenv( CTAKES_HOME );
      if ( cTakesHome != null && !cTakesHome.isEmpty() ) {
         paths.add( cTakesHome + "/" + loci );
         paths.add( cTakesHome + "/resources/" + loci );
      }
      if ( dir != null && !dir.isEmpty() ) {
         File ancestor = new File( dir );
         while ( ancestor.getParentFile() != null ) {
            ancestor = ancestor.getParentFile();
            paths.add( ancestor + "/" + loci );
            paths.add( ancestor + "/ctakes/" + loci );
            paths.add( ancestor + "/resources/" + loci );
         }
      }
      return paths;
   }

   /**
    * @param location some absolute or relative resource or file location
    * @return a collection made from {@link #getUrlSearchPaths(String)} and {@link #getFileSearchPaths(String)}
    */
   static private Collection<String> getAllSearchPaths( final String location ) {
      final Collection<String> locations = new ArrayList<>( getUrlSearchPaths( location ) );
      locations.addAll( getFileSearchPaths( location ) );
      return locations;
   }

   /**
    * Fetches stream
    * Search order is by:
    * 1. By resource stream in classpath
    * 2. By file on filesystem
    * 3. By resource url in classpath
    *
    * @param location some string representing the full or partial location of a resource
    * @return an input stream for the resource
    * @throws FileNotFoundException if no resource could be found
    */
   static public InputStream getAsStream( final String location ) throws FileNotFoundException {
      return getAsStream( FileLocator.class, location );
   }

   /**
    * Fetches stream
    * Search order is by:
    * 1. By resource stream in classpath
    * 2. By file on filesystem
    * 3. By resource url in classpath
    *
    * @param clazz    some class whose classloader should be used
    * @param location some string representing the full or partial location of a resource
    * @return an input stream for the resource
    * @throws FileNotFoundException if no resource could be found
    */
   static public InputStream getAsStream( final Class<?> clazz, final String location ) throws FileNotFoundException {
      final InputStream stream = getStreamQuiet( clazz, location );
      if ( stream != null ) {
         return stream;
      }
      throw new FileNotFoundException( "No stream available for " + location );
   }

   /**
    * Fetches stream without throwing exceptions.
    * Search order is by:
    * 1. By resource stream in classpath
    * 2. By file on filesystem
    * 3. By resource url in classpath
    *
    * @param location some string representing the full or partial location of a resource
    * @return an input stream for the resource
    */
   static public InputStream getStreamQuiet( final String location ) {
      return getStreamQuiet( FileLocator.class, location );
   }

   /**
    * Fetches stream without throwing exceptions.
    * Search order is by:
    * 1. By resource stream in classpath
    * 2. By file on filesystem
    * 3. By resource url in classpath
    *
    * @param clazz    some class whose classloader should be used
    * @param location some string representing the full or partial location of a resource
    * @return an input stream for the resource
    */
   static public InputStream getStreamQuiet( final Class<?> clazz, final String location ) {
      final Collection<String> urlPaths = getUrlSearchPaths( location );
      final InputStream stream = urlPaths.stream()
            .map( l -> getStreamOnly( clazz, l ) )
            .filter( Objects::nonNull )
            .findFirst()
            .orElse( null );
      if ( stream != null ) {
         return stream;
      }
      final Collection<String> allPaths = getAllSearchPaths( location );
      File file = allPaths.stream()
            .map( FileLocator::getFileOnly )
            .filter( Objects::nonNull )
            .findFirst()
            .orElse( null );
      if ( file != null ) {
         try {
            return new FileInputStream( file );
         } catch ( FileNotFoundException fnfE ) {
            // do nothing
         }
      }
      final URL url = urlPaths.stream()
            .map( l -> getResourceOnly( clazz, l ) )
            .filter( Objects::nonNull )
            .findFirst()
            .orElse( null );
      if ( url != null ) {
         try {
            final URI indexUri = new URI( url.toExternalForm() );
            if ( !indexUri.isOpaque() ) {
               return new FileInputStream( new File( indexUri ) );
            }
         } catch ( URISyntaxException | FileNotFoundException multE ) {
            return null;
         }
      }
      return null;
   }

   /**
    * Fetches stream without throwing exceptions only by finding a stream in the classpath
    *
    * @param clazz    some class whose classloader should be used
    * @param location some string representing the full or partial location of a resource
    * @return an input stream for the resource
    */
   static private InputStream getStreamOnly( final Class<?> clazz, final String location ) {
      try {
         //Get from classpath according to given class
         InputStream stream = clazz.getClassLoader().getResourceAsStream( location );
         if ( stream != null ) {
            return stream;
         }
         stream = clazz.getResourceAsStream( location );
         if ( stream != null ) {
            return stream;
         }
      } catch ( Exception e ) {
         return null;
      }
      return null;
   }

   /**
    * Fetches resource.
    * Search order is by:
    * 1. By file on filesystem
    * 2. By resource url in classpath
    *
    * @param location some string representing the full or partial location of a resource
    * @return an url for the resource
    * @throws FileNotFoundException if no resource could be found
    */
   static public URL getResource( final String location ) throws FileNotFoundException {
      return getResource( FileLocator.class, location );
   }

   /**
    * Fetches resource.
    * Search order is by:
    * 1. By file on filesystem
    * 2. By resource url in classpath
    *
    * @param clazz    some class whose classloader should be used
    * @param location some string representing the full or partial location of a resource
    * @return an url for the resource
    * @throws FileNotFoundException if no resource could be found
    */
   static public URL getResource( final Class<?> clazz, final String location ) throws FileNotFoundException {
      final URL url = getResourceQuiet( clazz, location );
      if ( url != null ) {
         return url;
      }
      throw new FileNotFoundException( "No Resource at " + location );
   }

   /**
    * Fetches resource without throwing exceptions.
    * Search order is by:
    * 1. By file on filesystem
    * 2. By resource url in classpath
    *
    * @param location some string representing the full or partial location of a resource
    * @return an url for the resource
    */
   static public URL getResourceQuiet( final String location ) {
      return getResourceQuiet( FileLocator.class, location );
   }

   /**
    * Fetches resource without throwing exceptions.
    * Search order is by:
    * 1. By file on filesystem
    * 2. By resource url in classpath
    *
    * @param clazz    some class whose classloader should be used
    * @param location some string representing the full or partial location of a resource
    * @return an url for the resource
    */
   static public URL getResourceQuiet( final Class<?> clazz, final String location ) {
      final Collection<String> allPaths = getAllSearchPaths( location );
      final File file = allPaths.stream()
            .map( FileLocator::getFileOnly )
            .filter( Objects::nonNull )
            .findFirst()
            .orElse( null );
      if ( file != null ) {
         try {
            return file.toURI().toURL();
         } catch ( MalformedURLException urlE ) {
            // do nothing
         }
      }
      final URL url = getUrlSearchPaths( location ).stream()
            .map( l -> getResourceOnly( clazz, l ) )
            .filter( Objects::nonNull )
            .findFirst()
            .orElse( null );
      if ( url != null ) {
         return url;
      }
      return null;
   }

   /**
    * Fetches resource without throwing exceptions only by finding a resource in the classpath
    *
    * @param clazz    some class whose classloader should be used
    * @param location some string representing the full or partial location of a resource
    * @return an url for the resource
    */
   static private URL getResourceOnly( final Class<?> clazz, final String location ) {
      final ClassLoader classLoader = clazz.getClassLoader();
      final URL url = classLoader.getResource( location );
      if ( url != null ) {
         LOGGER.debug( location + " found at " + url.toExternalForm() );
         return url;
      }
      return clazz.getResource( location );
   }

   /**
    * Calls {@link #getFile(String)}
    *
    * @deprecated use {@link #getFile(String)}
    */
   @Deprecated
   static public File locateFile( final String location ) throws FileNotFoundException {
      return locateFile( FileLocator.class, location );
   }

   /**
    * Calls {@link #getFile(Class, String)}
    * @deprecated use {@link #getFile(Class, String)}
    */
   @Deprecated
   static public File locateFile( final Class<?> clazz, final String location ) throws FileNotFoundException {
      return getFile( clazz, location );
   }

   /**
    * Fetches file.
    * Search order is by:
    * 1. By file on filesystem
    * 2. By resource url in classpath
    * 3. By resource stream in classpath
    * If a stream is found then it is copied to a temporary file and that file is returned.
    *
    * @param location some string representing the full or partial location of a resource
    * @return an file for the resource
    * @throws FileNotFoundException if a file cannot be found or temporary file created
    */
   static public File getFile( final String location ) throws FileNotFoundException {
      return getFile( FileLocator.class, location );
   }

   /**
    * Fetches file.
    * Search order is by:
    * 1. By file on filesystem
    * 2. By resource url in classpath
    * 3. By resource stream in classpath
    * If a stream is found then it is copied to a temporary file and that file is returned.
    *
    * @param clazz    some class whose classloader should be used
    * @param location some string representing the full or partial location of a resource
    * @return an file for the resource
    * @throws FileNotFoundException if a file cannot be found or temporary file created
    */
   static public File getFile( final Class<?> clazz, final String location ) throws FileNotFoundException {
      final File file = getFileQuiet( clazz, location );
      if ( file != null ) {
         return file;
      }
      throw new FileNotFoundException( "No File found for " + location );
   }

   /**
    * Fetches file without throwing exceptions.
    * Search order is by:
    * 1. By file on filesystem
    * 2. By resource url in classpath
    * 3. By resource stream in classpath
    * If a stream is found then it is copied to a temporary file and that file is returned.
    *
    * @param location some string representing the full or partial location of a resource
    * @return an file for the resource or null if none is found
    */
   static public File getFileQuiet( final String location ) {
      return getFileQuiet( FileLocator.class, location );
   }

   /**
    * Fetches file without throwing exceptions.
    * Search order is by:
    * 1. By file on filesystem
    * 2. By resource url in classpath
    * 3. By resource stream in classpath
    * If a stream is found then it is copied to a temporary file and that file is returned.
    *
    * @param clazz    some class whose classloader should be used
    * @param location some string representing the full or partial location of a resource
    * @return an file for the resource or null if none is found
    */
   static public File getFileQuiet( final Class<?> clazz, final String location ) {
      final Collection<String> allPaths = getAllSearchPaths( location );
      final File file = allPaths.stream()
            .map( FileLocator::getFileOnly )
            .filter( Objects::nonNull )
            .findFirst()
            .orElse( null );
      if ( file != null ) {
         return file;
      }
      final Collection<String> urlPaths = getUrlSearchPaths( location );
      final URL url = urlPaths.stream()
            .map( l -> getResourceOnly( clazz, l ) )
            .filter( Objects::nonNull )
            .findFirst()
            .orElse( null );
      if ( url != null ) {
         try {
            final URI uri = new URI( url.toExternalForm() );
            if ( !uri.isOpaque() ) {
               return new File( uri );
            }
         } catch ( URISyntaxException uriE ) {
            // do nothing
         }
      }
      final InputStream stream = urlPaths.stream()
            .map( l -> getStreamOnly( clazz, l ) )
            .filter( Objects::nonNull )
            .findFirst()
            .orElse( null );
      if ( stream != null ) {
         return createTempFile( stream, location );
      }
      return null;
   }

   /**
    * Fetches file without throwing exceptions only by finding an existing file in the filesystem.
    *
    * @param location some string representing the full or partial location of a file
    * @return a discovered file or null
    */
   static private File getFileOnly( final String location ) {
      String loci = location;
      if ( loci.startsWith( "\"" ) && loci.endsWith( "\"" ) ) {
         loci = loci.substring( 1, loci.length() - 1 );
      }
      File file = new File( loci );
      if ( file.exists() ) {
         return file;
      }
      return null;
   }


   /**
    * @param stream   an input stream that exists within the classpath
    * @param location some originally requested file location
    * @return a temporary file containing the contents of the stream
    */
   static private File createTempFile( final InputStream stream, final String location ) {
      String loci = location;
      if ( loci.startsWith( "\"" ) && loci.endsWith( "\"" ) ) {
         loci = loci.substring( 1, loci.length() - 1 );
      }
      final String tempName = loci.replace( '/', '_' ).replace( '\\', '_' );
      synchronized (TempFileHolder.INSTANCE) {
         final File file = TempFileHolder.INSTANCE.getFile( tempName );
         if ( file != null ) {
            return file;
         }
         try ( InputStream reader = new BufferedInputStream( stream ) ) {
            final File tempFile = File.createTempFile( tempName, null );
            tempFile.deleteOnExit();
            java.nio.file.Files.copy(
                  reader,
                  tempFile.toPath(),
                  StandardCopyOption.REPLACE_EXISTING );
            TempFileHolder.INSTANCE.addFile( tempName, tempFile );
            return TempFileHolder.INSTANCE.getFile( tempName );
         } catch ( IOException ioE ) {
            LOGGER.error( ioE.getMessage() );
         }
      }
      return null;
   }


   /**
    * Calls a {@link #getFile(Class, String)} and returns the path of the file or "" if none.
    * @throws FileNotFoundException if the file cannot be found
    * @deprecated use {@link #getFile(String)} and {@link File#getPath()}
    */
   @Deprecated
   static public String getFullPath( final String relativePath ) throws FileNotFoundException {
      return getFullPath( FileLocator.class, relativePath );
   }

   /**
    * Calls a {@link #getFile(Class, String)} and returns the path of the file or "" if none.
    * @throws FileNotFoundException if the file cannot be found
    * @deprecated use {@link #getFile(Class, String)} and {@link File#getPath()}
    */
   @Deprecated
   static public String getFullPath( final Class<?> clazz, final String relativePath ) throws FileNotFoundException {
      final String fullPath = getFullPathQuiet( clazz, relativePath );
      if ( fullPath != null && !fullPath.isEmpty() ) {
         return fullPath;
      }
      final StringBuilder sb = new StringBuilder();
      sb.append( "Could not find " ).append( relativePath ).append( "\nas absolute or in $CLASSPATH :\n" );
      final ClassLoader classLoader = clazz.getClassLoader();
      final URL[] classpathUrls = ((URLClassLoader)classLoader).getURLs();
      for ( URL url : classpathUrls ) {
         sb.append( url.getFile() ).append( "\n" );
      }
      final String cwd = System.getProperty( "user.dir" );
      sb.append( "or in working directory : " ).append( cwd ).append( "\n" );
      sb.append( "or in any parent thereof (with or without /ctakes/)\n" );
      final String cTakesHome = System.getenv( CTAKES_HOME );
      sb.append( "or in $CTAKES_HOME : " ).append( cTakesHome );
      LOGGER.error( sb.toString() );
      throw new FileNotFoundException( "No File exists at " + relativePath );
   }

   /**
    * Calls a {@link #getFile(Class, String)} and returns the path of the file or "" if none.
    * @deprecated use {@link #getFileQuiet(Class, String)} and {@link File#getPath()}
    */
   @Deprecated
   static public String getFullPathQuiet( final String relativePath ) {
      return getFullPathQuiet( FileLocator.class, relativePath );
   }

   /**
    * Calls a {@link #getFile(Class, String)} and returns the path of the file or "" if none.
    * @deprecated use {@link #getFileQuiet(Class, String)} and {@link File#getPath()}
    */
   @Deprecated
   static public String getFullPathQuiet( final Class<?> clazz, final String relativePath ) {
      final File file = getFileQuiet( clazz, relativePath );
      if ( file == null ) {
         return "";
      }
      return file.getPath();
   }


}