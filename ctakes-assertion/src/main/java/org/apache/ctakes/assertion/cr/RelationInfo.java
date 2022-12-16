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
package org.apache.ctakes.assertion.cr;

import java.util.HashSet;

import com.google.common.base.Objects;

/**
 * Information about a relation that includes the info about the relation's arguments.
 * 
 * @author dmitriy dligach
 *
 */
public class RelationInfo {

  public String id1;       // id of the first argument
  public String id2;       // id of the second argument
  public String role1;     // position of first arg (e.g. Argument)
  public String role2;     // semantic type of second arg (e.g. Related_to)
  public String category;  // relation type e.g. co_occurs_with
  
  RelationInfo(String id1, String id2, String role1, String role2, String category) {
    this.id1 = id1; // id of the first argument
    this.id2 = id2; // id of the second argument
    this.role1 = role1;
    this.role2 = role2;
    this.category = category;
  }
  
  @Override
  public String toString() {
  	return String.format("<%s, %s, %s, %s, %s>", id1, id2, role1, role2, category);
  }
  
  /*
   * Returns true if two relation instances (represented as RelationInfo objects)
   * have the same arguments. Useful for debugging mipacq data which may contain duplicate relation instances. 
   */
  @Override
  public boolean equals(Object object) {
  	if (this == object) {
  		return true;
    }
    if (object == null || getClass() != object.getClass()) {
  		return false;
    }

    final RelationInfo that = (RelationInfo) object;
  	return this.id1.equals(that.id1) &&
		   this.id2.equals(that.id2);
  }
  
  /*
   * Hash code must match equals() method. 
   */
  @Override
  public int hashCode()
  {
  	return Objects.hashCode(this.id1, this.id2);
  }
  
  public static void main(String[] args) {
	
  	RelationInfo ri1 = new RelationInfo("1", "2", "Argument", "Related_to", "location_of");
  	RelationInfo ri2 = new RelationInfo("1", "2", "zzzzzzzz", "xxxxxxxxxx", "yyyyyyyyyyy");
  	RelationInfo ri3 = new RelationInfo("1", "2", "kkkkkkkk", "llllllllll", "mmmmmmmmmmm");
  	
  	System.out.println(ri1.equals(ri2));
  	
  	HashSet<RelationInfo> uniqueRelations = new HashSet<RelationInfo>();
  	
  	System.out.println(ri1.hashCode() + "\t" + ri2.hashCode());
  	
  	uniqueRelations.add(ri1);
  	uniqueRelations.add(ri2);
  	
  	System.out.println(uniqueRelations);
  	
  	System.out.println(uniqueRelations.contains(ri1));
  	System.out.println(uniqueRelations.contains(ri2));
  	System.out.println(uniqueRelations.contains(ri3));
  }
}