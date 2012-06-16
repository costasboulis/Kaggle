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

public class AdarModel extends Model {
	private Logger logger = LoggerFactory.getLogger(getClass());
	private HashMap<Integer, HashSet<Integer>> regularIndex;
	private File dataFile;
	private List<HashSet<Integer>> userIdsInSameCluster;
	private HashMap<Integer, Integer> clusterMemberships;
	private int numberOfClusters;
	
	public void setNumberOfClusters(int numClusters) {
		this.numberOfClusters = numClusters;
	}
	public void readClusterMemberships(File clusterMembershipsFile) {
		clusterMemberships = new HashMap<Integer, Integer>();
		userIdsInSameCluster = new ArrayList<HashSet<Integer>>(numberOfClusters);
		for (int c = 0; c < numberOfClusters; c ++) {
			userIdsInSameCluster.add(new HashSet<Integer>());
		}
		try {
			BufferedReader reader = new BufferedReader(new FileReader(dataFile));
			String line = null;
			
			while ((line = reader.readLine()) != null) {
				String[] fields = line.split(" ");
				
				double max = 0.0;
				int maxIndex = -1;
				for (int i = 1; i <fields.length - 1; i = i + 2) {
					Double prob = Double.parseDouble(fields[i + 1]);
					if (max < prob) {
						max = prob;
						maxIndex = Integer.parseInt(fields[i]);
					}
				}
				int sourceId = Integer.parseInt(fields[0]);
				clusterMemberships.put(sourceId, maxIndex);
				userIdsInSameCluster.get(maxIndex).add(sourceId);
			}
			reader.close();
		}
		catch (FileNotFoundException ex) {
			logger.error("No file \"" + clusterMembershipsFile.getAbsolutePath() + "\"");
			return;
		}
		catch (IOException ex) {
			logger.error("Error while reading from file \"" + clusterMembershipsFile.getAbsolutePath() + "\"");
			return;
		}
	}
	
	public void setDataFile(File dataFile) {
		this.dataFile = dataFile;
	}
	
	public void train() {
		logger.info("Beginning training");
		this.regularIndex = new HashMap<Integer, HashSet<Integer>>();
		try {
			BufferedReader reader = new BufferedReader(new FileReader(dataFile));
			String line = reader.readLine();
			
			while ((line = reader.readLine()) != null) {
				String[] users = line.split(",");
				int sourceUserId = Integer.parseInt(users[0]);
				int targetUserId = Integer.parseInt(users[1]);
				
				HashSet<Integer> l = regularIndex.get(sourceUserId);
				if (l == null) {
					l = new HashSet<Integer>();
					regularIndex.put(sourceUserId, l);
				}
				l.add(targetUserId);
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
		
		HashSet<Integer> source = regularIndex.get(userId);
		if (source == null) {
			return null;
		}
		
		List<AttributeObject> topTargets = new ArrayList<AttributeObject>();
		for (Map.Entry<Integer, HashSet<Integer>> me : regularIndex.entrySet()) {
			int targetUserId = me.getKey();
			if (userId == targetUserId) {
				continue;
			}
			HashSet<Integer> target = me.getValue();
			HashSet<Integer> hmSource = target.size() < source.size() ? target : source;
			HashSet<Integer> hmTarget = target.size() < source.size() ? source : target;
			HashSet<Integer> overlap = new HashSet<Integer>();
			for (Integer index : hmSource) {
				if (hmTarget.contains(index)) {
					overlap.add(index);
				}
			}
			if (overlap.size() == 0) {
				continue;
			}
			
			double adarScore = 0.0;
			for (Integer commonFriend : overlap) {
				HashSet<Integer> fof = regularIndex.get(commonFriend);
				if (fof == null) {
					adarScore += 1.0;
				}
				else {
					adarScore += 1.0 /(1.0 + Math.log(fof.size() + 1.0));
				}
			}
			
			if (adarScore > 0.0) {
				topTargets.add(new AttributeObject(targetUserId, adarScore));
			}
		}
		Collections.sort(topTargets);
		
		
		List<Integer> l = new ArrayList<Integer>();
		for (AttributeObject attObj : topTargets) {
			int targetId = attObj.getUID();
			
			if (source.contains(targetId) || userId == targetId) {
				continue;
			}
			
			l.add(targetId);
			
			if (l.size() >= TOP_N_PREDICTED) {
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
	String testData = "c:\\kaggle\\test.csv";
	String predictions = "c:\\kaggle\\Adar.csv";
	*/
		if (args.length != 3) {
			System.err.println("Usage: AdarModel trainingDataFile testDataFile predictionsFile");
			System.exit(-1);
		}
		String trainingData = args[0];
		String testData = args[1];
		String predictions = args[2];
		
		AdarModel model = new AdarModel();
		model.setDataFile(new File(trainingData));
	
		model.train();
	
		File predictionsFile = new File(predictions);
		File testDataFile = new File(testData);
		model.writePredictions(predictionsFile, testDataFile);
		
		File predictionsTop10File = new File(predictions + "_top10.txt");
		AdarModel.chooseTop10Predictions(predictionsFile, predictionsTop10File);
	}
	
}
