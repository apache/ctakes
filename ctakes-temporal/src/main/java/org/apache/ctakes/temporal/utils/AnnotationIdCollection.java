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
package org.apache.ctakes.temporal.utils;

import org.apache.ctakes.typesystem.type.relation.BinaryTextRelation;
import org.apache.uima.jcas.tcas.Annotation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Author: SPF
 * Affiliation: CHIP-NLP
 * Date: 6/17/13
 */
public class AnnotationIdCollection {

	static public final int NO_ENTITY_ID = Integer.MIN_VALUE;


	// Key = Annotation (named, event, timex), Value = id number for each "unique" reference.
	// Co-referent entities will have the same id number
	private final Map<Annotation,Integer> _entityIdMap;
	// Key = id number for each "unique" reference, value = primary entity
	private final Map<Integer,Annotation> _idAnnotationMap;


	public AnnotationIdCollection(ArrayList<BinaryTextRelation> temporalRelation){ //final AnnotationCollection annotationCollection ) {
		_entityIdMap = new HashMap<Annotation, Integer>();
		_idAnnotationMap = new HashMap<Integer, Annotation>();
		
		Set<Annotation> allAnnotations = new HashSet<Annotation>();
		for (BinaryTextRelation relation : temporalRelation) {
			allAnnotations.add(relation.getArg1().getArgument());
			allAnnotations.add(relation.getArg2().getArgument());
		}
		final List<Annotation> annotationsList = new ArrayList<Annotation>( allAnnotations );
		Collections.sort( annotationsList, ArgComparator.INSTANCE );// just so that closure counts are consistent
		
		int id = 0;
		for (Annotation annotation : annotationsList) {
			_entityIdMap.put( annotation, id );
			_idAnnotationMap.put( id, annotation );
			id++;			
		}

		//      int id = 0;
		//      for ( Annotation entity : annotationCollection.getNamedEntities() ) {
		//         _entityIdMap.put( entity, id );
		//         _idAnnotationMap.put( id, entity );
		//         id++;
		//      }
		//      for ( Annotation entity : annotationCollection.getTimes() ) {
		//         _entityIdMap.put( entity, id );
		//         _idAnnotationMap.put( id, entity );
		//         id++;
		//      }
		//      for ( Annotation entity : annotationCollection.getEvents() ) {
		//         _entityIdMap.put( entity, id );
		//         _idAnnotationMap.put( id, entity );
		//         id++;
		//      }
		//      for ( CoreferenceChain chain : annotationCollection.getCoreferenceEntities() ) {
		//         for ( Annotation entity : chain ) {
		//            _entityIdMap.put( entity, id );
		//         }
		//         _entityIdMap.put( chain, id );
		//         _idAnnotationMap.put( id, chain );
		//         id++;
		//      }

	}


	/**
	 * @return a collection of unique ids for all unique entities
	 */
	public Set<Integer> getAnnotationIds() {
		//      return Collections.unmodifiableSet( _idAnnotationMap.keySet() );
		return Collections.unmodifiableSet( new HashSet<Integer>( _entityIdMap.values() ) );
	}

	/**
	 * @param entity some entity, be it unique on its own or part of a coreference chain
	 * @return an id for the entity
	 */
	public int getAnnotationId( final Annotation entity ) {
		final Integer id = _entityIdMap.get( entity );
		if ( id != null ) {
			return id;
		}
		return getIdByAnnotationEqual( entity );
	}

	private int getIdByAnnotationEqual( final Annotation entity ) {
		for ( Annotation referenceAnnotation : _entityIdMap.keySet() ) {
			if ( referenceAnnotation.equals( entity ) ) {
				final int id = _entityIdMap.get( referenceAnnotation );
				// Prevent future iteration searches
				_entityIdMap.put( entity, id );
				return id;
			}
		}
		return getIdByTextSpan( entity.getBegin(), entity.getEnd() );
	}

	private int getIdByTextSpan( final int begin, int end ) {
		for ( Annotation referenceAnnotation : _entityIdMap.keySet() ) {
			if ( referenceAnnotation.getBegin()==begin &&  referenceAnnotation.getEnd()==end){//.getTextSpan().equals( textSpan ) ) {
				return _entityIdMap.get( referenceAnnotation );
			}
//			// TODO necessary?
//			if ( referenceAnnotation instanceof CoreferenceChain ) {
//				for ( Annotation coreference : (CoreferenceChain)referenceAnnotation ) {
//					if ( coreference.getTextSpan().equals( textSpan ) ) {
//						return _entityIdMap.get( referenceAnnotation );
//					}
//				}
//			}
		}
		return NO_ENTITY_ID;
	}


	/**
	 * @param entityId some id
	 * @return the NamedAnnotation, Event, Timex, or CoreferenceChain with the given referenceId
	 */
	public Annotation getAnnotation( final int entityId ) {
		return _idAnnotationMap.get( entityId );
	}

	public Annotation getAnnotation( final Annotation entity ) {
		final int id = getAnnotationId( entity );
		if ( id != NO_ENTITY_ID ) {
			return getAnnotation( id );
		}
		return null;
	}
	
	
	static private enum ArgComparator implements Comparator<Annotation> {
		INSTANCE;
		/**
		 * {@inheritDoc}
		 */
		public int compare( final Annotation arg1, final Annotation arg2 ) {
			if(arg1 == null) return -1;
			else if(arg2 == null) return 1;
			final int startDiff = arg1.getBegin() - arg2.getBegin();
			if ( startDiff != 0 ) {
				return startDiff;
			}
			return arg1.getEnd() - arg2.getEnd();
		}		
	}

}
