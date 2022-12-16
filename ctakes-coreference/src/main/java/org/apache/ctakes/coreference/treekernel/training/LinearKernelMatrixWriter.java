/*
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
package org.apache.ctakes.coreference.treekernel.training;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Scanner;

import org.apache.ctakes.coreference.util.ThreadDelegator;
import org.apache.ctakes.utils.kernel.Kernel;
import org.apache.ctakes.utils.kernel.LinearKernel;
import org.apache.ctakes.utils.kernel.PolyKernel;
import org.apache.ctakes.utils.kernel.RBFKernel;


import libsvm.svm_node;

import opennlp.tools.parser.Parse;

public class LinearKernelMatrixWriter implements ThreadDelegator {

	static int NUMTHREADS = 4;
	private int busyThreads = 0;
	private String inputFile;
	private String outputFile;
	
	public LinearKernelMatrixWriter(String in, String out){
		inputFile = in;
		outputFile = out;
	}
	
	public void run(Kernel kernel){
		int lineNum = 1;
		Scanner scanner;
		ArrayList<ArrayList<Double>> rows = new ArrayList<ArrayList<Double>>();
		ArrayList<svm_node[]> vectors = new ArrayList<svm_node[]>();
		ArrayList<String> labels = new ArrayList<String>();
		
		try {
			scanner = new Scanner(new File(inputFile));

			while(scanner.hasNextLine()){
				String line = scanner.nextLine().trim();
				String label = line.substring(0,2).trim();

				labels.add(label);
				
				vectors.add(getNodes(line));
				rows.add(new ArrayList<Double>(lineNum));
				lineNum++;
			}
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
			System.err.println("Could not open input file!");
			System.exit(1);
		}
		System.out.println("Input complete.");
		RowFillerThread.setObjects(vectors);
		
		RowFillerThread cur = null;
		for(int i = 0; i < vectors.size(); i++){
			if(i % 100 == 0){
				System.out.println("Computing matrix row: " + i);
			}
			while(busyThreads >= NUMTHREADS){  //(freeThreads.isEmpty()){
				try {
//					System.out.println("All threads busy... sleeping for 100ms...");
					Thread.sleep(100);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			cur = new RowFillerThread(this, kernel, rows.get(i), i);
			cur.start();
			busyThreads++;
		}
		
		PrintWriter out;
		String format = "%1.6f";
		double max = 0.0;
		try {
			out = new PrintWriter(outputFile);
			for(int i = 0; i < rows.size(); i++){
				ArrayList<Double> entries = rows.get(i);
				out.print(labels.get(i));
				out.print(" 0:");
				out.print(i+1);
				for(int j = 0; j < rows.size(); j++){
					out.print(" ");
					out.print(j+1);
					out.print(":");
					if(i >= j){
						out.printf(format, entries.get(j));
						if(entries.get(j) > max) max = entries.get(j);
					}else{
						out.printf(format, rows.get(j).get(i));
					}
				}
				out.println();
			}
			out.flush();
			out.close();
			System.err.println("Max value of matrix is: " + max);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.err.println("Error opening output file: " + outputFile);
		}

	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if(args.length < 2){
			System.err.println("Not enough arguments!");
			System.exit(1);
		}
		String dir = "/home/tmill/Projects/coref/training/";
		String config = "treeKernels";
		String path = dir+config;
		LinearKernel linear = new LinearKernel(false);
		PolyKernel poly = new PolyKernel(3, 0.0, false);
		RBFKernel rbf = new RBFKernel(0.5);
		Kernel kernel = rbf;
		
		LinearKernelMatrixWriter lkmw;
		lkmw = new LinearKernelMatrixWriter(path + "/ne/training.downsampled.libsvm", path + "/ne/featRBFMatrix.downsampled.out");
		lkmw.run(kernel);
//		lkmw = new LinearKernelMatrixWriter(path + "/dem/training.downsampled.libsvm", path + "/dem/featPolyMatrix.downsampled.out");
//		lkmw.run(kernel);
//		lkmw = new LinearKernelMatrixWriter(path + "/pronoun/training.downsampled.libsvm", path + "/pronoun/featLinearMatrix.downsampled.out");
//		lkmw.run(kernel);
	}
	
	
	public synchronized void threadDone(RowFillerThread t){
//		System.out.println("Thread " + t.getRowNum() + " finished.");
		busyThreads--;
	}
	
	private static svm_node[] getNodes(String nodeStr) {
		String[] vals = nodeStr.substring(2).split(" ");
		svm_node[] nodes = new svm_node[vals.length];
		for(int i = 0; i < vals.length; i++){
			String[] map = vals[i].split(":");
			nodes[i] = new svm_node();
			nodes[i].index = Integer.parseInt(map[0]);
			nodes[i].value = Double.parseDouble(map[1]);
		}
		return nodes;
	}

}
