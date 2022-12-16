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
package org.apache.ctakes.ytex.kernel.evaluator;

import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ctakes.ytex.kernel.metric.ConceptSimilarityService;


/**
 * Before comparing semantic distance, use this kernel to filter by semantic
 * type.
 * <p/>
 * Modes:
 * <li>MAINSUI (default): concept's main semantic types must overlap
 * <li>TUI: concept's TUIs must overlap.
 * <p/>
 * The MAINSUI mode is taken from Sujeevan Aseervatham's semantic kernel. It
 * maps all semantic types to a handful of semantic types.
 * <p/>
 * The corpusName parameter specifies the concepts for which cuis' semantic
 * types will be loaded
 * 
 * @author vijay
 * 
 */
public class SemanticTypeKernel extends CacheKernel {
	private static final Log log = LogFactory.getLog(SemanticTypeKernel.class);
	private static final String MAINSUI = "MAINSUI";
	private static final String TUI = "TUI";

	public static int getMainSem(int sui) {
		switch (sui) {
		case 52:
		case 53:
		case 56:
		case 51:
		case 64:
		case 55:
		case 66:
		case 57:
		case 54:
			return 0;
		case 17:
		case 29:
		case 23:
		case 30:
		case 31:
		case 22:
		case 25:
		case 26:
		case 18:
		case 21:
		case 24:
			return 1;
		case 116:
		case 195:
		case 123:
		case 122:
		case 118:
		case 103:
		case 120:
		case 104:
		case 200:
		case 111:
		case 196:
		case 126:
		case 131:
		case 125:
		case 129:
		case 130:
		case 197:
		case 119:
		case 124:
		case 114:
		case 109:
		case 115:
		case 121:
		case 192:
		case 110:
		case 127:
			return 2;
		case 185:
		case 77:
		case 169:
		case 102:
		case 78:
		case 170:
		case 171:
		case 80:
		case 81:
		case 89:
		case 82:
		case 79:
			return 3;
		case 203:
		case 74:
		case 75:
			return 4;
		case 20:
		case 190:
		case 49:
		case 19:
		case 47:
		case 50:
		case 33:
		case 37:
		case 48:
		case 191:
		case 46:
		case 184:
			return 5;
		case 87:
		case 88:
		case 28:
		case 85:
		case 86:
			return 6;
		case 83:
			return 7;
		case 100:
		case 3:
		case 11:
		case 8:
		case 194:
		case 7:
		case 12:
		case 99:
		case 13:
		case 4:
		case 96:
		case 16:
		case 9:
		case 15:
		case 1:
		case 101:
		case 2:
		case 98:
		case 97:
		case 14:
		case 6:
		case 10:
		case 204: // vng missing sui
		case 5:
			return 8;
		case 71:
		case 168:
		case 73:
		case 72:
		case 167:
			return 9;
		case 91:
		case 90:
			return 10;
		case 93:
		case 92:
		case 94:
		case 95:
			return 11;
		case 38:
		case 69:
		case 68:
		case 34:
		case 70:
		case 67:
			return 12;
		case 43:
		case 201:
		case 45:
		case 41:
		case 44:
		case 42:
		case 32:
		case 40:
		case 39:
			return 13;
		case 60:
		case 65:
		case 58:
		case 59:
		case 63:
		case 62:
		case 61:
			return 14;
		default:
			break;
		}
		return -1;
	}

	private ConceptSimilarityService conceptSimilarityService;
	private String corpusName;
	private Map<String, Set<Integer>> cuiMainSuiMap = new HashMap<String, Set<Integer>>();
	private Map<String, BitSet> cuiTuiMap = null;
	private List<String> tuiList = null;
	private String cuiTuiQuery;
	// private DataSource dataSource;
	// private SimpleJdbcTemplate simpleJdbcTemplate;
	// private JdbcTemplate jdbcTemplate;

	private String mode = "MAINSUI";

	// private PlatformTransactionManager transactionManager;

	// private void addCuiTuiToMap(Map<String, String> tuiMap, String cui,
	// String tui) {
	// // get 'the' tui string
	// if (tuiMap.containsKey(tui))
	// tui = tuiMap.get(tui);
	// else
	// tuiMap.put(tui, tui);
	// Set<String> tuis = cuiTuiMap.get(cui);
	// if (tuis == null) {
	// tuis = new HashSet<String>();
	// cuiTuiMap.put(cui, tuis);
	// }
	// tuis.add(tui);
	// }

