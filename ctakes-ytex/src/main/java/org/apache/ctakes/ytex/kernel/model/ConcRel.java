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
package org.apache.ctakes.ytex.kernel.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.ctakes.ytex.kernel.metric.LCSPath;


import com.google.common.collect.ImmutableSet;

public class ConcRel implements java.io.Serializable {
	private static final Logger log = Logger.getLogger(ConcRel.class.getName());
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public static List<String> crListToString(List<ConcRel> crList) {
		if (crList != null) {
			List<String> path = new ArrayList<String>(crList.size());
			for (ConcRel cr : crList)
				path.add(cr.getConceptID());
			return path;
		} else {
			return null;
		}
	}

	/**
	 * get least common subsumer of the specified concepts and its distance from
	 * root.
	 * 
	 * @deprecated
	 * 
	 * @param c1
	 * @param c2
	 * @return
	 */
	public static ObjPair<ConcRel, Integer> getLeastCommonConcept(ConcRel c1,
			ConcRel c2) {
		if (log.isLoggable(Level.FINE)) {
			log.fine("getLeastCommonConcept(" + c1 + "," + c2 + ")");
		}
		// result
		ObjPair<ConcRel, Integer> res = new ObjPair<ConcRel, Integer>(null,
				Integer.MAX_VALUE);
		// concept 1's parent distance map
		Map<ConcRel, Integer> cand1 = new HashMap<ConcRel, Integer>();
		// concept 2's parent distance map
		Map<ConcRel, Integer> cand2 = new HashMap<ConcRel, Integer>();

		// parents of concept 1
		HashSet<ConcRel> parC1 = new HashSet<ConcRel>();
		parC1.add(c1);
		// parents of concept 2
		HashSet<ConcRel> parC2 = new HashSet<ConcRel>();
		parC2.add(c2);
		HashSet<ConcRel> tmp = new HashSet<ConcRel>();
		HashSet<ConcRel> tmp2;

		int dist = 0;
		// changed to start distance with 1 - we increment at the end of the
		// loop
		// we always look at the parents, so the distance has to start with 1
		// if one concept is the parent of the other, this would return 0 if
		// dist starts with 0
		// int dist = 1;
		// while there are parents
		// this does a dual-breadth first search
		// parC1 are the dist'th ancestors of concept 1
		// parC2 are the dist'th ancestors of concept 2
		while (!parC1.isEmpty() || !parC2.isEmpty()) {
			// grandparents
			tmp.clear();
			// go through parents of concept1
			for (Iterator<ConcRel> it = parC1.iterator(); it.hasNext();) {
				ConcRel cr = it.next();
				// checkif it's in the map concept2's parent distance map
				// - map of distances from concept 1
				if (cand2.containsKey(cr)) {
					res.v1 = cr;
					res.v2 = dist + cand2.get(cr).intValue();
					// return
					return res;
				}
				// not in the map - add it to the concept-distance map
				cand1.put(cr, dist);
				// add the grandparents to the tmp set
				tmp.addAll(cr.parents);
			}
			// remove concepts already in concept1's parent distance map from
			// the grandparent map
			tmp.removeAll(cand1.keySet());
			// tmp2 becomes the parents of c1
			tmp2 = parC1;
			// par c1 becomes grandparents minus parents
			parC1 = tmp;
			// tmp becomes tmp2, which is going to be killed in the next line
			tmp = tmp2;

			tmp.clear();
			// repeat everything for concept2 - go up one level
			for (Iterator<ConcRel> it = parC2.iterator(); it.hasNext();) {
				ConcRel cr = it.next();
				if (cand1.containsKey(cr)) {
					res.v1 = cr;
					res.v2 = dist + cand1.get(cr).intValue();
					return res;
				}
				cand2.put(cr, dist);
				tmp.addAll(cr.parents);
			}
			tmp.removeAll(cand2.keySet());
			tmp2 = parC2;
			parC2 = tmp;
			tmp = tmp2;

			++dist;
		}

		return res;
	}

