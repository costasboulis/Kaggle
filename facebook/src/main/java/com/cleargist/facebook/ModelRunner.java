package com.cleargist.facebook;

import java.io.File;

public class ModelRunner {

	public static void main(String[] args) {
		Model model = new CorrelationsModel();
		
		File dataFile = new File("c:\\kaggle\\train.csv");
		model.readData(dataFile);
	}
}
