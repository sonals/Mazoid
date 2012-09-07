package ss.beadmaze;

import ss.beadmaze.Maze.Direction;
import ss.beadmaze.Maze.Orientation;

import java.util.Iterator;
import java.util.List;
import java.util.LinkedList;

public class Edge {
	
	// TODO: Replace the LinkedList below with Array like one below
	//private int arr[] = new int[Direction.NONE.ordinal()];
	private class VertexContainer {  
		Vertex vertex;
		List<Integer> edges;
		public VertexContainer(Vertex ver) {
			vertex = ver;
			edges = new LinkedList<Integer>();    
		}
	}

	/*
	 * The graph is stored as an adjacency list here
	 */
	private VertexContainer vertexTable[];

	public Vertex getVertex(int id) {
		return vertexTable[id].vertex;
	}
	
	public int getVertexCount() {
		return vertexTable.length;
	}
	
	public Edge(Vertex junctions[]) {
		vertexTable = new VertexContainer[junctions.length];
		for (int i = 0; i< junctions.length; i++) {
			assert(vertexTable[junctions[i].getIndex()] == null);
			vertexTable[junctions[i].getIndex()] = new VertexContainer(junctions[i]);
		}
	}
	
	public void addLinks(Vertex v1, Vertex v2) {
		Direction dir = v1.getDirection(v2);
		assert((dir != Direction.ERROR) && ( dir != Direction.NONE));
		if (!vertexTable[v1.getIndex()].edges.contains(v2.getIndex()))
			vertexTable[v1.getIndex()].edges.add(v2.getIndex());
		if (!vertexTable[v2.getIndex()].edges.contains(v1.getIndex()))
			vertexTable[v2.getIndex()].edges.add(v1.getIndex());
	}
	
	public static Orientation getOrientation(Direction dir) {
		switch (dir) {
		case EAST:
		case WEST:
			return Orientation.X_AXIS;
		case NORTH:
		case SOUTH:
			return Orientation.Y_AXIS;
		case NONE:
			return Orientation.NONE;
		default:
			return Orientation.ERROR;
		}
	}
	
	public Vertex findVertex(Vertex node, Direction dir) {
		for (Iterator<Integer> j = vertexTable[node.getIndex()].edges.iterator(); j.hasNext();) { 
			Vertex ver = vertexTable[j.next()].vertex;
			if (node.getDirection(ver) == dir) {
				return ver;
			}
		}
		return null;		
	}
}
