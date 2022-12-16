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
package org.apache.ctakes.ytex.kernel.dao;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ctakes.ytex.dao.DBUtil;
import org.apache.ctakes.ytex.kernel.model.KernelEvaluation;
import org.apache.ctakes.ytex.kernel.model.KernelEvaluationInstance;
import org.hibernate.Query;
import org.hibernate.SessionFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;


public class KernelEvaluationDaoImpl implements KernelEvaluationDao {
	private SessionFactory sessionFactory;
	private static final Log log = LogFactory
			.getLog(KernelEvaluationDaoImpl.class);
	private PlatformTransactionManager transactionManager;

	public PlatformTransactionManager getTransactionManager() {
		return transactionManager;
	}

	public void setTransactionManager(
			PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
		txTemplate = new TransactionTemplate(this.transactionManager);
		txTemplate
				.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);
	}

	private TransactionTemplate txTemplate;

	public SessionFactory getSessionFactory() {
		return sessionFactory;
	}

	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see dao.KernelEvaluationDao#storeNorm(java.lang.String, int, double)
	 */
	public void storeNorm(KernelEvaluation kernelEvaluation, long instanceId,
			double norm) {
		storeKernel(kernelEvaluation, instanceId, instanceId, norm);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see dao.KernelEvaluationDao#getNorm(java.lang.String, int)
	 */
	public Double getNorm(KernelEvaluation kernelEvaluation, long instanceId) {
		return getKernel(kernelEvaluation, instanceId, instanceId);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see dao.KernelEvaluationDao#storeKernel(java.lang.String, int, int,
	 * double)
	 */
	public void storeKernel(KernelEvaluation kernelEvaluation,
			long instanceId1, long instanceId2, double kernel) {
		long instanceId1s = instanceId1 <= instanceId2 ? instanceId1
				: instanceId2;
		long instanceId2s = instanceId1 <= instanceId2 ? instanceId2
				: instanceId1;
		// don't bother with the delete so we can batch insert the kernel eval
		// delete existing norm
		// if (getKernel(name, instanceId1, instanceId2) != null) {
		// Query q = this.getSessionFactory().getCurrentSession()
		// .getNamedQuery("deleteKernelEvaluation");
		// q.setInteger("kernelEvaluationId",
		// kernelEvaluation.getKernelEvaluationId());
		// q.setInteger("instanceId1", instanceId1s);
		// q.setInteger("instanceId2", instanceId2s);
		// q.executeUpdate();
		// if (log.isWarnEnabled())
		// log.warn("replacing kernel, instanceId1: " + instanceId1s
		// + ", instanceId2: " + instanceId2s + ", name: " + name);
		// }
		KernelEvaluationInstance g = new KernelEvaluationInstance(
				kernelEvaluation.getKernelEvaluationId(), instanceId1s,
				instanceId2s, kernel);
		this.getSessionFactory().getCurrentSession().save(g);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see dao.KernelEvaluationDao#getKernel(java.lang.String, int, int)
	 */
	public Double getKernel(KernelEvaluation kernelEvaluation,
			long instanceId1, long instanceId2) {
		long instanceId1s = instanceId1 <= instanceId2 ? instanceId1
				: instanceId2;
		long instanceId2s = instanceId1 <= instanceId2 ? instanceId2
				: instanceId1;
		Query q = this.getSessionFactory().getCurrentSession()
				.getNamedQuery("getKernelEvaluation");
		q.setCacheable(true);
		q.setInteger("kernelEvaluationId",
				kernelEvaluation.getKernelEvaluationId());
		q.setLong("instanceId1", instanceId1s);
		q.setLong("instanceId2", instanceId2s);
		KernelEvaluationInstance g = (KernelEvaluationInstance) q
				.uniqueResult();
		if (g != null) {
			return g.getSimilarity();
		} else {
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<KernelEvaluationInstance> getAllKernelEvaluationsForInstance(
			KernelEvaluation kernelEvaluation, long instanceId) {
		Query q = this.getSessionFactory().getCurrentSession()
				.getNamedQuery("getAllKernelEvaluationsForInstance1");
		q.setInteger("kernelEvaluationId",
				kernelEvaluation.getKernelEvaluationId());
		q.setLong("instanceId", instanceId);
		List<KernelEvaluationInstance> kevals = q.list();
		Query q2 = this.getSessionFactory().getCurrentSession()
				.getNamedQuery("getAllKernelEvaluationsForInstance2");
		q2.setInteger("kernelEvaluationId",
				kernelEvaluation.getKernelEvaluationId());
		q2.setLong("instanceId", instanceId);
		kevals.addAll(q2.list());
		return kevals;
	}

	@Override
	public KernelEvaluation storeKernelEval(
			final KernelEvaluation kernelEvaluation) {
		KernelEvaluation kEval = getKernelEval(
				kernelEvaluation.getCorpusName(),
				kernelEvaluation.getExperiment(), kernelEvaluation.getLabel(),
				kernelEvaluation.getFoldId(), kernelEvaluation.getParam1(),
				kernelEvaluation.getParam2());
		if (kEval == null) {
			txTemplate.execute(new TransactionCallback<Object>() {

				@Override
				public Object doInTransaction(TransactionStatus txStatus) {
					try {
						getSessionFactory().getCurrentSession().save(
								kernelEvaluation);
					} catch (Exception e) {
						log.warn("couldn't save kernel evaluation, maybe somebody else did. try to retrieve kernel eval");
						if (log.isDebugEnabled())
							log.debug("error saving kernel eval", e);
						txStatus.setRollbackOnly();
					}
					return null;
				}
			});
			kEval = getKernelEval(kernelEvaluation.getCorpusName(),
					kernelEvaluation.getExperiment(),
					kernelEvaluation.getLabel(), kernelEvaluation.getFoldId(),
					kernelEvaluation.getParam1(), kernelEvaluation.getParam2());
		}
		return kEval;
	}

	public KernelEvaluation getKernelEval(String name, String experiment,
			String label, int foldId, double param1, String param2) {
		Query q = this.getSessionFactory().getCurrentSession()
				.getNamedQuery("getKernelEval");
		q.setString("corpusName", name);
		q.setString("experiment", DBUtil.nullToEmptyString(experiment));
		q.setString("label", DBUtil.nullToEmptyString(label));
		q.setInteger("foldId", foldId);
		q.setDouble("param1", param1);
		q.setString("param2", DBUtil.nullToEmptyString(param2));
		return (KernelEvaluation) q.uniqueResult();
	}
}
