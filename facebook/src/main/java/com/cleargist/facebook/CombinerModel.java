package com.cleargist.facebook;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CombinerModel extends Model {
	private Logger logger = LoggerFactory.getLogger(getClass());
	private List<HashMap<Integer, List<Integer>>> systems;
	private File[] combinedSystems;
	private double[] systemScores;
	private int length; // Length of the biggest list
	
	public void setCombinedSystems(File[] combined, double[] scores) {
		this.combinedSystems = combined;
		this.systemScores = scores;
	}
	
	public void setLength(int length) {
		this.length = length;
	}
	
	public void train() {
		this.systems = new ArrayList<HashMap<Integer, List<Integer>>>(this.combinedSystems.length);
		for (File system : this.combinedSystems) {
			HashMap<Integer, List<Integer>> hm = new HashMap<Integer, List<Integer>>();
			this.systems.add(hm);
			try {
				BufferedReader reader = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(system))));
				String lineStr = reader.readLine();
				while ((lineStr = reader.readLine()) != null) {
					String[] fields = lineStr.split(",");
					
					int userId = Integer.parseInt(fields[0]);
					List<Integer> l = new ArrayList<Integer>();
					if (fields.length > 1) {
						String[] friends = fields[1].split(" ");
						for (String s : friends) {
							int friendId = Integer.parseInt(s);
							
							l.add(friendId);
						}
					}
					
					
					hm.put(userId, l);
				}
				reader.close();
			}
			catch (FileNotFoundException ex) {
				logger.error("Could not find stats file " + system.getAbsolutePath());
				System.exit(-1);
			}
			catch (IOException ex) {
				logger.error("Could not read from stats file " + system.getAbsolutePath());
				System.exit(-1);
			}
		}
	}
	
	public int[] predict(int userID) {
		HashMap<Integer, Double> hm = new HashMap<Integer, Double>();
		int k = 0;
		for (HashMap<Integer, List<Integer>> system : systems) {
			List<Integer> friends = system.get(userID);
			double rank = this.length;
			for (Integer friend : friends) {
				Double v = hm.get(friend);
				double score = rank * systemScores[k];
				if (v == null) {
					hm.put(friend, score);
				}
				else {
					hm.put(friend, v + score);
				}
				
				rank -= 1.0;
			}
			
			k ++;
		}
		
		List<AttributeObject> l = new ArrayList<AttributeObject>(hm.size());
		for (Map.Entry<Integer, Double> me : hm.entrySet()) {
			l.add(new AttributeObject(me.getKey(), me.getValue()));
		}
		Collections.sort(l);
		
		int len = l.size() > 10 ? 10 : l.size();
		int[] predicted = new int[len];
		k = 0;
		for (AttributeObject attObj : l.subList(0,len)) {
			predicted[k] = attObj.getUID();
			
			k ++;
		}
		
		return predicted;
	}
	
	public static void main(String[] args) {
		String combinedSystem1 = "c:\\kaggle\\ReciprocalWithScorePredictions.csv.gz";
		String combinedSystem2 = "c:\\kaggle\\JaccardPredictions_014087.csv.gz";
		String combinedSystem3 = "c:\\kaggle\\FriendOfFriendsPredictions.csv.gz";
		double[] scores = {0.6199, 0.14087, 0.19340};
		
		File[] combinedSystems = new File[3];
		combinedSystems[0] = new File(combinedSystem1);
		combinedSystems[1] = new File(combinedSystem2);
		combinedSystems[2] = new File(combinedSystem3);
		String testData = "c:\\kaggle\\test.csv";
		String predictions = "c:\\kaggle\\CombinedPredictions_Jaccard_FOF_BackwardLinks.csv";
		
		CombinerModel model = new CombinerModel();
		model.setLength(50);
		model.setCombinedSystems(combinedSystems, scores);
		
		model.train();
		
		model.writePredictions(new File(predictions), new File(testData));
	}
}
