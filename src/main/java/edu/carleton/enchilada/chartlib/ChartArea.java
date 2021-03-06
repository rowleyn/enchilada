/**
 * 
 */
package edu.carleton.enchilada.chartlib;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Line2D;
import java.util.*;

/**
 * @author olsonja
 *
 */
public class ChartArea extends AbstractMetricChartArea {
	protected ArrayList<Dataset> datasets;
	protected boolean drawXAxisAsDateTime;
	protected int barWidth = 3;
	
	/**
	 * 
	 */
	public ChartArea() {
		super();
		drawXAxisAsDateTime = false;
		datasets = new ArrayList<Dataset>();
		// TODO Auto-generated constructor stub
	}
	
	public ChartArea(Dataset dataset) {
		super();
		drawXAxisAsDateTime = false;
		datasets = new ArrayList<Dataset>();
		datasets.add(dataset);
		// TODO Auto-generated constructor stub
	}

	/**
	 * Draws the data, drawing one line for each DataPoint in the Dataset
	 * This has the potential to be slow if there are many lines shorter than a pixel.  But 
	 * this ensures the lines will be drawn in a way that is consistent with the way points are drawn.
	 * If a chart with only lines is wanted, and efficiency is a concern, a LineChartArea should be used.
	 * 
	 * @param g2d
	 */
	protected void drawDataLines(Graphics2D g2d,Dataset dataset){
			if(dataset == null) return;
			Rectangle dataArea = getDataAreaBounds();
			//setup
			Shape oldClip = g2d.getClip();
			Stroke oldStroke = g2d.getStroke();
			g2d.setColor(foregroundColor);
			g2d.clip(dataArea);	//constrains drawing to the data value
			g2d.setStroke(new BasicStroke(1.5f));
			
			//Keep track of the last point, both in pixel units and in chart units
			int lastXCoord = Integer.MIN_VALUE;
			double lastXValue = Double.NEGATIVE_INFINITY;
			double lastYCoord = -999.0;
			int numPoints = 0;
			//Iterate through the whole dataset because you need to draw lines 
			// even if most of the line is off-screen (when you zoom in one one point
			// there are still lines connecting it to its neighbors even if you can't
			// see the neighbors)
			
			SortedSet<DataPoint> drawable = dataset.tailSet(new DataPoint(xAxis.getMin(), 0));
			if(drawable.size()>0){
				Iterator<DataPoint> iterator 
				= drawable.iterator();
			SortedSet<DataPoint> headSet = dataset.headSet(drawable.first());
			DataPoint head = null;
			if(headSet.size()> 0 ){
				head = headSet.last();
			}
			
			while(iterator.hasNext())
			{
				DataPoint curPoint = iterator.next();
				
				double x = curPoint.x, y = curPoint.y;
				
				//if you've finished the drawable points, stop
				if((x >xAxis.getMax())&&(lastXValue >xAxis.getMax()))break;
				
				int xCoord = (int) (dataArea.x+xAxis.relativePosition(x) 
						* dataArea.width);
				double yCoord = (dataArea.y + dataArea.height 
						- (yAxis.relativePosition(y) * dataArea.height));
			
				if(numPoints==0){
					if(head!=null){
					int headxCoord = (int) (dataArea.x+xAxis.relativePosition(head.x) 
							* dataArea.width);
					double headyCoord = (dataArea.y + dataArea.height 
							- (yAxis.relativePosition(head.y) * dataArea.height));
					g2d.draw(new Line2D.Double((double) headxCoord, headyCoord, (double) xCoord, yCoord));
					}
				}else
				if(lastXCoord == xCoord){// &&lastYCoord == yCoord){
					;
				}else{
					// draw the line if either the current point is within the bounds or the last point
					// was within the bounds
					// Note: for the last point you don't need to check the upper bounds because
					// the loop should terminate.
					//if((x >= xAxis.getMin()&&x <=xAxis.getMax())||(lastXValue >= xAxis.getMin()))
							g2d.draw(new Line2D.Double((double) lastXCoord, lastYCoord, (double) xCoord, yCoord));
				}
				numPoints++;
				lastXCoord = xCoord;
				lastYCoord = yCoord; // maybe get rid of
				lastXValue= x;
			}
			}
			if (dataset.first().x < xAxis.getMin()) {
				drawMorePointsIndicator(0, g2d);
			}
			if (dataset.last().x > xAxis.getMax()) {
				drawMorePointsIndicator(1, g2d);
			}
			
			
			//cleanup
			g2d.setClip(oldClip);
			g2d.setStroke(oldStroke);
			
		}

