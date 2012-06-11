package com.cleargist.facebook;

import java.io.File;

public class ModelRunner {

	public static void main(String[] args) {
		String trainingData = "c:\\kaggle\\train.csv";
		String testData = "c:\\kaggle\\test_part3.csv";
		String predictions = "c:\\kaggle\\predictions.csv";
		
		Model model = new JaccardModel();
		
		File dataFile = new File(trainingData);
		model.readData(dataFile);
		
		model.writePredictions(new File(predictions), new File(testData));
	}
}