	/**
	 * concepts have overlapping semantic types? yes return 1, else return 0
	 */
	public double innerEvaluate(Object o1, Object o2) {
		if (o1 == null || o2 == null)
			return 0;
		else if (o1.equals(o2))
			return 1.0;
		else if (this.getMode() == null || this.getMode().length() == 0
				|| MAINSUI.equals(this.getMode()))
			return mainSuiCheck(o1, o2);
		else if (TUI.equals(this.getMode()))
			return tuiCheck(o1, o2);
		else {
			log.error("invalid mode");
			throw new RuntimeException("invalid mode");
		}
	}

	public ConceptSimilarityService getConceptSimilarityService() {
		return conceptSimilarityService;
	}

	public String getCorpusName() {
		return corpusName;
	}

	public String getCuiTuiQuery() {
		return cuiTuiQuery;
	}

	//
	// public DataSource getDataSource() {
	// return dataSource;
	// }

	public String getMode() {
		return mode;
	}

	// public PlatformTransactionManager getTransactionManager() {
	// return transactionManager;
	// }

	public void init() {
		// TransactionTemplate t = new
		// TransactionTemplate(this.transactionManager);
		// t.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);
		// t.execute(new TransactionCallback<Object>() {
		// @Override
		// public Object doInTransaction(TransactionStatus arg0) {
		cuiTuiMap = conceptSimilarityService.getCuiTuiMap();
		tuiList = conceptSimilarityService.getTuiList();
		initCuiMainSuiMap();
		// return null;
		// }
		// });
	}

	/**
	 * init the cui -> 'main sui' map.
	 */
	private void initCuiMainSuiMap() {
		if (cuiTuiMap != null) {
			for (Map.Entry<String, BitSet> cuiTui : cuiTuiMap.entrySet()) {
				cuiMainSuiMap.put(cuiTui.getKey(),
						tuiToMainSui(cuiTui.getValue()));
			}
		}
	}

	// /**
	// * init cui-tui map from query
	// */
	// public void initCuiTuiMapFromQuery() {
	// this.jdbcTemplate.query(this.cuiTuiQuery, new RowCallbackHandler() {
	// // don't duplicate tui strings to save memory
	// Map<String, String> tuiMap = new HashMap<String, String>();
	//
	// @Override
	// public void processRow(ResultSet rs) throws SQLException {
	// String cui = rs.getString(1);
	// String tui = rs.getString(2);
	// addCuiTuiToMap(tuiMap, cui, tui);
	// }
	// });
	// }

	/**
	 * 
	 * @param o1
	 *            cui
	 * @param o2
	 *            cui
	 * @return concepts have overlapping main semantic types, return 1, else
	 *         return 0
	 */
	private double mainSuiCheck(Object o1, Object o2) {
		Set<Integer> tuis1 = cuiMainSuiMap.get((String) o1);
		Set<Integer> tuis2 = cuiMainSuiMap.get((String) o2);
		// only compare the two if they have a common semantic type
		if (tuis1 != null && tuis2 != null
				&& !Collections.disjoint(tuis1, tuis2)) {
			return 1;
		} else {
			return 0;
		}
	}

	public void setConceptSimilarityService(
			ConceptSimilarityService conceptSimilarityService) {
		this.conceptSimilarityService = conceptSimilarityService;
	}

	public void setCorpusName(String corpusName) {
		this.corpusName = corpusName;
	}

	public void setCuiTuiQuery(String cuiTuiQuery) {
		this.cuiTuiQuery = cuiTuiQuery;
	}

	// public void setDataSource(DataSource dataSource) {
	// this.dataSource = dataSource;
	// // this.simpleJdbcTemplate = new SimpleJdbcTemplate(dataSource);
	// this.jdbcTemplate = new JdbcTemplate(dataSource);
	// }

	public void setMode(String mode) {
		this.mode = mode;
	}

	// public void setTransactionManager(
	// PlatformTransactionManager transactionManager) {
	// this.transactionManager = transactionManager;
	// }

	/**
	 * 
	 * @param o1
	 *            cui
	 * @param o2
	 *            cui
	 * @return concepts have overlapping tuis, return 1, else return 0
	 */
	private double tuiCheck(Object o1, Object o2) {
		if(cuiTuiMap == null)
			return 0;
		BitSet tuis1 = this.cuiTuiMap.get((String) o1);
		BitSet tuis2 = this.cuiTuiMap.get((String) o2);
		if (tuis1 != null && tuis2 != null && tuis1.intersects(tuis2)) {
			return 1;
		} else {
			return 0;
		}
	}

	public Set<Integer> tuiToMainSui(BitSet tuis) {
		Set<Integer> mainSui = new HashSet<Integer>(tuis.size());
		for (int i = tuis.nextSetBit(0); i >= 0; i = tuis.nextSetBit(i + 1)) {
			String tui = this.tuiList.get(i);
			mainSui.add(getMainSem(Integer.parseInt(tui.substring(1))));
		}
		return mainSui;
	}

}
