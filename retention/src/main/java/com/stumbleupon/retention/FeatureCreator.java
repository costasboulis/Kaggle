package com.stumbleupon.retention;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ArffSaver;


public class FeatureCreator {
	private Logger logger = LoggerFactory.getLogger(getClass());
	private static int AGE_COLUMN = 0;
	private static int GENDER_COLUMN = 1;
	private static int CATEGORIES_COLUMN = 13;
	private static int RETAIN_COLUMN = 59;
	private LinkedHashMap<String, Integer> categories;
	
	
	private Instance extractRetain(String line) {
		if (line == null) {
			return null;
		}
		
		FastVector attributes = new FastVector(1);
		
		FastVector attributeValues = new FastVector(2);
		attributeValues.addElement("RETAINED");
		attributeValues.addElement("NOT_RETAINED");
		
		attributes.addElement(new Attribute("RETAIN", attributeValues));
		Instances instances = new Instances("tmp", attributes, 1);
		
		Instance instance = new Instance(1);
		instance.setDataset(instances);
		
		int retain = -1;
		try {
			retain = Integer.parseInt(line);
			instance.setValue(0, retain == 1 ? "RETAINED" : "NOT_RETAINED");
		}
		catch (NumberFormatException ex) {
			logger.warn("Cannot parse retain for \"" + line + "\"");
			instance.setMissing(0);
		}
		
		return instance;
	}
	
	private Instance extractAge(String line) {
		FastVector attributes = new FastVector(1);
		attributes.addElement(new Attribute("AGE"));
		Instances instances = new Instances("tmp", attributes, 1);
		
		Instance instance = new Instance(1);
		instance.setDataset(instances);
		
		if (line == null || line.isEmpty()) {
			instance.setMissing(0);
		}
		else {
			int age = -1;
			try {
				age = Integer.parseInt(line);
				instance.setValue(0, age);
			}
			catch (NumberFormatException ex) {
				logger.warn("Cannot parse age for \"" + line + "\"");
				instance.setMissing(0);
			}
		}
		
		
		return instance;
	}
	
	private Instance extractGender(String line) {
		FastVector attributes = new FastVector(1);
		attributes.addElement(new Attribute("GENDER"));
		Instances instances = new Instances("tmp", attributes, 1);
		
		Instance instance = new Instance(1);
		instance.setDataset(instances);
		
		if (line == null || line.isEmpty()) {
			instance.setMissing(0);
		}
		else {
			int gender = -1;
			try {
				gender = Integer.parseInt(line);
				if (gender == 1) {
					instance.setValue(0, 0.0);
				}
				else if (gender == 2) {
					instance.setValue(0, 1.0);
				}
				else {
					instance.setMissing(0);
				}
				
			}
			catch (NumberFormatException ex) {
				logger.warn("Cannot parse age for \"" + line + "\"");
				instance.setMissing(0);
			}
		}
		
		
		return instance;
	}
	
	
	private Instance extractCategories(String line) {
		FastVector attributes = new FastVector(this.categories.size());
		for (String category : this.categories.keySet()) {
			String categoryName = getCategoryName(category);
			Attribute attribute = new Attribute(categoryName);
			attributes.addElement(attribute);
		}
		Instances instances = new Instances("tmp", attributes, this.categories.size());
		
		Instance instance = new Instance(this.categories.size());
		instance.setDataset(instances);
		
		
		for (Map.Entry<String, Integer> me : categories.entrySet()) {
			int attIndex = me.getValue();
			instance.setValue(attIndex, 0.0);
		}
		
		if (line == null || line.isEmpty()) {
			return instance;
		}
		
		String[] fields = line.split(";");
		for (String category : fields) {
			Integer attIndex = categories.get(category);
			if (attIndex == null) {
				continue;
			}
			int indx = attIndex.intValue();
			instance.setValue(indx, 1.0);
		}
		
		return instance;
	}
	
	private String getCategoryName(String category) {
		return "CATEGORY_" + category.replaceAll(" ", "");
	}
	
