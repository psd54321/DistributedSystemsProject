/**
 * 
 */
package kvstore;

import java.util.LinkedList;

/**
 * @author Prathamesh
 *
 */
public class Graph {
	public Log log;
	private LinkedList<Log> list[];
	int time;

	Graph(int v) {

		list = new LinkedList[v];
		for (int i = 0; i < v; i++) {
			list[i] = new LinkedList();

		}
	}

	void addEdge(Log v, Log e) {
		boolean flag = false;
		for (int i = 0; i < list[v.getIndex()].size(); i++) {
			if (list[v.getIndex()].get(i).getIndex() == e.getIndex()) {
				flag = true;
				break;
			}
		}
		if (!flag) {
			list[v.getIndex()].add(e);
		}
	}

	public LinkedList<Log>[] getList() {
		return list;
	}

	public void setList(LinkedList<Log>[] list) {
		this.list = list;
	}
	
	
}
