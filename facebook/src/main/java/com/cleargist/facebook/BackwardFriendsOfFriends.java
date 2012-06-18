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

/**
 * A->B<-C => A->C
 * @author kboulis
 *
 */
public class BackwardFriendsOfFriends extends Model {
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
		HashSet<Integer> uniqueUsers = new HashSet<Integer>();
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
				
				if (!uniqueUsers.contains(sourceUserId)) {
					uniqueUsers.add(sourceUserId);
					if (uniqueUsers.size() % 10000 == 0) {
						logger.info("Reading training data for " + uniqueUsers.size() + " users");
					}
				}
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
		
		HashMap<Integer, Integer> hm = new HashMap<Integer, Integer>();
		for (Integer friend : source) {
			HashSet<Integer> target = reverseIndex.get(friend);
			if (target == null) {
				continue;
			}
			
			for (Integer backwdFriend : target) {
				if (source.contains(backwdFriend) || userId == backwdFriend) {
					continue;
				}
				Integer count = hm.get(backwdFriend);
				if (count == null) {
					hm.put(backwdFriend, 1);
				}
				else {
					hm.put(backwdFriend, count + 1);
				}
			}
		}
		
		List<AttributeObject> l = new ArrayList<AttributeObject>(hm.size());
		for (Map.Entry<Integer, Integer> me : hm.entrySet()) {
			l.add(new AttributeObject(me.getKey(), me.getValue()));
		}
		Collections.sort(l);
		
		int len = l.size() > TOP_N_PREDICTED ? TOP_N_PREDICTED : l.size();
		int[] predictedFriends = new int[len];
		int k = 0;
		for (AttributeObject attObj : l.subList(0, len)) {
			predictedFriends[k] = attObj.getUID();
			
			k ++;
		}
		return predictedFriends;
	}
	
	public static void main(String[] args) {
		
		if (args.length != 3) {
			System.err.println("Usage: BackwardFriendsOfFriends trainingDataFile testDataFile predictionsFile");
			System.exit(-1);
		}
		
		String trainingData = args[0];
		String testData = args[1];
		String predictions = args[2];
		
		BackwardFriendsOfFriends model = new BackwardFriendsOfFriends();
		model.setDataFile(new File(trainingData));
		
		model.train();
		
		File predictionsFile = new File(predictions);
		model.writePredictions(predictionsFile, new File(testData));
		
		File predictionsTop10File = new File(predictions + "_top10.txt");
		BackwardFriendsOfFriends.chooseTop10Predictions(predictionsFile, predictionsTop10File);
	}
}
