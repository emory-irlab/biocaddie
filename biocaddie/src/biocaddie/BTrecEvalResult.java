package biocaddie;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

public class BTrecEvalResult {
	public static HashMap<String, HashSet<Integer>> DocQueryRels = 
			new HashMap<String, HashSet<Integer>>();
	public static HashMap<Integer, HashMap<String, Integer>> 
		QueryDocRels = new HashMap<>();
	public static List<String> DocumentDOCIDs;
	public String trecEvalInput, trecEvalOutput;
	public float MAP, NDCG, PAt10;
	public float[] PR = new float[11];
	private static Random rnd;
	
	static{
		try {
			rnd = new Random(System.currentTimeMillis());
			DocumentDOCIDs = Files.readAllLines(Paths.get(
					BGlobalVar.DocumentDOCID), Charset.forName("UTF-8"));
			HashSet<Integer> qRels;
			HashMap<String, Integer> dRels;
			List<String> relLines = Files.readAllLines(Paths.get(
					BGlobalVar.Judgments), Charset.forName("UTF-8"));
			for (int i = 0; i < relLines.size(); ++i) {
				String[] tokens = relLines.get(i).split(" ");				
				if (tokens.length < 4) {
					BLib.println(relLines.get(i) + "\n");
				}
				if (tokens[3].equals("1") || tokens[3].equals("2")) {
					int qValue = Integer.parseInt(tokens[0]);
					if (DocQueryRels.containsKey(tokens[2])){
						qRels = DocQueryRels.get(tokens[2]);
					}
					else {
						qRels = new HashSet<Integer>();
						DocQueryRels.put(tokens[2], qRels);
					}
					qRels.add(qValue);
					
					if (QueryDocRels.containsKey(qValue)) {
						dRels = QueryDocRels.get(qValue);
					}
					else {
						dRels = new HashMap<String, Integer>();
						QueryDocRels.put(qValue, dRels);
					}
					dRels.put(tokens[2], Integer.parseInt(tokens[3]));
				}
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void RunTreceval(BQueryResult result) throws Exception {
		BQueryChainResult tempChain = new BQueryChainResult("temp", "");
		tempChain.queries.add(result);
		RunTreceval(tempChain);
	}
	
	public static void RunTreceval(BQueryChainResult chain) throws Exception {
		PrepareTrecInputs(chain);
		String tempPath = BGlobalVar.Results + "temp" + 
				System.currentTimeMillis() + "_" + 
				rnd.nextInt() + ".txt";
		PrintStream tempRank = new PrintStream(tempPath);
		tempRank.print(chain.trec.trecEvalInput);
		tempRank.close();
		ProcessBuilder trecProcConfig = new ProcessBuilder(
				BGlobalVar.TrecEvalFile, "-q", "-m", "all_trec", 
				BGlobalVar.Judgments, tempPath);
		Process trecProc = trecProcConfig.start();
		BufferedReader stdIn = new BufferedReader(
				new InputStreamReader(trecProc.getInputStream()));
		String line = "";
		List<String> output = new ArrayList<String>();
		while ((line = stdIn.readLine()) != null){
			output.add(line + "\n");
		}
		Files.delete(Paths.get(tempPath));
		stdIn.close();
		PrepareOutputs(chain, output);
	}
	
	public static void PrepareTrecInputs(BQueryChainResult chain) 
			throws Exception {
		StringBuilder inputAccum = new StringBuilder();
		for (int i = 0; i < chain.queries.size(); ++i){
			String in = GetTrecevalInput(chain.queries.get(i));
			chain.queries.get(i).trec = new BTrecEvalResult();
			chain.queries.get(i).trec.trecEvalInput = in;
			inputAccum.append(in);
		}
		chain.trec = new BTrecEvalResult();
		chain.trec.trecEvalInput = inputAccum.toString();
	}
	
	private static String GetTrecevalInput(BQueryResult result) throws Exception {
		StringBuilder input = new StringBuilder();
		String queryNo = String.format("%03d", result.QueryNo);
//		String queryNo = "T" + result.QueryNo;
		for (int i = 0; i < result.results.size(); ++i){
			String docID = DocumentDOCIDs.get(result.results.get(i).idAtIndex);
			input.append(queryNo + " ");
			if (DocQueryRels.containsKey(docID)){
				HashSet<Integer> qrels = DocQueryRels.get(docID);
				if (qrels.contains(result.QueryNo)){
					input.append("1 ");
					result.results.get(i).isRelevant = true;
				}
				else{
					input.append("0 ");
				}
			}
			else{
				input.append("0 ");
			}
			input.append(docID + " " + (i + 1) + " " + 
					result.results.get(i).scoreAtRetrieve + " My\n");
		}
		return input.toString();
	}

	private static void PrepareOutputs(BQueryChainResult chain, 
			List<String> trecevalResult) {
		int prInd = 0;
		for (int qInd = 0; qInd < chain.queries.size(); ++qInd){
			BTrecEvalResult cur = chain.queries.get(qInd).trec;
			prInd = 0;
			for (int lInd = 0; lInd < trecevalResult.size(); ++lInd) {
				List<String> toks = new ArrayList<String>(Arrays.asList(
						trecevalResult.get(lInd).split("\\s+")));
				toks.removeAll(Arrays.asList("", null));
				if (isInt(toks.get(1)) && 
						Integer.parseInt(toks.get(1)) == 
						chain.queries.get(qInd).QueryNo) {
					if (toks.get(0).equals("map")) {
						cur.MAP = Float.parseFloat(toks.get(2));
					}
					else if (toks.get(0).equals("ndcg")) {
						cur.NDCG = Float.parseFloat(toks.get(2));
					}
					else if (toks.get(0).equals("P_10")) {
						cur.PAt10 = Float.parseFloat(toks.get(2));
					}
					else if (toks.get(0).startsWith("iprec_at_recall_")) {
						cur.PR[prInd] = Float.parseFloat(toks.get(2));
						++prInd;
					}
				}
			}
		}
		prInd = 0;
		for (int lInd = 0; lInd < trecevalResult.size(); ++lInd) {
			List<String> toks = new ArrayList<String>(Arrays.asList(
					trecevalResult.get(lInd).split("\\s+")));
			toks.removeAll(Arrays.asList("", null));
			if (!isInt(toks.get(1))) {
				if (toks.get(0).equals("map")) {
					chain.trec.MAP = Float.parseFloat(toks.get(2));
				}
				else if (toks.get(0).equals("ndcg")) {
					chain.trec.NDCG = Float.parseFloat(toks.get(2));
				}
				else if (toks.get(0).equals("P_10")) {
					chain.trec.PAt10 = Float.parseFloat(toks.get(2));
				}
				else if (toks.get(0).startsWith("iprec_at_recall_")) {
					chain.trec.PR[prInd] = Float.parseFloat(toks.get(2));
					++prInd;
				}
			}
		}
		StringBuilder output = new StringBuilder();
		for (int i = 0; i < trecevalResult.size(); ++i)
			output.append(trecevalResult.get(i));
		chain.trec.trecEvalOutput = output.toString();
	}
	
	private static boolean isInt(String text) {
		try {
			Integer.parseInt(text);
			return true;
		}
		catch (Exception e) {
			return false;
		}
	}

	@Override
	public String toString() {
		return "NDCG: " + NDCG + 
				" MAP: " + MAP +
				" P@10: " + PAt10;
	}
	
	public String toStringFull() {
		StringBuilder sb = new StringBuilder(
				"NDCG: " + NDCG + 
				" MAP: " + MAP +
				" P@10: " + PAt10);
		sb.append("\tPR: ");
		for (int ind = 0; ind < PR.length; ++ind) {
			sb.append(PR[ind] + " ");
		}
		return sb.toString();
	}
	
	public void sum(BTrecEvalResult op) {
		MAP += op.MAP;
		NDCG += op.NDCG;
		PAt10 += op.PAt10;
		for (int ind = 0; ind < PR.length; ++ind) {
			PR[ind] += op.PR[ind];
		}
	}
	
	public void ave(int count) {
		MAP /= count;
		NDCG /= count;
		PAt10 /= count;
		for (int ind = 0; ind < PR.length; ++ind) {
			PR[ind] /= count;
		}
	}
	
	
}
