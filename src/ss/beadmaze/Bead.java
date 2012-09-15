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

import ss.beadmaze.Maze.Orientation;
import ss.beadmaze.Maze.Direction;

public class Bead {
	private Edge edge;
	private Location currentLocation;
	private Vertex ver1;
	private Vertex ver2;
	public Bead(Edge e, Vertex v1, int g) {
		edge = e;
		ver1 = v1;
		ver2 = v1;
		// Always have a captive copy of Location object we own since 
		// we call its mutable functions. 
		currentLocation = new Location(v1.getLocation());
		assert(ver1.getDirection(ver2) != Direction.ERROR);
	}

	public Bead(Edge e, Vertex v1, Vertex v2, Location l, int g) {
		edge = e;
		ver1 = v1;
		ver2 = v2;
		// Always have a captive copy of Location object we own since 
		// we call its mutable functions. 
		currentLocation = new Location(l);
	}
	
	public Location getCurrentLocation() {
		return currentLocation;
	}

	Vertex getVertex1() {
		return ver1;
	}

	Vertex getVertex2() {
		return ver2;
	}

	int move(int total, Orientation orient, int beadJumpGap) {
		if (total == 0) {
			return 0;
		}
		assert(orient != Orientation.NONE);
		Direction currentDir = ver1.getDirection(ver2);
		Orientation currentOrient = Edge.getOrientation(currentDir);

		if (currentOrient == Orientation.NONE) {
			assert(ver1 == ver2);
			// We are on a vertex. First determine the direction which we
			// should take. Is there a vertex in that direction? In the
			// following example we are at P2. Hence V1 == P2 and V2 == P2.
			//  [P1]-----[P2]-----[P3]
			//             |
			//             |
			//             |
			//            [P4]
			Direction dir = Direction.NONE;
			if (orient == Orientation.X_AXIS) {
				dir = (total > 0) ? Direction.EAST : Direction.WEST;
			}
			else {
				dir = (total > 0) ? Direction.SOUTH : Direction.NORTH;
			}
			Vertex vnext = edge.findVertex(ver2, dir);
			if (vnext == null) {
				// There is private int trackJumpPlay = 10;no vertex in that direction. e.g. NORTH in our case
				return 0;
			}
			// So we found a vertex in the direction we intend to move. If
			// [1] dir == East; V2 moves to P2
			// [2] dir == West; V1 moves to P1
			// [3] dir == South; V2 moves to P4
			// [4] dir == North; V1 moves to P5 -- assuming there was a P5 in the North
			// which is not true for the case above
			/*
			if ((dir == Direction.EAST) || (dir == Direction.SOUTH)) {
				ver2 = vnext;
			}
			else {
				ver1 = vnext;
			}
			 */
			ver2 = vnext;
		}
		else if (currentOrient != orient) {
			// If we are on X-AXIS direction we can not go on Y-AXIS
			// Only time we can switch axis is when we are on a vertex
			// That case is handled above.
			// Here we handle trackJumpPlay
			Orientation tangent = Maze.getOpposite(currentOrient);
			assert(tangent == orient);
			int dist1 = currentLocation.getDistance(ver1.getLocation(), currentOrient);
			int dist2 = currentLocation.getDistance(ver2.getLocation(), currentOrient);
			if ((Math.abs(dist1) < Math.abs(dist2)) && 
					(Math.abs(dist1) <= beadJumpGap)) {
				// We need to close up to ver1 first and then go in the direction requested
				moveOnAxis(dist1, currentOrient, beadJumpGap);
			}
			else if ((Math.abs(dist2) <= Math.abs(dist1)) && 
					(Math.abs(dist2) <= beadJumpGap)) {
				// We need to close up to ver2 first and then go in the direction requested
				moveOnAxis(dist2, currentOrient, beadJumpGap);
			}
			else {
				return 0;
			}
		}

		return moveOnAxis(total, orient, beadJumpGap); 
	}

	private int moveOnAxis(int total, Orientation orient, int beadJumpGap) {
		Direction targetDir = Direction.NONE;
		if (orient == Orientation.X_AXIS) {
			targetDir = (total > 0) ? Direction.EAST : Direction.WEST;
		}
		else if (orient == Orientation.Y_AXIS) {
			targetDir = (total > 0) ? Direction.SOUTH : Direction.NORTH;
		}
		else {
			assert(false);
			return 0;
		}

		Direction currentDir = ver1.getDirection(ver2);

		if (targetDir != currentDir) {
			Vertex swap = ver1;
			ver1 = ver2;
			ver2 = swap;
		}		

		int dist = currentLocation.getDistance(ver2.getLocation(), orient);

		if (Math.abs(total) < Math.abs(dist)) {
			currentLocation.moveBy(total, orient);
			return total;
		}
		else if (Math.abs(total) > Math.abs(dist)) {
			currentLocation.moveBy(dist, orient);
			ver1 = ver2;
			int result = dist;
			result += move(total - dist, orient, beadJumpGap);
			return result;
		}
		else {
			assert(total == dist); 
			currentLocation.moveBy(dist, orient);
			ver1 = ver2;
			return dist;
		}
	}

	void move(Vertex vertex) {
		ver1 = vertex;
		ver2 = vertex;
		// Always have a captive copy of Location object we own since 
		// we call its mutable functions. 
		currentLocation = new Location(ver1.getLocation());
	}
	
	void move(Vertex vertex1, Vertex vertex2, Location loc) {
		ver1 = vertex1;
		ver2 = vertex2;
		// Always have a captive copy of Location object we own since 
		// we call its mutable functions. 
		currentLocation = new Location(loc);
	}
}
