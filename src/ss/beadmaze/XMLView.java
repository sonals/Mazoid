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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.EnumSet;

import ss.beadmaze.Maze.Direction;

public class XMLView {
	private Maze maze = null;
	private int tabCount = 0;
	private PrintWriter fileStream = null;
	
	public XMLView(Maze m) {
		maze = m;
	}
	
	public boolean print(FileOutputStream f) {
	 	try {
	 		fileStream = new PrintWriter(f);
			print(maze);
			fileStream.close();
		} catch (IOException e) {
			return false;
		}
		return true;
	}
	
	private void print(Maze maze) throws IOException {
		printTab(); 
		fileStream.write("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
		fileStream.write("<maze");
		printAttribute("vertexcount", maze.getEdge().getVertexCount());
		printAttribute("width", maze.getWidth());
		printAttribute("height", maze.getHeight());
		printAttribute("start", maze.getStartVertex().getIndex());
		printAttribute("end", maze.getEndVertex().getIndex());
		fileStream.write(">\n");	
		print(maze.getEdge());
		print(maze.getBead());
		printTab();
		fileStream.write("</maze>\n");
	}
	// nandita 452-1471
	private void print(Vertex junction) throws IOException {
		tabCount++;
		printTab();
		fileStream.write("<vertex");
		printAttribute("id", junction.getIndex());
		print(junction.getLocation());
		printLinks(maze.getEdge(), junction);	
		fileStream.write("/>\n");
		tabCount--;
	}
	
	private void print(Edge edge) throws IOException {
		int vertexCount = edge.getVertexCount();
		for (int i = 0; i< vertexCount; i++) {
			print(edge.getVertex(i));
		}
	}
	
	private void printLinks(Edge edge, Vertex ver) throws IOException {
		tabCount++;
		for (Direction dir : EnumSet.range(Direction.EAST, Direction.NONE)) {
			Vertex v = edge.findVertex(ver, dir);
			if (v == null) {
				continue;
			}
			printAttribute(dir.toString(), v.getIndex());
		}
		tabCount--;
	}
	
	private void print(Bead bead) throws IOException {
		tabCount++;
		printTab();
		fileStream.write("<bead ");
		print(bead.getCurrentLocation());
		printAttribute("v1", bead.getVertex1().getIndex());
		printAttribute("v2", bead.getVertex2().getIndex());
		fileStream.write("/>\n");
		tabCount--;
	}
	
	private void printTab() throws IOException {
		for (int i=0; i<tabCount; i++) {
			fileStream.write("    ");
		}
	}
	
	private void print(Location loc) throws IOException {
		fileStream.write("loc=\"" + loc.getX() + ", " + loc.getY() + "\"");
	}
	
	private void printAttribute(String name, int value) throws IOException {
		fileStream.write(" " + name + "=\"" + value + "\"");
	}
}
