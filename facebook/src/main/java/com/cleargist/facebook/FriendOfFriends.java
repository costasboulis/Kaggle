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

public class FriendOfFriends extends Model {
	private Logger logger = LoggerFactory.getLogger(getClass());
	private HashMap<Integer, HashSet<Integer>> regularIndex;
	private File dataFile;
	
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
		
		HashSet<Integer> hs = regularIndex.get(userID);
		if (hs == null) {
			return null;
		}
		
		HashMap<Integer, Integer> countsOfFriends = new HashMap<Integer, Integer>();
		for (Integer friendId : hs) {
			HashSet<Integer> friendsOfFriend = regularIndex.get(friendId);
			if (friendsOfFriend == null) {
				continue;
			}
			
			for (Integer friendOfFriendsId : friendsOfFriend) {
				if (hs.contains(friendOfFriendsId) || friendOfFriendsId == userID) {
					continue;
				}
				Integer cnt = countsOfFriends.get(friendOfFriendsId);
				if (cnt == null) {
					countsOfFriends.put(friendOfFriendsId, 1);
				}
				else {
					countsOfFriends.put(friendOfFriendsId, cnt + 1);
				}
			}
			
		}
		if (countsOfFriends.size() == 0) {
			return null;
		}
		
		List<AttributeObject> rankedList = new ArrayList<AttributeObject>(countsOfFriends.size());
		for (Map.Entry<Integer, Integer> me : countsOfFriends.entrySet()) {
			rankedList.add(new AttributeObject(me.getKey(), me.getValue()));
		}
		Collections.sort(rankedList);
		
		int len = rankedList.size() > TOP_N_PREDICTED ? TOP_N_PREDICTED : rankedList.size();
		int[] predicted = new int[len];
		int k = 0;
		for (AttributeObject attObj : rankedList) {
			predicted[k] = attObj.getUID();
			
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
		String predictions = "c:\\kaggle\\FriendOfFriendsPredictions.csv";
		
		FriendOfFriends model = new FriendOfFriends();
		File dataFile = new File(trainingData);
		
		model.setDataFile(dataFile);
		model.train();
		
		model.writePredictions(new File(predictions), new File(testData));
	}
}
