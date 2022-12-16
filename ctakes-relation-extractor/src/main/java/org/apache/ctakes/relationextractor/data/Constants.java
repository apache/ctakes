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
package org.apache.ctakes.relationextractor.data;

import java.util.Arrays;
import java.util.HashSet;

/**
 * Various global settings.
 * 
 * @author dmitriy dligach
 *
 */
public class Constants {
	
	public static final String mipacqAllXmlPath = "/home/dima/sharp/cloud/mipacq/xml/all/";
	public static final String mipacqTrainXmlPath = "/home/dima/sharp/cloud/mipacq/xml/train/";
	public static final String mipacDevXmlPath = "/home/dima/sharp/cloud/mipacq/xml/dev/";
	public static final String mipacqTestXmlPath = "/home/dima/sharp/cloud/mipacq/xml/test/";

	public static final String shareAllXmlPath = "/home/dima/sharp/cloud/share/xml/all/";
	public static final String shareTrainXmlPath = "/home/dima/sharp/cloud/share/xml/train/";
	public static final String shareDevXmlPath = "/home/dima/sharp/cloud/share/xml/dev/";
	public static final String shareTestXmlPath = "/home/dima/sharp/cloud/share/xml/test/";
	
	public static final String sharpAllXmlPath = "/home/dima/sharp/cloud/sharp/xml/all/";
	public static final String sharpTrainXmlPath = "/home/dima/sharp/cloud/sharp/xml/train/";
	public static final String sharpDevXmlPath = "/home/dima/sharp/cloud/sharp/xml/dev/";
	public static final String sharpTestXmlPath = "/home/dima/sharp/cloud/sharp/xml/test/";
	
	// high priority sharp relations
	public static final HashSet<String> sharpRelationsHighPriority = 
			new HashSet<String>(Arrays.asList("location_of", "degree_of", "prevents", "compicates", "disrupts"));
	
	// low priority sharp relations
	public static final HashSet<String> sharpRelationsLowPriority = 
			new HashSet<String>(Arrays.asList("manifestation_of", "affects", "manages/treats", "causes/brings_about",
					"contraindicates", "diagnoses", "indicates", "is_indicated_for"));
	
	// all sharp relations
	public static final HashSet<String> sharpRelationsAll = 
			new HashSet<String>(Arrays.asList("location_of", "degree_of", "prevents", "compicates", "disrupts",
					"manifestation_of", "affects", "manages/treats", "causes/brings_about",
					"contraindicates", "diagnoses", "indicates", "is_indicated_for"));
	
	// sharp relations selected for march 2012 deliverable
	public static final HashSet<String> sharpRelationsSelected = 
			new HashSet<String>(Arrays.asList("manages/treats", "degree_of", "causes/brings_about", "location_of", "indicates"));
}