	/**
	 * Draws the a (small) X for each data point per horizontal pixel.
	 * 
	 * @param g2d
	 */
	protected void drawDataPoints(Graphics2D g2d, Dataset dataset) {
		if(dataset == null||dataset.isEmpty()) return;
		Rectangle dataArea = getDataAreaBounds();
		
		boolean drawnMoreLeft = false, drawnMoreRight = false;
		
		int maxX = 0;
		
		//	loops through all data points, drawing each one as
		//  a scatter plot...
		Iterator<DataPoint> iterator = dataset.iterator();
		while(iterator.hasNext())
		{
			DataPoint curPoint = iterator.next();
			
			double x = curPoint.x, y = curPoint.y;
			double pointPos = xAxis.relativePosition(x);
			if (pointPos < 0 && !drawnMoreLeft) {
				drawnMoreLeft = true;
				drawMorePointsIndicator(0, g2d);
			}
			else if (pointPos > 1 && !drawnMoreRight) {
				drawnMoreRight = true;
				drawMorePointsIndicator(1, g2d);
			}else{
				int xCoord = (int) (dataArea.x + xAxis
						.relativePosition(curPoint.x)
						* dataArea.width);
				double yCoord = (dataArea.y + dataArea.height - (yAxis
						.relativePosition(curPoint.y) * dataArea.height));
				drawPoint(g2d, xCoord, yCoord);
			}
			/*} else if (x < 0 && !drawnMoreLeft) {
				drawnMoreLeft = true;
				drawMorePointsIndicator(0, g2d);
				// puts a null bar in the array to hold its place
				// barsTemp[index] = null;
			} else if (!drawnMoreRight) {
				drawnMoreRight = true;
				drawMorePointsIndicator(1, g2d);
			}*/
		}
		
	}
	
	protected void drawPoint(Graphics2D g2d,double xCoord, double yCoord){
		drawPointX(g2d,xCoord,yCoord);
	}
	
	protected void drawPointBar(Graphics2D g2d,double xCoord, double yCoord){
		Object oldAntialias = g2d.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
		Shape oldClip = g2d.getClip();
		Stroke oldStroke = g2d.getStroke();
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_OFF);
		
		Rectangle dataArea = getDataAreaBounds();
		Rectangle bar;
		bar = new Rectangle(
				(int)( xCoord - barWidth / 2), //centers the bar on the value
				(int)( yCoord),
				(int)(barWidth),
				(int)(-1*yCoord)+ (dataArea.y + dataArea.height) );
		
		//draw the bar
		g2d.setStroke(new BasicStroke(1, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		g2d.setColor(foregroundColor);				
		g2d.fill(bar);
		
		//draws border around bar
		g2d.setColor(Color.BLACK);
		g2d.draw(bar);
		
		
		g2d.setClip(oldClip);
		g2d.setStroke(oldStroke);
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
				oldAntialias);
	}
	
