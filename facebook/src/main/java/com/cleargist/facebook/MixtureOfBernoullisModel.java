package com.cleargist.facebook;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



public class MixtureOfBernoullisModel extends Model {
	private Logger logger = LoggerFactory.getLogger(getClass());
	private List<HashMap<Integer, Double>> logProb;
	private List<List<AttributeObject>> prob; // For each cluster, ranked list of p( edge | cluster)
	private List<Double> logPriors;
	private List<Double> logProbUnseen;
	private List<HashMap<Integer, Double>> ss1;
	private double[] ss0;
	private double numberOfUsers;
	private int numberOfClusters;
	private int numberOfIterations; 
	private int numberOfUsersPerChunk;
	private static double ALPHA = 0.01;
	private static double BETA = 1.0;
	private static double UPPER_LOG_THRESHOLD = 10.0;
	private static double LOWER_LOG_THRESHOLD = -10.0;
	private static int TOP_N = 20;
	private static double SUM_CLUSTER_THRESHOLD = 0.98;  // If the sum_m{p(c|user)} is less than threshold do a normalization of cluster posteriors 
	private static String SUFF_STATS_DIRECTORY = "C:\\kaggle\\MixBernoulliStats";
	private Random random;
	
	public void setNumberOfClusters(int c) {
		this.numberOfClusters = c;
	}
	
	public void setNumberOfIterations(int iters) {
		this.numberOfIterations = iters;
	}
	
	public void setNumberOfUsersPerChunk(int numUsers) {
		this.numberOfUsersPerChunk = numUsers;
	}
	
	public void trainSingleIteration(boolean isInitial) {
		
		// Calculate the sufficient statistics
		File path = new File(SUFF_STATS_DIRECTORY);
		if (!path.exists()) {
			path.mkdir();
		}
		File[] listOfFiles = path.listFiles();
		for (int i = 0; i < listOfFiles.length; i ++) {
			if (listOfFiles[i].isFile())  {
				listOfFiles[i].delete();
			}
		}
		
		int startIndx = 0;
		int endIndx = startIndx + numberOfUsersPerChunk;
		int chunk = 0;
		while (true) {
			if (endIndx > data.size()) {
				break;
			}
			logger.info("Calculating sufficient statistics for chunk " + chunk);
			calculateSufficientStatistics(startIndx, endIndx, path, isInitial);
			startIndx = endIndx;
			endIndx = startIndx + numberOfUsersPerChunk;
			chunk ++;
		}
		logger.info("Calculating sufficient statistics for chunk " + chunk);
		calculateSufficientStatistics(startIndx, data.size(), path, isInitial);
		
		// Now merge the sufficient statistics
		logger.info("Merging suff. stats");
		listOfFiles = path.listFiles();
		File mergedStatsFile = null;
		if (listOfFiles[0].isFile())  {
			mergedStatsFile = listOfFiles[0];
		}
		for (int i = 1; i < listOfFiles.length; i++) {
		 
			if (listOfFiles[i].isFile())  {
			   File statsFile = listOfFiles[i];
			   
			   mergeSufficientStatistics(mergedStatsFile, statsFile);
			}
		}
		
		
		// Estimate the parameters
		this.estimate(mergedStatsFile);
	}
	
	public void train() {
		
		logger.info("Initializing model");
		this.random = new Random();
		trainSingleIteration(true);
		for (int iter = 1; iter < this.numberOfIterations; iter ++) {
			logger.info("Iteration " + iter);
			trainSingleIteration(false);
		}
		
	}

	private void initSufficientStats() {
		this.ss1 = new ArrayList<HashMap<Integer, Double>>();
		for (int m = 0; m < this.numberOfClusters; m ++) {
			this.ss1.add(new HashMap<Integer, Double>());
		}
		this.ss0 = new double[this.numberOfClusters];
		this.numberOfUsers = 0.0;
	}
	
