package com.cleargist.facebook;


import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO : Compute sparse co-occurrence matrix 250K x 1.6M and store it in disk

public class JaccardModel extends Model {
	private Logger logger = LoggerFactory.getLogger(getClass());
	
	
	public void train() {
		
	}
	
	public int[] predict(int userId) {
		// Find the destination nodes for userId
		int indx = Collections.binarySearch(data, new User(userId, null));
		if (indx < 0) {  // UserId does not appear in training data
			return null;
		}
		
		
		HashSet<Integer> source = data.get(indx).getFriends();
		double sourceLength = (double)source.size();
		List<AttributeObject> topTargets = new ArrayList<AttributeObject>();
		for (User targetUser : data) {
			if (userId == targetUser.getUserID()) {
				continue;
			}
			HashSet<Integer> target = targetUser.getFriends();
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
				topTargets.add(new AttributeObject(targetUser.getUserID(), jaccard));
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
}
