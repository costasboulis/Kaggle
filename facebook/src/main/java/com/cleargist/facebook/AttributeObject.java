package com.cleargist.facebook;


public class AttributeObject implements Comparable<AttributeObject>{
	private int uid;
	private double score;
	
	public AttributeObject(int uid, double s) {
		this.uid = uid;
		score = s;
	}
	
	public double getScore() {
		return score;
	}
	
	public int getUID() {
		return this.uid;
	}
	
	public int compareTo(AttributeObject us) {
		double diff = this.score - us.getScore();
		if (diff < 0.0) {
			return 1;
		}
		else if (diff > 0.0) {
			return -1;
		}
		return 0;
	}
}

