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

import org.apache.ctakes.constituency.parser.treekernel.TreeKernel;
import org.apache.ctakes.coreference.util.ThreadDelegator;


import opennlp.tools.parser.Parse;

public class TreeKernelTrainer implements ThreadDelegator {

	static int NUMTHREADS = 4;
	private int busyThreads = 0;
	private String inFile;
	private String outFile;
	
	public TreeKernelTrainer(String inputFile, String outputFile){
		inFile = inputFile;
		outFile = outputFile;
	}
	
	public void run(TreeKernel kernel){
		int lineNum = 1;
		Scanner scanner;
		ArrayList<ArrayList<Double>> rows = new ArrayList<ArrayList<Double>>();
		ArrayList<Parse> trees = new ArrayList<Parse>();
		ArrayList<String> labels = new ArrayList<String>();
		
		try {
			scanner = new Scanner(new File(inFile));

			while(scanner.hasNextLine()){
				String line = scanner.nextLine();
				String[] parts = line.split(":", 2);
				String label = parts[0].trim();
				String treeStr = parts[1].trim();

				labels.add(label);
				trees.add(Parse.parseParse(treeStr));
				rows.add(new ArrayList<Double>(lineNum));
				lineNum++;
			}
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
			System.err.println("Could not open input file!");
			System.exit(1);
		}
		System.out.println("Input complete.");
		RowFillerThread.setObjects(trees);
		
		RowFillerThread cur = null;
		for(int i = 0; i < trees.size(); i++){
			if(i % 100 == 0){
				System.out.println("Computing matrix row: " + i);
			}
			while(busyThreads >= NUMTHREADS){  //(freeThreads.isEmpty()){
				try {
//					System.out.println("All threads busy... sleeping for 100ms...");
					Thread.sleep(10);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			cur = new RowFillerThread(this, kernel, rows.get(i), i);
			cur.start();
			threadStarted();
		}
		
		System.err.println("Waiting for threads to finish...");
		while(busyThreads > 0){
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		System.err.println("Writing to file...");
		String format = "%1.4f";
		PrintWriter out;
		double max=0.0;
		try {
			out = new PrintWriter(outFile);
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
			System.err.println("Error opening output file: " + outFile);
		}

	}
	
	public void threadStarted(){
		changeNumThreads(1);
	}
	
	public void threadDone(RowFillerThread t){
//		System.out.println("Thread " + t.getRowNum() + " finished.");
//		busyThreads--;
		changeNumThreads(-1);
	}
	
	public synchronized void changeNumThreads(int i){
		busyThreads += i;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
//		if(args.length < 2){
//			System.err.println("Not enough arguments!");
//			System.exit(1);
//		}
		String dir = "/home/tmill/Projects/coref/training/";
		String config = "treeKernels";
		String path = dir+config;
		
		TreeKernel kernel = new TreeKernel(false);
		TreeKernelTrainer tkt;
		tkt = new TreeKernelTrainer(path + "/ne/trees.downsampled.txt", path + "/ne/matrix.downsampled.out");
		tkt.run(kernel);
//		tkt = new TreeKernelTrainer(path + "/dem/trees.downsampled.txt", path + "/dem/matrix.downsampled.out");
//		tkt.run();
//		tkt = new TreeKernelTrainer(path + "/pronoun/trees.downsampled.txt", path + "/pronoun/matrix.downsampled.out");
//		tkt.run(kernel);
//		tkt = new TreeKernelTrainer("/home/tmill/Projects/parser/reranker/small.txt", "/home/tmill/Projects/parser/reranker/matrix.txt");
//		tkt.run();
	}
}