	/**
	 * 
	 * @param c1
	 *            concept1
	 * @param c2
	 *            concept2
	 * @param lcses
	 *            least common subsumers, required
	 * @param paths
	 *            paths between concepts via lcses, optional. Key - lcs. Value -
	 *            2 element list corresponding to paths to lcs from c1 and c2
	 * @return path length, -1 if no lcs
	 */
	public static int getLeastCommonConcept(ConcRel c1, ConcRel c2,
			Set<ConcRel> lcses, Map<ConcRel, LCSPath> paths) {
		if (log.isLoggable(Level.FINE)) {
			log.fine("getLeastCommonConcept(" + c1 + "," + c2 + ")");
		}
		// concept 1's parent distance map
		Map<ConcRel, Integer> cand1 = new HashMap<ConcRel, Integer>();
		// concept 2's parent distance map
		Map<ConcRel, Integer> cand2 = new HashMap<ConcRel, Integer>();
		// paths corresponding to parents
		// we only calculate these if they are asked of us
		Map<ConcRel, List<ConcRel>> paths1 = paths != null ? new HashMap<ConcRel, List<ConcRel>>()
				: null;
		Map<ConcRel, List<ConcRel>> paths2 = paths != null ? new HashMap<ConcRel, List<ConcRel>>()
				: null;

		// parents of concept 1
		HashSet<ConcRel> parC1 = new HashSet<ConcRel>();
		parC1.add(c1);
		// parents of concept 2
		HashSet<ConcRel> parC2 = new HashSet<ConcRel>();
		parC2.add(c2);
		// temporary hashset for scratch work
		// not clear if this really reduces memory overhead
		HashSet<ConcRel> tmp = new HashSet<ConcRel>();
		HashSet<ConcRel> candidateLCSes = new HashSet<ConcRel>();

		int maxIter = -1;
		int dist = 0;
		int minDist = Integer.MAX_VALUE - 1;
		// continue the search while there are parents left
		// check maxIter - if this is some non-negative number
		// then it must be greater than 0
		while ((!parC1.isEmpty() || !parC2.isEmpty())
				&& (maxIter < 0 || maxIter != 0)) {
			// get next iteration of ancestors, save them
			updateParent(cand1, parC1, tmp, dist, paths1);
			updateParent(cand2, parC2, tmp, dist, paths2);
			// get the intersection across the ancestors
			tmp.clear();
			tmp.addAll(cand1.keySet());
			tmp.retainAll(cand2.keySet());
			tmp.removeAll(candidateLCSes);
			// if there is something in the intersection, we have a potential
			// winner. however, we can't stop here
			// example: ascites/hepatoma in snomed C2239176 C0003962
			// e.g. one path could be 3-3, but a shorter path could be 4-1
			// we would only find the 4-1 path after 4 iterations
			if (!tmp.isEmpty()) {
				// add candidates so we don't have to look at them in future
				// iterations
				candidateLCSes.addAll(tmp);
				// remove candidates' parents from the parent collections for
				// the next iterations
				removeParents(tmp, parC1);
				removeParents(tmp, parC2);
				// add the lcs candidates and their path length
				// even though we have a hit, we can't stop here
				// there could be uneven path lengths.
				// to account for this, the 1st time we find an lcs
				// we set maxIter to the minimum path length to either concept
				// from the lcs. if we can't find a match after maxIter
				// iterations, then we know that what we've found is a winner
				for (ConcRel lcs : tmp) {
					// path length for current lcs
					int distTmp = cand1.get(lcs) + cand2.get(lcs) + 1;
					// only add it to the list of lcses if it is less than or
					// equal to the current minimal path length
					if (distTmp <= minDist) {
						if (distTmp < minDist) {
							// we have a new best minimal path length
							// clear the current lcses
							lcses.clear();
						}
						minDist = distTmp;
						lcses.add(lcs);
					}
					// all additional lcses must be found within maxIter
					// iterations. maxIter is the shortest path between
					// the lcs and a concept
					int minLcsToConceptLen = Math.min(cand1.get(lcs),
							cand2.get(lcs));
					if (maxIter < 0 || maxIter > minLcsToConceptLen) {
						maxIter = minLcsToConceptLen;
					}
				}
			}
			// reduce maximum number of iterations left
			maxIter--;
			++dist;
		}
		if (lcses.isEmpty())
			return -1;
		else {
			if (paths != null) {
				for (ConcRel lcs : lcses) {
					LCSPath lcsPath = new LCSPath();
					lcsPath.setLcs(lcs.getConceptID());
					lcsPath.setConcept1Path(crListToString(paths1.get(lcs)));
					lcsPath.setConcept2Path(crListToString(paths2.get(lcs)));
					paths.put(lcs, lcsPath);
				}
			}
			return minDist;
		}
	}

