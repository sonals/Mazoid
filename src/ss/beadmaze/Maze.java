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
import android.os.Bundle;

public class Maze {
	public enum Direction {
		   EAST,
		   WEST,
		   NORTH,
		   SOUTH,
		   NONE,
		   ERROR
	}
	
	public enum Orientation {
		X_AXIS,
		Y_AXIS,
		NONE,
		ERROR
	}
	
	private Bead bead;
	private Edge edge;
	private Vertex endVertex;
	private Vertex startVertex;
	private int height;
	private int width;
	private int beadVertexStickiness = 5;
	
	private String beadVertex1 = "beadVertex1";
	private String beadVertex2 = "beadVertex2";
	private String beadLocX = "beadLocX";
	private String beadLocY = "beadLocY";
	
	/*
	 * Utility function 
	 */
	
	public static Orientation getOpposite(Orientation orient) {
		Orientation result = Orientation.ERROR;
		switch (orient) {
		case X_AXIS:
			result = Orientation.Y_AXIS;
			break;
		case Y_AXIS:
			result = Orientation.X_AXIS;
			break;
		case NONE:
			result = Orientation.NONE;
			break;
		default:
			break;
		}
		return result;
	}
	
	/*
	 * Call this when the Bead is at the start location. Get the start
	 * location from the bead itself
	 */
	public Maze(Bead b, Edge e, Vertex ev, int h, int w, int stickiness) {
		bead = b;
		edge = e;
		startVertex = bead.getVertex1();
		endVertex = ev;
		height = h;
		width = w;
		beadVertexStickiness = stickiness;
	}
	
	/*
	 * Call this when the Bead is at some random location. 
	 */
	public Maze(Bead b, Edge e, Vertex sv, Vertex ev, int h, int w, int stickiness) {
		bead = b;
		edge = e;
		startVertex = sv;
		endVertex = ev;
		height = h;
		width = w;
		beadVertexStickiness = stickiness;
	}
	
	public Location getBeadLocation() {
		return bead.getCurrentLocation();
	}
	
	Edge getEdge() {
		return edge;
	}
	
	Bead getBead() {
		return bead;
	}
	
	public Vertex getStartVertex() {
		return startVertex;
	}
	
	public Vertex getEndVertex() {
		return endVertex;
	}

	public int moveBead(int total, Orientation orient) {
            int result = bead.move(total, orient, beadVertexStickiness);
            if (result == 0)
                return 0;
            
            if (bead.getVertex1() != bead.getVertex2())
                return 1;
            
            if (bead.getVertex1() != endVertex) 
                return 1;
            // Reached end
			return -1;
	}

	public void resetBead() { 
        bead.move(startVertex);
	}
	
	public int getHeight() {
		return height;
	}

	public int getWidth() {
		return width;
	}
	
	public int getVertexCount() {
		return edge.getVertexCount();
	}
	
	public Vertex getVertex(int id) {
		return edge.getVertex(id);		
	}
	
	public Vertex findVertex(Vertex node, Direction dir) {
		return edge.findVertex(node, dir);		
	}

	public boolean saveState(Bundle stateBundle, FileOutputStream fileStream) {
		stateBundle.putInt(beadVertex1, bead.getVertex1().getIndex());
		stateBundle.putInt(beadVertex2, bead.getVertex2().getIndex());
		stateBundle.putInt(beadLocX, bead.getCurrentLocation().getX());
		stateBundle.putInt(beadLocY, bead.getCurrentLocation().getY());
		XMLView xv = new XMLView(this);
		return xv.print(fileStream);
	}
	
	public void restoreState(Bundle stateBundle) {
		int verId1 = stateBundle.getInt(beadVertex1);
		int verId2 = stateBundle.getInt(beadVertex2);
		int locX = stateBundle.getInt(beadLocX);
		int locY = stateBundle.getInt(beadLocY);
		bead.move(getVertex(verId1), getVertex(verId2), new Location(locX, locY));
	}
	
	public boolean isBeadVirtexStickinessHigh() {
		return (beadVertexStickiness == 5);
	}
	
	public void toggleBeadVirtexStickiness() {
		if (beadVertexStickiness == 5) {
			beadVertexStickiness = 1;
		}
		else {
			beadVertexStickiness = 5;
		}
	}
}


