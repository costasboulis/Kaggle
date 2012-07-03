package com.stumbleupon.retention.features;

public class NumericFeature extends Feature{
	private float f;
	private boolean isMissing;
	
	public NumericFeature() {
		super.type = FeatureType.NUMERIC;
		this.f = 0.0f;
		super.name = null;
		this.isMissing = true;
	}
	public NumericFeature(String name) {
		super.type = FeatureType.NUMERIC;
		this.f = 0.0f;
		super.name = name;
		this.isMissing = true;
	}
	
	public NumericFeature(float f) {
		super.type = FeatureType.NUMERIC;
		this.f = f;
		super.name = null;
		this.isMissing = false;
	}
	
	public NumericFeature(float f, String name) {
		super.type = FeatureType.NUMERIC;
		this.f = f;
		super.name = name;
		this.isMissing = false;
	}
	
	public String getType() {
		StringBuffer sb = new StringBuffer();
		sb.append("@ATTRIBUTE "); sb.append(name); sb.append(" NUMERIC");
		return sb.toString();
	}
	
	public String getValue() {
		return this.isMissing ? "?" : Float.toString(this.f);
	}
}