	private void init(File data) {
		categories = new LinkedHashMap<String, Integer>();
		int cnt = 0;
		try {
			BufferedReader reader = new BufferedReader(new FileReader(data));
			String line = reader.readLine();
			while ((line = reader.readLine()) != null) {
				String[] fields = line.split(",");
				String categoriesField = fields[CATEGORIES_COLUMN];
				String[] categoriesFields = categoriesField.split(";");
				for (String category : categoriesFields) {
					if (!categories.containsKey(category)) {
						categories.put(category, cnt);
						cnt ++;
					}
				}
			}
			reader.close();
			
			logger.info("Found " + categories.size() + " categories");
		}
		catch (FileNotFoundException ex) {
			logger.error("Could not find file " + data.getAbsolutePath());
			System.exit(-1);
		}
		catch (IOException ex) {
			logger.error("Could not read from file " + data.getAbsolutePath());
			System.exit(-1);
		}
	}
	
	public Instances createInstances(File data) {
		
		init(data);
		Instances instances = null;
		try {
			BufferedReader reader = new BufferedReader(new FileReader(data));
			String line = reader.readLine();
			int numLines = 0;
			while ((line = reader.readLine()) != null) {
				
				if (numLines % 1000 == 0) {
					logger.info("Processed " + numLines + " lines");
				}
				String[] fields = line.split(",");
				
				// AGE
				String ageField = fields[AGE_COLUMN];
				Instance ageInstance = extractAge(ageField);
				
				// GENDER
				String genderField = fields[GENDER_COLUMN];
				Instance genderInstance = extractGender(genderField);
				
				// CATEGORIES
				String categoriesField = fields[CATEGORIES_COLUMN];
				Instance categoriesInstance = extractCategories(categoriesField);
				
				// RETAIN
				String retainField = fields[RETAIN_COLUMN];
				Instance retainInstance = extractRetain(retainField);
				
				
				Instance instance = new Instance(ageInstance);
				instance = instance.mergeInstance(genderInstance);
				instance = instance.mergeInstance(categoriesInstance);
				
				
				instance = instance.mergeInstance(retainInstance);
				String classLabel = retainInstance.stringValue(0);
				
				
				if (instances == null) {
					FastVector attributes = new FastVector();
					attributes.appendElements(getAttributes(ageInstance));
					attributes.appendElements(getAttributes(genderInstance));
					attributes.appendElements(getAttributes(categoriesInstance));
					attributes.appendElements(getAttributes(retainInstance));
					
					instances = new Instances("data", attributes, 100000);
					instances.setClassIndex(attributes.size() - 1);
				}
				instance.setDataset(instances);
				instance.setClassValue(classLabel);
				
				instances.add(instance);
				
				numLines ++;
			}
			reader.close();
		}
		catch (FileNotFoundException ex) {
			logger.error("Could not find file " + data.getAbsolutePath());
			System.exit(-1);
		}
		catch (IOException ex) {
			logger.error("Could not read from file " + data.getAbsolutePath());
			System.exit(-1);
		}
		
		return instances;
	}
	
	private FastVector getAttributes(Instance instance) {
		FastVector attributes = new FastVector(instance.numAttributes());
		for (int i = 0; i < instance.numAttributes(); i ++) {
			attributes.addElement(instance.attribute(i));
		}
		return attributes;
	}
	
	public static void writeData(Instances instances, File file) {
		ArffSaver saver = new ArffSaver();
    	saver.setInstances(instances);
    	try {
    		saver.setFile(file);
        	saver.writeBatch();
    	}
    	catch (Exception ex) {
    		System.err.println("Could not write to file " + file.getAbsolutePath());
    		return;
    	}	
	}
	
    public static void main( String[] args ) {
    	String data = "c:\\StumbleUpon\\data-scientist\\retention_data.csv";
    	
    	FeatureCreator featureCreator = new FeatureCreator();
    	Instances dataSet = featureCreator.createInstances(new File(data));
    	
    	for (int numFold = 0; numFold < 10; numFold ++) {
    		Instances testData = dataSet.testCV(10, numFold);
    		Instances trainData = dataSet.trainCV(10, numFold);
    		
    		String testDataFilename = "c:\\StumbleUpon\\data\\test-" + numFold + ".arff";
    		featureCreator.writeData(testData, new File(testDataFilename));
    		
    		String trainDataFilename = "c:\\StumbleUpon\\data\\train-" + numFold + ".arff";
    		featureCreator.writeData(trainData, new File(trainDataFilename));
    		
    		int totalSamples = testData.numInstances() + trainData.numInstances();
    		System.out.println("total samples : " + totalSamples);
    	}
    	
    }
}
