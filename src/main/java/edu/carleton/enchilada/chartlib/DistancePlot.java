package edu.carleton.enchilada.chartlib;

import java.awt.Color;
import java.util.ArrayList;

public class DistancePlot extends Chart {
	private Dataset distData;
	
	public DistancePlot() {
		title = "";
		hasKey = false;
		distData = new Dataset();
		
		makeChartAreas();
		
		setupLayout();
		
		packData(false, true, true); //updates the Y axis scale.
	}
	
	public void packData(boolean packX, boolean packY, boolean forceY){
		super.packData(packX, packY, forceY);
	}
	
	public DistancePlot(Dataset distData){
		this();
		this.distData = distData;
		packData(false, true, true); //updates the Y axis scale.
	}
	
	protected void makeChartAreas(){
		chartAreas.clear();
		
		ChartArea distChart = new BarChartArea(distData);
		distChart.setForegroundColor(Color.BLACK);
		distChart.setBarWidth(4); // set this dynamically with zoom? disable zoom entirely?
		distChart.setTitleX("Normalized distance");
		distChart.setTitleY("Count");
		distChart.xAxis.setTicks(1, 0);
		distChart.setXMin(0.0);
		distChart.setXMax(2.0);
		chartAreas.add(distChart);
	}
	
	public void display(Dataset distData) {
		setTitleY(0, "Count");
		this.distData = distData;
		makeChartAreas();
		setupLayout();
	}
}
