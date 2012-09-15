/*
 * Copyright (c) 2010, 2012, Sonal Santan < sonal DOT santan AT gmail DOT com >
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */


package ss.beadmaze;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Stack;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class AutoMazeBuilder {
	private Vertex beadStart = null;
	private Vertex beadEnd = null;
	private int mHeight = 0;
	private int mWidth = 0;
	private int mPathWidth = 0;
	private int mXCount = 0;
	private int mYCount = 0;
	private int mLevelStartCount = 0;
	private int mLevelDeltaCount = 0;
	private final int mVisitedMax = 1;
	private Random mRandGen = null;
	private VertexContainer junctions[] = null;
	
	private class VertexContainer {
		Vertex vertex = null;
		List<Integer> edges = null;
		int mVisited = 0;
		int distance = 0x0ffffff;
		public VertexContainer(Vertex ver) {
			vertex = ver;
			edges = new LinkedList<Integer>();
		}
	}
	
	public AutoMazeBuilder(int w, int h, int p) {
		mWidth = w;
		mHeight = h;
		mPathWidth = p;
		mRandGen = new Random();
	}
	
	public Maze build(String xmlIn, int level) {
		FileInputStream f = null;
		try {
			f = new FileInputStream(xmlIn);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		return build(f, level);
	}
	
	public Maze build(InputStream stream, int level) {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = null;
		Document doc = null;
		try {
			builder = factory.newDocumentBuilder();
			doc = builder.parse(stream);
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		if (!traverseXML(doc, level)) {
			return null;
		}
		
		if (!createVertices(level)) {
			return null;
		}
		
		generateRandomLinks();
		
		Vertex arr[] = new Vertex[junctions.length];
		for (int i=0; i<arr.length; i++) {
			arr[i] = junctions[i].vertex;
		}
		
		Edge edge = new Edge(arr);
		for (int i=0; i<arr.length; i++) {
			for (Iterator<Integer> j = junctions[i].edges.iterator(); j.hasNext();) { 
				Vertex ver = junctions[j.next()].vertex;
				edge.addLinks(junctions[i].vertex, ver);
			}	
		}
		Bead bead = new Bead(edge, beadStart, 10);
		return new Maze(bead, edge, beadEnd, mHeight, mWidth, mPathWidth / 2); 
	}

	private boolean traverseXML(Node node, int level) {
		boolean result = true;
	    switch (node.getNodeType()) {
	    case Node.DOCUMENT_NODE:
	    case Node.TEXT_NODE:
        case Node.COMMENT_NODE:
	    	break;
        case Node.ELEMENT_NODE:
            result = processElement((Element)node, level);
            break;
        case Node.ATTRIBUTE_NODE:
        case Node.CDATA_SECTION_NODE:
        case Node.DOCUMENT_FRAGMENT_NODE:
        case Node.DOCUMENT_TYPE_NODE:
        case Node.ENTITY_NODE:
        case Node.ENTITY_REFERENCE_NODE:
        case Node.NOTATION_NODE:
        case Node.PROCESSING_INSTRUCTION_NODE:
        default:
            assert (false) : node.toString();
            result = false;
        	break;            
        }
        if (!result) {
        	return false;
        }
        
        NodeList clist = node.getChildNodes();
        for (int i=0; i<clist.getLength(); i++)
        {
			result = result && traverseXML(clist.item(i), level);
        }
		return result;
	}
	
    private boolean processElement(Element n, int level) {
    	boolean result = true;
    	if (n.getNodeName().equalsIgnoreCase("levelstart")) {
    		mLevelStartCount = decodeInteger(n, "count");
    	}
    	else if (n.getNodeName().equalsIgnoreCase("leveldelta")) {
    		mLevelDeltaCount = decodeInteger(n, "count");
    	}
    	return result;
    }

    private Integer decodeInteger(Element element, String name) {
    	String idStr = element.getAttribute(name);
    	if ((idStr == null) || (idStr.length() == 0)) {
    		return null;
    	}
    	idStr.trim();
    	if ((idStr.charAt(0) == '"') && (idStr.charAt(idStr.length() - 1) == '"')) {
    		idStr = idStr.substring(1, idStr.length() -2);
    	}
    	return Integer.parseInt(idStr);	
    }
    
    private boolean createVertices(int level) {
    	// Note: ((mXCount - 1) * cellSize * (mYCount - 1) * cellSize) == totalUseableArea
    	int cellSize = (int)Math.sqrt(((mHeight -  2 * mPathWidth) * (mWidth - 2 * mPathWidth)) / 
    									(mLevelStartCount + level * mLevelDeltaCount));
		mXCount = mWidth/cellSize + 1;
		mYCount = mHeight/cellSize + 1;
    	junctions = new VertexContainer[mYCount * mXCount];
		for (int y = 0; y < mYCount; y++) {
			for (int x = 0; x < mXCount; x++) {
				int id = y * mXCount + x;
				// Center the vertices leaving mPathWidth space in all direction
				Location loc = new Location(mPathWidth + (x * (mWidth - 2 * mPathWidth))/(mXCount - 1), 
											mPathWidth + (y * (mHeight - 2 * mPathWidth))/(mYCount - 1));
				junctions[id] = new VertexContainer(new Vertex(loc, id));		
			}
		}
		return true;
	}
	
	private void generateRandomLinks() {
		beadStart = junctions[0].vertex;
    	beadEnd = junctions[0].vertex;
		Stack<Integer> stack = new Stack<Integer>();
		stack.push(0);
		junctions[0].distance = 0;
		visitVertexIterative(stack);
		for (int i = 0; i < junctions.length; i++) {
			if (junctions[i].mVisited == 0)
				continue;
			if (junctions[i].distance > junctions[beadEnd.getIndex()].distance) {
				// Update the farthest known vertex
				beadEnd = junctions[i].vertex;
			}
		}	
	}
	
	@SuppressWarnings("unused")
	private void visitVertexRecursive(Stack<Integer> stack) {
		junctions[stack.peek()].mVisited++;
		int neighbor = selectNeighbor(stack.peek());
		if (neighbor > 0) {
			// Found an unvisited neighbor.  
			junctions[stack.peek()].edges.add(neighbor);
			int currentDistance = junctions[stack.peek()].distance;
			stack.push(neighbor);
			if (junctions[stack.peek()].distance > currentDistance + 1) {
				junctions[stack.peek()].distance = currentDistance + 1;
			}
			visitVertexRecursive(stack);
		}
		else {
			stack.pop();
			if (stack.isEmpty())
				return;
			visitVertexRecursive(stack);
		}
	}

	/*
	 * This function has to be iterative, recursive version overflows stack
	 */
	private void visitVertexIterative(Stack<Integer> stack) {
		while (!stack.isEmpty()) {
			junctions[stack.peek()].mVisited++;
			int neighbor = selectNeighbor(stack.peek());
			if (neighbor > 0) {
				// Found an unvisited neighbor.  
				junctions[stack.peek()].edges.add(neighbor);
				int currentDistance = junctions[stack.peek()].distance;
				stack.push(neighbor);
				if (junctions[stack.peek()].distance > currentDistance + 1) {
					junctions[stack.peek()].distance = currentDistance + 1;
				}
			}
			else {
				stack.pop();	
			}
		}
	}
	
	private int selectNeighbor(int id) {
		// Get the co-ordinates of this id
		int x = id % mXCount;
		int y = id / mXCount;
		// Next populate an array of it valid neighbors
		int neighbor[] = {-1, -1, -1, -1}; // E, W, N, S
		if ((x + 1) < mXCount) {
			neighbor[0] = y * mXCount + x + 1;
		}
		if ((x - 1) >= 0) {
			neighbor[1] = y * mXCount + x - 1;
		}
		if ((y - 1) >= 0) {
			neighbor[2] = (y - 1) * mXCount + x;
		}
		if ((y + 1) < mYCount) {
			neighbor[3] = (y + 1) * mXCount + x;
		}
		
		// Now randomly select one neighbor	
		int validNeighborCount = 0;
		for (int i = 0; i < neighbor.length; i++) {
			if (neighbor[i] < 0) // happens for vertices on the boundary 
				continue;
			if (junctions[neighbor[i]].mVisited >= mVisitedMax) {
				neighbor[i] = -1;
				continue;
			}
			validNeighborCount++;
		}
		
		if (validNeighborCount == 0)
			return -1;
	
		// Collect the valid ids in neighbor array and pack them up in the front
		int j = 0;
		for (int i = 0; i < neighbor.length; i++) {
			if (neighbor[i] < 0)
				continue;
			neighbor[j++] = neighbor[i];
		}
		
		if (validNeighborCount == 1)
			return neighbor[0];
		
		return neighbor[mRandGen.nextInt(validNeighborCount)];
	}	
}
