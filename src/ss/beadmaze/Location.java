package ss.beadmaze;

import ss.beadmaze.Maze.Direction;
import ss.beadmaze.Maze.Orientation;

public class Location {

	private	int x;
	private int y; 

	public Location(int xx, int yy) {
		x = xx;
		y = yy;
	}
	
	public Location(Location loc) {
		x = loc.x;
		y = loc.y;
	}
	
	public int getX() {
		return x;
	}
	
	public int getY() {
		return y;
	}
	
	public void moveBy(int val, Orientation orient) {
		if (orient == Orientation.X_AXIS) {
			moveXby(val);
		}
		else if (orient == Orientation.Y_AXIS) {
			moveYby(val);
		}
		else {
			assert(false);
		}
	}
	
	private void moveXby(int val) {
		x += val;
	}
	
	private void moveYby(int val) {
		y += val;
	}
	
	public int getDistance(Location target, Orientation orient) {
		if (orient == Orientation.X_AXIS) {
			return getXdistance(target);
		}
		else if (orient == Orientation.Y_AXIS) {
			return getYdistance(target);
		}
		else {
			assert(false);
			return 0;
		}
		
	}
	
	public Direction getDirection(Location target) {
		if ((target.getX() > getX()) && 
			(target.getY() == getY())) {
			return Direction.EAST;
		}
		else if ((target.getX() < getX()) &&
				 (target.getY() == getY())) {
			return Direction.WEST;
		}
		else if ((target.getY() < getY()) &&
				 (target.getX() == getX())) {
			return Direction.NORTH;
		}
		else if ((target.getY() > getY()) &&
				 (target.getX() == getX())) {
			return Direction.SOUTH;
		}
		else if ((target.getY() == getY()) &&
				 (target.getX() == getX())) {
			return Direction.NONE;	
		}
		else {
			return Direction.ERROR; 
		}
	}
	
	private int getXdistance(Location target) {
		return (target.x - x);
	}
	
	private int getYdistance(Location target) {
		return (target.y - y);
	}
}
