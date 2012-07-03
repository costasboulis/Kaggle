package com.stumbleupon.retention.features;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class NominalFeature extends Feature {
	public static String NEWLINE = System.getProperty("line.separator");
	private int nominal;
	private int numCategories;
	private final Logger logger = LoggerFactory.getLogger(getClass());
	
	public NominalFeature(int numCategories, int i) {
		this.numCategories = numCategories;
		this.nominal = i;
		super.name = null;
	}
	
	public NominalFeature(int numCategories, int i, String name) {
		this.numCategories = numCategories;
		this.nominal = i;
		super.name = name;
	}
	
	
	public String getType() {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < numCategories - 1; i ++) {
			sb.append("@ATTRIBUTE "); sb.append(name); sb.append(Integer.toString(i)); sb.append(" NUMERIC"); 
			sb.append(NEWLINE); 
		}
		sb.append("@ATTRIBUTE "); sb.append(name); sb.append(Integer.toString(numCategories-1));  sb.append(" NUMERIC");
		return sb.toString();
	}
	
	
	public String getValue() {
		if (nominal < 0 || nominal >= numCategories) {
			logger.error("Out of Bounds when creating nominal feature");
			return "?";
		}
		StringBuffer sb = new StringBuffer();
		if (nominal == 0) {
			sb.append("1.0");
		}
		else {
			sb.append("0.0");
		}
		for (int i = 1 ; i < numCategories; i ++) {
			if (i == nominal) {
				sb.append(",1.0");
			}
			else {
				sb.append(",0.0");
			}
		}
		return sb.toString();
	}
	
	public int getNumCategories() {
		return this.numCategories;
	}
	
	public String getName() {
		return this.name;
	}
	
	public int getNominal() {
		return this.nominal;
	}
}
