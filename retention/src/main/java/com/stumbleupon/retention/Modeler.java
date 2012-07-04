package com.stumbleupon.retention;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import weka.classifiers.Classifier;
import weka.classifiers.evaluation.ThresholdCurve;
import weka.classifiers.functions.Logistic;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;


public class Modeler {
	private Logger logger = LoggerFactory.getLogger(getClass());
	public static String newline = System.getProperty("line.separator");
	
	public static Instances readData(String filename) {
		Instances data = null;
		try {
			DataSource source = new DataSource(filename);
			data = source.getDataSet();
			if (data.classIndex() == -1) {
				data.setClassIndex(data.numAttributes() - 1);
			}
		}
		catch (Exception ex) {
			System.err.println("Could not read from " + filename);
			System.exit(-1);
		}
		
		return data;
	}

	private void calculatePrecisionRecallCurve(double[] hyps, int[] refs, File out) {
		BufferedWriter writer = null;
		try {
			writer = new BufferedWriter(new FileWriter(out));
		}
		catch (Exception ex) {
			logger.error("Could not write to file " + out.getAbsolutePath());
			return;
		}
		int[][] conf = new int[2][];
		conf[0] = new int[2];
		conf[1] = new int[2];
		
		for (double threshold = 0.0; threshold < 0.99; threshold += 0.01) {
			// Create confusion matrix
			int[] hypsBinary = new int[hyps.length];
			for (int i = 0; i < hyps.length; i ++) {
				hypsBinary[i] = hyps[i] > threshold ? 1 : 0;
			}
			conf[0][0] = 0; conf[0][1] = 0;
			conf[1][0] = 0; conf[1][1] = 0;
			for (int i = 0; i < hypsBinary.length; i ++) {
				conf[hypsBinary[i]][refs[i]] ++;
			}
			
			// Calculate precision recall
			double precision = (double)conf[1][1] / (double)(conf[1][1] + conf[1][0]);
			double recall = (double)conf[1][1] / (double)(conf[1][1] + conf[0][1]);
			
			StringBuffer sb = new StringBuffer();
			sb.append(threshold); sb.append(";"); sb.append(precision); sb.append(";"); sb.append(recall); sb.append(newline);
			
			try {
				writer.write(sb.toString());
				writer.flush();
			}
			catch (Exception ex) {
				logger.error("Could not write to file " + out.getAbsolutePath());
				return;
			}
		}
		try {
			writer.close();
		}
		catch (Exception ex) {
			logger.error("Could not write to file " + out.getAbsolutePath());
			return;
		}
	}
	
	private double[][] calculatePrecisionRecallCurve(double[] hyps, int[] refs) {
		
		double[][] curve = new double[2][];
		curve[0] = new double[100];
		curve[1] = new double[100];
		
		int[][] conf = new int[2][];
		conf[0] = new int[2];
		conf[1] = new int[2];
		
		int k = 0;
		for (double threshold = 0.0; threshold < 1.0; threshold += 0.01) {
			// Create confusion matrix
			int[] hypsBinary = new int[hyps.length];
			for (int i = 0; i < hyps.length; i ++) {
				hypsBinary[i] = hyps[i] > threshold ? 1 : 0;
			}
			conf[0][0] = 0; conf[0][1] = 0;
			conf[1][0] = 0; conf[1][1] = 0;
			for (int i = 0; i < hypsBinary.length; i ++) {
				conf[hypsBinary[i]][refs[i]] ++;
			}
			
			// Calculate precision recall
			double precision = (double)conf[1][1] / (double)(conf[1][1] + conf[1][0]);
			double recall = (double)conf[1][1] / (double)(conf[1][1] + conf[0][1]);
			
			curve[0][k] = precision;
			curve[1][k] = recall;
			
			k ++;
		}
		
		return curve;
	}
	
	public void evaluate(Classifier model, File results) {
		
		double[][] avgCurve = new double[2][];
		avgCurve[0] = new double[100];
		avgCurve[1] = new double[100];
		for (int i = 0; i < avgCurve[0].length; i ++) {
			avgCurve[0][i] = 0.0;
			avgCurve[1][i] = 0.0;
		}
		int numFolds = 10;
		for (int numFold = 0; numFold < numFolds; numFold ++) {
			String trainData = "c:\\StumbleUpon\\data\\train-" + numFold + ".arff";
			String testData = "c:\\StumbleUpon\\data\\test-" + numFold + ".arff";
			 
			Instances trainInstances = Modeler.readData(trainData);
			try {
				model.buildClassifier(trainInstances);
				logger.info("Model building for fold " + numFold + " completed");
			}
			catch (Exception ex) {
				logger.error("Could not build model for fold " + numFold);
				return;
			}
			
			Instances testInstances = Modeler.readData(testData);
			try {
				double[] hyps = new double[testInstances.numInstances()];
				int[] refs = new int[testInstances.numInstances()];
				for (int i = 0; i < testInstances.numInstances(); i ++) {
					Instance testInstance = testInstances.instance(i);
					double[] probs = model.distributionForInstance(testInstance);
					hyps[i] = probs[0];
					refs[i] = (int)testInstance.classValue();
				}
				logger.info("Finished making predictions for fold " + numFold);
				
				double[][] curve = calculatePrecisionRecallCurve(hyps, refs);
				
				for (int i = 0; i < avgCurve[0].length; i ++) {
					avgCurve[0][i] += curve[0][i];
					avgCurve[1][i] += curve[1][i];
				}
				
			}
			catch (Exception ex) {
				logger.error("Could not evaluate model");
				return;
			}
		}
		
		
		BufferedWriter writer = null;
		try {
			writer = new BufferedWriter(new FileWriter(results));
		}
		catch (Exception ex) {
			logger.error("Could not write to file " + results.getAbsolutePath());
			return;
		}
		for (int i = 0; i < avgCurve[0].length; i ++) {
			double avgPrecision = avgCurve[0][i] / (double)numFolds;
			double avgRecall = avgCurve[1][i] / (double)numFolds;
			try {
				writer.write(avgPrecision + ";" + avgRecall + newline);
				writer.flush();
			}
			catch (Exception ex) {
				logger.error("Could not write to file " + results.getAbsolutePath());
				return;
			}
		}
		
		try {
			writer.close();
		}
		catch (Exception ex) {
			logger.error("Could not write to file " + results.getAbsolutePath());
			return;
		}
	}
	
	public static void main( String[] args ) {
		 
		String evaluationsFilename = "c:\\StumbleUpon\\logistic.txt";
		 Modeler modeler = new Modeler();
		 modeler.evaluate(new Logistic(), new File(evaluationsFilename));
	    	
	 }
}