	private void mergeSufficientStatistics(File mergedStatsFile, File statsFile) {
		
		initSufficientStats();
		
		double chunkLogProb = 0.0;
		double totLogProb = 0.0;
		// Load in memory the statsFile
		try {
			BufferedReader reader = new BufferedReader(new FileReader(statsFile));
			String lineStr = reader.readLine();
			chunkLogProb = Double.parseDouble(lineStr);
			lineStr = reader.readLine();
			this.numberOfUsers = Double.parseDouble(lineStr);
			lineStr = reader.readLine();
			String[] fields = lineStr.split(" ");
			for (int m = 0; m < this.numberOfClusters; m ++) {
				this.ss0[m] = Double.parseDouble(fields[m]);
			}
			while ((lineStr = reader.readLine()) != null) {
				fields = lineStr.split(" ");
				int m = Integer.parseInt(fields[0]);
				
				HashMap<Integer, Double> hm = ss1.get(m);
				if (hm == null) {
					hm = new HashMap<Integer, Double>();
					ss1.set(m, hm);
				}
				for (int k = 1; k < fields.length - 1; k = k + 2) {
					int indx = Integer.parseInt(fields[k]);
					double value = Double.parseDouble(fields[k + 1]);
					
					hm.put(indx, value);
				}
			}
			reader.close();
		}
		catch (FileNotFoundException ex) {
			logger.error("Could not find stats file " + statsFile.getAbsolutePath());
			System.exit(-1);
		}
		catch (IOException ex) {
			logger.error("Could not read from stats file " + statsFile.getAbsolutePath());
			System.exit(-1);
		}
		
		
		
		
		// Read line-by-line the mergedKey, merge with statsKey and write to new local file
		String localFilename = "merged_" + "_" + statsFile.getName();
		File localFile = new File(localFilename);
		BufferedWriter bw = null;
		try {
			bw = new BufferedWriter(new FileWriter(localFile));
		}
		catch (Exception ex) {
			logger.error("Could not write to stats file " + localFile.getAbsolutePath());
			System.exit(-1);
		}
		
		try {
			BufferedReader reader = new BufferedReader(new FileReader(mergedStatsFile));
			String lineStr = reader.readLine();
			totLogProb = chunkLogProb + Double.parseDouble(lineStr);
			lineStr = reader.readLine();
			double mergedN = Double.parseDouble(lineStr) + this.numberOfUsers;
			StringBuffer sb = new StringBuffer();
			sb.append(totLogProb); sb.append(newline);
			sb.append(mergedN); sb.append(newline);
			
			lineStr = reader.readLine();
			String[] fields = lineStr.split(" ");
			double v = Double.parseDouble(fields[0]) + ss0[0];
			sb.append(v);
			for (int m = 1; m < this.numberOfClusters; m ++) {
				v = Double.parseDouble(fields[m]) + ss0[m];
				sb.append(" "); sb.append(v);
			}
			sb.append(newline);
			
			try {
				bw.write(sb.toString());
				bw.flush();
			}
			catch (Exception ex) {
				logger.error("Could not write to stats file " + localFile.getAbsolutePath());
				System.exit(-1);
			}
			
			
			while ((lineStr = reader.readLine()) != null) {
				fields = lineStr.split(" ");
				int m = Integer.parseInt(fields[0]);
				
				sb = new StringBuffer();
				sb.append(m);
				HashMap<Integer, Double> hm = this.ss1.get(m);
				for (int k = 1; k < fields.length - 1; k = k + 2) {
					int indx = Integer.parseInt(fields[k]);
					double value = Double.parseDouble(fields[k + 1]);
					
					Double val = hm.get(indx);
					if (val == null) {
						sb.append(" "); sb.append(indx); sb.append(" "); sb.append(value);
					}
					else {
						sb.append(" "); sb.append(indx); sb.append(" "); sb.append(value + val);
					}
				}
				sb.append(newline);
				bw.write(sb.toString());
				bw.flush();
			}
			reader.close();
		}
		catch (FileNotFoundException ex) {
			logger.error("Could not find stats file " + mergedStatsFile.getAbsolutePath());
			System.exit(-1);
		}
		catch (IOException ex) {
			logger.error("Could not read from stats file " + mergedStatsFile.getAbsolutePath());
			System.exit(-1);
		}
		
		try {
			bw.close();
		}
		catch (Exception ex) {
			logger.error("Could not write to stats file " + localFile.getAbsolutePath());
			System.exit(-1);
		}
		
		
		// Remove stats file
		statsFile.delete();
		mergedStatsFile.delete();
		localFile.renameTo(mergedStatsFile);
	}
	
