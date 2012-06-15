package com.cleargist.facebook;



import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class InboundJaccardModel extends Model {
	private Logger logger = LoggerFactory.getLogger(getClass());
	private HashMap<Integer, HashSet<Integer>> reverseIndex;
	private HashMap<Integer, HashSet<Integer>> regularIndex;
	private File dataFile;
	
	public void setDataFile(File dataFile) {
		this.dataFile = dataFile;
	}
	
	public void train() {
		logger.info("Beginning training");
		this.reverseIndex = new HashMap<Integer, HashSet<Integer>>();
		this.regularIndex = new HashMap<Integer, HashSet<Integer>>();
		try {
			BufferedReader reader = new BufferedReader(new FileReader(dataFile));
			String line = reader.readLine();
			
			while ((line = reader.readLine()) != null) {
				String[] users = line.split(",");
				int sourceUserId = Integer.parseInt(users[0]);
				int targetUserId = Integer.parseInt(users[1]);
				
				HashSet<Integer> l = reverseIndex.get(targetUserId);
				if (l == null) {
					l = new HashSet<Integer>();
					reverseIndex.put(targetUserId, l);
				}
				l.add(sourceUserId);
				
				HashSet<Integer> r = regularIndex.get(sourceUserId);
				if (r == null) {
					r = new HashSet<Integer>();
					regularIndex.put(sourceUserId, r);
				}
				r.add(targetUserId);
			}
			reader.close();
		}
		catch (FileNotFoundException ex) {
			logger.error("No file \"" + dataFile.getAbsolutePath() + "\"");
			return;
		}
		catch (IOException ex) {
			logger.error("Error while reading from file \"" + dataFile.getAbsolutePath() + "\"");
			return;
		}
		
		logger.info("Finished training");
	}
	
	public int[] predict(int userId) {
		
		HashSet<Integer> source = reverseIndex.get(userId);
		if (source == null) {
			return null;
		}
		double sourceLength = (double)source.size();
		List<AttributeObject> topTargets = new ArrayList<AttributeObject>();
		for (Map.Entry<Integer, HashSet<Integer>> me : reverseIndex.entrySet()) {
			int targetUserId = me.getKey();
			if (userId == targetUserId) {
				continue;
			}
			HashSet<Integer> target = me.getValue();
			HashSet<Integer> hmSource = target.size() < source.size() ? target : source;
			HashSet<Integer> hmTarget = target.size() < source.size() ? source : target;
			double numCommon = 0.0;
			for (Integer index : hmSource) {
				if (hmTarget.contains(index)) {
					numCommon += 1.0;
				}
			}
			
			double jaccard = numCommon > 0.0 ? numCommon / (target.size() + sourceLength - numCommon) : 0.0;
			if (jaccard > 0.0) {
				topTargets.add(new AttributeObject(targetUserId, jaccard));
			}
		}
		Collections.sort(topTargets);
		
		source = regularIndex.get(userId);
		List<Integer> l = new ArrayList<Integer>();
		for (AttributeObject attObj : topTargets) {
			int targetId = attObj.getUID();
			
			if (source != null && source.contains(targetId)) {
				continue;
			}
			if (userId == targetId) {
				continue;
			}
			
			l.add(targetId);
			
			if (l.size() >= 10) {
				break;
			}
		}
		
		if (l.size() == 0) {
			return null;
		}
		int[] predictedFriends = new int[l.size()];
		int k = 0;
		for (Integer targetId : l) {
			predictedFriends[k] = targetId;
			
			k ++;
		}
		return predictedFriends;
	}
	
	public static void main(String[] args) {
		/*
		String trainingData = "c:\\kaggle\\train.csv";
		String testData = "c:\\kaggle\\test2.csv";
		String predictions = "c:\\kaggle\\InboundJaccard2.csv";
		*/
		if (args.length != 3) {
			System.err.println("Usage: InboundJaccardModel trainingDataFile testDataFile predictionsFile");
			System.exit(-1);
		}
		
		String trainingData = args[0];
		String testData = args[1];
		String predictions = args[2];
		
		InboundJaccardModel model = new InboundJaccardModel();
		model.setDataFile(new File(trainingData));
		
		model.train();
		
		model.writePredictions(new File(predictions), new File(testData));
	}
}