	/**
	 * remove the parents of candidate lcses from the list of parents we were
	 * planning on looking at in the next iteration
	 * 
	 * @param lcses
	 * @param parents
	 */
	private static void removeParents(HashSet<ConcRel> lcses,
			HashSet<ConcRel> parents) {
		for (ConcRel lcs : lcses) {
			parents.removeAll(lcs.parents);
		}
	}

	/**
	 * perform 1 iteration of breadth-first search on lcs. update the various
	 * collections with the next iteration of ancestors.
	 * 
	 * @param cand1
	 * @param parC1
	 * @param tmp
	 * @param dist
	 */
	private static void updateParent(Map<ConcRel, Integer> cand1,
			HashSet<ConcRel> parC1, HashSet<ConcRel> tmp, int dist,
			Map<ConcRel, List<ConcRel>> paths) {
		tmp.clear();
		// go through parents of concept1
		for (Iterator<ConcRel> it = parC1.iterator(); it.hasNext();) {
			ConcRel cr = it.next();
			if (!cand1.containsKey(cr)) {
				// not in the map - add it to the concept-distance map
				cand1.put(cr, dist);
				// add the grandparents to the tmp set
				tmp.addAll(cr.parents);
				if (paths != null) {
					// add the path to the parent to the map of paths
					List<ConcRel> pathCR = paths.get(cr);
					for (ConcRel parent : cr.parents) {
						if (!paths.containsKey(parent)) {
							// path to parent = path to child + child
							List<ConcRel> path = new ArrayList<ConcRel>(
									pathCR != null ? pathCR.size() + 1 : 1);
							if (pathCR != null)
								path.addAll(pathCR);
							path.add(cr);
							paths.put(parent, path);
						}
					}
				}
			}
		}
		// remove concepts already in concept1's parent distance map from
		// the grandparent map
		tmp.removeAll(cand1.keySet());
		// parents for the next iteration
		parC1.clear();
		parC1.addAll(tmp);
	}

	/**
	 * children of this concept
	 */
	private Set<ConcRel> children;
	private int[] childrenArray;
	
	private short depth;

	private double intrinsicInfoContent;

	/**
	 * id of this concept
	 */
	private String nodeCUI;

	private int nodeIndex;

	/**
	 * parents of this concept
	 */
	private Set<ConcRel> parents;

	/**
	 * for java object serialization, need to avoid default serializer behavior
	 * of writing out entire object graph. just write the parent/children object
	 * ids and resolve the connections after loading this object.
	 */
	private int[] parentsArray;

	public ConcRel(String cui, int nodeIndex) {
		nodeCUI = cui;
		parents = new HashSet<ConcRel>();
		children = new HashSet<ConcRel>();
		parentsArray = null;
		childrenArray = null;
		this.nodeIndex = nodeIndex;
	}

	/**
	 * reconstruct the relationships to other ConcRel objects
	 * 
	 * @param db
	 */
	public void constructRel(List<ConcRel> db) {
		ImmutableSet.Builder<ConcRel> pBuilder = new ImmutableSet.Builder<ConcRel>(); 
		for (int c : parentsArray)
			pBuilder.add(db.get(c));
		parents = pBuilder.build();
		parentsArray = null;

		ImmutableSet.Builder<ConcRel> cBuilder = new ImmutableSet.Builder<ConcRel>(); 
		for (int c : childrenArray)
			cBuilder.add(db.get(c));
		children = cBuilder.build();
		childrenArray = null;
	}

