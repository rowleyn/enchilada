package edu.carleton.enchilada.chartlib;

import java.awt.Color;
import java.util.ArrayList;

public class SizePlot extends Chart {
	private Dataset sizeData;
	
	public SizePlot() {
		title = "";
		hasKey = false;
		sizeData = new Dataset();
		
		makeChartAreas();
		
		setupLayout();
		
		packData(false, true, true); //updates the Y axis scale.
	}
	
	public void packData(boolean packX, boolean packY, boolean forceY){
		super.packData(packX, packY, forceY);
	}
	
	public SizePlot(Dataset sizeData){
		this();
		this.sizeData = sizeData;
		packData(false, true, true); //updates the Y axis scale.
	}
	
	protected void makeChartAreas(){
		chartAreas.clear();
		
		ChartArea sizeChart = new BarChartArea(sizeData);
		sizeChart.setForegroundColor(Color.BLACK);
		sizeChart.setBarWidth(4); // set this dynamically with zoom? disable zoom entirely?
		sizeChart.setTitleX("Particle diameter");
		sizeChart.setTitleY("Count");
		sizeChart.xAxis.setTicks(10, 0);
		sizeChart.setXMin(0.0);
		sizeChart.setXMax(2.0);
		chartAreas.add(sizeChart);
	}
	
	public void display(Dataset sizeData) {
		setTitleY(0, "Count");
		this.sizeData = sizeData;
		makeChartAreas();
		setupLayout();
	}
}
