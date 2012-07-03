package com.stumbleupon.retention.features;

public abstract class Feature {
	protected String name;
	public enum FeatureType {NUMERIC, NOMINAL, STRING, DATE};
	
	protected FeatureType type;
	
	public String getName() {
		return name != null ? name : "FEATURE";
	}
	
	public abstract String getType();
	public abstract String getValue();
}

