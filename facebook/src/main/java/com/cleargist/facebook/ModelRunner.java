package com.cleargist.facebook;

import java.io.File;

public class ModelRunner {

	public static void main(String[] args) {
		String trainingData = "c:\\kaggle\\train.csv";
		String testData = "c:\\kaggle\\test.csv";
		String predictions = "c:\\kaggle\\predictions.csv";

		/*
		Model model = new JaccardModel();
		*/
		MixtureOfBernoullisModel model = new MixtureOfBernoullisModel();
		model.setNumberOfClusters(10);
		model.setNumberOfIterations(10);
		model.setNumberOfUsersPerChunk(100000);
		
		File dataFile = new File(trainingData);
		model.readData(dataFile);
		model.train();
		
		model.writeClusterMemberships(new File("c:\\kaggle\\clusterMemberships.txt"));
		model.writePredictions(new File(predictions), new File(testData));
	}
}
