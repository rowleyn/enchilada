/* ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is EDAM Enchilada's ZoomableChart class.
 *
 * The Initial Developer of the Original Code is
 * The EDAM Project at Carleton College.
 * Portions created by the Initial Developer are Copyright (C) 2005
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 * Jonathan Sulman sulmanj@carleton.edu
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either the GNU General Public License Version 2 or later (the "GPL"), or
 * the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the MPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the MPL, the GPL or the LGPL.
 *
 * ***** END LICENSE BLOCK ***** */

/*
 * Created on Mar 3, 2005
 *
 */

package edu.carleton.enchilada.chartlib;


import javax.swing.event.MouseInputListener;
import javax.swing.*;
import java.awt.event.*;
import java.awt.*;



/**
 * 
 * ZoomableChart is an extended wrapper for Chart.
 * It implements mouse and keyboard-controlled zooming
 * with visual feedback.
 * In order to provide visual feedback for mouse zooming,
 * this class is implemented as a JLayeredPane with two layers:
 * a lower layer for drawing the chart, and an upper layer for drawing
 * mouse feedback over the chart.
 * <p>
 * It could be a good project to make the chart be double buffered, so that it
 * does not have to redraw from scratch every time you drag the mouse to zoom.
 * The ComponentListener interface might be useful for detecting when the chart
 * really needs to be redrawn, rather than when the cached copy can get stuck
 * on the screen.
 * <p>
 * This might also be done with a RepaintManager, or with the Swing property
 * setDoubleBuffered, or something.  I don't understand how all that works.
 * http://java.sun.com/products/jfc/tsc/articles/painting/
 * 
 * Silenced a lot of the output here -- MM 2014
 * 
 * @author sulmanj
 * @author smitht
 * @author olsonja
 * @author jtbigwoo
 * 
 */
