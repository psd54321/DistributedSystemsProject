/**
 * 
 */
package kvstore;

import java.util.Iterator;
import java.util.LinkedList;

public class Graph {
	private LinkedList<Integer> adjList[];
	private int NumVertices;

	Graph(int v) {
		NumVertices = v;
		adjList = new LinkedList[v];
		for (int i = 0; i < v; i++) {
			adjList[i] = new LinkedList();
		}
	}

	void addEdge(int v, int e) {
		boolean flag = false;
		for (int i = 0; i < adjList[v].size(); i++) {
			if (adjList[v].get(i) == e) {
				flag = true;
				break;
			}
		}
		if (!flag) {
			adjList[v].add(e);
		}
	}

	boolean isReachable(int s, int d) {
		LinkedList<Integer> temp;
		boolean visited[] = new boolean[NumVertices];
		LinkedList<Integer> queue = new LinkedList<Integer>();
		visited[s] = true;
		queue.add(s);
		Iterator<Integer> i;
		while (queue.size() != 0) {
			s = queue.poll();
			int n;
			i = adjList[s].listIterator();
			while (i.hasNext()) {
				n = i.next();
				if (n == d) {
					return true;
				}
				if (!visited[n]) {
					visited[n] = true;
					queue.add(n);
				}
			}
		}
		return false;
	}

	boolean isCyclic() {
		boolean visited[] = new boolean[NumVertices];
		boolean recursionStack[] = new boolean[NumVertices];
		for (int i = 0; i < NumVertices; i++) {
			if (isCyclicUtil(i, visited, recursionStack)) {
				return true;
			}
		}
		return false;
	}

	boolean isCyclicUtil(int v, boolean[] visited, boolean[] recursionStack) {
		if (visited[v] == false) {
			visited[v] = true;
			recursionStack[v] = true;

			Iterator<Integer> i;
			i = adjList[v].listIterator();
			int n;
			while (i.hasNext()) {
				n = i.next();
				if (!visited[n] && isCyclicUtil(n, visited, recursionStack)) {
					return true;
				} else if (recursionStack[n]) {
					return true;
				}
			}
		}
		recursionStack[v] = false;
		return false;
	}

	public LinkedList<Integer>[] getList() {
		return adjList;
	}

	public void setList(LinkedList<Integer>[] adjList) {
		this.adjList = adjList;
	}

}
