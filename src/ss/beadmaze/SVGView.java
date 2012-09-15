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
import java.io.FileWriter;
import java.io.IOException;

import ss.beadmaze.Maze.Direction;

public class SVGView {
	private String file;
	private Maze maze;
	private int tabCount;
	private FileWriter fileStream;
	private final int beadRadius = 10;
	
	public SVGView(String f, Maze m) {
		file = f;
		maze = m;
		tabCount = 0;
	}
	
	public boolean print() {
	 	try {
			fileStream = new FileWriter(file);
			print(maze);
			fileStream.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	private void print(Maze maze) throws IOException {
		printTab();
		fileStream.write("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
		fileStream.write("<!DOCTYPE svg PUBLIC \"-//W3C//DTD SVG 1.1//EN\" \"http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd\">\n");	
		//fileStream.write("<svg width=\"100%\" height=\"100%\" version=\"1.1\" xmlns=\"http://www.w3.org/2000/svg\">\n");
		fileStream.write("<svg");
		printAttribute("width", maze.getWidth());
		printAttribute("height", maze.getHeight());
		printAttribute("viewBox", "0 0 " + Integer.toString(maze.getWidth()) + " " + Integer.toString(maze.getHeight()));
		fileStream.write(">\n");
		tabCount++;
		printTab();
		fileStream.write("<rect");
		printAttribute("x", 0);
		printAttribute("y", 0);
		printAttribute("width", maze.getWidth());
		printAttribute("height", maze.getHeight());
		printStyleAttribute("green", "seagreen");
		fileStream.write("/>\n");
		print(maze.getEdge());
		print(maze.getBead());
		tabCount--;
		printTab();
		fileStream.write("</svg>\n");
	}
	// nandita 452-1471
	private void print(Edge edge) throws IOException {
		int vertexCount = edge.getVertexCount();
		printTab();
		fileStream.write("<g");
		printAttribute("id", "path");
		printStyleAttribute(null, "orange");
		fileStream.write(">\n");
		tabCount++;
		printTab();
		fileStream.write("<desc>Paths of the Maze</desc>\n");
		for (int i = 0; i< vertexCount; i++) {
			printLinks(edge, edge.getVertex(i));
		}
		tabCount--;
		printTab();
		fileStream.write("</g>\n");
		/*
		printTab();
		fileStream.write("<g");
		printAttribute("id", "junctions");
		printStyleAttribute("pink", "pink", 1);
		fileStream.write(">\n");
		tabCount++;
		printTab();
		fileStream.write("<desc>Junctions of the Maze</desc>\n");
		for (int i = 0; i< vertexCount; i++) {
			print(edge.getVertex(i));
		}
		tabCount--;
		printTab();
		fileStream.write("</g>\n");
		tabCount--;
		*/
	}
	
	private void printLinks(Edge edge, Vertex junction) throws IOException {
		printLinks(edge, junction, Direction.EAST);
		printLinks(edge, junction, Direction.SOUTH);
	}
	
	private void printLinks(Edge edge, Vertex junction, Direction dir) throws IOException {
		Vertex v = edge.findVertex(junction, dir);	
		if (v == null) {
			return;
		}
		
		int x = junction.getLocation().getX() - beadRadius;
		int y = junction.getLocation().getY() - beadRadius;
		int width, height;
		
		if (dir == Direction.EAST) {
			width = v.getLocation().getX() - x + beadRadius ;
			height = 2 * beadRadius;
		}
		else {
			assert(dir == Direction.SOUTH);
			width = 2 * beadRadius;
			height = v.getLocation().getY() - y + beadRadius;
		}
		
		printTab();
		fileStream.write("<rect");
		printAttribute("x", x);
		printAttribute("y", y);
		printAttribute("width", width);
		printAttribute("height", height);
		fileStream.write("/>\n");
	}
	
	private void print(Bead bead) throws IOException {
		tabCount++;
		printTab();
		fileStream.write("<circle");
		printAttribute("cx", bead.getCurrentLocation().getX());
		printAttribute("cy", bead.getCurrentLocation().getY());
		printAttribute("r", beadRadius);
		printStyleAttribute("pink", "red", 2);
		fileStream.write("/>\n");
		tabCount--;
	}
	
	private void printTab() throws IOException {
		for (int i=0; i<tabCount; i++) {
			fileStream.write("    ");
		}
	}
	
	private void printAttribute(String name, int value) throws IOException {
		fileStream.write(" " + name + "=\"" + value + "\"");
	}
	
	private void printAttribute(String name, String value) throws IOException {
		fileStream.write(" " + name + "=\"" + value + "\"");
	}
	
	private void printStyleAttribute(String stroke, String fill, int strokeWidth, float strokeOpacity) throws IOException {
		String value = new String();
		if ((stroke != null) && (stroke.length() > 0)) {
			value = "stroke: " + stroke;
		}
		if (strokeWidth != -1) {
			value = value.concat("; stroke-width: " + strokeWidth);
		}
		if ((fill != null) && (fill.length() > 0)) {			
			value = value.concat("; fill: " + fill);
		}
		if (strokeOpacity != -1.0) {
			value = value.concat("; stroke-opacity: " + strokeOpacity);
		}
		printAttribute("style", value);
	}
	
	private void printStyleAttribute(String stroke, String fill) throws IOException {
		printStyleAttribute(stroke, fill, -1, -1.0f);	
	}
	
	private void printStyleAttribute(String stroke, String fill, int strokeWidth) throws IOException {
		printStyleAttribute(stroke, fill, strokeWidth, -1.0f);	
	}
}


