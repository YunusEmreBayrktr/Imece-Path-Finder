import java.awt.Color;
import java.awt.Graphics;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Scanner;

public class IMECEPathFinder{
	  public int[][] grid;
	  public int[][] grayscaleMap;
	  public int height, width;
	  public int maxFlyingHeight;
	  public double fuelCostPerUnit, climbingCostPerUnit;

	  
	  
	  public IMECEPathFinder(String filename, int rows, int cols, int maxFlyingHeight, double fuelCostPerUnit, double climbingCostPerUnit){

		  grid = new int[rows][cols];
		  grayscaleMap = new int[rows][cols];
		  this.height = rows;
		  this.width = cols;
		  this.maxFlyingHeight = maxFlyingHeight;
		  this.fuelCostPerUnit = fuelCostPerUnit;
		  this.climbingCostPerUnit = climbingCostPerUnit;

		  try {
			fillGrid(filename);
		  } catch (FileNotFoundException e) {e.printStackTrace();}
	  }
	  
	  
	  
	  public void getGrayscaleMap() {
		  int minValue = Integer.MAX_VALUE;
	      int maxValue = Integer.MIN_VALUE;
	        
	      // Find the minimum and maximum values in the grid
	      for (int[] row : grid) {
	          for (int value : row) {
	              minValue = Math.min(minValue, value);
	              maxValue = Math.max(maxValue, value);
	          }
	      }
	        
	      // Rescale the values in the grid
	      for (int i = 0; i < height; i++) {
	          for (int j = 0; j < width; j++) {
	              int value = grid[i][j];
	              int rescaledValue = (int) (((value - minValue) / (double) (maxValue - minValue)) * 255);
	              grayscaleMap[i][j] = rescaledValue;
	          }
	      }
	        
	      // Write to .dat file
		  try (BufferedWriter writer = new BufferedWriter(new FileWriter("grayscaleMap.dat"))) {
		      for (int i = 0; i < height; i++) {
		          for (int j = 0; j < width; j++) {
		              writer.write(String.valueOf(grayscaleMap[i][j]));
		              if (j != width - 1) {
		                  writer.write(" ");
		              }
		          }
		          writer.newLine();
		      }
		  } catch (IOException e) {
		      e.printStackTrace();
		  }
	  }


	  
	  /**
	   * Draws the grid using the given Graphics object.
	   * Colors should be grayscale values 0-255, scaled based on min/max elevation values in the grid
	   */
	  public void drawGrayscaleMap(Graphics g){
		  for (int i = 0; i < height; i++){
			  for (int j = 0; j < width; j++) {
				  int value = grayscaleMap[i][j];
				  g.setColor(new Color(value, value, value));
				  g.fillRect(j, i, 1, 1);
			  }
		  }
	  }

	  
	  
	/**
	 * Get the most cost-efficient path from the source Point start to the destination Point end
	 * using Dijkstra's algorithm on pixels.
	 * @return the List of Points on the most cost-efficient path from start to end
	 */
	  public List<Point> getMostEfficientPath(Point start, Point end) {
		 List<Point> path = new ArrayList<>();
		  
		 double[][] costGrid = new double[height][width];
		 Point[][] parentGrid = new Point[height][width];
		  
		 // Fill the costs with infinity
		 for (int i = 0; i < height; i++) {
		     Arrays.fill(costGrid[i], Double.POSITIVE_INFINITY);
		 }
		  
		 // Priority queue for min cost point
		 PriorityQueue<Point> queue = new PriorityQueue<>((a, b) -> {
		     double costA = costGrid[a.y][a.x];
		     double costB = costGrid[b.y][b.x];
		     return Double.compare(costA, costB);
		 });
		  
		 // Setting starting point cost as 0
		 costGrid[start.y][start.x] = 0;
		 queue.offer(start);

		 int[][] directions = {
		         {-1, 0},   // West
		         {1, 0},    // East
		         {0, -1},   // North
		         {0, 1},    // South
		         {-1, 1},   // South West
		         {-1, -1},  // North West
		         {1, 1},    // South East
		         {1, -1}    // North East
		 };
		  
		 // Dijkstra's algorithm
		 while (!queue.isEmpty()) {
		     Point current = queue.poll();

		     if (current.x == end.x && current.y == end.y) {
		         break;
		     }

		     for (int[] dir : directions) {
		         int ny = current.y + dir[0];
		         int nx = current.x + dir[1];

		         if (ny >= 0 && ny < height && nx >= 0 && nx < width) {
		             if (grid[ny][nx] <= maxFlyingHeight) {
		                 double newCost = costGrid[current.y][current.x] + calculateCost(current, new Point(nx, ny));

		                 if (newCost < costGrid[ny][nx]) {
		                     costGrid[ny][nx] = newCost;
		                     parentGrid[ny][nx] = current;
		                     queue.offer(new Point(nx, ny));
		                 }
		             }
		         }
		     }
		 }
		  
		 // Retriving the most cost efficient path
		 Point current = end;
		 while (current != null) {
		     path.add(current);
		     current = parentGrid[current.y][current.x];
		 }

		 Collections.reverse(path);
		 return path;
	  }

	  
	
