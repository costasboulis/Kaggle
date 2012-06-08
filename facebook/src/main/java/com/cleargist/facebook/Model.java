package com.cleargist.facebook;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
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
			HashSet<Integer> friends = new HashSet<Integer>();
			friends.add(targetUserId);
			int numConnections = 1;
			while ((line = reader.readLine()) != null) {
				users = line.split(",");
				sourceUserId = Integer.parseInt(users[0]);
				targetUserId = Integer.parseInt(users[1]);
				
				if (currentUserId != sourceUserId) {
					User user = new User(currentUserId, friends);
					data.add(user);
					if (data.size() % 100000 == 0) {
						logger.info("Read " + data.size() + " users");
					}
					friends = new HashSet<Integer>();
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
	
	public void writePredictions(File outFile, File inFile) {
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
			int numUsers = 0;
			while ((line = reader.readLine()) != null) {
				
				if (numUsers % 100 == 0) {
					logger.info("Processed " + numUsers + " users");
				}
				
				int userID = Integer.parseInt(line);
				
				int[] friends = predict(userID);
				
				StringBuffer sb = new StringBuffer();
				sb.append(userID); sb.append(",");
				if (friends != null) {
					for (int i = 0; i < friends.length; i ++) {
						if (i > 0) {
							sb.append(" ");
						}
						sb.append(friends[i]);
					}
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
				
				numUsers ++;
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
	
	private double calculatePrecisionAtLevelK(int[] predicted, HashSet<Integer> reference, int k) {
		int positionK = k >= predicted.length ? predicted.length - 1 : k;
		
		double numFound = 0.0;
		for (int i = 0; i <= positionK; i ++) {
			boolean found = reference.contains(predicted[i]);
			if (found) {
				numFound += 1.0;
			}
		}
		double precision = numFound > 0.0 ? numFound / (double)(positionK + 1.0) : 0.0;
		
		return precision;
	}
	
	private double calculateAveragePrecision(int[] predicted, HashSet<Integer> reference) {
		double sum = 0.0;
		for (int k = 0; k < predicted.length; k ++) {
			boolean found = reference.contains(predicted[k]);
			if (found) {
				sum += calculatePrecisionAtLevelK(predicted, reference, k);
			}
		}
		
		return sum / (double)reference.size();
	}
	
	public double calculateMeanAveragePrecision(File referenceFile) {
		double map = 0.0;
		double numberOfUsers = 0.0;
		int numberOfNonPredictions = 0;
		try {
			BufferedReader reader = new BufferedReader(new FileReader(referenceFile));
			String line = reader.readLine();
			while ((line = reader.readLine()) != null) {
				String[] fields = line.split(",");
				
				int userID = Integer.parseInt(fields[0]);
				if (fields.length < 2) {
					// No reference data
					numberOfUsers += 1.0;
					continue;
				}
				
				int[] predicted = predict(userID);
				if (predicted == null) {
					numberOfNonPredictions ++;
					// average precision is zero
				}
				else {
					String[] referenceFriends = fields[1].trim().split(" ");
					HashSet<Integer> reference = new HashSet<Integer>();
					for (int i = 0; i < referenceFriends.length; i ++) {
						int ref = Integer.parseInt(referenceFriends[i]);
						
						reference.add(ref);
					}
					
					double avgPrecision = calculateAveragePrecision(predicted, reference);
					
					map += avgPrecision;
				}
				
				
				numberOfUsers += 1.0;
			}
			reader.close();
			
			logger.info("Could not make predictions for " + numberOfNonPredictions + " users");
			return map / numberOfUsers;
		}
		catch (FileNotFoundException ex) {
			logger.error("No file \"" + referenceFile.getAbsolutePath() + "\"");
		}
		catch (IOException ex) {
			logger.error("Error while reading from file \"" + referenceFile.getAbsolutePath() + "\"");
		}
		return 0.0;
	}
	
	public abstract int[] predict(int userID);
	
	public abstract void train(File trainingFile);
}