	protected void drawPointX(Graphics2D g2d,double xCoord, double yCoord){
		Shape oldClip = g2d.getClip();
		Stroke oldStroke = g2d.getStroke();
		g2d.setColor(Color.BLACK);
		g2d.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		int radius = 0;
		// Radius changed to 0 for more clear figures. The x's were too
		// cumbersome for dense plots. If this sticks, we can remove
		// the radius variable entirely here. -- dmusican
		g2d.draw(new Line2D.Double((double)xCoord-radius, yCoord-radius, (double)xCoord+radius, yCoord+radius));
		g2d.draw(new Line2D.Double((double)xCoord+radius, yCoord-radius, (double)xCoord-radius, yCoord+radius));
		g2d.setClip(oldClip);
		g2d.setStroke(oldStroke);
	}
	
	/* (non-Javadoc)
	 * @see chartlib.GenericChartArea#drawData(java.awt.Graphics2D)
	 */
	@Override
	protected void drawData(Graphics2D g2d) {
		drawDataPoints(g2d,datasets.get(0));
	}
	

	/**
	 * Sets the bounds of either or both axes to fit the dataset.
	 * If the dataset is empty, leaves the axes alone.
	 * If only packY is true, adjusts the data to fit the y values of the points
	 * within the current x axis window.
	 * @param packX Whether to change the x axis.
	 * @param packY Whether to change the y axis.
	 */
	public void packData(boolean packX, boolean packY, boolean forceY){
		
			Dataset dataset = datasets.get(0);
			
			//empty dataset: do nothing
			if (dataset == null || dataset.size() == 0 || (packX == false && packY == false))
				return;

			double xmin = 0, ymin = 0, xmax = 0, ymax = 0;

			if(packX == true) {
				double[][] bounds = findAllMinsMaxes(dataset);
				xmin = bounds[0][0];
				xmax = bounds[0][1];
				ymin = bounds[1][0];
				ymax = bounds[1][1];
			} else {
				double[] bounds = findYMinMax(dataset);
				if (bounds == null) return;
				ymin = bounds[0];
				ymax = bounds[1];
			}
			
			double newXmin = 0, newXmax = 0, newYmin = 0, newYmax = 0;
			
			//one element:
			if(xmin == xmax && ymin == ymax)
			{
				newXmin = xmin - xmin / 2;
				newXmax = xmax + xmax / 2;
				newYmin = 0;
				newYmax = ymax + ymax / 10;
			}
			else
			{
//				adds some extra space on the edges
				newXmin = xmin - ((xmax - xmin) / 10);
				newXmax = xmax + ((xmax - xmin) / 10);
				newYmin = forceY ? 0 : (ymin - ((ymax - ymin) / 10));
				newYmax = ymax + ((ymax - ymin) / 10);
			}
			if (packX) {
				xAxis.setRange(newXmin, newXmax);
			}
			if (packY) {
				yAxis.setRange(newYmin, newYmax);
			}
				
			createAxes();
			repaint();
			//System.out.println("Size:\n"+this.getSize().height+"\t"+this.getSize().width);
			
	}
	
	public Dataset getDataset(int i){
		return datasets.get(i);
	}
	
	public void setDataset(int i, Dataset newDataset){
		datasets.set(i,newDataset);
	}
	
	public void setDataset(Dataset newDataset){
		datasets.clear();
		datasets.add(newDataset);
	}
	
