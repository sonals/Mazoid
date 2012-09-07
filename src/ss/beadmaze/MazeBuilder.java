package ss.beadmaze;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import ss.beadmaze.Maze.Direction;

public class MazeBuilder {
	private int gameStart = 0;
	private int gameEnd = 0;
	private Document doc = null;
	private int height = 0;
	private int width = 0;
	private int mPathWidth = 0;
	private int xmlHeight = 0;
	private int xmlWidth = 0;
	
	private class VertexContainer {
		Vertex vertex;
		List<Integer> edges;
		public VertexContainer(Vertex ver) {
			vertex = ver;
			edges = new LinkedList<Integer>();
		}
	}
	
	private VertexContainer junctions[] = null;
	private Integer beadV1;
	private Integer beadV2;
	private Location beadLoc;
	
	public MazeBuilder(int w, int h, int p) {
		width = w;
		height = h;
		mPathWidth = p;
	}
	
	public Maze build(String xmlIn) {
		FileInputStream f = null;
		try {
			f = new FileInputStream(xmlIn);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		return build(f);
	}
	
	public Maze build(InputStream stream) {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = null;
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
		if (!traverseXML(doc)) {
			return null;
		}
		
		if ((xmlHeight <= 0) || (xmlWidth <= 0)) {
			return null;
		}
		
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
		Bead bead = new Bead(edge, junctions[beadV1].vertex, junctions[beadV2].vertex, beadLoc, 10);
		return new Maze(bead, edge, junctions[gameStart].vertex, junctions[gameEnd].vertex, 
				        height, width, mPathWidth / 2); 
	}
	
	private boolean traverseXML(Node node) {
		boolean result = true;
	    switch (node.getNodeType()) {
	    case Node.DOCUMENT_NODE:
	    case Node.TEXT_NODE:
        case Node.COMMENT_NODE:
	    	break;
        case Node.ELEMENT_NODE:
            result = processElement((Element)node);
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
			result = result && traverseXML(clist.item(i));
        }
		return result;
	}
	
    private boolean processElement(Element n) {
    	boolean result = true;
    	if (n.getNodeName().equalsIgnoreCase("maze")) {
    		int count = decodeInteger(n, "vertexcount");
    		junctions = new VertexContainer[count];
    		xmlHeight = decodeInteger(n, "height");
    		xmlWidth = decodeInteger(n, "width");
    		gameStart = decodeInteger(n, "start");
    		gameEnd = decodeInteger(n, "end");
        }
        else if (n.getNodeName().equalsIgnoreCase("bead")) {
            result = processBead(n);
        }  
        else if (n.getNodeName().equalsIgnoreCase("vertex")) {
            result = processVertex(n);
        }      
    	return result;
    }

    private boolean processBead(Element element) {
    	beadV1 = decodeInteger(element, "v1");
    	beadV2 = decodeInteger(element, "v2");
    	beadLoc = decodeLocation(element);
    	return true;
    }
    
    private boolean processVertex(Element element) {
    	int id = decodeInteger(element, "id");
    	Location loc = decodeLocation(element);
    	if (junctions[id] != null) {
    		return false;
    	}
    	junctions[id] = new VertexContainer(new Vertex(loc, id));
    	for (Direction dir : EnumSet.range(Direction.EAST, Direction.NONE)) {
    		Integer i = decodeInteger(element, dir.toString());
    		if (i != null) {
    			junctions[id].edges.add(i);
    		}
    	}
    	return true;
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
    
    private Location decodeLocation(Element element) {
    	String locStr = element.getAttribute("loc");
    	if (locStr == null) {
    		return null;
    	}
    	int spliceIndex = locStr.indexOf(',');
    	String xLoc = locStr.substring(0, spliceIndex).trim();
    	String yLoc = locStr.substring(spliceIndex + 1).trim();
    	int x = (Integer.parseInt(xLoc) * width) / xmlWidth;
    	int y = (Integer.parseInt(yLoc) * height) / xmlHeight;
    	return new Location(x, y);
    }
}
