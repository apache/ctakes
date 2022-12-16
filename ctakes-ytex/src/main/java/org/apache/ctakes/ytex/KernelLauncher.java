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
package org.apache.ctakes.ytex;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.ctakes.ytex.kernel.tree.InstanceTreeBuilder;
import org.apache.ctakes.ytex.kernel.tree.TreeMappingInfo;
import org.springframework.context.ApplicationContext;
import org.springframework.context.access.ContextSingletonBeanFactoryLocator;
import org.springframework.context.support.FileSystemXmlApplicationContext;


public class KernelLauncher {
	private static Options initOptions() {
		Option oStoreInstanceMap = OptionBuilder
				.withArgName("instanceMap.obj")
				.hasArg()
				.withDescription(
						"store the instanceMap.  Use prior to running the kernel evaluations in parallel.")
				.create("storeInstanceMap");
		Option oEvaluateKernel = OptionBuilder
				.withDescription(
						"evaluate kernel specified in application context on the instances. If instanceMap is specified, load instance from file system, else from db.")
				.create("evalKernel");
		Option exportBagOfWords = OptionBuilder
				.withDescription(
						"exportBagOfWords.  Must specify property file")
				.hasArg().create("exportBagOfWords");
		Option exportType = OptionBuilder
				.withDescription("exportType.  either libsvm or weka").hasArg()
				.create("exportType");
		Option oLoadInstanceMap = OptionBuilder
				.withArgName("instanceMap.obj")
				.hasArg()
				.withDescription(
						"load instanceMap from file system instead of from db.  Use after storing instance map.")
				.create("loadInstanceMap");
		Option oEvalMod = OptionBuilder
				.withDescription(
						"for parallelization, split the instances into mod slices")
				.hasArg().create("mod");
		Option oEvalSlice = OptionBuilder
				.withDescription(
						"for parallelization, parameter that determines which slice we work on.  If this is not specified, nMod threads will be started to evaluate all slices in parallel.")
				.hasArg().create("slice");
		Option oBeanref = OptionBuilder
				.withArgName("classpath*:simSvcBeanRefContext.xml")
				.hasArg()
				.withDescription(
						"use specified beanRefContext.xml, default classpath*:simSvcBeanRefContext.xml")
				.create("beanref");
		Option oAppctx = OptionBuilder
				.withArgName("kernelApplicationContext")
				.hasArg()
				.withDescription(
						"use specified applicationContext, default kernelApplicationContext")
				.create("appctx");
		Option oBeans = OptionBuilder
				.withArgName("beans-corpus.xml")
				.hasArg()
				.withDescription(
						"use specified beans.xml, no default.  This file is typically required.")
				.create("beans");

		Option oHelp = new Option("help", "print this message");
		Options options = new Options();
		OptionGroup og = new OptionGroup();
		og.addOption(oStoreInstanceMap);
		og.addOption(oEvaluateKernel);
		og.addOption(exportBagOfWords);
		options.addOptionGroup(og);
		// options.addOption(oStoreInstanceMap);
		options.addOption(oEvaluateKernel);
		// options.addOption(exportBagOfWords);
		options.addOption(exportType);
		options.addOption(oLoadInstanceMap);
		options.addOption(oEvalMod);
		options.addOption(oEvalSlice);
		options.addOption(oBeanref);
		options.addOption(oAppctx);
		options.addOption(oBeans);
		options.addOption(oHelp);

		return options;
	}

