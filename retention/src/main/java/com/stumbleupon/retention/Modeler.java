package com.stumbleupon.retention;

import java.io.File;

import weka.classifiers.Evaluation;
import weka.classifiers.evaluation.ThresholdCurve;
import weka.classifiers.functions.Logistic;
import weka.core.Instances;


public class Modeler {
	 public static void main( String[] args ) {
		 String data = "c:\\StumbleUpon\\data-scientist\\retention_data.csv";
	    	
		 FeatureCreator featureCreator = new FeatureCreator();
		 Instances dataSet = featureCreator.createInstances(new File(data));
		 
		 Logistic model = new Logistic();
		 try {
			 Evaluation eval = new Evaluation(dataSet);
			 ThresholdCurve curve = new ThresholdCurve();
			 curve.getCurve(predictions);
		 }
		 catch (Exception ex) {
			 System.err.println("Could not evaluate model");
			 System.exit(-1);
		 }
		 
	    	
	 }
}
