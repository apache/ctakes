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
package org.apache.ctakes.ytex.uima.mapper;

import java.io.ByteArrayOutputStream;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.GZIPOutputStream;

import javax.sql.DataSource;

import org.apache.commons.collections.map.CaseInsensitiveMap;
import org.apache.commons.jxpath.JXPathContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ctakes.ytex.dao.DBUtil;
import org.apache.ctakes.ytex.uima.model.Document;
import org.apache.ctakes.ytex.uima.model.DocumentAnnotation;
import org.apache.ctakes.ytex.uima.model.UimaType;
import org.apache.ctakes.ytex.uima.types.DocKey;
import org.apache.ctakes.ytex.uima.types.KeyValuePair;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.impl.XmiCasSerializer;
import org.apache.uima.cas.text.AnnotationIndex;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.cas.FSList;
import org.apache.uima.jcas.cas.NonEmptyFSList;
import org.apache.uima.jcas.cas.TOP;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.util.XMLSerializer;
import org.hibernate.Query;
import org.hibernate.SessionFactory;
import org.hibernate.dialect.Dialect;
import org.hibernate.jdbc.Work;
import org.hibernate.persister.entity.AbstractEntityPersister;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.BiMap;
import com.google.common.collect.Collections2;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.SetMultimap;

/**
 * Map document annotations to the database.
 * 
 * @author vijay
 * 
 */