	private void calculateSufficientStatistics(int startIndex, int endIndex, File path, boolean isInitial) {
		
		initSufficientStats();
		double totLogProb = 0.0;
		for (User user : data.subList(startIndex, endIndex)) {
			
			// Now update the sufficient statistics
			double[] probs = isInitial ? calculateInitialClusterPosteriors() : calculateClusterPosteriors(user);
			for (int m = 0 ; m < this.numberOfClusters; m ++) {
				if (probs[m] == 0.0) {
					continue;
				}
				
				HashMap<Integer, Double> hm = ss1.get(m);
				if (hm == null) {
					hm = new HashMap<Integer, Double>();
					ss1.set(m, hm);
				}
				for (Integer indx : user.getFriends()) {
					Double v = hm.get(indx);
					if (v == null) {
						hm.put(indx, probs[m]);
					}
					else {
						hm.put(indx, v + probs[m]);
					}
				}
				
				ss0[m] += probs[m];
			}
			
			this.numberOfUsers += 1.0;
			
			totLogProb += isInitial ? 0.0 : calculateLogProb(probs, user);
		}
		
		// Now write the sufficient statistics
		File localFile = new File(path.getAbsolutePath() + "\\" + "stats_" + startIndex + "_" + endIndex + ".txt");
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(localFile));
			bw.write(totLogProb + newline);
			bw.write(this.numberOfUsers + newline);
			StringBuffer sb = new StringBuffer();
			sb.append(ss0[0]);
			for (int m = 1; m < this.numberOfClusters; m ++) {
				sb.append(" "); sb.append(ss0[m]);
			}
			sb.append(newline);
			bw.write(sb.toString());
			for (int m = 0; m < this.numberOfClusters; m ++) {
				sb = new StringBuffer();
				sb.append(m);
				for (Map.Entry<Integer, Double> entry : ss1.get(m).entrySet()) {
					sb.append(" "); sb.append(entry.getKey()); sb.append(" "); sb.append(entry.getValue());
				}
				sb.append(newline);
				bw.write(sb.toString());
				bw.flush();
			}
			bw.flush();
			bw.close();
		}
		catch (Exception ex) {
			logger.error("Could not write to local file " + localFile.getAbsolutePath());
			System.exit(-1);
		}
	}
	
	private double[] calculateInitialClusterPosteriors() {
		double[] probs = new double[this.numberOfClusters];
		
		int m = this.random.nextInt(this.numberOfClusters);
		
		for (int v = 0; v < this.numberOfClusters; v ++) {
			probs[v] = m == v ? 1.0 : 0.0;
		}
		
		return probs;
	}
	
	private double[] calculateClusterPosteriors(User user) {
		
		double[] logProbs = new double[numberOfClusters];
		
		for (int m = 0; m < numberOfClusters; m ++) {
			logProbs[m] = logPriors.get(m);
			for (Integer indx : user.getFriends()) {
				
				Double vTmp = logProb.get(m).get(indx);
				Double v = vTmp == null ? logProbUnseen.get(m) : vTmp;
				
				logProbs[m] += v;
			}
		}
		
		// Now that the logProbs are estimated calculate p[m]
		double[] probs = new double[numberOfClusters];
		double totProbs = 0.0;
		for (int m = 0; m < numberOfClusters; m ++) {
			boolean probsFound = false;
			List<Double> diffs = new ArrayList<Double>();
			for (int j = 0; j < numberOfClusters; j ++) {
				if (m == j) {
					continue;
				}
				
				double diff = logProbs[j] - logProbs[m];
				if (diff > UPPER_LOG_THRESHOLD) {
					probs[m] = 0.0;
					
					probsFound = true;
					break;
				}
				else if (diff < LOWER_LOG_THRESHOLD) {
					continue;
				}
				diffs.add(diff);
			}
			if (!probsFound) {
				double sum = 0.0;
				for (Double d : diffs) {
					sum += Math.exp(d);
				}
				probs[m] = 1.0 / (1.0 + sum);
				
				totProbs += probs[m];
			}
		}
		
		// Do a final normalization
		if (totProbs < SUM_CLUSTER_THRESHOLD) {
			for (int m = 0; m < numberOfClusters; m ++) {
				probs[m] /= totProbs;
			}
		}
		
		return probs;
	}
	
	public void estimate(File mergedStatsFile) {
		logger.info("Estimating parameters");
		try {
			BufferedReader reader = new BufferedReader(new FileReader(mergedStatsFile));
			String lineStr = reader.readLine();
			double totLogProb = Double.parseDouble(lineStr);
			logger.info("Log prob : " + totLogProb);
			lineStr = reader.readLine();
			this.numberOfUsers = 0.0;
			try {
				this.numberOfUsers = Double.parseDouble(lineStr);
			}
			catch (NumberFormatException ex) {
				logger.error("Cannot parse value of numberOfUsers : " + lineStr);
				System.exit(-1);
			}
			if (this.numberOfUsers <= 0.0) {
				logger.info("Invalid value for numberOfUsers " + this.numberOfUsers);
				System.exit(-1);
			}
			
			// Estimate logPriors and logProbUnseen
			lineStr = reader.readLine();
			String[] fields = lineStr.split(" ");
			
			this.logPriors = new ArrayList<Double>(this.numberOfClusters);
			this.logProbUnseen = new ArrayList<Double>(this.numberOfClusters);
			double[] ss0 = new double[this.numberOfClusters];
			for (int m = 0; m < this.numberOfClusters; m ++) {
				double v = 0.0;
				try {
					v = Double.parseDouble(fields[m]);
				}
				catch (NumberFormatException ex) {
					logger.error("Cannot parse value " + fields[m]);
					System.exit(-1);
				}
				if (v <= 0.0) {
					logger.error("Invalid counts for cluster " + m + "  : (" + v + ")");
					System.exit(-1);
				}
				ss0[m] = v;
				
				double u = Math.log(v / this.numberOfUsers);
				logPriors.add(u);
				
				double v1 = Math.log(ALPHA / (v + ALPHA + BETA));
				
				logProbUnseen.add(v1);
			}
			
			// Estimate logProb
			this.logProb = new ArrayList<HashMap<Integer, Double>>(this.numberOfClusters);
			for (int m = 0; m < this.numberOfClusters; m ++) {
				this.logProb.add(new HashMap<Integer, Double>());
			}
			while ((lineStr = reader.readLine()) != null) {
				fields = lineStr.split(" ");
				int m = -1;
				try {
					m = Integer.parseInt(fields[0]);
				}
				catch (NumberFormatException ex) {
					logger.error("Cannot parse cluster index " + m);
					System.exit(-1);
				}
				if (m < 0) {
					logger.error("Invalid value for cluster index (" + m + ")");
					System.exit(-1);
				}
				
				for (int i = 1; i < fields.length - 1; i = i + 2) {
					
					int indx = -1;
					try {
						indx = Integer.parseInt(fields[i]);
					}
					catch (NumberFormatException ex) {
						logger.error("Cannot parse attribute index " + fields[i]);
						System.exit(-1);
					}
					
					double v = 0.0;
					try {
						v = Double.parseDouble(fields[i + 1]);
					}
					catch (NumberFormatException ex) {
						logger.error("Cannot parse attribute counts " + fields[i + 1]);
						System.exit(-1);
					}
					
					double logv1 = Math.log((v + ALPHA) / (ss0[m] + ALPHA + BETA));
					
					logProb.get(m).put(indx, logv1);
				}
			}
			reader.close();
		}
		catch (FileNotFoundException ex) {
			logger.error("Cannot find file " + mergedStatsFile.getAbsolutePath());
			System.exit(-1);
		}
		catch (IOException ex) {
			logger.error("Cannot read from file " + mergedStatsFile.getAbsolutePath());
			System.exit(-1);
		}
		
		// Now estimate prob(edge | cluster) - needed for the prediction
		this.prob = new ArrayList<List<AttributeObject>>(this.numberOfClusters);
		for (int m = 0; m < this.numberOfClusters; m ++) {
			List<AttributeObject> l = new ArrayList<AttributeObject>(logProb.get(m).size());
			for (Map.Entry<Integer, Double> me : logProb.get(m).entrySet()) {
				l.add(new AttributeObject(me.getKey(), me.getValue()));
			}
			Collections.sort(l);
			
			int len = l.size() >= TOP_N ? TOP_N : l.size();
			List<AttributeObject> topNList = new ArrayList<AttributeObject>(len);
			for (AttributeObject attObj : l) {
				topNList.add(new AttributeObject(attObj.getUID(), Math.exp(attObj.getScore())));
				
				if (topNList.size() >= TOP_N)  {
					break;
				}
			}
			
			this.prob.add(topNList);
		}
	}
	
	private double calculateLogProb(double[] posteriors, User user) {
		
		double maxPosterior = 0.0;
		int maxIndex = 0;
		for (int m = 0; m < this.numberOfClusters; m ++) {
			if (posteriors[m] > maxPosterior) {
				maxPosterior = posteriors[m];
				maxIndex = m;
			}
		}
		
		double totProb = 0.0;
		for (Integer indx : user.getFriends()) {
			Double logV = logProb.get(maxIndex).get(indx);
			if (logV == null) {
				totProb += logProbUnseen.get(maxIndex);
			}
			else {
				totProb += logV;
			}
		}
		
		return totProb;
	}
	
	public void writeClusterMemberships(File memberships) {
		BufferedWriter writer = null;
		try {
			writer = new BufferedWriter(new FileWriter(memberships));
			for (User user : data) {
				StringBuffer sb = new StringBuffer();
				sb.append(user.getUserID());
				double[] clusterProbs = calculateClusterPosteriors(user);
				for (int m = 0; m < numberOfClusters; m ++) {
					if (clusterProbs[m] == 0.0) {
						continue;
					}
					
					sb.append(" "); sb.append(m); sb.append(" "); sb.append(clusterProbs[m]);
				}
				sb.append(newline);
				writer.write(sb.toString());
				writer.flush();
			}
			writer.close();
		}
		catch (IOException ex) {
			logger.error("Cannot write to file " + memberships.getAbsolutePath());
			System.exit(-1);
		}
		
		
	}
	
	public int[] predict(int userId) {
		int indx = Collections.binarySearch(data, new User(userId, null));
		if (indx < 0) {  // UserId does not appear in training data
			return null;
		}
		
		User user = data.get(indx);
		double[] clusterProbs = calculateClusterPosteriors(user);
		
		HashMap<Integer, Double> hm = new HashMap<Integer, Double>();
		for (int m = 0; m < numberOfClusters; m ++) {
			if (clusterProbs[m] == 0.0) {
				continue;
			}
			
			List<AttributeObject> topList = prob.get(m).size() > TOP_N ? prob.get(m).subList(0, TOP_N) : prob.get(m);
			for (AttributeObject attObj : topList) {
				Double score = hm.get(attObj.getUID());
				if (score == null) {
					hm.put(attObj.getUID(), clusterProbs[m] * attObj.getScore());
				}
				else {
					hm.put(attObj.getUID(), score + clusterProbs[m] * attObj.getScore());
				}
			}
		}
		
		if (hm.size() == 0) {
			return null;
		}
		
		List<AttributeObject> rankedList = new ArrayList<AttributeObject>(hm.size());
		for (Map.Entry<Integer, Double> me : hm.entrySet()) {
			int uId = me.getKey();
			if (!user.getFriends().contains(uId)) {
				rankedList.add(new AttributeObject(me.getKey(), me.getValue()));
			}
		}
		Collections.sort(rankedList);
		
		int len = rankedList.size() > TOP_N_PREDICTED ? TOP_N_PREDICTED : rankedList.size();
		int[] results = new int[len];
		int k = 0;
		for (AttributeObject attObj : rankedList) {
			results[k] = attObj.getUID();
			k ++;
			
			if (k >= len) {
				break;
			}
		}
		
		return results;
	}
	
	public static void main(String[] args) {
		String trainingData = "c:\\kaggle\\train.csv";
		String testData = "c:\\kaggle\\test.csv";
		String predictions = "c:\\kaggle\\MixBernoullis_trial_2.csv";

		
		MixtureOfBernoullisModel model = new MixtureOfBernoullisModel();
		model.setNumberOfClusters(10);
		model.setNumberOfIterations(5);
		model.setNumberOfUsersPerChunk(100000);
		
		File dataFile = new File(trainingData);
		model.readData(dataFile);
		model.train();
		
//		model.writeClusterMemberships(new File("c:\\kaggle\\clusterMemberships.txt"));
		model.writePredictions(new File(predictions), new File(testData));
	}
	
}
