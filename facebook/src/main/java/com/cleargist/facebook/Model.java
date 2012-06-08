package com.cleargist.facebook;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public abstract class Model {
	private Logger logger = LoggerFactory.getLogger(getClass());
	public static String newline = System.getProperty("line.separator");
	protected List<User> data;
	
	/**
	 * Reads data from file. Assumes that all target users of the same source user are written consecutively
	 * 
	 * @param dataFile
	 */
	public void readData(File dataFile) {
		try {
			BufferedReader reader = new BufferedReader(new FileReader(dataFile));
			String line = reader.readLine();
			
			data = new LinkedList<User>();
			line = reader.readLine();
			String[] users = line.split(",");
			int sourceUserId = Integer.parseInt(users[0]);
			int targetUserId = Integer.parseInt(users[1]);
			int currentUserId = sourceUserId;
			List<Integer> friends = new LinkedList<Integer>();
			friends.add(targetUserId);
			int numConnections = 1;
			while ((line = reader.readLine()) != null) {
				if (data.size() % 100000 == 0) {
					logger.info("Read " + data.size() + " users");
				}
				users = line.split(",");
				sourceUserId = Integer.parseInt(users[0]);
				targetUserId = Integer.parseInt(users[1]);
				
				if (currentUserId != sourceUserId) {
					User user = new User(currentUserId, friends);
					data.add(user);
					friends = new LinkedList<Integer>();
					currentUserId = sourceUserId;
				}
				
				friends.add(targetUserId);
				
				numConnections ++;
			}
			// Persist last user
			User user = new User(currentUserId, friends);
			data.add(user);
			reader.close();
			
			logger.info("Read " + data.size() + " users and " + numConnections + " connections");
		}
		catch (FileNotFoundException ex) {
			logger.error("No file \"" + dataFile.getAbsolutePath() + "\"");
			return;
		}
		catch (IOException ex) {
			logger.error("Error while reading from file \"" + dataFile.getAbsolutePath() + "\"");
			return;
		}
		
	}
	
	public void writePredictions(File outFile, File inFile) throws Exception {
		try {
			BufferedReader reader = new BufferedReader(new FileReader(inFile));
			BufferedWriter out = new BufferedWriter(new FileWriter(outFile));
			try {
				out.write("source_node,destination_nodes" + newline);
			}
			catch (IOException ex) {
				logger.error("Error while writing to file \"" + outFile.getAbsolutePath() + "\"");
				return;
			}
			
			String line = reader.readLine();
			while ((line = reader.readLine()) != null) {
				int userID = Integer.parseInt(line);
				
				int[] friends = predict(userID);
				
				StringBuffer sb = new StringBuffer();
				sb.append(userID);
				for (int i = 0; i < friends.length; i ++) {
					if (i == 0) {
						sb.append(",");
					}
					else {
						sb.append(" ");
					}
					sb.append(friends[i]);
				}
				sb.append(newline);
				try {
					out.write(sb.toString());
					out.flush();
				}
				catch (IOException ex) {
					logger.error("Error while writing to file \"" + outFile.getAbsolutePath() + "\"");
					return;
				}
			}
			reader.close();
			out.close();
		}
		catch (FileNotFoundException ex) {
			logger.error("No file \"" + inFile.getAbsolutePath() + "\"");
			return;
		}
		catch (IOException ex) {
			logger.error("Error while reading from file \"" + inFile.getAbsolutePath() + "\"");
			return;
		}
		
	}
	
	public float calculateMeanAveragePrecision(File referenceFile) {
		return 0.0f;
	}
	
	public abstract int[] predict(int userID);
}
