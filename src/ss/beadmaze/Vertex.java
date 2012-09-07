package ss.beadmaze;

import ss.beadmaze.Maze.Direction;

public class Vertex {	
	private int index;
	private Location location;

	public Vertex(Location loc, int id) {
		location = loc;
		index = id;
	}
	
	public Location getLocation() {
		return location;
	}
	
	public int getIndex() {
		return index;
	}
	
	public Direction getDirection(Vertex target) {
		Direction dir = location.getDirection(target.getLocation());
		assert(dir != Direction.ERROR);
		return dir;
	}
}
