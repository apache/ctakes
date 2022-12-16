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
import java.util.Scanner;

public class KernelMatrixCompositor {

	// TODO - Think about how to combine kernels here because it affects how the program works, can they be read
	// one at a time and computed in place or do I need to read them all in and then compute one cell at a time?
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if(args.length < 4){
			System.err.println("Usage: KernelMatrixCompositor <numExamples> <Matrix 1 file> <Matrix 1 multiplier> <Matrix 2 file> <Matrix 2 multiplier ... <Matrix N file> <Matrix N multipler> <Matrix output>");
			System.exit(1);
		}
		
		PrintWriter out=null;
		try {
			out = new PrintWriter(args[args.length-1]);
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
			System.err.println("Unable to open output file... exiting!");
			System.exit(1);
		}
		int dim = Integer.parseInt(args[0]);
//		int sumInd = args.length-1;
		
		double[][] matrix = new double[dim][dim];
		
		int[] labels = new int[dim];
		
		Scanner scanner = null;
		// read in all matrices
		for(int argnum = 1; argnum < args.length-1; argnum+=2){
			System.out.println("Reading file... " + args[argnum] + " with weight... " + args[argnum+1]);
			try {
				scanner = new Scanner(new File(args[argnum]));
				double multiplier = Double.parseDouble(args[argnum+1]);
				int lineNum = 0;
				while(scanner.hasNextLine()){
					String line = scanner.nextLine();
					String[] parts = line.trim().split(" ");
					if(argnum == 1){
						// only have to fill in the labels array once.
						labels[lineNum] = Integer.parseInt(parts[0]);
					}
					
					// skip j== 1 because that's just the line number+1
					int i = lineNum;
					for(int j = 2; j < parts.length; j++){
						String[] node = parts[j].split(":");
						matrix[i][j-2] += (multiplier * Double.parseDouble(node[1]));
					}
					lineNum++;
				}
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		System.out.println("Printing out composite matrix...");
		for(int i = 0; i < dim; i++){
			out.print(labels[i]);
			out.print(" 0:");
			out.print(i+1);
			for(int j = 0; j < dim; j++){
				out.print(" ");
				out.print(j+1);
				out.print(":");
				out.print(matrix[i][j]);
			}
			out.println();
		}
		out.close();
		System.out.println("Done!");
	}
}