	public int depthMax() {
		int d = 0;
		for (Iterator<ConcRel> it = children.iterator(); it.hasNext();) {
			ConcRel child = it.next();
			int dm = child.depthMax() + 1;
			if (dm > d)
				d = dm;
		}
		return d;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ConcRel other = (ConcRel) obj;
		if (nodeIndex != other.nodeIndex)
			return false;
		return true;
	}

	public Set<ConcRel> getChildren() {
		return children;
	}
	public int[] getChildrenArray() {
		return childrenArray;
	}
	public String getConceptID() {
		return nodeCUI;
	}

	public short getDepth() {
		return depth;
	}

	public double getIntrinsicInfoContent() {
		return intrinsicInfoContent;
	}

	public int getNodeIndex() {
		return nodeIndex;
	}

	public Set<ConcRel> getParents() {
		return parents;
	}

	public int[] getParentsArray() {
		return parentsArray;
	}

	/**
	 * recursively build all paths to root from a concept - add elements from
	 * set of parents.
	 * 
	 * @param lpath
	 *            current path from children to this concept
	 * @param allPaths
	 *            list of all paths
	 * @param depth
	 *            current depth
	 * @param depthMax
	 */
	public void getPath(List<ConcRel> lpath, List<List<ConcRel>> allPaths,
			int depth, int depthMax) {
		if (depth >= depthMax)
			return;
		if (lpath == null)
			lpath = new ArrayList<ConcRel>();

		lpath.add(this);

		if (isRoot()) {
			// add a copy to the list of all paths
			allPaths.add(new ArrayList<ConcRel>(lpath));
		} else {
			// recurse
			for (ConcRel p : parents) {
				p.getPath(lpath, allPaths, depth + 1, depthMax);
			}
		}
		lpath.remove(lpath.size() - 1);
	}

	/**
	 * is the specified concept an ancestor of this concept?
	 * 
	 * @param cui
	 * @return
	 */
	public boolean hasAncestor(String cui) {
		if (nodeCUI.equals(cui))
			return true;
		for (ConcRel c : parents) {
			if (c.hasAncestor(cui))
				return true;
		}
		return false;
	}

	@Override
	public int hashCode() {
		return nodeIndex;
	}

	public boolean isLeaf() {
		return children.isEmpty();
	}

	public boolean isRoot() {
		return parents.isEmpty();
	}

	/**
	 * read parent/children concept ids, not the objects
	 */
	private void readObject(java.io.ObjectInputStream in)
			throws java.io.IOException, ClassNotFoundException {
		nodeCUI = (String) in.readObject();
		this.nodeIndex = in.readInt();
		this.intrinsicInfoContent = in.readDouble();
		this.depth = in.readShort();
		parentsArray = (int[]) in.readObject();
		childrenArray = (int[]) in.readObject();
		parents = new HashSet<ConcRel>(parentsArray.length);
		children = new HashSet<ConcRel>(childrenArray.length);
	}

	// public static ObjPair<ConcRel, Integer> getLeastCommonConcept(
	// Vector<Vector<ConcRel>> allPaths1, Vector<Vector<ConcRel>> allPaths2) {
	// ObjPair<ConcRel, Integer> res = new ObjPair<ConcRel, Integer>(null,
	// Integer.MAX_VALUE);
	// ObjPair<ConcRel, Integer> tmp = new ObjPair<ConcRel, Integer>(null,
	// Integer.MAX_VALUE);
	//
	// int n = 0;
	// for (Vector<ConcRel> path1 : allPaths1) {
	// // if(n++>200)
	// // break;
	// int n2 = 0;
	// for (Vector<ConcRel> path2 : allPaths2) {
	// // if(n2++>200)
	// // break;
	// if (getCommonConcept(path1, path2, tmp) != null) {
	// if (tmp.v2.intValue() < res.v2.intValue()) {
	// res.v1 = tmp.v1;
	// res.v2 = tmp.v2;
	// }
	// }
	// }
	// }
	//
	// return res;
	// }

