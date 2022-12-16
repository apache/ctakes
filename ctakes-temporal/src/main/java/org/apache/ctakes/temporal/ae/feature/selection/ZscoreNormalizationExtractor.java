package org.apache.ctakes.temporal.ae.feature.selection;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.cleartk.ml.Feature;
import org.cleartk.ml.Instance;
import org.cleartk.ml.feature.extractor.CleartkExtractorException;
import org.cleartk.ml.feature.transform.OneToOneTrainableExtractor_ImplBase;

/**
 * Use z-score to normalize every numerical feature
 * @author Chen Lin
 * 
 */
public class ZscoreNormalizationExtractor<OUTCOME_T, FOCUS_T extends Annotation> extends
OneToOneTrainableExtractor_ImplBase<OUTCOME_T> {

	private boolean isTrained;

	// This is read in after training for use in transformation
	private Map<String, MeanStdPair> meanStdMap;

	public ZscoreNormalizationExtractor(String name) {
		super(name);
		isTrained = false;
	}

	@Override
	public Feature transform(Feature feature) {
		String featureName = feature.getName();
		Object featureValue = feature.getValue();
		if (featureValue instanceof Number) {
			MeanStdPair stats = this.meanStdMap.get(featureName);

			double mmn = 0.5d; // this is the default value we will return if we've never seen the feature
			// before

			double value = ((Number) feature.getValue()).doubleValue();
			// this is the typical case
			if (stats != null) {
				mmn = (value - stats.mean) / (stats.std);
			}
			return new Feature("Zscore_NORMED_" + featureName, mmn);
		}
		return feature;
	}

	@Override
	public void train(Iterable<Instance<OUTCOME_T>> instances) {
		Map<String, ZscoreRunningStat> featureStatsMap = new HashMap<>();

		// keep a running mean and standard deviation for all applicable features
		for (Instance<OUTCOME_T> instance : instances) {
			// Grab the matching zmus (zero mean, unit stddev) features from the set of all features in an
			// instance
			for (Feature feature : instance.getFeatures()) {

				String featureName = feature.getName();
				Object featureValue = feature.getValue();
				if (featureValue instanceof Number) {
					ZscoreRunningStat stats;
					if (featureStatsMap.containsKey(featureName)) {
						stats = featureStatsMap.get(featureName);
					} else {
						stats = new ZscoreRunningStat();
						featureStatsMap.put(featureName, stats);
					}
					stats.add(((Number) featureValue).doubleValue());
				} else {
					System.err.println("Ignore non-numeric feature from normalization: "+ featureName + " with Value: " + featureValue);
					continue;
				}

			}
		}

		this.meanStdMap = new HashMap<>();
		for (Map.Entry<String, ZscoreRunningStat> entry : featureStatsMap.entrySet()) {
			ZscoreRunningStat stats = entry.getValue();
			this.meanStdMap.put(entry.getKey(), new MeanStdPair(stats.getMean(), stats.getStdDev()));
		}

		this.isTrained = true;
	}

	@Override
	public void save(URI zmusDataUri) throws IOException {
		// Write out tab separated values: feature_name, mean, stddev
		File out = new File(zmusDataUri);
		BufferedWriter writer = null;
		writer = new BufferedWriter(new FileWriter(out));

		for (Map.Entry<String, MeanStdPair> entry : this.meanStdMap.entrySet()) {
			MeanStdPair pair = entry.getValue();
			writer.append(String.format(Locale.ROOT, "%s\t%f\t%f\n", entry.getKey(), pair.mean, pair.std));
		}
		writer.close();
	}

	@Override
	public void load(URI zmusDataUri) throws IOException {
		// Reads in tab separated values (feature name, min, max)
		File in = new File(zmusDataUri);
		BufferedReader reader = null;
		this.meanStdMap = new HashMap<>();
		reader = new BufferedReader(new FileReader(in));
		String line = null;
		while ((line = reader.readLine()) != null) {
			String[] featureMeanStddev = line.split("\\t");
			this.meanStdMap.put(
					featureMeanStddev[0],
					new MeanStdPair(
							Double.parseDouble(featureMeanStddev[1]),
							Double.parseDouble(featureMeanStddev[2])));
		}
		reader.close();

		this.isTrained = true;
	}

	private static class MeanStdPair {

		public MeanStdPair(double mean, double std) {
			this.mean = mean;
			this.std  = std;
		}

		public double mean;

		public double std;
	}

	public static class ZscoreRunningStat implements Serializable {

		/**
		 * for a named feature, maintain its all values, sum, size and mean,
		 * calculate the std if needed
		 */
		private static final long serialVersionUID = 1L;
		private List<Double> data;
		private double sum;
		private double mean;
		private int n;

		public ZscoreRunningStat() {
			this.clear();
		}

		public void add(double x) {
			this.n++;
			this.sum += x;
			this.mean = this.sum/this.n;
		}

		public void clear() {
			this.data = new ArrayList<>();
			this.sum 	= 0;
			this.n	= 0;
			this.mean	= 0;
		}

		public int getNumSamples() {
			return this.n;
		}

		private double getVariance()
		{
			double temp = 0;
			for(double a :data)
				temp += (mean-a)*(mean-a);
			return temp/this.n;
		}

		public double getStdDev()
		{
			return Math.sqrt(getVariance());
		}

		public double getMean(){
			return this.mean;
		}

	}

	public List<Feature> extract(JCas view, FOCUS_T focusAnnotation) throws CleartkExtractorException {
		// TODO Auto-generated method stub
		return null;
	}
}
