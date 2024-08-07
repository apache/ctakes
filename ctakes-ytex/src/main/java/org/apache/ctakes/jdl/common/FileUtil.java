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
package org.apache.ctakes.jdl.common;


import org.apache.ctakes.core.resource.FileLocator;

import java.io.File;

/**
 * @deprecated Use FileLocator in ctakes-core.
 * Utility to get a file.
 * 
 * @author mas
 */
@Deprecated
public final class FileUtil {
	private FileUtil() {
	}



	/**
	 * @param fileName
	 *            the name of the file
	 * @return the file if exist in one of all the javaClassPaths otherwise null
	 */
	public static File getFile(final String fileName) {
		return FileLocator.getFileQuiet( fileName );
	}

	/**
	 * @param file
	 *            the file to get
	 * @param defaultString
	 *            the defaultString to return if the file is null
	 * @return the canonical pathname string
	 */
	public static String getCanonical(File file, String defaultString) {
		if ( file == null ) {
			return defaultString;
		}
		final File foundFile = FileLocator.getFileQuiet( file.getPath() );
		if ( foundFile != null ) {
			return foundFile.getPath();
		}
		return defaultString;
	}

	/**
	 * @param file
	 *            the file to get
	 * @return the canonical pathname string or USER_DIR if the file is null
	 */
	public static String getCanonical(File file) {
		return getCanonical(file, System.getProperty( "user.dir" ));
	}

}
