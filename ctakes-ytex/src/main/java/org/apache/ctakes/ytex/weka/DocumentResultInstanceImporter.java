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
package org.apache.ctakes.ytex.weka;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.SessionFactory;

public class DocumentResultInstanceImporter implements
		WekaResultInstanceImporter {
	private SessionFactory sessionFactory;
	private static final Log log = LogFactory
			.getLog(DocumentResultInstanceImporter.class);

	public SessionFactory getSessionFactory() {
		return sessionFactory;
	}

	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	@Override
	public void importInstanceResult(Integer instanceNumber,
			List<String> instanceKey, String task, int classAuto,
			int classGold, List<Double> predictions) {
		// if (instanceKey.size() < 1) {
		try {
			int documentId = Integer.parseInt(instanceKey.get(0));
			if (documentId > 0) {
				// todo fix this
//				Document doc = (Document) this.getSessionFactory()
//						.getCurrentSession().get(Document.class, documentId);
//				if (doc != null) {
//					DocumentClass docClass = new DocumentClass();
//					docClass.setDocument(doc);
//					docClass.setClassAuto(classAuto);
//					docClass.setClassGold(classGold);
//					docClass.setTask(task);
//					this.getSessionFactory().getCurrentSession().save(docClass);
//				} else {
//					log.error("no document for id: " + documentId);
//				}
			} else {
				log.error("Invalid instance id: " + instanceKey
						+ ", instanceNumber: " + instanceNumber);
			}
		} catch (NumberFormatException nfe) {
			log.error("could not parse document id: " + instanceKey
					+ ", instanceNumber: " + instanceNumber, nfe);
		}
		// } else {
		// log.error("no attributes in key, instanceNumber: " + instanceNumber);
		// }
	}
}
