package com.cleargist.facebook;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReciprocalModel extends Model {
	private Logger logger = LoggerFactory.getLogger(getClass());
	private HashMap<Integer, List<Integer>> reverseIndex;
	private HashMap<Integer, HashSet<Integer>> regularIndex;
	private File dataFile;
	
	public void setDataFile(File dataFile) {
		this.dataFile = dataFile;
	}
	
	public void train() {
		
		logger.info("Beginning training");
		this.reverseIndex = new HashMap<Integer, List<Integer>>();
		this.regularIndex = new HashMap<Integer, HashSet<Integer>>();
		try {
			BufferedReader reader = new BufferedReader(new FileReader(dataFile));
			String line = reader.readLine();
			
			while ((line = reader.readLine()) != null) {
				String[] users = line.split(",");
				int sourceUserId = Integer.parseInt(users[0]);
				int targetUserId = Integer.parseInt(users[1]);
				
				List<Integer> l = reverseIndex.get(targetUserId);
				if (l == null) {
					l = new ArrayList<Integer>();
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
	
	public int[] predict(int userID) {
		List<Integer> l = reverseIndex.get(userID);
		if (l == null) {
			return null;
		}
		
		HashSet<Integer> hs = regularIndex.get(userID);
		
		List<Integer> predictedFriends = new ArrayList<Integer>();
		for (Integer reverseFriendId : l) {
			
			if (hs == null || !hs.contains(reverseFriendId)) {  
				predictedFriends.add(reverseFriendId);
			}
		}
		
		int len = predictedFriends.size() > TOP_N_PREDICTED ? TOP_N_PREDICTED : predictedFriends.size();
		int[] predicted = new int[len];
		int k = 0;
		for (Integer indx : predictedFriends) {
			predicted[k] = indx;
			
			k ++;
			
			if (k >= len) {
				break;
			}
		}
		
		return predicted;
	}
	
	public static void main(String[] args) {
		String trainingData = "c:\\kaggle\\train.csv";
		String testData = "c:\\kaggle\\test.csv";
		String predictions = "c:\\kaggle\\ReciprocalPredictions.csv";
		
		ReciprocalModel model = new ReciprocalModel();
		File dataFile = new File(trainingData);
		
		model.setDataFile(dataFile);
		model.train();
		
		model.writePredictions(new File(predictions), new File(testData));
	}
	
}
