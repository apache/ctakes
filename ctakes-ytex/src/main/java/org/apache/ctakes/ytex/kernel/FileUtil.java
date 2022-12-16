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
package org.apache.ctakes.ytex.kernel;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * miscellaneous utility functions used for data import/export
 * 
 * @author vijay
 * 
 */
public class FileUtil {
	static Pattern pFold = Pattern.compile("fold(\\d+)_");
	static Pattern pRun = Pattern.compile("run(\\d+)_");
	static Pattern pLabel = Pattern.compile("label([^_]+)_");

	/**
	 * extract fold from file name produced by file util
	 * 
	 * @param filename
	 * @return null if not in file name
	 */
	public static int parseFoldFromFileName(String filename) {
		Matcher m = pFold.matcher(filename);
		if (m.find()) {
			return Integer.parseInt(m.group(1));
		} else
			return 0;
	}

	/**
	 * extract run from file name produced by file util
	 * 
	 * @param filename
	 * @return null if not in file name
	 */
	public static Integer parseRunFromFileName(String filename) {
		Matcher m = pRun.matcher(filename);
		if (m.find()) {
			return Integer.parseInt(m.group(1));
		} else
			return 0;
	}

	/**
	 * extract label from file name produced by file util
	 * 
	 * @param filename
	 * @return null if not in file name
	 */
	public static String parseLabelFromFileName(String filename) {
		Matcher m = pLabel.matcher(filename);
		if (m.find()) {
			return m.group(1);
		} else
			return null;
	}

	/**
	 * construct file name with label, run, fold with format
	 * <tt>label[label]_run[run]_fold[fold]_</tt> only put in the non-null
	 * pieces.
	 * 
	 * @param outdir
	 * @param label
	 * @param run
	 * @param fold
	 * @return
	 */
	public static String getFoldFilePrefix(String outdir, String label,
			Integer run, Integer fold) {
		StringBuilder builder = new StringBuilder();
		if (outdir != null && outdir.length() > 0) {
			builder.append(outdir);
			if (!outdir.endsWith("/") && !outdir.endsWith("\\"))
				builder.append(File.separator);
		}
		if (label != null && label.length() > 0) {
			builder.append("label").append(label);
			if ((run != null && run > 0) || (fold != null && fold > 0))
				builder.append("_");
		}
		if (run != null && run > 0) {
			builder.append("run").append(Integer.toString(run));
			if (fold != null && fold > 0)
				builder.append("_");
		}
		if (fold != null && fold > 0) {
			builder.append("fold").append(Integer.toString(fold));
		}
		return builder.toString();
	}

	/**
	 * generate file name for given outdir and 'scope'
	 * 
	 * @param outdir
	 * @see #getFoldFilePrefix
	 * @param label
	 * @see #getFoldFilePrefix
	 * @param run
	 * @see #getFoldFilePrefix
	 * @param fold
	 * @see #getFoldFilePrefix
	 * @param suffix
	 *            added to file
	 * @return
	 */
	public static String getScopedFileName(String outdir, String label,
			Integer run, Integer fold, String suffix) {
		String filename = FileUtil.getFoldFilePrefix(outdir, label, run, fold);
		if (filename.length() > 0 && !filename.endsWith("/")
				&& !filename.endsWith("\\") && !filename.endsWith("."))
			filename += "_";
		filename += suffix;
		return filename;
	}

	public static String addFilenameToDir(String outdir, String filename) {
		StringBuilder builder = new StringBuilder();
		if (outdir != null && outdir.length() > 0) {
			builder.append(outdir);
			if (!outdir.endsWith("/") && !outdir.endsWith("\\"))
				builder.append(File.separator);
		}
		builder.append(filename);
		return builder.toString();
	}

	/**
	 * construct file name for train/test set, will be like
	 * <tt>label[label]_run[run]_fold[fold]_train</tt>
	 * 
	 * @param outdir
	 * @param label
	 * @param run
	 * @param fold
	 * @param train
	 * @return
	 */
	public static String getDataFilePrefix(String outdir, String label,
			Integer run, Integer fold, Boolean train) {
		StringBuilder builder = new StringBuilder(getFoldFilePrefix(outdir,
				label, run, fold));
		if ((label != null && label.length() > 0)
				|| (run != null && run > 0) || (fold != null && fold > 0))
			builder.append("_");
		if (train != null) {
			if (train.booleanValue())
				builder.append("train");
			else
				builder.append("test");
		}
		return builder.toString();
	}

	public static void createOutdir(String outdir) throws IOException {
		if (outdir != null && outdir.length() > 0) {
			File outdirF = new File(outdir);
			if (outdirF.exists()) {
				if (!outdirF.isDirectory()) {
					throw new IOException(
							"outdir exists but is not a directory " + outdir);
				}
			} else {
				if (!outdirF.mkdirs()) {
					throw new IOException("could not create directory: "
							+ outdir);
				}
			}
		}

	}

	public static boolean checkFileRead(String file) {
		return (new File(file)).canRead();
	}

	/**
	 * file filter to get directories
	 * 
	 * @author vijay
	 * 
	 */
	public static class DirectoryFileFilter implements FileFilter {
		@Override
		public boolean accept(File pathname) {
			return pathname.isDirectory();
		}
	}

	/**
	 * get files that start with specified prefix. just the file name, not
	 * preceding directories, are checked.
	 * 
	 * @author vijay
	 * 
	 */
	public static class PrefixFileFilter implements FileFilter {
		String prefix = null;

		public PrefixFileFilter(String prefix) {
			this.prefix = prefix;
		}

		@Override
		public boolean accept(File pathname) {
			return pathname.getName().startsWith(prefix);
		}

	}

	/**
	 * filter files by suffix
	 * 
	 * @author vijay
	 * 
	 */
	public static class SuffixFileFilter implements FileFilter {
		String suffix = null;

		public SuffixFileFilter(String prefix) {
			this.suffix = prefix;
		}

		@Override
		public boolean accept(File pathname) {
			return pathname.getName().endsWith(suffix);
		}

	}

	public static Properties loadProperties(String fileName,
			boolean systemOverride) throws IOException {
		Properties kernelProps = new Properties();
		InputStream is = null;
		boolean propsLoaded = false;
		if (fileName != null && fileName.length() > 0) {
			try {
				is = new BufferedInputStream(new FileInputStream(fileName));
				if (fileName.endsWith(".xml"))
					kernelProps.loadFromXML(is);
				else
					kernelProps.load(is);
				propsLoaded = true;
			} catch (FileNotFoundException fe) {
				// do nothing - options not required
			} finally {
				if (is != null)
					is.close();
			}
		}
		if (systemOverride) {
			kernelProps.putAll(System.getProperties());
			propsLoaded = true;
		}
		if (propsLoaded)
			return kernelProps;
		else
			return null;
	}

	public static Double getDoubleProperty(Properties props, String propKey,
			Double defaultProp) {
		Double propValue = null;
		String propStr = props.getProperty(propKey);
		if (propStr != null && propStr.length() > 0) {
			try {
				propValue = Double.parseDouble(propStr);
			} catch (NumberFormatException nfe) {
			}
		}
		return propValue != null ? propValue : defaultProp;
	}

	public static Integer getIntegerProperty(Properties props, String propKey,
			Integer defaultProp) {
		Integer propValue = null;
		String propStr = props.getProperty(propKey);
		if (propStr != null && propStr.length() > 0) {
			try {
				propValue = Integer.parseInt(propStr);
			} catch (NumberFormatException nfe) {
			}
		}
		return propValue != null ? propValue : defaultProp;
	}

}