public class ZoomableChart extends JLayeredPane implements MouseInputListener,
		AdjustmentListener, KeyListener {

	//the two layers
	private Zoomable chart;
	private ChartZoomGlassPane glassPane;
	
	private JScrollBar scrollBar;
	
	// these are the maximum and minimum indices, in chart coordinates,
	// that are displayed.
	private double cScrollMin = 0;
	private double cScrollMax = 300;
	
	// this is the value of cScrollMax that is returned to when you go to
	// the default zoom level.
	private double defaultCScrollMax = cScrollMax;
	private double defaultCScrollMin = cScrollMin;
	
	// these are the minimum and maximum indices in scrollbar coordinates,
	// which are different from chart coordinates, sadly.
	private final int S_SCROLL_MIN = 0;
	private final int S_SCROLL_MAX = Integer.MAX_VALUE;
	// they can't be the same coordinates because some chart coordinates are
	// too big to be represented by integers, but integers are all that
	// scrollbars know how to deal with.
	
	// a rather arbitrary limit on the distance that you can zoom in.
	private final int MIN_ZOOM = 5;
	
	private boolean forceY = false;
	
	/**
	 * Constructs a new ZoomableChart.
	 * @param chart The chart the zoomable chart will display.
	 */
	public ZoomableChart(Zoomable chart)
	{
		this.chart = chart;
		this.glassPane = new ChartZoomGlassPane();
		// set the maximum and minimum x values for the chart
		double[] xRange = chart.getVisibleXRange();
		if (xRange != null) {
			cScrollMin = xRange[0];
			cScrollMax = xRange[1];
			defaultCScrollMax = xRange[1];
			defaultCScrollMin = xRange[0];
		}
		
		//on an unzoomed chart, the bar fills the whole range.
		scrollBar = new JScrollBar(JScrollBar.HORIZONTAL, S_SCROLL_MIN, S_SCROLL_MAX,
					S_SCROLL_MIN, S_SCROLL_MAX);
		scrollBar.setModel(new DefaultBoundedRangeModel(S_SCROLL_MIN, S_SCROLL_MAX,
					S_SCROLL_MIN, S_SCROLL_MAX));
		scrollBar.addAdjustmentListener(this);
		
		//layout for stacking components
		setLayout(new OverlayLayout(this));
		
		JPanel bottomPanel = new JPanel(new BorderLayout());
		
		JPanel chartPanel = new JPanel(new BorderLayout());
		// add this as a listener to the chart
		// so that you hear the zooming clicks
		if (chart instanceof Component) {
			Component c = (Component) chart;
			chartPanel.add(c, BorderLayout.CENTER);
			c.addMouseMotionListener(this);
			c.addMouseListener(this);
		} else {
			// I can't figure out how to re-jigger inheritance so that Zoomable
			// can force things to be Components.  Nor do I know if I should. -tom
			throw new RuntimeException("Whoops!  Time to redesign chartlib!");
		}
		bottomPanel.add(chartPanel,BorderLayout.CENTER);
		bottomPanel.add(scrollBar, BorderLayout.SOUTH);
		add(bottomPanel, JLayeredPane.DEFAULT_LAYER);
		add(glassPane, JLayeredPane.DRAG_LAYER);
		
		setFocusable(true);
		//addKeyListener(this);
		
		// give the cursor it's special crosshairs
		// This may need to  include the scrollbar
		glassPane.setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
		scrollBar.updateUI();
	}
	
	/**
	 * Find the maximum accessible value on the chart.
	 * @return a number in x-coordinates on the chart.
	 */
	public double getCScrollMax() {
		return cScrollMax;
	}
	/**
	 * Set the maximum value, in chart coordinates, that will be accessible
	 * with the scroll bar all the way to the right.
	 * <p>
	 * When the user does something like zoomOutHalf, more of the chart will
	 * be visible.  But when you go to another atom, or something like that,
	 * the maximum value will get set back to this.
	 */
	public void setCScrollMax(double defaultXmax) {
		this.cScrollMax = defaultXmax;
		this.defaultCScrollMax = defaultXmax;
		//zoom out to ensure correctness of new values
		zoomOut();
		
	}
	/**
	 * Find the minimum accessible value on the chart.
	 * @return a number in x-coordinates on the chart.
	 */
	public double getCScrollMin() {
		return cScrollMin;
	}
	/**
	 * Sets the minimum value accessible with the scrollbar.
	 * @param defaultXmin
	 */
	public void setCScrollMin(double defaultXmin) {
		this.cScrollMin = defaultXmin;
		//zoom out to ensure correctness of new values
		zoomOut();
	}
	
	
	/**
	 * This lets the class know where a drag may have started.
	 * Updates the GlassPane's start point variable.
	 * If the mouse isn't in a chart, sets the start point to null.
	 * 
	 * Part of the MouseListener interface.
	 */
	public void mousePressed(MouseEvent e) 
	{
		//System.out.println("Chart size: "+(((Chart)chart).getSize().width)+"\t"+(((Chart)chart).getSize().height));
		//System.out.println("ChartArea size: "+(((Chart)chart).chartAreas.get(0).getSize().width)+"\t"+(((Chart)chart).chartAreas.get(0).getSize().height));
		// if it's a right mouse click
		// and inside the chart area
		// draw the grey thing
		if(e.getButton() == MouseEvent.BUTTON1 )
		{
			
			if (chart.isInDataArea(e.getPoint())) {
				glassPane.start = e.getPoint();
			} else glassPane.start = null;
		} else {
			glassPane.start = null;
			glassPane.end = null;
			glassPane.drawLine = false;
			repaint();
		}
		//System.out.println("mouse clicked: "+glassPane.start);
	}
	/*
	Chart size: 634	575
	ChartArea size: 614	273
	
	Chart size: 634	575
	ChartArea size: 600	300
	*/

	/** 
	 * If the drag is within one of the charts, (if the start point is non-null)
	 * draws a pattern following the x coordinate of the drag.
	 * Updates the glass pane's end point variable.
	 * 
	 * Part of MouseListener
	 */
	public void mouseDragged(MouseEvent e) {
		Point oldEnd;
		// keep drawing the gray thing
		if(glassPane.start != null)
		{
			glassPane.drawLine = true;
			
			/* 
			 * don't need to change for scrollbar changes, since this just
			 * sees if the point is on the chart or not. 
			 */
			if(glassPane.end != null) oldEnd = glassPane.end;
			else oldEnd = e.getPoint();
			if(chart.isInDataArea(e.getPoint()))
			{
				// if we're still in the graph area, just move the end of the glasspane
				// to where the mouse is.
				glassPane.end = e.getPoint();
			}
			else
			{
				// if we're off the edge of the graph area, call getDataAreaEdge to move
				// the end of the glasspane to the closest spot to the mouse. (bug 2525223)
				// @see chartlib.Zoomable.getDataAreaEdge()
				glassPane.end = e.getPoint();
				glassPane.end.x = chart.getDataAreaEdge(glassPane.start, e.getPoint());
			}
			if(glassPane.start.x < oldEnd.x)
			{
				repaint(glassPane.start.x - 10,
						glassPane.start.y - 5,
						oldEnd.x + 20 - glassPane.start.x,
						10);
			}
			else
			{
				repaint(oldEnd.x - 10,
						glassPane.start.y - 5,
						glassPane.start.x + 20 - oldEnd.x,
						10);
			}
		}else{
			//The mouse click was invalid
			//System.out.println("No glasspane.start!!");
		}
	}

	/**
	 * Lets the class know a drag has ended.
	 */
	public void mouseReleased(MouseEvent e) {
		// the mouse was released.
		// do the zoom if you should
		glassPane.drawLine = false;
		if(glassPane.start != null && glassPane.end != null)
		{
			//if(chart.isInDataArea(e.getPoint())){
				//System.out.println("mouse released: "+glassPane.start+"\t"+glassPane.end);
				performZoom();
			//}
		}
		// whether or not a zoom was made, clear glassPane's start and end!
		glassPane.start=null;
		glassPane.end = null;
		repaint();
	}
	
	/**
	 * Called whenever the scroll bar is scrolled, this changes the viewed
	 * area of the chart to fit the values of the scroll bar.
	 */
	public void adjustmentValueChanged(AdjustmentEvent e) {
		
		//System.out.println("value changed: "+e.getValue());
		// e.getValue is the new value of the scrollbar
		// as passed in scrollbar.setValues(newXMin
		int scrollmin = e.getValue();
		//scrollbar.getVisibleAmount should be extent, or xMax-xMin
		// as passed in scrollbar.setValues(newXMin
		int scrollmax = scrollmin + scrollBar.getVisibleAmount();
		//System.out.println("extent: "+scrollBar.getVisibleAmount());
		
//		 extent/visibleAmount should never be <=0
		//  enforce minimum scroll width of "2"
		//  uses ++ and -- to keep view centered at minimum width
		//     -rzeszotj
		if(scrollmin == scrollmax){
			System.out.println("scroll values bad: "+scrollmin+"\t"+scrollmax);
			scrollmax++;
			scrollmin--;
		}
		//Just in case the impossible happens, don't make it worse
		if(scrollmin > scrollmax){
			System.out.println("scroll values bad: "+scrollmin+"\t"+scrollmax);
			repaint();
			return;
		}
		
		//convert back to chart coords
		double xmin = scrollToChart(scrollmin);
		double xmax = scrollToChart(scrollmax);
		
		try
		{
			chart.setXAxisBounds(xmin, xmax);
			chart.packData(false, true, forceY);
		}
		catch (IllegalArgumentException ex){
			System.out.println("Illegal Argument: "+e.getValue());
			//ex.printStackTrace();
		}
	}
	
	public void test(int value, int extent){
		System.out.println("value changed: "+value);
		// e.getValue is the new value of the scrollbar
		// as passed in scrollbar.setValues(newXMin
		int scrollmin = value;
		//scrollbar.getVisibleAmount should be extent, or xMax-xMin
		// as passed in scrollbar.setValues(newXMin
		int scrollmax = scrollmin + extent;
		System.out.println("extent: "+extent);
		
		if(scrollmin >= scrollmax){
			System.out.println("scroll values bad: "+scrollmin+"\t"+scrollmax);
			repaint();
			return;
		}
		
		//convert back to chart coords
		double xmin = scrollToChart(scrollmin);
		double xmax = scrollToChart(scrollmax);
		
		System.out.println("reconverted chart coords: "+xmin+"\t"+xmax);
		
		try
		{
			chart.setXAxisBounds(xmin, xmax);
			chart.packData(false, true, forceY);
		}
		catch (IllegalArgumentException ex){
			System.out.println("Illegal Argument");
		}
	}
	/**
	 * Convert from a number given by the scroll bar to a number that
	 * makes sense to the chart (like x-coordinates).
	 * @param scrollValue
	 * @return
	 */
	private double scrollToChart(int scrollValue) {
		double maxExtent = cScrollMax - cScrollMin;
		if(((((double) scrollValue) / Integer.MAX_VALUE) * maxExtent) 
			+ cScrollMin<0){
			if(((((double) scrollValue) / Integer.MAX_VALUE) * maxExtent)<0){
				if((((double) scrollValue) / Integer.MAX_VALUE)<0){
					//System.err.println("divide by big number < 0");
				}else{
					//System.err.println("mult by maxExtent < 0");
				}
			}else{
				//System.err.println("plus scrollMin < 0");
			}
		}
		return ((((double) scrollValue) / Integer.MAX_VALUE) * maxExtent) 
			+ cScrollMin;
	}
	
	/**
	 * Convert from a number given by the chart to one for the scrollbar.
	 * @param chartValue
	 * @return
	 */
	private int chartToScroll(double chartValue) {
		double maxExtent = cScrollMax - cScrollMin;
		if((((chartValue - cScrollMin) / maxExtent) * Integer.MAX_VALUE)<0){
				if(((chartValue - cScrollMin) / maxExtent)<0){
					if((chartValue - cScrollMin)<0){
						System.err.println("chartValue < cScrollMin!");
					}else{
						System.err.println("divide by maxExtent < 0");
					}
				}else{
					System.err.println("mult by big number < 0");
				}
			}
		return (int) 
			(((chartValue - cScrollMin) / maxExtent) * Integer.MAX_VALUE);
	}
	
	/**
	 * Checks the glassPane coordinates to make sure they define
	 * a valid zoom and converts the glassPane coordinates into chart area coordinates.
	 *
	 */
	private void performZoom()
	{
		System.out.println("performing zoom");
		Point minPoint = new Point(glassPane.start);
		Point maxPoint = new Point(glassPane.end);
		double xmin, xmax;
		
		repaint();	//get rid of grey dots
		
		// find the relevant chart
		//int chartIndex = ((Chart)chart).getChartIndexAt(minPoint);
		// set the cScroll values to the max and min values of the relevant chartArea
		//cScrollMax = ((Chart)chart).getXmax(chartIndex);
		//cScrollMin = ((Chart)chart).getXmin(chartIndex);
		
		//makes a left-to-right drag equivalent to a right-to-left drag
		if(minPoint.x > maxPoint.x){
			minPoint.x = maxPoint.x;
			maxPoint.x = glassPane.start.x;
		}
		//don't zoom if the distance dragged (in pixels) is too small
		if(maxPoint.x - minPoint.x < MIN_ZOOM)
		{
			System.out.println("Zoom length too small: "+(maxPoint.x-minPoint.x));
			return; //zooms that are too small
		}
		// retrieve the data values(in chart coords)
		// represented at the max and min points
		xmin = chart.getDataValueForPoint(minPoint).x;
		xmax = chart.getDataValueForPoint(maxPoint).x;
		// these are in chart coordinates.
		
		//don't zoom if the two points(in pixels) round to the same point(in chart units)
		if(xmin >= xmax)
		{
			System.out.println("Same point(in chart units)");
			return; //another case of zooms that are too small
		}
		
		zoom(xmin, xmax);
		
	}
	
	/**
	 * Set the zoom so that newXmin and newXmax will be the minimum and maximum
	 * visible coordinates on the chart---though not necessarily the maximum
	 * accessible, by moving the scroll bar.
	 * @param newXmin
	 * @param newXmax
	 */
	protected void zoom(double newXmin, double newXmax)
	{
		//System.out.println("zooming from "+newXmin+" to "+newXmax);
		//System.out.println("current min: "+cScrollMin+", max: "+cScrollMax);
		
		//You should never be able to zoom out past the scroll min and max
		//You must change the min and max to scroll out further.
		assert((cScrollMin<=newXmin));
		assert((cScrollMax>=newXmax));
		
		// disable the scrollBar if you've zoomed out past Xmin and Xmax
		// enable the scrollBar otherwise
		if (newXmin <= cScrollMin && newXmax >= cScrollMax) {
			scrollBar.setEnabled(false);
		} else {
			scrollBar.setEnabled(true);
		}
		//System.out.println("chart max/min values: "+cScrollMin+"\t"+cScrollMax);
		
		//convert everything to 'scrollbar' units
		int scrollMin = chartToScroll(newXmin);
		int scrollMax = chartToScroll(newXmax);
		
		// changing the values on the scrollbars activates the adjustmentValueChanged method
		// since ZoomableChart is an ActionListener
		// This is better than having the code here, because the same things need to happen
		// when you drag the scroll bar (ie. when other things call adjustmentValueChanged
		
		// Note: this may not actually call adjustmentValueChanged if
		// the values if for arguments:
		//setValues(int newValue,
        //int newExtent,
        //int newMin,
        //int newMax)
		// minimum <= value <= value+extent <= maximum

		//update the scroll bar
		//System.out.println("scrollBar.setValues: "+(scrollMin+1)+"\n"+(scrollMax-scrollMin-1)+"\n"
		//		+S_SCROLL_MIN+"\t"+S_SCROLL_MAX);
		scrollBar.setValues(S_SCROLL_MIN, S_SCROLL_MAX,
				S_SCROLL_MIN, S_SCROLL_MAX);
		scrollBar.setValues(scrollMin+1, scrollMax - scrollMin-1,
				S_SCROLL_MIN, S_SCROLL_MAX);
		// XXX: why twice?  to force the scrollBar to realise that something has
		// changed.  true, that's a dumb way to do it, but i'm not smart. -tom
		//this.test(scrollMin, scrollMax - scrollMin);
		scrollBar.setBlockIncrement(scrollMax - scrollMin);
		
		scrollBar.updateUI();
		//System.out.println("zoomed");
	}
	
	/**
	 * Zoom out so that the current view of the Chart occupies half of the
	 * viewing area.  This also makes sure that nothing less than 0 is visible.
	 *
	 */
	public void zoomOutHalf() {
		double[] range = chart.getVisibleXRange();
		if (range == null) return;
		double xmin = range[0], xmax = range[1];
		double diff = (xmax - xmin) / 2.0;
		xmin -= diff; xmax += diff;
		
		/*
		 * if we would be zooming to the left of 0, change it so we're not.
		 */
		if (xmin < 0) {
			xmax = xmax + (- xmin);
			xmin = 0;
		}
		if(cScrollMin>xmin)xmin=cScrollMin;
		if(cScrollMax<xmax)xmax=cScrollMax;
		zoom(xmin, xmax);
	}
	
	public void zoomOut(){
		zoom(cScrollMin,cScrollMax/2);
		zoom(cScrollMin,cScrollMax);
	}
	
//	/**
//	 * For testing: outputs the chart point of the click.
//	 */
//	public void mouseClicked(MouseEvent e) {
//		int cIndex = chart.getChartAt(e.getPoint(),true);
//		
//		java.awt.geom.Point2D.Double p; 
//		if(cIndex != -1)
//		{
//			p = chart.getDataValueForPoint(cIndex, e.getPoint());
//			//System.out.println("Point clicked in chart " + cIndex);
//			//System.out.println("Coordinates: " + p.x + ", " + p.y);
//		}
//		
//	}
//	
	
	//extra mouseListener events.
	public void mouseClicked(MouseEvent e) {}
	public void mouseMoved(MouseEvent arg0) {}
	public void mouseEntered(MouseEvent arg0) {}
	public void mouseExited(MouseEvent arg0) {}
	
	/**
	 * 
	 * @author sulmanj
	 *
	 * Transparent pane that draws feedback for mouse zooming.
	 */
	private class ChartZoomGlassPane extends javax.swing.JPanel
	{
		public boolean drawLine = false;
		public Point start;
		public Point end;
		
		public ChartZoomGlassPane()
		{
			//well, you can see through it, can't you?
			setOpaque(false);
		}
		
		
		/**
		 * During a drag, paints a horizontal line following the mouse.
		 */
		protected void paintComponent(Graphics g)
		{
			Graphics2D g2d = (Graphics2D)g.create();
			if(drawLine && start != null && end != null)
			{
				drawDragFeedback(g2d);
			}
			g2d.dispose();
		}
		
		
		/**
		 * Draws a a pattern to indicate where the mouse has been dragged.
		 * @param g
		 */
		public void drawDragFeedback(Graphics2D g)
		{
			g.setColor(Color.GRAY);
			g.setStroke(new BasicStroke(3));
			g.fillRect(start.x-5, start.y-5, 10,10);
			g.drawLine(start.x, start.y, 
					end.x, start.y); // a horizontal line
			g.fillRect(end.x-5, start.y-5, 10, 10);
		}
	}

	public void keyPressed(KeyEvent e) {
		if (e.getKeyCode() == KeyEvent.VK_Z) {
			zoom(cScrollMin, defaultCScrollMax);
		}
	}

	public void keyReleased(KeyEvent e) {}

	public void keyTyped(KeyEvent e) {
		
	}

	/**
	 * Returns whether or not to force the bottom of the Y axis to be at 0.
	 * @author smitht
	 */
	public boolean isForceY() {
		return forceY;
	}

	/**
	 * Set whether or not to force the bottom of the Y axis to be at 0.
	 */
	public void setForceY(boolean forceY) {
		this.forceY = forceY;
	}

}