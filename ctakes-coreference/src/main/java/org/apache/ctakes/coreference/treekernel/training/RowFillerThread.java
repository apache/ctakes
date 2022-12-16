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

import java.util.ArrayList;

import org.apache.ctakes.constituency.parser.treekernel.TreeKernel;
import org.apache.ctakes.coreference.util.ThreadDelegator;
import org.apache.ctakes.utils.kernel.Kernel;


import opennlp.tools.parser.Parse;

public class RowFillerThread extends Thread {
	private ArrayList<Double> row = null;
	private static ArrayList<?> objects;
	private Kernel kernel = null;
	private ThreadDelegator parent = null;
	private int rowNum = 0;
	private long start;
	
	public RowFillerThread(ThreadDelegator parent, Kernel k, ArrayList<Double> row, int i){
		this.kernel = k;
		this.row = row;
		this.rowNum = i;
		this.parent = parent;
	}
	
	@Override
	public void run() {
		super.run();
		if(rowNum % 100 == 0) start = System.currentTimeMillis();
		Object p1 = objects.get(rowNum);
		for(int j = 0; j <= rowNum; j++){
			Object p2 = objects.get(j);
			double sim = kernel.eval(p1,p2);
			row.add(sim);
		}
		
		if(rowNum % 100 == 0){
			System.out.println("Row " + rowNum + " took " + (System.currentTimeMillis()-start)+  "ms");
		}
		parent.threadDone(this);
	}
	
	public void setRow(ArrayList<Double> row, int i){
		this.row = row;
		this.rowNum = i;
	}

	public static void setObjects(ArrayList<?> objects) {
		RowFillerThread.objects = objects;
	}
	
	public int getRowNum(){
		return rowNum;
	}
}