	// public static ConcRel getCommonConcept(Vector<ConcRel> path1,
	// Vector<ConcRel> path2, ObjPair<ConcRel, Integer> oVals) {
	// ConcRel common = null;
	// int dist = Integer.MAX_VALUE;
	// int index1 = path1.size() - 1;
	// int index2 = path2.size() - 1;
	// while (index1 >= 0 && index2 >= 0) {
	// ConcRel r1 = path1.get(index1);
	// if (r1.equals(path2.get(index2))) {
	// common = r1;
	// dist = index1 + index2;
	// --index1;
	// --index2;
	// } else
	// break;
	// }
	//
	// oVals.v1 = common;
	// oVals.v2 = dist;
	//
	// return common;
	// }

	public void setChildrenArray(int[] childrenArray) {
		this.childrenArray = childrenArray;
	}

	public void setConceptID(String nodeCUI) {
		this.nodeCUI = nodeCUI;
	}

	public void setDepth(short depth) {
		this.depth = depth;
	}

	public void setIntrinsicInfoContent(double intrinsicInfoContent) {
		this.intrinsicInfoContent = intrinsicInfoContent;
	}

	public void setNodeIndex(int nodeIndex) {
		this.nodeIndex = nodeIndex;
	}

	public void setParentsArray(int[] parentsArray) {
		this.parentsArray = parentsArray;
	}

	@Override
	public String toString() {
		return "ConcRel [nodeCUI=" + nodeCUI + "]";
	}

	/**
	 * serialize parent/children concept ids, not the objects
	 */
	private void writeObject(java.io.ObjectOutputStream out)
			throws java.io.IOException {
		out.writeObject(nodeCUI);
		out.writeInt(this.nodeIndex);
		out.writeDouble(this.intrinsicInfoContent);
		out.writeShort(this.depth);
		if (parentsArray == null) {
			parentsArray = new int[parents.size()];
			int i = 0;
			for (ConcRel c : parents)
				parentsArray[i++] = c.getNodeIndex();
		}
		if (childrenArray == null) {
			childrenArray = new int[children.size()];
			int i = 0;
			for (ConcRel c : children)
				childrenArray[i++] = c.getNodeIndex();
		}

		out.writeObject(parentsArray);
		out.writeObject(childrenArray);
		parentsArray = null;
		childrenArray = null;
	}

	// public static void main(String[] args) {
	// int c1 = 18563; // 4903;
	// int c2 = 18670; // 175695;
	//
	// ConcRel r1 = MetaDB.concRelDB.cuiRelDB.get(c1);
	// ConcRel r2 = MetaDB.concRelDB.cuiRelDB.get(c2);
	// if (r1 == null)
	// System.out.println("No rel for " + c1);
	// if (r2 == null)
	// System.out.println("No rel for " + c2);
	//
	// if (r1 == null || r2 == null)
	// return;
	//
	// Vector<Vector<ConcRel>> allPaths1 = new Vector<Vector<ConcRel>>();
	// Vector<Vector<ConcRel>> allPaths2 = new Vector<Vector<ConcRel>>();
	//
	// r1.getPath(null, allPaths1, 0, 1000);
	// r2.getPath(null, allPaths2, 0, 1000);
	//
	// int i = 0;
	// System.out.println("***Paths for " + c1);
	// i = 0;
	// for (Vector<ConcRel> vc : allPaths1) {
	// System.out.print("#P" + (i++) + ": ");
	// i++;
	// for (ConcRel cr : vc) {
	// System.out.print("->" + cr.nodeCUI);
	// }
	// System.out.println("");
	// }
	//
	// System.out.println("***Paths for " + c2);
	// i = 0;
	// for (Vector<ConcRel> vc : allPaths2) {
	// System.out.print("##P" + (i++) + ": ");
	// for (ConcRel cr : vc) {
	// System.out.print("->" + cr.nodeCUI);
	// }
	// System.out.println("");
	// }
	//
	// ObjPair<ConcRel, Integer> obp = getLeastCommonConcept(allPaths1,
	// allPaths2);
	// System.out.println("Common concept :"
	// + (obp.v1 == null ? "none" : obp.v1.nodeCUI));
	// System.out.println("dist: " + obp.v2);
	//
	// obp = getLeastCommonConcept(r1, r2);
	// System.out.println("Common concept2 :"
	// + (obp.v1 == null ? "none" : obp.v1.nodeCUI));
	// System.out.println("dist: " + obp.v2);
	//
	// }
}
