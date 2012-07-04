package com.stumbleupon.retention;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
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
	private static int COUNTRY_COLUMN = 4;
	private static int EMAIL_COLUMN = 5;
	private static int NUMSTUMBLES_COLUMN = 6;
	private static int NUMTHUMBSDOWN_COLUMN = 7;
	private static int NUMTHUMBSUP_COLUMN = 8;
	private static int ADULT_COLUMN = 9;
	private static int XRATED_COLUMN = 10;
	private static int NUMCOMMENTS_COLUMN = 11;
	private static int NUMHITS_COLUMN = 12;
	private static int CATEGORIES_COLUMN = 13;
	private static int NUMFRIENDS_COLUMN = 14;
	private static int NUMSHARES_COLUMN = 15;
	private static int HASPIC_COLUMN = 16;
	private static int SIGNUPBROWSER_COLUMN = 17;
	private static int S1LENGTH_COLUMN = 18;
	private static int S1NUMSTUMBLES_COLUMN = 19;
	private static int S1NUMTOPICS_COLUMN = 20;
	private static int S1THUMBSUPS_COLUMN = 21;
	private static int S1THUMBSDOWNS_COLUMN = 22;
	private static int S1FIRSTRATING_COLUMN = 23;
	private static int S1FIRSTTHUMBUP_COLUMN = 24;
	private static int S1FIRSTTHUMBDOWN_COLUMN = 25;
	private static int INSTALLDEVICES_COLUMN = 26;
	
	private static int RETAIN_COLUMN = 59;
	private LinkedHashMap<String, Integer> categories;
	private LinkedHashMap<String, Integer> emails;
	private HashMap<String, Integer> signupBrowsers;
	private HashMap<String, Integer> countries;
	private HashMap<String, Integer> installDevices;
	
	public FeatureCreator() {
		emails = new LinkedHashMap<String, Integer>();
		emails.put("gmail", 0);
		emails.put("yahoo", 1);
		emails.put("hotmail", 2);
		emails.put("aol", 3);
		emails.put("edu", 4);
		emails.put("other", 5);
	}
	
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
	
	private Instance extractInteger(String line, String name) {
		FastVector attributes = new FastVector(1);
		attributes.addElement(new Attribute(name));
		Instances instances = new Instances(name, attributes, 1);
		
		Instance instance = new Instance(1);
		instance.setDataset(instances);
		
		if (line == null || line.isEmpty()) {
			instance.setMissing(0);
		}
		else {
			int value = -1;
			try {
				value = Integer.parseInt(line);
				instance.setValue(0, value);
			}
			catch (NumberFormatException ex) {
				logger.warn("Cannot parse integer for \"" + line + "\"");
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
	
	/*
	 * Dummy variable, use N-1 binary variables to represent a categorical variable with N values
	 */
	private Instance extractNominal(String line, String name, HashMap<String, Integer> indices) {
		
		FastVector attributes = new FastVector(indices.size() - 1);
		for (int i = 0; i < indices.size() - 1; i ++) {
			String categoryName = name + i;
			Attribute attribute = new Attribute(categoryName);
			attributes.addElement(attribute);
		}
		Instances instances = new Instances(name, attributes, indices.size() - 1);
		
		Instance instance = new Instance(indices.size() - 1);
		instance.setDataset(instances);
		
		
		
		
		if (line == null || line.isEmpty()) {
			for (int i = 0; i < indices.size() - 1; i ++) {
				instance.setMissing(i);
			}
			return instance;
		}
		
		
		Integer index = indices.get(line);
		if (index == null) {
			for (int i = 0; i < indices.size() - 1; i ++) {
				instance.setMissing(i);
			}
			return instance;
		}
		
		for (int i = 0; i < indices.size() - 1; i ++) {
			instance.setValue(i, 0.0);
		}
		if (index < indices.size() - 1) {
			instance.setValue(index, 1.0);
		}
		return instance;
	}
	
	/*
	 * Dummy variable, use N-1 binary variables to represent a categorical variable with N values
	 */
	private Instance extractEmail(String line) {
		
		FastVector attributes = new FastVector(this.emails.size() - 1);
		for (int i = 0; i < this.emails.size() - 1; i ++) {
			String categoryName = "EMAIL_" + i;
			Attribute attribute = new Attribute(categoryName);
			attributes.addElement(attribute);
		}
		Instances instances = new Instances("email", attributes, this.emails.size() - 1);
		
		Instance instance = new Instance(this.emails.size() - 1);
		instance.setDataset(instances);
		
		
		
		
		if (line == null || line.isEmpty()) {
			for (int i = 0; i < this.emails.size() - 1; i ++) {
				instance.setMissing(i);
			}
			return instance;
		}
		
		String key = "NULL";
		if (line.equals("***@gmail.com")) {
			key = "gmail";
		}
		else if (line.equals("***@yahoo.com")) {
			key = "yahoo";
		}
		else if (line.equals("***@aol.com")) {
			key = "aol";
		}
		else if (line.equals("***@hotmail.com")) {
			key = "hotmail";
		}
		else if (line.equals("***@***")) {
			key = "other";
		}
		else if (line.endsWith("edu")) {
			key = "edu";
		}
		Integer index = emails.get(key);
		if (index == null) {
			for (int i = 0; i < this.emails.size() - 1; i ++) {
				instance.setMissing(i);
			}
			return instance;
		}
		
		for (int i = 0; i < this.emails.size() - 1; i ++) {
			instance.setValue(i, 0.0);
		}
		if (index < emails.size() - 1) {
			instance.setValue(index, 1.0);
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
		signupBrowsers = new HashMap<String, Integer>();
		countries = new HashMap<String, Integer>();
		installDevices = new HashMap<String, Integer>();
		int cnt = 0; int cnt2 = 0; int cnt3 = 0; int cnt4 = 0;
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
				
				String signupBrowser = fields[SIGNUPBROWSER_COLUMN];
				if (!signupBrowsers.containsKey(signupBrowser)) {
					signupBrowsers.put(signupBrowser, cnt2);
					
					cnt2 ++;
				}
				
				String country = fields[COUNTRY_COLUMN];
				if (!countries.containsKey(country)) {
					countries.put(country, cnt3);
					
					cnt3 ++;
				}
				
				String device = fields[INSTALLDEVICE_COLUMN];
				if (!installDevices.containsKey(device)) {
					installDevices.put(device, cnt4);
					
					cnt4 ++;
				}
			}
			reader.close();
			
			logger.info("Found " + categories.size() + " categories");
			logger.info("Found " + signupBrowsers.size() + " browsers");
			logger.info("Found " + countries.size() + " countries");
			logger.info("Found " + installDevices.size() + " devices");
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
				Instance ageInstance = extractInteger(ageField, "AGE");
				
				// GENDER
				String genderField = fields[GENDER_COLUMN];
				Instance genderInstance = extractGender(genderField);
				
				// CITY
				
				// STATE
				
				// COUNTRY
				String countryField = fields[COUNTRY_COLUMN];
				Instance countryInstance = extractNominal(countryField, "COUNTRY", countries);
				
				// EMAIL 
				String emailField = fields[EMAIL_COLUMN];
				Instance emailInstance = extractEmail(emailField);
				
				// NUMSTUMBLES
				String numStumblesField = fields[NUMSTUMBLES_COLUMN];
				Instance numStumblesInstance = extractInteger(numStumblesField, "NUM_STUMBLES");
				
				// NUM_THUMBSDOWN
				String numThumbsDownField = fields[NUMTHUMBSDOWN_COLUMN];
				Instance numThumbsDownInstance = extractInteger(numThumbsDownField, "NUM_THUMBSDOWN");
				
				// NUM_THUMBSUP
				String numThumbsUpField = fields[NUMTHUMBSUP_COLUMN];
				Instance numThumbsUpInstance = extractInteger(numThumbsUpField, "NUM_THUMBSUP");
				
				// ADULT
				String adultField = fields[ADULT_COLUMN];
				Instance adultInstance = extractInteger(adultField, "ADULT");
				
				// XRATED
				HashMap<String, Integer> xratedCategories = new HashMap<String, Integer>();
				xratedCategories.put("0", 0);
				xratedCategories.put("1", 1);
				xratedCategories.put("2", 2);
				String xratedField = fields[XRATED_COLUMN];
				Instance xratedInstance = extractNominal(xratedField, "XRATED", xratedCategories);
				
				// NUM_COMMENTS
				String numCommentsField = fields[NUMCOMMENTS_COLUMN];
				Instance numCommentsInstance = extractInteger(numCommentsField, "NUM_COMMENTS");
				
				// NUM_HITS
				String numHitsField = fields[NUMHITS_COLUMN];
				Instance numHitsInstance = extractInteger(numHitsField, "NUM_HITS");
				
				// CATEGORIES
				String categoriesField = fields[CATEGORIES_COLUMN];
				Instance categoriesInstance = extractCategories(categoriesField);
				
				// NUM_FRIENDS
				String numFriendsField = fields[NUMFRIENDS_COLUMN];
				Instance numFriendsInstance = extractInteger(numFriendsField, "NUM_FRIENDS");
				
				// NUM_SHARES
				String numSharesField = fields[NUMSHARES_COLUMN];
				Instance numSharesInstance = extractInteger(numSharesField, "NUM_SHARES");
				
				// HAS_PIC
				String hasPicField = fields[HASPIC_COLUMN];
				Instance hasPicInstance = extractInteger(hasPicField, "HAS_PIC");
				
				// SIGNUP_BROWSER
				String signupBrowserField = fields[SIGNUPBROWSER_COLUMN];
				Instance signupBrowserInstance = extractNominal(signupBrowserField, "SIGNUP_BROWSER", signupBrowsers);
				
				// S1_LENGTH
				String s1Field = fields[S1LENGTH_COLUMN];
				Instance s1Instance = extractInteger(s1Field, "S1LENGTH");
				
				// S1_NUMSTUMBLES
				String s1NumStumblesField = fields[S1NUMSTUMBLES_COLUMN];
				Instance s1NumStumblesInstance = extractInteger(s1NumStumblesField, "S1NUMSTUMBLES");
				
				// S1_NUMTOPICS
				String s1NumTopicsField = fields[S1NUMTOPICS_COLUMN];
				Instance s1NumTopicsInstance = extractInteger(s1NumTopicsField, "S1NUMTOPICS");
				
				// S1_THUMBSUPS
				String s1ThumbsUpsField = fields[S1THUMBSUPS_COLUMN];
				Instance s1ThumbsUpsInstance = extractInteger(s1ThumbsUpsField, "S1THUMBSUPS");
				
				// S1_THUMBSDOWN
				String s1ThumbsDownsField = fields[S1THUMBSDOWNS_COLUMN];
				Instance s1ThumbsDownsInstance = extractInteger(s1ThumbsDownsField, "S1THUMBSDOWN");
				
				// S1_FIRSTRATING
				String s1FirstRatingField = fields[S1FIRSTRATING_COLUMN];
				Instance s1FirstRatingInstance = extractInteger(s1FirstRatingField, "S1FIRSTRATING");
				
				// S1_FIRSTTHUMBUP
				String s1FirstThumbUpField = fields[S1FIRSTTHUMBUP_COLUMN];
				Instance s1FirstThumbUpInstance = extractInteger(s1FirstThumbUpField, "S1FIRSTTHUMBUP");
				
				// S1_FIRSTTHUMBDOWN
				String s1FirstThumbDownField = fields[S1FIRSTTHUMBDOWN_COLUMN];
				Instance s1FirstThumbDownInstance = extractInteger(s1FirstThumbDownField, "S1FIRSTTHUMBDOWN");
				
				// INSTALL_DEVICE
				String installDevicesField = fields[INSTALLDEVICES_COLUMN];
				Instance installDevicesInstance = extractNominal(installDevicesField, "INSTALL_DEVICES", installDevices);
				
				// RETAIN
				String retainField = fields[RETAIN_COLUMN];
				Instance retainInstance = extractRetain(retainField);
				
				
				Instance instance = new Instance(ageInstance);
				instance = instance.mergeInstance(genderInstance);
				instance = instance.mergeInstance(countryInstance);
				instance = instance.mergeInstance(emailInstance);
				instance = instance.mergeInstance(numStumblesInstance);
				instance = instance.mergeInstance(numThumbsDownInstance);
				instance = instance.mergeInstance(numThumbsUpInstance);
				instance = instance.mergeInstance(adultInstance);
				instance = instance.mergeInstance(xratedInstance);
				instance = instance.mergeInstance(numCommentsInstance);
				instance = instance.mergeInstance(numHitsInstance);
				instance = instance.mergeInstance(categoriesInstance);
				instance = instance.mergeInstance(numFriendsInstance);
				instance = instance.mergeInstance(numSharesInstance);
				instance = instance.mergeInstance(hasPicInstance);
				instance = instance.mergeInstance(signupBrowserInstance);
				instance = instance.mergeInstance(s1Instance);
				instance = instance.mergeInstance(s1NumStumblesInstance);
				instance = instance.mergeInstance(s1NumTopicsInstance);
				instance = instance.mergeInstance(s1ThumbsUpsInstance);
				instance = instance.mergeInstance(s1ThumbsDownsInstance);
				instance = instance.mergeInstance(s1FirstRatingInstance);
				instance = instance.mergeInstance(s1FirstThumbUpInstance);
				instance = instance.mergeInstance(s1FirstThumbDownInstance);
				instance = instance.mergeInstance(installDevicesInstance);
				
				instance = instance.mergeInstance(retainInstance);
				String classLabel = retainInstance.stringValue(0);
				
				
				if (instances == null) {
					FastVector attributes = new FastVector();
					attributes.appendElements(getAttributes(ageInstance));
					attributes.appendElements(getAttributes(genderInstance));
					attributes.appendElements(getAttributes(countryInstance));
					attributes.appendElements(getAttributes(emailInstance));
					attributes.appendElements(getAttributes(numStumblesInstance));
					attributes.appendElements(getAttributes(numThumbsDownInstance));
					attributes.appendElements(getAttributes(numThumbsUpInstance));
					attributes.appendElements(getAttributes(adultInstance));
					attributes.appendElements(getAttributes(xratedInstance));
					attributes.appendElements(getAttributes(numCommentsInstance));
					attributes.appendElements(getAttributes(numHitsInstance));
					attributes.appendElements(getAttributes(categoriesInstance));
					attributes.appendElements(getAttributes(numFriendsInstance));
					attributes.appendElements(getAttributes(numSharesInstance));
					attributes.appendElements(getAttributes(hasPicInstance));
					attributes.appendElements(getAttributes(signupBrowserInstance));
					attributes.appendElements(getAttributes(s1Instance));
					attributes.appendElements(getAttributes(s1NumStumblesInstance));
					attributes.appendElements(getAttributes(s1NumTopicsInstance));
					attributes.appendElements(getAttributes(s1ThumbsUpsInstance));
					attributes.appendElements(getAttributes(s1ThumbsDownsInstance));
					attributes.appendElements(getAttributes(s1FirstRatingInstance));
					attributes.appendElements(getAttributes(s1FirstThumbUpInstance));
					attributes.appendElements(getAttributes(s1FirstThumbDownInstance));
					attributes.appendElements(getAttributes(installDevicesInstance));
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
    		FeatureCreator.writeData(testData, new File(testDataFilename));
    		
    		String trainDataFilename = "c:\\StumbleUpon\\data\\train-" + numFold + ".arff";
    		FeatureCreator.writeData(trainData, new File(trainDataFilename));
    		
    		int totalSamples = testData.numInstances() + trainData.numInstances();
    		System.out.println("total samples : " + totalSamples);
    	}
    	
    }
}
