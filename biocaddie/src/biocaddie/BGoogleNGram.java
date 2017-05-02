package biocaddie;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;

public class BGoogleNGram {
	private int _startYear;
	private int _endYear;
	private int _smoothing;
	private boolean _caseSensitive;
	private String URL1;
	private String URL2;
	private final String wordDelimiter = "%2C";
	private final String inwordSpace = "+";
	private final String lineIndicator = "  var data = [{";
	
	public BGoogleNGram(int startYear, int endYear,
			int smoothing, boolean caseSensitive) {
		_startYear = startYear;
		_endYear = endYear;
		_smoothing = smoothing;
		_caseSensitive = caseSensitive;
		URL1 = "https://books.google.com/ngrams/graph?content=";
		URL2 = (!_caseSensitive ? "&case_insensitive=on" : "") +
				"&year_start=" + _startYear + 
				"&year_end=" + _endYear + 
				"&corpus=15" + 
				"&smoothing=" + _smoothing;
	}
	
	private String getURL(String[] terms) {
		StringBuilder sb = new StringBuilder();
		sb.append(URL1);
		sb.append(terms[0].replace(" ", inwordSpace));
		for (int ind = 1; ind < terms.length; ++ind) {
			sb.append(wordDelimiter);
			sb.append(terms[ind].replace(" ", inwordSpace));
		}
		sb.append(URL2);
		return sb.toString();
	}
	
	private String findTagValue(String line, String tag) {
		int pos = line.indexOf(tag);
		int st = line.indexOf("\"", pos + tag.length());
		int en = line.indexOf("\"", st + 1);
		String result = line.substring(st + 1, en);
		return result;
	}
	
	private double[] extractTimeseries(String line) {
		String tag = "\"timeseries\":";
		int pos = line.indexOf(tag);
		int st = line.indexOf("[", pos + tag.length());
		int en = line.indexOf("]", st + 1);
		String list = line.substring(st + 1, en);
		String[] tokens = list.split(",");
		double[] result = new double[tokens.length];
		for (int ind = 0; ind < tokens.length; ++ind) {
			result[ind] = Double.parseDouble(tokens[ind]);
		}
		return result;
	}
	
	private ArrayList<Integer> findIndexes(String[] terms, String word) {
		ArrayList<Integer> result = new ArrayList<>();
		String lower = word.toLowerCase().trim();
		int allPos = lower.indexOf("(all)");
		if (allPos >= 0) {
			lower = lower.substring(0, allPos - 1).trim();
		}
		int ind = 0;
		while (ind < terms.length) {
			if (terms[ind].toLowerCase().trim().equals(lower)) {
				result.add(ind);
			}
			++ind;
		}
		return result;
	}
	
	private Double[] extractProbs(String[] terms, 
			String line) {
		Double[] result = new Double[terms.length];
		for (int i = 0; i < result.length; ++i) {
			result[i] = 0.0;
		}
		int stPos = line.indexOf("{");
		int enPos = line.indexOf("}", stPos + 1);
		while (stPos >= 0 && enPos >= 0) {
			String termLine = line.substring(stPos + 1, enPos);
			String word = findTagValue(termLine, "\"ngram\":");
			String type = findTagValue(termLine, "\"type\":");
			double[] timeseries = extractTimeseries(termLine);
			if (type.equals("NGRAM") || 
					type.equals("CASE_INSENSITIVE")) {
				ArrayList<Integer> pos = findIndexes(terms, word);
				for (int i = 0; i < pos.size(); ++i)
					result[pos.get(i)] = timeseries[timeseries.length - 1];
			}
			stPos = line.indexOf("{", enPos + 1);
			enPos = line.indexOf("}", stPos + 1);
		}
		return result;
	}
	
	public Double[] getBatchProbs(String[] terms) {
		String address = getURL(terms);
		Double[] result = null;
		InputStream inSt = null;
		try {
	        URL url = new URL(address);
	        inSt = url.openStream();
	        BufferedReader br = new BufferedReader(
	        		new InputStreamReader(inSt));
	        String line;
	        while ((line = br.readLine()) != null) {
	        	if (line.length() > lineIndicator.length()) {
		        	if (line.substring(0, lineIndicator.length()).
		        			equals(lineIndicator)) {
			            result = extractProbs(terms, line);
			            break;
		        	}
	        	}
	        }
	    }
		catch (Exception err) {
			err.printStackTrace();
		}
		finally {
	        try {
	            if (inSt != null) 
	            	inSt.close();
	        } catch (Exception err) {
	        	err.printStackTrace();
	        }
	    }
		return result;
	}
	
	public Double[] getProbs(String[] terms) {
		ArrayList<Double> result = new ArrayList<>();
		int start = 0;
		int batchSize = 10;
		while (start < terms.length) {
			int to = Math.min(start + batchSize, terms.length);
			String[] batch = Arrays.copyOfRange(terms, start, to);
			Double[] vals = getBatchProbs(batch);
			result.addAll(Arrays.asList(vals));
			start = to;
		}
		return result.toArray(new Double[0]);
	}
	
}
