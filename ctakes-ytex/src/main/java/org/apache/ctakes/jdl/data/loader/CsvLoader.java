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
package org.apache.ctakes.jdl.data.loader;

import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVStrategy;
import org.apache.ctakes.jdl.data.base.JdlConnection;
import org.apache.ctakes.jdl.schema.xdl.CsvLoadType;
import org.apache.ctakes.jdl.schema.xdl.CsvLoadType.Column;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import java.io.*;
import java.lang.reflect.Constructor;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.Format;
import java.util.HashMap;
import java.util.Map;

/**
 * Loader of CSV file.
 * 
 * @author mas
 */
public class CsvLoader extends Loader {
	private CSVParser parser;
	private CsvLoadType loader;
	/**
	 * copied from CSVStrategy
	 */
	static final char DISABLED = '\ufffe';

	static private final Logger LOGGER = LoggerFactory.getLogger( "CsvLoader" );
	private Map<String, Format> formatMap;

	/**
	 * @param loader
	 *            the loader
	 * @param file
	 *            the file
	 * @throws FileNotFoundException
	 *             exception
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public CsvLoader(final CsvLoadType loader, final File file)
			throws FileNotFoundException {
		InputStream inputStrem = new FileInputStream(file);
		Reader reader = new InputStreamReader(inputStrem);
		char delimiter = toChar(loader.getDelimiter());
		char encapsulator = (loader.getEncapsulator() == null || loader
				.getEncapsulator().length() == 0) ? CSVStrategy.ENCAPSULATOR_DISABLED
				: toChar(loader.getEncapsulator());
		LOGGER.info(String.format("delimiter %d encapsulator %d", (int)delimiter, (int)encapsulator));
		CSVStrategy strategy = new CSVStrategy(delimiter, encapsulator, CSVStrategy.COMMENTS_DISABLED,
				CSVStrategy.ESCAPE_DISABLED, true, true, false, true);
		parser = new CSVParser(reader, strategy);
		this.loader = loader;
		formatMap = new HashMap<String, Format>();
		try {
			for (Column col : loader.getColumn()) {
				if (col.getFormat() != null && col.getFormat().length() > 0) {
					Class cf = Class.forName(col.getFormat());
					Constructor ccf = cf.getConstructor(String.class);
					this.formatMap.put(col.getName(),
							(Format) ccf.newInstance(col.getPattern()));
				}
			}
		} catch (Exception e) {
			throw new RuntimeException("oops", e);
		}

	}

	static private char toChar( final String text ) {
		if ( text == null || text.isEmpty() ) {
			throw new IllegalArgumentException();
		}
		return text.charAt( 0 );
	}

	static private char toChar( final Character c ) {
		if ( c == null ) {
			throw new IllegalArgumentException();
		}
		return c;
	}

	/**
	 * @param loader
	 *            the loader to manage
	 * @return the sql string
	 */
	public final String getSqlInsert(final CsvLoadType loader) {
		String query = "insert into " + loader.getTable() + " (";
		String values = ") values (";
		for (Column column : loader.getColumn()) {
			if (isNotTrue(column.isSkip())) {
				query += column.getName() + ",";
				values += "?,";
			}
		}
		return removeEnd(query, ",")
				+ removeEnd(values, ",") + ")";
	}

	static private boolean isNotTrue( final Boolean value ) {
		return value == null || !value;
	}

	static private boolean isTrue( final Boolean value ) {
		return value != null && value;
	}

	static private String removeEnd( final String text, final String endSplitter ) {
		final int lastIndex = text.lastIndexOf( endSplitter );
		if ( lastIndex < 0 ) {
			return text;
		}
		return text.substring( 0, lastIndex );
	}

	/**
	 * @param jdlConnection
	 *            the jdlConnection to manage
	 */
	@Override
	public final void dataInsert(final JdlConnection jdlConnection) {
		String sql = getSqlInsert(loader);
			LOGGER.info(sql);
		Number ncommit = loader.getCommit();
		int rs = (loader.getSkip() == null) ? 0 : loader.getSkip().intValue();
		PreparedStatement preparedStatement = null;
		try {
			jdlConnection.setAutoCommit(false);
			// String[][] values = parser.getAllValues();
			preparedStatement = jdlConnection.getOpenConnection()
					.prepareStatement(sql);
			boolean leftoversToCommit = false;
			// for (int r = rs; r < values.length; r++) {
			String[] row = null;
			int r = 0;
			do {
				row = parser.getLine();
				if (row == null)
					break;
				if (r < rs) {
					r++;
					continue;
				}
				r++;
				try {
					int cs = 0; // columns to skip
					int ce = 0; // columns from external
					int c = 0;
					// PreparedStatement preparedStatement = jdlConnection
					// .getOpenConnection().prepareStatement(sql);
					// if (ncommit == null) {
					// jdlConnection.setAutoCommit(true);
					// } else {
					// jdlConnection.setAutoCommit(false);
					// }
					for (Column column : loader.getColumn()) {
						if (isTrue(column.isSkip())) {
							cs++;
						} else {
							c++;
							Object value = column.getConstant();
							ce++;
							if (value == null) {
								if (column.getSeq() != null) {
									value = r + column.getSeq().intValue();
								} else {
									// value = values[r][c + cs - ce];
									value = row[c + cs - ce];
									ce--;
								}
							}
							if (value == null
									|| (value instanceof String && ((String) value)
											.length() == 0))
								preparedStatement.setObject(c, null);
							else {
								// if there is a formatter, parse the string
								if (this.formatMap
										.containsKey(column.getName())) {
									try {
										preparedStatement
												.setObject(
														c,
														this.formatMap
																.get(column
																		.getName())
																.parseObject(
																		(String) value));
									} catch (Exception e) {
										System.err.println("Could not format '"
												+ value + "' for column "
												+ column.getName()
												+ " on line " + r);
										e.printStackTrace(System.err);
										throw new RuntimeException(e);
									}
								} else {
									preparedStatement.setObject(c, value);
								}
							}
						}
					}
					preparedStatement.addBatch();
					leftoversToCommit = true;
					// preparedStatement.executeBatch();
					// executeBatch(preparedStatement);
					// if (!jdlConnection.isAutoCommit()
					// && (r % ncommit.intValue() == 0)) {
					if (r % ncommit.intValue() == 0) {
						preparedStatement.executeBatch();
						jdlConnection.commitConnection();
						leftoversToCommit = false;
						LOGGER.info("inserted " + ncommit.intValue() + " rows");
					}
				} catch (SQLException e) {
					// e.printStackTrace();
					throw new RuntimeException(e);
				}
			} while (row != null);
			if (leftoversToCommit) {
				preparedStatement.executeBatch();
				jdlConnection.commitConnection();
				leftoversToCommit = false;
			}
			LOGGER.info("inserted " + (r - rs) + " rows total");
		} catch (InstantiationException e) {
			LOGGER.error("", e);
		} catch (IllegalAccessException e) {
			LOGGER.error("", e);
		} catch (ClassNotFoundException e) {
			LOGGER.error("", e);
		} catch (IOException e) {
			LOGGER.error("", e);
		} catch (SQLException e) {
			throw new RuntimeException(e);
		} finally {
			if (preparedStatement != null) {
				try {
					preparedStatement.close();
				} catch (Exception e) {
				}
			}
		}
		// try {
		// if (!jdlConnection.isAutoCommit()) {
		// jdlConnection.commitConnection();
		// }
		// jdlConnection.closeConnection();
		// } catch (SQLException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }
	}
}
