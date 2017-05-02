package biocaddie;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class BExpansion {
	private final static int keggStart = 3;
	private static HashMap<String, String[]> savedKEGG = null;

	private static void loadKEGG() throws Exception {
		savedKEGG = new HashMap<>();
		BufferedReader br = new BufferedReader(new FileReader(
				BGlobalVar.KEGGSaved));
		String word = null;
		while ((word = br.readLine()) != null) {
			String[] relatedWords = br.readLine().split(" ");
			savedKEGG.put(word, relatedWords);
		}
		br.close();
	}

	private static void saveKEGG() throws Exception {
		StringBuilder sb = new StringBuilder();
		ArrayList<String> words = new ArrayList<>(savedKEGG.keySet());
		for (int wInd = 0; wInd < words.size(); ++wInd) {
			sb.append(words.get(wInd) + "\n");
			String[] relatedWords = savedKEGG.get(words.get(wInd));
			for (int rInd = 0; rInd < relatedWords.length; ++rInd) {
				sb.append(relatedWords[rInd] + " ");
			}
			sb.append("\n");
		}
		PrintStream ps = new PrintStream(BGlobalVar.KEGGSaved);
		ps.print(sb.toString());
		ps.close();
	}
	
	private static ArrayList<String> runKEGG(String query) 
			throws Exception {
		ProcessBuilder keggProcConf = new ProcessBuilder(
				"python", BGlobalVar.KEGG, query);
		Process keggProc = keggProcConf.start();
		BufferedReader stdInput = new BufferedReader(
				new InputStreamReader(keggProc.getInputStream()));
		String line = "";
		ArrayList<String> output = new ArrayList<>();
		while ((line = stdInput.readLine()) != null){
			output.add(line);
		}
		stdInput.close();
		return output;
	}
	
	public static ArrayList<String> ExpandKEGG(String query) 
			throws Exception {
		if (savedKEGG == null) {
			loadKEGG();
		}
		if (!savedKEGG.containsKey(query)) {
			ArrayList<String> lines = runKEGG(query);
			HashSet<String> temp = new HashSet<>();
			for (int lInd = keggStart; lInd < lines.size(); ++lInd) {
				ArrayList<String> tokens = BIndexCreator.Tokenize(
						lines.get(lInd));
				for (int tInd = 0; tInd < tokens.size(); ++tInd) {
					if (!temp.contains(tokens.get(tInd))) {
						temp.add(tokens.get(tInd));
					}
				}
			}
			String[] words = temp.toArray(new String[0]);
			savedKEGG.put(query, words);
			saveKEGG();
		}
		String[] terms = savedKEGG.get(query);
		return new ArrayList<>(Arrays.asList(terms));
	}
	
	private static HashMap<String, String[]> savedHGNC = null;
	
	private static void loadHGNC() throws Exception {
		savedHGNC = new HashMap<>();
		BufferedReader br = new BufferedReader(new FileReader(
				BGlobalVar.HGNCSaved));
		br.readLine();
		String line = null;
		while ((line = br.readLine()) != null) {
			String[] tokens = line.split("\t");
			if (tokens.length > 1 && tokens[1].length() > 0) {
				int tabPos = line.indexOf("\t");
				String exp = line.substring(tabPos + 1);
				String[] news = BIndexCreator.Tokenize(exp).toArray(
						new String[0]);
				String[] gs = tokens[1].split(",");
				for (int tInd = 0; tInd < gs.length; ++tInd) {
					savedHGNC.put(gs[tInd].trim().toLowerCase(), news);
				}
			}
		}
		br.close();
	}
	
	public static ArrayList<String> ExpandHGNC(String query) 
			throws Exception {
		if (savedHGNC == null) {
			loadHGNC();
		}
		String[] terms = savedHGNC.get(query.trim().toLowerCase());
		ArrayList<String> result = new ArrayList<>();
		if (terms != null) {
			for (String t : terms) {
				result.add(t);
			}
		}
		return result;
	}
	
	private static HashMap<String, String[]> savedWeb = null;
	
	private static void loadWeb() throws Exception {
		savedWeb = new HashMap<>();
		BufferedReader br = new BufferedReader(new FileReader(
				BGlobalVar.WebSaved));
		String query = null;
		while ((query = br.readLine()) != null) {
			String[] result = br.readLine().split(" ");
			result = BIndexCreator.RemoveCommonWords(
					Arrays.asList(result)).toArray(new String[0]);
			savedWeb.put(query, result);
		}
		br.close();
	}
	
	private static void saveWeb() throws Exception {
		StringBuilder sb = new StringBuilder();
		ArrayList<String> qs = new ArrayList<>(savedWeb.keySet());
		for (int qInd = 0; qInd < qs.size(); ++qInd) {
			sb.append(qs.get(qInd) + "\n");
			String[] result = savedWeb.get(qs.get(qInd));
			for (int rInd = 0; rInd < result.length; ++rInd) {
				sb.append(result[rInd] + " ");
			}
			sb.append("\n");
		}
		PrintStream ps = new PrintStream(BGlobalVar.WebSaved);
		ps.print(sb.toString());
		ps.close();
	}

	private static ArrayList<String> removeStops(ArrayList<String> list) {
		ArrayList<String> result = new ArrayList<>();
		for (int ind = 0; ind < list.size(); ++ind) {
			if (!BIndexCreator.stopWords.contains(list.get(ind))) {
				result.add(list.get(ind));
			}
		}
		return result;
	}
	
	public static ArrayList<String> ExpandWeb(String fullQuery)
			throws Exception {
		if (savedWeb == null) {
			loadWeb();
		}
		String[] terms = fullQuery.split(" ");
		StringBuilder newQ = new StringBuilder();
		for (int tInd = 0; tInd < terms.length; ++tInd) {
			if (!BIndexCreator.IsCommonWord(terms[tInd])) {
				newQ.append(terms[tInd] + " ");
			}
		}
		if (!savedWeb.containsKey(newQ.toString())) {
			ArrayList<String> newTerms = new ArrayList<>();
			ArrayList<BGResult> wiki = BGoogleSearch.Search(
					newQ.toString().trim(), "wikipedia.org");
			if (wiki.size() > 0) {
				String body = BGoogleSearch.GetText(wiki.get(0).URL);
				newTerms.addAll(BIndexCreator.Tokenize(body));
			}
			Thread.sleep(6000);
			ArrayList<BGResult> ncbi = BGoogleSearch.Search(
					newQ.toString().trim(), "ncbi.nlm.nih.gov");
			if (ncbi.size() > 0) {
				String body = BGoogleSearch.GetText(ncbi.get(0).URL);
				newTerms.addAll(BIndexCreator.Tokenize(body));
			}
			Thread.sleep(6000);
			savedWeb.put(newQ.toString(), newTerms.toArray(new String[0]));
			saveWeb();
		}
		String[] result = savedWeb.get(newQ.toString());
		return new ArrayList<String>(Arrays.asList(result));
	}
	
	public static BBOW FindRelatedTerms(String query) 
			throws Exception {
		BBOW bb = new BBOW();
		String[] terms = query.split(" ");
		for (int tInd = 0; tInd < terms.length; ++tInd) {
			if (!BIndexCreator.IsCommonWord(terms[tInd])) {
				ArrayList<String> newTerms;
				newTerms = BExpansion.ExpandKEGG(terms[tInd]);
				bb.AddWords(newTerms);
				newTerms = BExpansion.ExpandHGNC(terms[tInd]);
				bb.AddWords(newTerms);
			}
		}
		ArrayList<String> webTerms = ExpandWeb(query);
		webTerms = removeStops(webTerms);
		bb.AddWords(webTerms);
		bb.RemoveText(query);
		return bb;
	}
	
}