	/**
	 * Calculate the most cost-efficient path from source to destination.
	 * @return the total cost of this most cost-efficient path when traveling from source to destination
	 */
	 public double getMostEfficientPathCost(List<Point> path) {
		 // Total cost of the path
		 double totalCost = 0.0;

		 Point prevPoint = null;
		 for (Point currentPoint : path) {
		     if (prevPoint != null) 
		         totalCost += calculateCost(prevPoint, currentPoint);
		     prevPoint = currentPoint;
		 }
		 
		 return totalCost;
	}


	 
	/**
	 * Draw the most cost-efficient path on top of the grayscale map from source to destination.
	 */
	public void drawMostEfficientPath(Graphics g, List<Point> path){
		g.setColor(new Color(0, 255, 0));
		for (Point point : path) {
			g.fillRect(point.x, point.y, 1, 1);
		}
	}

	
	
	/**
	 * Find an escape path from source towards East such that it has the lowest elevation change.
	 * Choose a forward step out of 3 possible forward locations, using greedy method described in the assignment instructions.
	 * @return the list of Points on the path
	 */
	public List<Point> getLowestElevationEscapePath(Point start){
	    List<Point> pathPointsList = new ArrayList<>();
	    pathPointsList.add(start);

	    int[][] directions = {
	            {0, 1},   // East
	            {-1, 1},  // Northeast
	            {1, 1}    // Southeast
	    };
	    
	    // Greedy algorithm
	    while (start.y < height - 1) {
	        Point next = null;
	        int minElevationChange = Integer.MAX_VALUE;

	        for (int[] dir : directions) {
	            int next_x = start.x + dir[1];
	            int next_y = start.y + dir[0];

	            if (next_x >= 0 && next_x < width) {
	                int elevationChange = Math.abs(grid[next_y][next_x] - grid[start.y][start.x]);

	                if (elevationChange < minElevationChange || (elevationChange == minElevationChange && dir[0] == 0)) {
	                    minElevationChange = elevationChange;
	                    next = new Point(next_x, next_y);
	                }
	            }
	        }

	        if (next == null) {
	            break; // No valid move
	        }

	        pathPointsList.add(next);
	        start = next;
	    }

	    return pathPointsList;
	}

	

	/**
	 * Calculate the escape path from source towards East such that it has the lowest elevation change.
	 * @return the total change in elevation for the entire path
	 */
	public int getLowestElevationEscapePathCost(List<Point> pathPointsList){
		int totalChange = 0;

		for (int i = 0; i < pathPointsList.size() - 1; i++) {
            Point current = pathPointsList.get(i);
            Point next = pathPointsList.get(i + 1);

            int elevationChange = Math.abs(grid[next.y][next.x] - grid[current.y][current.x]);
            totalChange += elevationChange;
        }

		return totalChange;
	}

	

	/**
	 * Draw the escape path from source towards East on top of the grayscale map such that it has the lowest elevation change.
	 */
	public void drawLowestElevationEscapePath(Graphics g, List<Point> pathPointsList){
		g.setColor(new Color(255, 255, 0));
		for (Point point : pathPointsList) {
			g.fillRect(point.x, point.y, 1, 1);
		}
	}
	
	
	
	private double calculateCost(Point start, Point end) {
		//Cost calculation for each step
        int dx = Math.abs(end.x - start.x);
        int dy = Math.abs(end.y - start.y);
        double distance = Math.sqrt(Math.pow(dx, 2) + Math.pow(dy, 2));
        
        int heightImpact = grid[end.y][end.x] - grid[start.y][start.x];
        
        if (heightImpact <= 0)
        	heightImpact = 0;
        
        return (distance * fuelCostPerUnit) + (climbingCostPerUnit * heightImpact );
    }
	
	
	
	private void fillGrid(String fileName) throws FileNotFoundException {
		// Read .dat file
		File file = new File(fileName);
		Scanner scanner = new Scanner(file);
		
		for(int i = 0; i<height ; i++) {
			for (int j = 0; j<width; j++) {
				this.grid[i][j] = scanner.nextInt();
			}
		}
		
		scanner.close();
	}
}
