package edu.carleton.enchilada.gui;

import edu.carleton.enchilada.collection.Collection;

public class AggregationException extends Exception {
	Collection collection;
	AggregationException(Collection collection){
		this.collection = collection;
	}
}
