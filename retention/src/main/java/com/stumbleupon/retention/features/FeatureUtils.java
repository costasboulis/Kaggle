package com.stumbleupon.retention.features;


import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.List;


import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;

public class FeatureUtils {
	public static String NEWLINE = System.getProperty("line.separator");
	
	public static Instances toInstances(List<List<Feature>> features) {
		Instances instances = new Instances("", null, 0);
		
		return instances;
	}
	
	public static Instances toInstance(List<Feature> features) {
		FastVector atts = new FastVector(features.size());
		int numAtts = 0;
		for (Feature f : features) {
			if (f instanceof NumericFeature) {
				NumericFeature nf = (NumericFeature) f;
				String[] fields = nf.getType().split(" ");
				String name = fields[1];
				Attribute att = new Attribute(name);
				atts.addElement(att);
				numAtts ++;
			}
			else if (f instanceof NominalFeature) {
				NominalFeature nf = (NominalFeature) f;
				String name = nf.getName();
				for (int i = 0; i < nf.getNumCategories(); i ++) {
					StringBuffer sb = new StringBuffer();
					sb.append(name); sb.append(i);
					Attribute att = new Attribute(sb.toString());
					atts.addElement(att);
				}
				numAtts += nf.getNumCategories();
			}
		}
		Instances instances = new Instances("ResidualPrice", atts, 1);
		
		Instance instance = new Instance(numAtts);
		int att = 0;
		for (Feature f : features) {
			if (f instanceof NumericFeature) {
				NumericFeature nf = (NumericFeature) f;
				if (nf.getValue().equals("?")) {
					instance.setMissing(att);
				}
				else {
					instance.setValue(att, Float.parseFloat(nf.getValue()));
				}
				att ++;
			}
			else if (f instanceof NominalFeature) {
				NominalFeature nf = (NominalFeature) f;
				for (int i = 0; i < nf.getNumCategories(); i ++) {
					instance.setValue(att, nf.getNominal() == i ? 1.0f : 0.0f);
					att ++;
				}
			}
		}
		instance.setDataset(instances);
		instances.add(instance);
		instances.setClassIndex(instances.numAttributes() - 1);
		
		return instances;
	}
	
	public static void writeFeatures(File file, List<List<Feature>> features) throws Exception {
		PrintWriter out = new PrintWriter(new FileWriter(file));
		out.println("@RELATION Price" + NEWLINE);
		
		// Write the @ATTRIBUTES part
		for (Feature f : features.get(0)) {
			StringBuffer sb = new StringBuffer();
			sb.append(f.getType());
			out.println(sb.toString());
		}
		// Write the @DATA part
		out.println(NEWLINE + "@DATA");
		for (List<Feature> fv : features) {
        	StringBuffer sb = new StringBuffer();
        	for (int i = 0; i < fv.size() - 1; i ++) {
        		Feature f = fv.get(i);
        		sb.append(f.getValue()); sb.append(",");
        	}
        	sb.append(fv.get(fv.size()-1).getValue());  sb.append(NEWLINE);
        	
        	out.print(sb.toString());
        }
		out.close();
        
    }
}