	public void setDatasets(Dataset[] newDatasets){
		datasets.clear();
		for(Dataset newDataset : newDatasets){
			datasets.add(newDataset);
		}
	}
	
	
	/**
	 * Finds the maximum and minimum x and y.  
	 * (Finds the (complete) range and domain of the dataset)
	 * @param dataset
	 * @return
	 */
	public double[][] findAllMinsMaxes(Dataset dataset) {
		double xmin, ymin, xmax, ymax;	
		xmin = ymin = Double.MAX_VALUE;
		xmax = ymax = Double.MIN_VALUE;
		DataPoint dp;
		double[][] ret = new double[2][2];
		ret[0][0] = 0;
		ret[0][1] = 0;
		ret[1][0] = 0;
		ret[1][1] = 0;
		if(dataset == null){
			return ret;
		}
		
		int size = dataset.size();
		java.util.Iterator iterator = dataset.iterator();

		while(iterator.hasNext())
		{
			dp = (DataPoint)(iterator.next());
			if(dp.y < ymin) ymin = dp.y;
			if(dp.y > ymax) ymax = dp.y;
			if(dp.x < xmin) xmin = dp.x;
			if(dp.x > xmax) xmax = dp.x;
		}
		
		ret[0][0] = xmin;
		ret[0][1] = xmax;
		ret[1][0] = ymin;
		ret[1][1] = ymax;
		return ret;
	}
	
	
	/**
	 * Finds the minimum and maximum y values PRESERVING the current domain of x.
	 * (Finds the range of the data for the current (restricted) domain.)
	 * @param dataset
	 * @return
	 */
	public double[] findYMinMax(Dataset dataset) {
		double xmin, ymin, xmax, lastXValue, ymax;	
		xmin = ymin = Double.POSITIVE_INFINITY;
		lastXValue = xmax = ymax = Double.NEGATIVE_INFINITY;
		DataPoint dp;
		
		double[] ret = new double[2];
		ret[0] = 0;
		ret[1] = 0;
		if(dataset == null){
			return ret;
		}
		
		int size = 0;
		SortedSet<DataPoint> drawable = dataset.tailSet(new DataPoint(xAxis.getMin(), 0));
		if(drawable.size()>0){
		Iterator<DataPoint> iterator 
			= drawable.iterator();
		SortedSet<DataPoint> headSet = dataset.headSet(drawable.first());
		DataPoint head = null;
		if(headSet.size()> 0 ){
			head = headSet.last();
			ymax = ymin = head.y;
			lastXValue = head.x;
		}
		
		
		while(iterator.hasNext())
		{
			dp = iterator.next();
			if((dp.x >xAxis.getMax())&&(lastXValue >xAxis.getMax()))break;
			if(dp.y < ymin) ymin = dp.y;
			if(dp.y > ymax) ymax = dp.y;
			size++;
		}
		}
		if (size == 0) return null;
		
		ret[0] = ymin;
		ret[1] = ymax;
		return ret;
	}
	
	

	/**
	 * @return Returns the barWidth.
	 */
	public int getBarWidth() {
		return barWidth;
	}

	/**
	 * @param barWidth The barWidth to set.
	 */
	public void setBarWidth(int barWidth) {
		this.barWidth = barWidth;
	}
	public Color getBackgroundColor() {
		return backgroundColor;
	}
	/**
	 * @param backgroundColor The backgroundColor to set.
	 */
	public void setBackgroundColor(Color backgroundColor) {
		this.backgroundColor = backgroundColor;
	}
	/**
	 * @return Returns the foregroundColor.
	 */
	public Color getForegroundColor() {
		return foregroundColor;
	}
	/**
	 * @param foregroundColor The foregroundColor to set.
	 */
	public void setForegroundColor(Color foregroundColor) {
		this.foregroundColor = foregroundColor;
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}
	
	
	public Double selectBar(Point p, int buf, Dataset peaks) {
		//System.out.println("ChartArea Size:\n"+this.getSize().height+"\t"+this.getSize().width);
		
		Rectangle testbar;
		Rectangle dataArea = getDataAreaBounds();
		Iterator<DataPoint> iterator = peaks.iterator();
		while (iterator.hasNext()) {
			DataPoint curPoint = iterator.next();

			double x = curPoint.x, y = curPoint.y;

			int xCoord = (int) (dataArea.x + xAxis.relativePosition(curPoint.x)
					* dataArea.width);
			double yCoord = (dataArea.y + dataArea.height - (yAxis
					.relativePosition(curPoint.y) * dataArea.height));
			
			testbar = new Rectangle(
					(int)( xCoord - barWidth / 2), //centers the bar on the value
					(int)(dataArea.y),
					(int)(barWidth),
					(int)( dataArea.height ));
			if (testbar.contains(p)){
				return new Double(x);
			}

		}
		return null;
	}
}