	private static void printHelp(Options options) {
		HelpFormatter formatter = new HelpFormatter();
		formatter
				.printHelp(
						"java org.apache.ctakes.ytex.kernel.evaluator.CorpusKernelEvaluatorImpl\n Main Options: -storeInstanceMap or -evalKernel",
						options);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		Options options = initOptions();

		if (args.length == 0) {
			printHelp(options);
		} else {
			CommandLineParser parser = new GnuParser();
			try {
				// parse the command line arguments
				CommandLine line = parser.parse(options, args);
				String storeInstanceMap = line
						.getOptionValue("storeInstanceMap");
				boolean evalKernel = line.hasOption("evalKernel");
				String exportBagOfWords = line
						.getOptionValue("exportBagOfWords");
				if (!evalKernel && storeInstanceMap == null
						&& exportBagOfWords == null) {
					System.out
							.println("specify either -evalKernel, -storeInstanceMap, or -exportBagOfWords");
					printHelp(options);
				} else {

					// parse the command line arguments
					// by default use the kernelBeanRefContext
					// when evaluating kernel, by default use the
					// simSvcBeanRefContext.xml
					// don't want to load that for other tasks as the simSvc
					// loads the UMLS object graph which needs lots of memory
					String beanRefContext = line.getOptionValue("beanref",
							evalKernel ? "classpath*:org/apache/ctakes/ytex/simSvcBeanRefContext.xml"
									: "classpath*:org/apache/ctakes/ytex/kernelBeanRefContext.xml");
					String contextName = line.getOptionValue("appctx",
							"kernelApplicationContext");
					String beans = line.getOptionValue("beans");
					ApplicationContext appCtx = (ApplicationContext) ContextSingletonBeanFactoryLocator
							.getInstance(beanRefContext)
							.useBeanFactory(contextName).getFactory();
					ApplicationContext appCtxSource = appCtx;
					if (beans != null) {
						appCtxSource = new FileSystemXmlApplicationContext(
								new String[] { beans }, appCtx);
					}
					if (storeInstanceMap != null) {
						storeInstanceMap(appCtxSource, storeInstanceMap, line);
//					} else if (evalKernel) {
//						evalKernel(appCtxSource, line);
//					} else if (exportBagOfWords != null) {
//						exportBagOfWords(appCtxSource, exportBagOfWords, line);
					}
				}
			} catch (ParseException e) {
				printHelp(options);
				throw e;
			}
		}
	}

//	private static void exportBagOfWords(ApplicationContext appCtxSource,
//			String exportBagOfWords, CommandLine line) throws IOException {
//		String beanName = "wekaBagOfWordsExporter";
//		if ("libsvm".equals(line.getOptionValue("exportType"))) {
//			beanName = "libsvmBagOfWordsExporter";
//		}
//		BagOfWordsExporter exporter = (BagOfWordsExporter) appCtxSource
//				.getBean(beanName);
//		exporter.exportBagOfWords(exportBagOfWords);
//	}

//	private static void evalKernel(ApplicationContext appCtxSource,
//			CommandLine line) throws Exception {
//		InstanceTreeBuilder builder = appCtxSource.getBean(
//				"instanceTreeBuilder", InstanceTreeBuilder.class);
//		CorpusKernelEvaluator corpusEvaluator = appCtxSource.getBean(
//				"corpusKernelEvaluator", CorpusKernelEvaluator.class);
//		String loadInstanceMap = line.getOptionValue("loadInstanceMap");
//		String strMod = line.getOptionValue("mod");
//		String strSlice = line.getOptionValue("slice");
//		int nMod = strMod != null ? Integer.parseInt(strMod) : 0;
//		Integer nSlice = null;
//		if (nMod == 0) {
//			nSlice = 0;
//		} else if (strSlice != null) {
//			nSlice = Integer.parseInt(strSlice);
//		}
//		Map<Integer, Node> instanceMap = null;
//		if (loadInstanceMap != null) {
//			instanceMap = builder.loadInstanceTrees(loadInstanceMap);
//		} else {
//			instanceMap = builder.loadInstanceTrees(appCtxSource.getBean(
//					"treeMappingInfo", TreeMappingInfo.class));
//		}
//		if (nSlice != null) {
//			corpusEvaluator.evaluateKernelOnCorpus(instanceMap, nMod, nSlice, false);
//		} else {
//			corpusEvaluator.evaluateKernelOnCorpus(instanceMap, nMod, false);
//		}
//	}

	private static void storeInstanceMap(ApplicationContext appCtxSource,
			String storeInstanceMap, CommandLine line) throws Exception {
		InstanceTreeBuilder builder = appCtxSource.getBean(
				"instanceTreeBuilder", InstanceTreeBuilder.class);
		builder.serializeInstanceTrees(
				appCtxSource.getBean("treeMappingInfo", TreeMappingInfo.class),
				storeInstanceMap);
	}
}