public class DocumentMapperServiceImpl implements DocumentMapperService,
		InitializingBean {
	/**
	 * holder for FeatureStruct attributes
	 * 
	 * @author vijay
	 * 
	 */
	public static class AnnoFSAttribute {
		private int annoBaseId;

		private FeatureStructure fs;

		private Integer index;

		public AnnoFSAttribute() {
			super();
		}

		public AnnoFSAttribute(int annoBaseId, FeatureStructure fs,
				Integer index) {
			super();
			this.annoBaseId = annoBaseId;
			this.fs = fs;
			this.index = index;
		}

		public int getAnnoBaseId() {
			return annoBaseId;
		}

		public FeatureStructure getFs() {
			return fs;
		}

		public Integer getIndex() {
			return index;
		}

		public void setAnnoBaseId(int annoBaseId) {
			this.annoBaseId = annoBaseId;
		}

		public void setFs(FeatureStructure fs) {
			this.fs = fs;
		}

		public void setIndex(Integer index) {
			this.index = index;
		}

	}

	public static class AnnoLink {
		private int childAnnoBaseId;
		private String feature;
		private int parentAnnoBaseId;

		public AnnoLink(int annoId, int childAnnoId, String feature) {
			this.parentAnnoBaseId = annoId;
			this.childAnnoBaseId = childAnnoId;
			this.feature = feature;
		}

		public int getChildAnnoBaseId() {
			return childAnnoBaseId;
		}

		public String getFeature() {
			return feature;
		}

		public int getParentAnnoBaseId() {
			return parentAnnoBaseId;
		}

		public void setChildAnnoBaseId(int childAnnoBaseId) {
			this.childAnnoBaseId = childAnnoBaseId;
		}

		public void setFeature(String feature) {
			this.feature = feature;
		}

		public void setParentAnnoBaseId(int parentAnnoBaseId) {
			this.parentAnnoBaseId = parentAnnoBaseId;
		}
	}

	private static final Log log = LogFactory
			.getLog(DocumentMapperServiceImpl.class);

	private static Set<Integer> numericTypes = new HashSet<Integer>();
	private static Set<Integer> stringTypes = new HashSet<Integer>();
	/**
	 * date format for analysis batch.
	 */
	private static final ThreadLocal<DateFormat> tlAnalysisBatchDateFormat = new ThreadLocal<DateFormat>() {
		public DateFormat initialValue() {
			return new SimpleDateFormat("yyyy-MM-dd HH:mm");
		}
	};
	static {
		stringTypes.addAll(Arrays.asList(Types.CHAR, Types.NCHAR,
				Types.VARCHAR, Types.NVARCHAR));
		numericTypes.addAll(Arrays.asList(Types.BIGINT, Types.BIT,
				Types.BOOLEAN, Types.TINYINT, Types.SMALLINT, Types.DECIMAL,
				Types.FLOAT, Types.DOUBLE, Types.INTEGER));
	}
	private Set<AnnoMappingInfo> annoMappingInfos;
	private int batchSize = 100;
	private DataSource dataSource;
	private String dbSchema;
	private String dbType;
	private Dialect dialect;
	private String dialectClassName;

	private CaseInsensitiveMap docTableCols = new CaseInsensitiveMap();

	private String formattedTableName = null;

	private JdbcTemplate jdbcTemplate;

	private Map<String, AnnoMappingInfo> mapAnnoMappingInfo = new HashMap<String, AnnoMappingInfo>();
	private SessionFactory sessionFactory;

	private ThreadLocal<Map<String, AnnoMappingInfo>> tl_mapAnnoMappingInfo = new ThreadLocal<Map<String, AnnoMappingInfo>>() {

		@Override
		protected Map<String, AnnoMappingInfo> initialValue() {
			return new HashMap<String, AnnoMappingInfo>();
		}

	};

	/**
	 * map of annotation to fields that need to be mapped
	 */
	private ThreadLocal<SetMultimap<String, String>> tl_mapFieldInfo = new ThreadLocal<SetMultimap<String, String>>() {
		@Override
		protected SetMultimap<String, String> initialValue() {
			return HashMultimap.create();
		}

	};
	private PlatformTransactionManager transactionManager;
	private Map<String, UimaType> uimaTypeMap = new HashMap<String, UimaType>();
	private Properties ytexProperties;

	private void addAnnoLinks(JCas jcas,
			BiMap<Annotation, Integer> mapAnnoToId, List<AnnoLink> listAnnoLinks) {
		Collection<AnnoMappingInfo> annoLinkInfos = Collections2.filter(this
				.getMapAnnoMappingInfo().values(),
				new Predicate<AnnoMappingInfo>() {

					@Override
					public boolean apply(AnnoMappingInfo mi) {
						return "anno_link".equalsIgnoreCase(mi.getTableName());
					}
				});
		for (AnnoMappingInfo mi : annoLinkInfos) {
			addAnnoLinks(jcas, mapAnnoToId, listAnnoLinks, mi);
		}
	}

	private void addAnnoLinks(JCas jcas,
			BiMap<Annotation, Integer> mapAnnoToId,
			List<AnnoLink> listAnnoLinks, AnnoMappingInfo mi) {
		Type t = jcas.getTypeSystem().getType(mi.getAnnoClassName());
		if (t != null) {
			ColumnMappingInfo cip = mi.getMapField().get("parent_anno_base_id");
			ColumnMappingInfo cic = mi.getMapField().get("child_anno_base_id");
			// get the parent and child features
			Feature fp = t.getFeatureByBaseName(cip.getAnnoFieldName());
			Feature fc = t.getFeatureByBaseName(cic.getAnnoFieldName());
			// get all annotations
			FSIterator<FeatureStructure> iter = jcas.getFSIndexRepository()
					.getAllIndexedFS(t);
			while (iter.hasNext()) {
				FeatureStructure fs = iter.next();
				// get parent and child feature values
				FeatureStructure fsp = fs.getFeatureValue(fp);
				FeatureStructure fsc = fs.getFeatureValue(fc);
				if (fsp != null && fsc != null) {
					// extract the parent annotation from the parent feature
					// value
					Object parentAnno = extractFeature(cip.getJxpath(), fsp);
					if (parentAnno instanceof Annotation) {
						Integer parentId = mapAnnoToId
								.get((Annotation) parentAnno);
						if (parentId != null) {
							// parent is persisted, look for child(ren)
							if (fsc instanceof FSList || fsc instanceof FSArray) {
								// this is a one-to-many relationship
								// iterate over children
								List<FeatureStructure> children = extractList(fsc);
								for (FeatureStructure child : children) {
									addLink(mapAnnoToId, listAnnoLinks,
											t.getShortName(), cic.getJxpath(),
											parentId, child);
								}
							} else {
								// this is a one-to-one relationship
								addLink(mapAnnoToId, listAnnoLinks,
										t.getShortName(), cic.getJxpath(),
										parentId, fsc);
							}
						}
					}
				}
			}
		}
	}

	/**
	 * add a link. apply jxpath as needed, get child anno id, and save the link
	 * 
	 * @param mapAnnoToId
	 *            map to find existing annos
	 * @param listAnnoLinks
	 *            list to populate
	 * @param linkType
	 *            anno_link.feature
	 * @param childJxpath
	 *            jxpath to child annotation feature value, can be null
	 * @param parentId
	 *            parent_anno_base_id
	 * @param child
	 *            child object to apply jxpath to, or which is already an
	 *            annotation
	 */
	private void addLink(BiMap<Annotation, Integer> mapAnnoToId,
			List<AnnoLink> listAnnoLinks, String linkType, String childJxpath,
			Integer parentId, FeatureStructure child) {
		Object childAnno = extractFeature(childJxpath, child);
		if (childAnno instanceof Annotation) {
			Integer childId = mapAnnoToId.get((Annotation) childAnno);
			if (childId != null) {
				listAnnoLinks.add(new AnnoLink(parentId, childId, linkType));
			}
		}
	}

	/**
	 * load the map of uima annotation class name to mapper class name from the
	 * database.
	 * 
	 * For some reason this is not getting executed within a transaction.
	 * Manually wrap the db access in a transaction.
	 * 
	 * 
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	public void afterPropertiesSet() {
		TransactionTemplate txTemplate = new TransactionTemplate(
				this.getTransactionManager());
		txTemplate
				.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRED);
		txTemplate.execute(new TransactionCallback<Object>() {

			@Override
			public Object doInTransaction(TransactionStatus arg0) {
				Query q = getSessionFactory().getCurrentSession()
						.getNamedQuery("getUimaTypes");
				List<UimaType> uimaTypes = q.list();
				for (UimaType uimaType : uimaTypes) {
					uimaTypeMap.put(uimaType.getUimaTypeName(), uimaType);
				}
				initDocKeyMapping();
				return null;
			}
		});
	}

	private Document createDocument(JCas jcas, String analysisBatch,
			boolean bStoreDocText, boolean bStoreCAS) {
		Document doc = new Document();
		if (bStoreDocText)
			doc.setDocText(jcas.getDocumentText());
		doc.setAnalysisBatch(analysisBatch == null
				|| analysisBatch.length() == 0 ? getDefaultAnalysisBatch()
				: analysisBatch);
		// look for the ctakes DocumentID anno
		if (setUimaDocId(jcas, doc,
				"org.apache.ctakes.typesystem.type.structured.DocumentID",
				"documentID") == null) {
			// look for the uima SourceDocumentInformation anno
			setUimaDocId(jcas, doc,
					"org.apache.uima.examples.SourceDocumentInformation", "uri");
		}
		// look for document
		if (bStoreCAS) {
			try {
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				GZIPOutputStream zipOut = new GZIPOutputStream(out);
				XmiCasSerializer ser = new XmiCasSerializer(
						jcas.getTypeSystem());
				XMLSerializer xmlSer = new XMLSerializer(zipOut, false);
				ser.serialize(jcas.getCas(), xmlSer.getContentHandler());
				zipOut.close();
				doc.setCas(out.toByteArray());
			} catch (Exception saxException) {
				log.error("error serializing document cas", saxException);
			}
		}
		return doc;
	}

	private void extractAndSaveDocKey(JCas jcas, Document doc) {
		AnnotationIndex<Annotation> idx = jcas
				.getAnnotationIndex(DocKey.typeIndexID);
		FSIterator<Annotation> annoIterator = idx.iterator();
		if (annoIterator.hasNext())
			this.saveDocKey(doc, (DocKey) annoIterator.next());
	}

	/**
	 * apply jxpath to object
	 * 
	 * @param jxpath
	 * @param child
	 * @return child if jxpath null, else apply jxpath
	 */
	private Object extractFeature(String jxpath, Object child) {
		return jxpath != null ? JXPathContext.newContext(child)
				.getValue(jxpath) : child;
	}

	/**
	 * covert a FSArray or FSList into a List<FeatureStructure>
	 * 
	 * @param fsc
	 * @return list, entries guaranteed not null
	 */
	private List<FeatureStructure> extractList(FeatureStructure fsc) {
		List<FeatureStructure> listFS = new ArrayList<FeatureStructure>();
		if (fsc != null) {
			if (fsc instanceof FSArray) {
				FSArray fsa = (FSArray) fsc;
				for (int i = 0; i < fsa.size(); i++) {
					FeatureStructure fsElement = fsa.get(i);
					if (fsElement != null)
						listFS.add(fsElement);
				}
			} else if (fsc instanceof FSList) {
				FSList fsl = (FSList) fsc;
				while (fsl instanceof NonEmptyFSList) {
					FeatureStructure fsElement = ((NonEmptyFSList) fsl)
							.getHead();
					if (fsElement != null)
						listFS.add(fsElement);
					fsl = ((NonEmptyFSList) fsl).getTail();
				}
			}
		}
		return listFS;
	}

	public Set<AnnoMappingInfo> getAnnoMappingInfos() {
		return annoMappingInfos;
	}

	public int getBatchSize() {
		return batchSize;
	}

	public DataSource getDataSource() {
		return jdbcTemplate.getDataSource();
	}

	public String getDbSchema() {
		return dbSchema;
	}

	public String getDbType() {
		return dbType;
	}

	private String getDefaultAnalysisBatch() {
		return tlAnalysisBatchDateFormat.get().format(new Date());
	}

	public String getDialectClassName() {
		return dialectClassName;
	}

	public Map<String, AnnoMappingInfo> getMapAnnoMappingInfo() {
		return mapAnnoMappingInfo;
	}

	private AnnoMappingInfo getMapInfo(FeatureStructure fs) {
		Type type = fs.getType();
		String className = type.getName();
		// if the key is there, then return it (may be null)
		AnnoMappingInfo mapInfo = null;
		if (this.tl_mapAnnoMappingInfo.get().containsKey(className)) {
			mapInfo = this.tl_mapAnnoMappingInfo.get().get(className);
		} else {
			// load the mappinginfo, save in cache
			mapInfo = initMapInfo(fs);
			this.tl_mapAnnoMappingInfo.get().put(className, mapInfo);
		}
		return mapInfo;
	}

	public SessionFactory getSessionFactory() {
		return sessionFactory;
	}

	private String getTablePrefix() {
		String tablePrefix = "";
		if ("mssql".equals(dbType)) {
			tablePrefix = dbSchema + ".";
		}
		return tablePrefix;
	}

	public PlatformTransactionManager getTransactionManager() {
		return transactionManager;
	}

	public Properties getYtexProperties() {
		return ytexProperties;
	}

	public void initDocKeyMapping() {
		AbstractEntityPersister cm = (AbstractEntityPersister) this.sessionFactory
				.getClassMetadata(Document.class);
		// figure out which columns are already mapped
		String[] propNames = cm.getPropertyNames();
		Set<String> mappedCols = new TreeSet<String>(
				String.CASE_INSENSITIVE_ORDER);
		for (String prop : propNames) {
			String cols[] = cm.getPropertyColumnNames(prop);
			mappedCols.addAll(Arrays.asList(cols));
		}
		// this.formattedTableName = DBUtil.formatTableName(cm.getTableName());
		this.formattedTableName = cm.getTableName();
		log.info("document table name = " + formattedTableName);
		final String query = "select * from " + formattedTableName
				+ " where 1=2";
		Connection conn = null;
		Statement stmt = null;
		ResultSet rs = null;
		try {
			conn = dataSource.getConnection();
			stmt = conn.createStatement();
			rs = stmt.executeQuery(query);
			ResultSetMetaData rsmd = rs.getMetaData();
			int nCols = rsmd.getColumnCount();
			for (int i = 1; i <= nCols; i++) {
				String colName = rsmd.getColumnName(i);
				if (!mappedCols.contains(colName)) {
					log.info("document candidate foreign key column: "
							+ colName);
					docTableCols.put(colName, rsmd.getColumnType(i));
				}
			}
			if (log.isDebugEnabled()) {
				log.debug("docTableCols: " + docTableCols);
			}
		} catch (SQLException e) {
			log.error("problem determining document table fields", e);
			throw new RuntimeException(e);
		} finally {
			try {
				if (rs != null)
					rs.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
			try {
				if (stmt != null)
					stmt.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
			try {
				if (conn != null)
					conn.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}

		}
	}

	/**
	 * load mapping info
	 * 
	 * @param type
	 * @return
	 */
	private AnnoMappingInfo initMapInfo(final FeatureStructure fs) {
		final Type type = fs.getType();
		final String annoName = type.getShortName().toLowerCase();
		AnnoMappingInfo mapInfoTmp;
		final UimaType ut = uimaTypeMap.get(type.getName());
		if (this.mapAnnoMappingInfo.containsKey(type.getName())) {
			mapInfoTmp = this.mapAnnoMappingInfo.get(type.getName()).deepCopy();
		} else {
			mapInfoTmp = new AnnoMappingInfo();
		}
		final AnnoMappingInfo mapInfo = mapInfoTmp;
		if (ut != null)
			mapInfo.setUimaTypeId(ut.getUimaTypeID());
		// first see if the table name has been set in beans-uima.xml
		if (Strings.isNullOrEmpty(mapInfo.getTableName())) {
			// next see if the table name has been set in ref_uima_type
			if (ut != null && !Strings.isNullOrEmpty(ut.getTableName()))
				mapInfo.setTableName(ut.getTableName());
			else
				// default to anno_[short name]
				mapInfo.setTableName("anno_" + annoName);
		}
		final List<Feature> features = type.getFeatures();
		// get the non primitive fields
		for (Feature f : features) {
			if (f.getRange().isArray()
					&& !f.getRange().getComponentType().isPrimitive()) {
				// add this field to the list of fields to store
				this.tl_mapFieldInfo.get()
						.put(type.getName(), f.getShortName());
			}
		}
		this.sessionFactory.getCurrentSession().doWork(new Work() {
			@Override
			public void execute(Connection conn) throws SQLException {
				ResultSet rs = null;

				try {
					DatabaseMetaData dmd = conn.getMetaData();
					// get columns for corresponding table
					// mssql - add schema prefix
					// oracle - convert table name to upper case
					rs = dmd.getColumns(
							null,
							"mssql".equals(dbType) || "hsql".equals(dbType) ? dbSchema
									: null,
							"orcl".equals(dbType) || "hsql".equals(dbType) ? mapInfo
									.getTableName().toUpperCase() : mapInfo
									.getTableName(), null);
					while (rs.next()) {
						String colName = rs.getString("COLUMN_NAME");
						int colSize = rs.getInt("COLUMN_SIZE");
						int dataType = rs.getInt("DATA_TYPE");
						if ("anno_base_id".equalsIgnoreCase(colName)) {
							// skip anno_base_id
							continue;
						}
						if ("uima_type_id".equalsIgnoreCase(colName)) {
							// see if there is a uima_type_id column
							// for FeatureStructures that are not annotations
							// there can be a field for the uima_type_id
							if (!(fs instanceof Annotation)
									&& Strings.isNullOrEmpty(mapInfo
											.getUimaTypeIdColumnName())) {
								mapInfo.setUimaTypeIdColumnName(colName);
							}
						} else if ("coveredText".equalsIgnoreCase(colName)) {
							// see if there is a coveredText column, store the
							// covered
							// text here
							ColumnMappingInfo coveredTextColumn = new ColumnMappingInfo();
							coveredTextColumn.setColumnName(colName);
							mapInfo.setCoveredTextColumn(coveredTextColumn);
							coveredTextColumn.setSize(colSize);
						} else {
							// possibility 1: the column is already mapped to
							// the field
							// if so, then just set the size
							if (!updateSize(mapInfo, colName, colSize, dataType)) {
								// possibility 2: the column is not mapped - see
								// if
								// it matches a field
								// iterate through features, see which match the
								// column
								for (Feature f : features) {
									String annoFieldName = f.getShortName();
									if (f.getRange().isPrimitive()
											&& annoFieldName
													.equalsIgnoreCase(colName)) {
										// primitive attribute
										ColumnMappingInfo fmap = new ColumnMappingInfo();
										fmap.setAnnoFieldName(annoFieldName);
										fmap.setColumnName(colName);
										fmap.setSize(colSize);
										fmap.setSqlType(dataType);
										mapInfo.getMapField()
												.put(colName, fmap);
										break;
									} else if (!f.getRange().isArray()
											&& !f.getRange().isPrimitive()
											&& annoFieldName
													.equalsIgnoreCase(colName)
											&& (dataType == Types.INTEGER
													|| dataType == Types.SMALLINT
													|| dataType == Types.BIGINT
													|| dataType == Types.NUMERIC
													|| dataType == Types.FLOAT || dataType == Types.DOUBLE)) {
										// this feature is a reference to
										// another
										// annotation.
										// this column is numeric - a match
										ColumnMappingInfo fmap = new ColumnMappingInfo();
										fmap.setAnnoFieldName(annoFieldName);
										fmap.setColumnName(colName);
										fmap.setSize(colSize);
										fmap.setSqlType(dataType);
										mapInfo.getMapField()
												.put(colName, fmap);
										break;
									}
								}
							}
						}
					}
				} finally {
					if (rs != null) {
						try {
							rs.close();
						} catch (SQLException e) {
						}
					}
				}
			}
		});
		// don't map this annotation if no fields match columns
		if (mapInfo.getMapField().size() == 0
				&& mapInfo.getCoveredTextColumn() == null
				&& Strings.isNullOrEmpty(mapInfo.getUimaTypeIdColumnName()))
			return null;
		// generate sql
		StringBuilder b = new StringBuilder("insert into ");
		b.append(this.getTablePrefix()).append(mapInfo.getTableName());
		b.append("(anno_base_id");
		// add coveredText column if available
		if (mapInfo.getCoveredTextColumn() != null) {
			b.append(", coveredText");
		}
		// add uima_type_id column if available
		if (mapInfo.getUimaTypeIdColumnName() != null) {
			b.append(", uima_type_id");
		}
		// add other fields
		for (Map.Entry<String, ColumnMappingInfo> fieldEntry : mapInfo
				.getMapField().entrySet()) {
			b.append(", ").append(dialect.openQuote())
					.append(fieldEntry.getValue().getColumnName())
					.append(dialect.closeQuote());
		}
		b.append(") values (?");
		// add coveredText bind param
		if (mapInfo.getCoveredTextColumn() != null) {
			b.append(", ?");
		}
		// add uimaTypeId bind param
		if (mapInfo.getUimaTypeIdColumnName() != null) {
			b.append(", ?");
		}
		// add bind params for other fields
		b.append(Strings.repeat(", ?", mapInfo.getMapField().size())).append(
				")");
		mapInfo.setSql(b.toString());
		if (log.isInfoEnabled())
			log.info("sql insert for type " + type.getName() + ": "
					+ mapInfo.getSql());
		if (log.isDebugEnabled())
			log.debug("initMapInfo(" + annoName + "): " + mapInfo);
		return mapInfo;
	}

	/**
	 * insert annotation containment links.
	 * 
	 * @param documentId
	 */
	private void insertAnnotationContainmentLinks(int documentId) {
		if (log.isTraceEnabled())
			log.trace("begin insertAnnotationContainmentLinks");
		Query q = sessionFactory.getCurrentSession().getNamedQuery(
				"insertAnnotationContainmentLinks");
		q.setInteger("documentID", documentId);
		q.executeUpdate();
		if (log.isTraceEnabled())
			log.trace("end insertAnnotationContainmentLinks");
	}

	private BiMap<Annotation, Integer> saveAnnoBase(final JCas jcas,
			final Set<String> setTypesToIgnore, final int docId) {
		final AnnotationIndex<Annotation> annoIdx = jcas
				.getAnnotationIndex(Annotation.typeIndexID);
		final List<Annotation> listAnno = new ArrayList<Annotation>(
				annoIdx.size());
		final BiMap<Annotation, Integer> mapAnnoToId = HashBiMap.create();
		final FSIterator<Annotation> annoIterator = annoIdx.iterator();
		this.sessionFactory.getCurrentSession().doWork(new Work() {

			@Override
			public void execute(Connection conn) throws SQLException {
				PreparedStatement ps = null;
				ResultSet rs = null;
				try {
					ps = conn
							.prepareStatement(
									"insert into "
											+ getTablePrefix()
											+ "anno_base (document_id, span_begin, span_end, uima_type_id) values (?, ?, ?, ?)",
									Statement.RETURN_GENERATED_KEYS);
					while (annoIterator.hasNext()) {
						Annotation anno = (Annotation) annoIterator.next();
						String annoClass = anno.getClass().getName();
						if (!setTypesToIgnore.contains(annoClass)
								&& uimaTypeMap.containsKey(annoClass)) {
							// should not ignore, and we know how to map this
							// annotation
							listAnno.add(anno);
							ps.setInt(1, docId);
							ps.setInt(2, anno.getBegin());
							ps.setInt(3, anno.getEnd());
							ps.setInt(4, uimaTypeMap.get(annoClass)
									.getUimaTypeID());
							ps.addBatch();
						}
					}
					ps.executeBatch();
					rs = ps.getGeneratedKeys();
					int annoIndex = 0;
					while (rs.next()) {
						mapAnnoToId.put(listAnno.get(annoIndex), rs.getInt(1));
						annoIndex++;
					}
				} catch (SQLException e) {
					throw new RuntimeException(e);
				} finally {
					if (rs != null) {
						try {
							rs.close();
						} catch (SQLException e) {
						}
					}
					if (ps != null) {
						try {
							ps.close();
						} catch (SQLException e) {
						}
					}
				}
			}
		});
		return mapAnnoToId;
	}

	private BiMap<Annotation, Integer> saveAnnoBaseHib(JCas jcas,
			Set<String> setTypesToIgnore, Document doc) {
		if (log.isTraceEnabled())
			log.trace("begin saveAnnoBaseHib");
		AnnotationIndex<Annotation> annoIdx = jcas
				.getAnnotationIndex(Annotation.typeIndexID);
		List<Annotation> listAnno = new ArrayList<Annotation>(annoIdx.size());
		Map<Annotation, DocumentAnnotation> mapAnnoToHib = new HashMap<Annotation, DocumentAnnotation>();
		FSIterator<Annotation> annoIterator = annoIdx.iterator();
		int count = 0;
		// iterate over annotations and save them
		while (annoIterator.hasNext()) {
			Annotation anno = (Annotation) annoIterator.next();
			String annoClass = anno.getClass().getName();
			if (!setTypesToIgnore.contains(annoClass)
					&& this.uimaTypeMap.containsKey(annoClass)) {
				// should not ignore, and we know how to map this annotation
				listAnno.add(anno);
				DocumentAnnotation hibAnno = new DocumentAnnotation();
				hibAnno.setDocument(doc);
				hibAnno.setBegin(anno.getBegin());
				hibAnno.setEnd(anno.getEnd());
				hibAnno.setUimaType(uimaTypeMap.get(annoClass));
				sessionFactory.getCurrentSession().save(hibAnno);
				if (++count % batchSize == 0)
					sessionFactory.getCurrentSession().flush();
				doc.getDocumentAnnotations().add(hibAnno);
				mapAnnoToHib.put(anno, hibAnno);
			}
		}
		sessionFactory.getCurrentSession().flush();
		BiMap<Annotation, Integer> mapAnnoToId = HashBiMap.create();
		for (Map.Entry<Annotation, DocumentAnnotation> e : mapAnnoToHib
				.entrySet()) {
			mapAnnoToId.put(e.getKey(), e.getValue().getDocumentAnnotationID());
		}
		if (log.isTraceEnabled())
			log.trace("end saveAnnoBaseHib");
		return mapAnnoToId;
	}

	/**
	 * bind the variables to the prepared statement
	 * 
	 * @param type
	 * @param mapInfo
	 * @param ps
	 * @param annoId
	 * @param anno
	 * @throws SQLException
	 */
	private void saveAnnoBindVariables(final Type type,
			final AnnoMappingInfo mapInfo, PreparedStatement ps, int annoId,
			FeatureStructure anno, final BiMap<Annotation, Integer> mapAnnoToId)
			throws SQLException {
		// set anno_base_id
		int argIdx = 1;
		ps.setInt(argIdx++, annoId);
		if (mapInfo.getCoveredTextColumn() != null) {
			String trunc = null;
			if (anno instanceof Annotation) {
				trunc = truncateString(((Annotation) anno).getCoveredText(),
						mapInfo.getCoveredTextColumn().getSize());
			}
			ps.setString(argIdx++, trunc);
		}
		if (!Strings.isNullOrEmpty(mapInfo.getUimaTypeIdColumnName())) {
			ps.setInt(argIdx++, mapInfo.getUimaTypeId());
		}
		// iterate over fields
		for (Map.Entry<String, ColumnMappingInfo> fieldEntry : mapInfo
				.getMapField().entrySet()) {
			ColumnMappingInfo fieldMapInfo = fieldEntry.getValue();
			String fieldName = fieldMapInfo.getAnnoFieldName();
			Feature feat = type.getFeatureByBaseName(fieldName);
			if (fieldMapInfo.getConverter() != null) {
				try {
					String prop = anno.getFeatureValueAsString(feat);
					ps.setObject(
							argIdx,
							fieldMapInfo.getConverter().convert(
									fieldMapInfo.getTargetType(), prop));
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			} else if (!feat.getRange().isPrimitive()) {
				// feature is a structure/annotation
				FeatureStructure fs = anno.getFeatureValue(feat);
				if (fs == null) {
					// feature is null - set the column to null
					ps.setNull(argIdx, fieldMapInfo.getSqlType());
				} else {
					if (fieldMapInfo.getJxpath() != null) {
						// jxpath to pull out feature attribute
						Object o = this.extractFeature(
								fieldMapInfo.getJxpath(), fs);
						if (o == null) {
							// extracted value null - set column to null
							ps.setNull(argIdx, fieldMapInfo.getSqlType());
						} else if (o instanceof String) {
							// string - truncate as needed
							String trunc = truncateString((String) o,
									fieldMapInfo.getSize());
							ps.setString(argIdx, trunc);
						} else {
							// set value
							ps.setObject(argIdx, o);
						}
					} else {
						// reference to another annotation - get the other
						// anno's id
						Integer refAnnoId = null;
						if (fs instanceof Annotation) {
							refAnnoId = mapAnnoToId.get(fs);
						}
						if (refAnnoId != null) {
							ps.setInt(argIdx, refAnnoId);
						} else {
							ps.setNull(argIdx, Types.INTEGER);
						}
					}
				}
			} else {
				if ("uima.cas.Integer".equals(feat.getRange().getName())) {
					ps.setInt(argIdx, anno.getIntValue(feat));
				} else if ("uima.cas.Short".equals(feat.getRange().getName())) {
					ps.setShort(argIdx, anno.getShortValue(feat));
				} else if ("uima.cas.Long".equals(feat.getRange().getName())) {
					ps.setLong(argIdx, anno.getLongValue(feat));
				} else if ("uima.cas.Float".equals(feat.getRange().getName())) {
					ps.setFloat(argIdx, anno.getFloatValue(feat));
				} else if ("uima.cas.Double".equals(feat.getRange().getName())) {
					ps.setDouble(argIdx, anno.getDoubleValue(feat));
				} else if ("uima.cas.Byte".equals(feat.getRange().getName())) {
					ps.setByte(argIdx, anno.getByteValue(feat));
				} else if ("uima.cas.Boolean".equals(feat.getRange().getName())) {
					ps.setBoolean(argIdx, anno.getBooleanValue(feat));
				} else if ("uima.cas.String".equals(feat.getRange().getName())) {
					String trunc = truncateString(anno.getStringValue(feat),
							fieldMapInfo.getSize());
					ps.setString(argIdx, trunc);
				}
			}
			argIdx++;
		}
	}

	/**
	 * insert composite attributes.
	 * 
	 * @param listFSA
	 */
	private void saveAnnoFS(final List<AnnoFSAttribute> listFSA,
			final BiMap<Annotation, Integer> mapAnnoToId) {
		if (listFSA.size() == 0)
			return;
		FeatureStructure fs = listFSA.get(0).getFs();
		final Type type = fs.getType();
		final AnnoMappingInfo mapInfo = this.getMapInfo(fs);
		// don't know how to map this feature
		if (mapInfo == null)
			return;
		// int chunks = (int) Math.ceil((double) listFSA.size()
		// / (double) this.batchSize);
		// for (int i = 0; i < chunks; i++) {
		// int start = i * this.batchSize;
		// int end = (i + 1) * this.batchSize;
		// if (end > listFSA.size())
		// end = listFSA.size();
		// final List<AnnoFSAttribute> chunkList = listFSA.subList(start, end);
		// jdbcTemplate.batchUpdate(mapInfo.getSql(),
		// new BatchPreparedStatementSetter() {
		//
		// @Override
		// public int getBatchSize() {
		// return chunkList.size();
		// }
		//
		// @Override
		// public void setValues(PreparedStatement ps, int idx)
		// throws SQLException {
		// AnnoFSAttribute fsa = chunkList.get(idx);
		// // todo pass array index for storage
		// saveAnnoBindVariables(type, mapInfo, ps,
		// fsa.getAnnoBaseId(), fsa.getFs(),
		// mapAnnoToId);
		// }
		// });
		// }
		chunkedBatchUpdate(mapInfo.getSql(), listFSA,
				new ChunkPreparedStatementSetter<AnnoFSAttribute>() {

					@Override
					public void setValues(PreparedStatement ps, int idx,
							AnnoFSAttribute fsa) throws SQLException {
						// todo pass array index for storage
						saveAnnoBindVariables(type, mapInfo, ps,
								fsa.getAnnoBaseId(), fsa.getFs(), mapAnnoToId);
					}
				});
	}

	/**
	 * @see #chunkedBatchUpdate
	 * @author vijay
	 * 
	 * @param <T>
	 */
	public static interface ChunkPreparedStatementSetter<T> {
		public abstract void setValues(PreparedStatement ps, int idx, T record)
				throws SQLException;
	}

	/**
	 * for the list l, perform l.size()/batchSize batch updates. Avoid mysql
	 * packet too large exceptions with large batch updates. Call spring
	 * jdbcTemplate.batchUpdate internally with sublists of l with size
	 * batchSize.
	 * 
	 * @param sql
	 * @param l
	 * @param cpss
	 */
	private <T> void chunkedBatchUpdate(String sql, List<T> l,
			final ChunkPreparedStatementSetter<T> cpss) {
		int chunks = (int) Math.ceil((double) l.size()
				/ (double) this.batchSize);
		for (int i = 0; i < chunks; i++) {
			int start = i * this.batchSize;
			int end = (i + 1) * this.batchSize;
			if (end > l.size())
				end = l.size();
			final List<T> chunkList = l.subList(start, end);
			jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {

				@Override
				public int getBatchSize() {
					return chunkList.size();
				}

				@Override
				public void setValues(PreparedStatement ps, int idx)
						throws SQLException {
					T record = chunkList.get(idx);
					cpss.setValues(ps, idx, record);
				}
			});
		}
	}

	/**
	 * save annotation to annotation links (many-to-many relationships)
	 * 
	 * @param listAnnoLinks
	 */
	private void saveAnnoLinks(final List<AnnoLink> listAnnoLinks) {
		if (log.isTraceEnabled())
			log.trace("begin saveAnnoLinks");
		// jdbcTemplate
		// .batchUpdate(
		// "insert into "
		// + this.getTablePrefix()
		// +
		// "anno_link(parent_anno_base_id, child_anno_base_id, feature) values (?, ?, ?)",
		// new BatchPreparedStatementSetter() {
		//
		// @Override
		// public int getBatchSize() {
		// return listAnnoLinks.size();
		// }
		//
		// @Override
		// public void setValues(PreparedStatement ps, int idx)
		// throws SQLException {
		// AnnoLink l = listAnnoLinks.get(idx);
		// ps.setInt(1, l.getParentAnnoBaseId());
		// ps.setInt(2, l.getChildAnnoBaseId());
		// ps.setString(3, l.getFeature());
		// }
		// });
		chunkedBatchUpdate(
				"insert into "
						+ this.getTablePrefix()
						+ "anno_link(parent_anno_base_id, child_anno_base_id, feature) values (?, ?, ?)",
				listAnnoLinks, new ChunkPreparedStatementSetter<AnnoLink>() {

					@Override
					public void setValues(PreparedStatement ps, int idx,
							AnnoLink l) throws SQLException {
						ps.setInt(1, l.getParentAnnoBaseId());
						ps.setInt(2, l.getChildAnnoBaseId());
						ps.setString(3, l.getFeature());
					}
				});
		if (log.isTraceEnabled())
			log.trace("end saveAnnoLinks");
	}

	/**
	 * save the annotation properties for a given type
	 * 
	 * @param mapIdToAnno
	 *            map of all annoIDs to Annotation
	 * @param annoIds
	 *            annotation ids for a single type
	 * @param listAnnoLinks
	 *            annotation to annotation links to save
	 */
	private void saveAnnoPrimitive(
			final BiMap<Annotation, Integer> mapAnnoToId,
			final Set<Integer> annoIds, final List<AnnoLink> listAnnoLinks) {
		if (log.isTraceEnabled())
			log.trace("begin saveAnnoPrimitive");
		final BiMap<Integer, Annotation> mapIdToAnno = mapAnnoToId.inverse();
		// nothing to do
		if (annoIds.size() == 0)
			return;
		// covert to array for spring batch update
		// final Integer[] annoIdArray = annoIds.toArray(new Integer[] {});
		final List<Integer> annoIdList = new ArrayList<Integer>(annoIds);
		// get mappinginfo
		// final TOP t = mapIdToAnno.get(annoIdArray[0]);
		final TOP t = mapIdToAnno.get(annoIdList.get(0));
		final Type type = t.getType();
		final AnnoMappingInfo mapInfo = this.getMapInfo(t);
		// get non primitive fields, insert them after inserting the annotation
		final Set<String> fsNames = this.tl_mapFieldInfo.get().get(
				type.getName());
		final ListMultimap<String, AnnoFSAttribute> mapAnnoToFS = ArrayListMultimap
				.create();
		// don't know how to map this annotation
		if (mapInfo == null)
			return;
		// jdbcTemplate.batchUpdate(mapInfo.getSql(),
		// new BatchPreparedStatementSetter() {
		//
		// @Override
		// public int getBatchSize() {
		// return annoIdArray.length;
		// }
		this.chunkedBatchUpdate(mapInfo.getSql(), annoIdList,
				new ChunkPreparedStatementSetter<Integer>() {

					@Override
					public void setValues(PreparedStatement ps, int idx,
							Integer annoId) throws SQLException {
						// get the entry
						// int annoId = annoIdArray[idx];
						Annotation anno = mapIdToAnno.get(annoId);
						saveAnnoBindVariables(type, mapInfo, ps, annoId, anno,
								mapAnnoToId);
						// pull out the composite fields for storage
						for (String fieldName : fsNames) {
							Feature feat = type.getFeatureByBaseName(fieldName);
							if (!feat.getRange().isPrimitive()) {
								// handle arrays and lists
								FeatureStructure fsCol = anno
										.getFeatureValue(feat);
								if (fsCol != null
										&& (fsCol instanceof FSArray || fsCol instanceof FSList)) {
									List<FeatureStructure> fsList = extractList(fsCol);
									int i = 0;
									for (FeatureStructure fs : fsList) {
										if (fs instanceof Annotation) {
											// annotations are linked via the
											// anno_link table
											Integer childAnnoId = mapAnnoToId
													.get(fs);
											if (childAnnoId != null) {
												listAnnoLinks.add(new AnnoLink(
														annoId, childAnnoId,
														feat.getShortName()));
											}
										} else {
											// featureStructs that are not
											// annotations get stored in their
											// own tables
											// with a many to one relationship
											// to the annotation
											mapAnnoToFS.put(fs.getType()
													.getName(),
													new AnnoFSAttribute(annoId,
															fs, i++));
										}
									}
								}
							} else {
								// handle primitive attributes
								mapAnnoToFS.put(
										feat.getRange().getName(),
										new AnnoFSAttribute(annoId, anno
												.getFeatureValue(feat), null));
							}
						}
					}
				}

		);
		for (String fsType : mapAnnoToFS.keySet()) {
			this.saveAnnoFS(mapAnnoToFS.get(fsType), mapAnnoToId);
		}
		if (log.isTraceEnabled())
			log.trace("end saveAnnoPrimitive");
	}

	// private void saveAnnotations(JCas jcas, Set<String> setTypesToIgnore,
	// int documentId) {
	// BiMap<Annotation, Integer> mapAnnoToId = saveAnnoBase(jcas,
	// setTypesToIgnore, documentId);
	// // split the annotations up by type
	// // create a map of class name to anno id
	// SetMultimap<String, Integer> mapTypeToAnnoId = HashMultimap.create();
	// for (Map.Entry<Annotation, Integer> annoEntry : mapAnnoToId.entrySet()) {
	// mapTypeToAnnoId.put(annoEntry.getKey().getClass().getName(),
	// annoEntry.getValue());
	// }
	// // allocate a list to store annotation links
	// List<AnnoLink> listAnnoLinks = new ArrayList<AnnoLink>();
	// // save annotation properties
	// for (String annoClass : mapTypeToAnnoId.keySet()) {
	// saveAnnoPrimitive(mapAnnoToId, mapTypeToAnnoId.get(annoClass),
	// listAnnoLinks);
	// }
	// addAnnoLinks(jcas, mapAnnoToId, listAnnoLinks);
	// // saveMarkablePairs(jcas, mapAnnoToId, listAnnoLinks);
	// // saveCoref(jcas, mapAnnoToId, listAnnoLinks);
	// saveAnnoLinks(listAnnoLinks);
	// }

	private void saveAnnotationsHib(JCas jcas,
			boolean bInsertAnnotationContainmentLinks,
			Set<String> setTypesToIgnore, Document doc) {
		if (log.isTraceEnabled())
			log.trace("begin saveAnnotationsHib");
		BiMap<Annotation, Integer> mapAnnoToId = saveAnnoBaseHib(jcas,
				setTypesToIgnore, doc);
		if (bInsertAnnotationContainmentLinks)
			insertAnnotationContainmentLinks(doc.getDocumentID());
		// split the annotations up by type
		// create a map of class name to anno id
		SetMultimap<String, Integer> mapTypeToAnnoId = HashMultimap.create();
		for (Map.Entry<Annotation, Integer> annoEntry : mapAnnoToId.entrySet()) {
			mapTypeToAnnoId.put(annoEntry.getKey().getClass().getName(),
					annoEntry.getValue());
		}
		// allocate a list to store annotation links
		List<AnnoLink> listAnnoLinks = new ArrayList<AnnoLink>();
		// save annotation properties
		for (String annoClass : mapTypeToAnnoId.keySet()) {
			saveAnnoPrimitive(mapAnnoToId, mapTypeToAnnoId.get(annoClass),
					listAnnoLinks);
		}
		addAnnoLinks(jcas, mapAnnoToId, listAnnoLinks);
		// saveMarkablePairs(jcas, mapAnnoToId, listAnnoLinks);
		// saveCoref(jcas, mapAnnoToId, listAnnoLinks);
		saveAnnoLinks(listAnnoLinks);
		if (log.isTraceEnabled())
			log.trace("end saveAnnotationsHib");
	}

	/**
	 * update the document table - set key values from dockey for the give
	 * document_id
	 * 
	 * @param document
	 *            document
	 * @param dk
	 *            key
	 */
	private void saveDocKey(Document document, DocKey dk) {
		int documentId = document.getDocumentID();
		FSArray fsa = dk.getKeyValuePairs();
		if (fsa == null || fsa.size() == 0)
			return;
		// build query dynamically
		StringBuilder queryBuilder = (new StringBuilder("update ")).append(
				formattedTableName).append(" set ");
		List<Object> args = new ArrayList<Object>();
		boolean bFirstArg = true;
		// iterate over key/value pairs
		for (int i = 0; i < fsa.size(); i++) {
			KeyValuePair kp = (KeyValuePair) fsa.get(i);
			String key = kp.getKey();
			if (key.equalsIgnoreCase("instance_id")) {
				// instance_id is something we 'know' about - set it
				document.setInstanceID(kp.getValueLong());
			} else if (key.equalsIgnoreCase("instance_key")) {
				document.setInstanceKey(kp.getValueString());
			} else if (this.docTableCols.containsKey(key)) {
				// only attempt to map keys that correspond to valid columns
				boolean badArg = false;
				// verify that the value matches the datatype
				// if valueString not null then assume integer
				if (kp.getValueString() != null
						&& stringTypes.contains(docTableCols.get(key))) {
					args.add(kp.getValueString());
				} else if (numericTypes.contains(docTableCols.get(key))) {
					args.add(kp.getValueLong());
				} else {
					// invalid type for argument
					badArg = true;
					log.warn("document_id: " + documentId
							+ ", bad type for key=" + key + ", value="
							+ kp.getValueString() == null ? kp.getValueLong()
							: kp.getValueString());
				}
				if (!badArg) {
					// update
					if (!bFirstArg) {
						queryBuilder.append(", ");
					}
					queryBuilder.append(DBUtil.formatFieldName(key));
					queryBuilder.append("=? ");
					bFirstArg = false;
				}
			} else {
				// don't know what to do with this key attribute
				log.warn("document_id: " + documentId
						+ ", could not map key attribute " + kp.getKey());
			}
		}
		if (args.size() > 0) {
			// have something to update - add the where condition
			queryBuilder.append(" where document_id = ?");
			args.add(documentId);
			String sql = queryBuilder.toString();
			if (log.isDebugEnabled()) {
				log.debug(sql);
			}
			jdbcTemplate.update(sql, args.toArray());
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * ytex.dao.mapper.DocumentMapperService#saveDocument(org.apache.uima.jcas
	 * .JCas, java.lang.String)
	 */
	public Integer saveDocument(final JCas jcas, final String analysisBatch,
			final boolean bStoreDocText, final boolean bStoreCAS,
			final boolean bInsertAnnotationContainmentLinks,
			final Set<String> setTypesToIgnore) {
		if (log.isTraceEnabled())
			log.trace("begin saveDocument");
		// communicate options to mappers using thread local variable
		final DefaultTransactionDefinition txDef = new DefaultTransactionDefinition(
				TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		txDef.setIsolationLevel("orcl".equals(this.dbType) ? TransactionDefinition.ISOLATION_READ_COMMITTED
				: TransactionDefinition.ISOLATION_READ_UNCOMMITTED);
		final TransactionTemplate txTemplate = new TransactionTemplate(
				this.getTransactionManager(), txDef);
		final int documentId = txTemplate
				.execute(new TransactionCallback<Integer>() {

					@Override
					public Integer doInTransaction(TransactionStatus arg0) {
						Document doc = createDocument(jcas, analysisBatch,
								bStoreDocText, bStoreCAS);
						sessionFactory.getCurrentSession().save(doc);
						// make sure the document has been saved
						getSessionFactory().getCurrentSession().flush();
						saveAnnotationsHib(jcas,
								bInsertAnnotationContainmentLinks,
								setTypesToIgnore, doc);
						extractAndSaveDocKey(jcas, doc);
						return doc.getDocumentID();
					}
				});
		if (log.isTraceEnabled())
			log.trace("end saveDocument");
		return documentId;
	}

	/**
	 * initialize mapAnnoMappingInfo from the set
	 * 
	 * @param annoMappingInfos
	 */
	public void setAnnoMappingInfos(Set<AnnoMappingInfo> annoMappingInfos) {
		this.annoMappingInfos = annoMappingInfos;
		for (AnnoMappingInfo mi : annoMappingInfos) {
			this.mapAnnoMappingInfo.put(mi.getAnnoClassName(), mi);
		}
	}

	public void setBatchSize(int batchSize) {
		this.batchSize = batchSize;
	}

	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
		jdbcTemplate = new JdbcTemplate(dataSource);
	}

	public void setDbSchema(String dbSchema) {
		this.dbSchema = dbSchema;
	}

	public void setDbType(String dbType) {
		this.dbType = dbType;
	}

	public void setDialectClassName(String dialectClassName) {
		this.dialectClassName = dialectClassName;
		try {
			this.dialect = (Dialect) Class.forName(dialectClassName)
					.newInstance();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public void setMapAnnoMappingInfo(
			Map<String, AnnoMappingInfo> mapAnnoMappingInfo) {
		this.mapAnnoMappingInfo = mapAnnoMappingInfo;
	}

	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	public void setTransactionManager(
			PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	/**
	 * get the document id from the specified type and feature.
	 * 
	 * @param jcas
	 * @param doc
	 * @param idType
	 * @param idFeature
	 * @return docId if found, else null
	 */
	private String setUimaDocId(JCas jcas, Document doc, String idType,
			String idFeature) {
		Type docIDtype = jcas.getTypeSystem().getType(idType);
		Feature docIDFeature = null;
		if (docIDtype != null)
			docIDFeature = docIDtype.getFeatureByBaseName(idFeature);
		if (docIDtype != null && docIDFeature != null) {
			// AnnotationIndex<Annotation> idx = jcas
			// .getAnnotationIndex(docIDtype);
			FSIterator<FeatureStructure> iter = jcas.getFSIndexRepository()
					.getAllIndexedFS(docIDtype);
			if (iter != null) {
				if (iter.hasNext()) {
					FeatureStructure docId = iter.next();
					String uimaDocId = docId.getStringValue(docIDFeature);
					if (!Strings.isNullOrEmpty(uimaDocId)) {
						uimaDocId = this.truncateString(uimaDocId, 256);
						doc.setInstanceKey(uimaDocId);
						return uimaDocId;
					}
				}
			}
		}
		return null;
	}

	public void setYtexProperties(Properties ytexProperties) {
		this.ytexProperties = ytexProperties;
	}

	private String truncateString(String val, int size) {
		String trunc = val;
		if (!Strings.isNullOrEmpty(val) && val.length() > size) {
			trunc = val.substring(0, size);
		}
		return trunc;
	}

	/**
	 * update column size for given column, if the column has been mapped
	 * 
	 * @param mapInfo
	 * @param colName
	 * @param colSize
	 * @return true column is mapped to a field
	 */
	private boolean updateSize(AnnoMappingInfo mapInfo, String colName,
			int colSize, int sqlType) {
		ColumnMappingInfo fi = mapInfo.getMapField().get(colName);
		if (fi != null) {
			fi.setSqlType(sqlType);
			if (fi.getSize() <= 0)
				fi.setSize(colSize);
			return true;
		}
		return false;
	}

}
