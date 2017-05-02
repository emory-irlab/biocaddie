package biocaddie;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;

import org.apache.commons.lang3.time.StopWatch;
import org.apache.lucene.search.CollectionStatistics;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.DefaultSimilarity;
import org.apache.lucene.search.similarities.Similarity;
import org.tartarus.snowball.ext.PorterStemmer;

public class BMainThread {
	
	public static void main(String[] args) throws Exception {
		BMallet.turnoffLoggers();
		List<String> cmd = new ArrayList<String>();
		if (args.length == 0) {
			BufferedReader bRead = new BufferedReader(
					new InputStreamReader(System.in));
			String cmdLine = bRead.readLine();
			cmd.addAll(Arrays.asList(cmdLine.split(" ")));
			cmd.removeAll(Arrays.asList("", null));
		}
		else {
			for (int ind = 0; ind < args.length; ++ind) {
				cmd.add(args[ind]);
			}
		}
		switch (cmd.get(0)) {
		case "index":
			BIndexCreator index = new BIndexCreator();
			index.Create(BGlobalVar.BasePath(), new DefaultSimilarity());
			break;
		case "exec":
			exec(cmd);
			break;
		case "retrieve":
			retrieve(cmd);
			break;
		case "rank":
			rank(cmd);
			break;
		case "show":
			show(cmd);			
			break;
		case "optimize":
			optimize(cmd);
			break;
		case "ltr":
			ltr(cmd);
			break;
		case "common":
			common(cmd);
			break;
		case "lda":
			lda(cmd);
			break;
		case "runcv":
			runcv(cmd);
			break;
		case "test":
			test(cmd);
			break;
		default:
			for (BRankLib.LTRstate = 1; BRankLib.LTRstate <= 9; ++BRankLib.LTRstate) {
				BLib.println(BRankLib.LTRstate + " state started");
				ltr(cmd);
				exec(cmd);
			}
			BLib.println("nothing");
		}
		BLib.println("done");
	}
	
	private static void showSummary(BQueryChainResult chain, boolean detail) {
		if (detail){
			for (int ind = 0; ind < chain.queries.size(); ++ind) {
				BLib.println("Query Number: " +
						chain.queries.get(ind).QueryNo + " >    " + chain.queries.get(ind).trec);
			}
			BLib.println("___________________________");
		}
		BLib.println("Average: " + chain.trec.toStringFull());
	}
	
	private static void showTopDocs(BQueryChainResult chain) {
		BLib.println();
		BLib.println();
		for (int qInd = 0; qInd < chain.queries.size(); ++qInd) {
			BQueryResult bqr = chain.queries.get(qInd);
			BLib.println("Top documents for query " + 
					bqr.QueryNo + ":");
			int count = Math.min(20, bqr.results.size());
			for (int dInd = 0; dInd < count; ++dInd) {
				String docid = BTrecEvalResult.DocumentDOCIDs.get(
						bqr.results.get(dInd).idAtIndex);
				BLib.println(docid);
			}
			BLib.println("___________________________");
		 }
	}
	
	private static void exec(List<String> cmd) throws Exception {
		boolean finalTest = false;
		BIRParam base = BIRParam.GetBest(true);
		BIRParam best = BIRParam.GetBest(false);
		List<BQuery> list = null;
		if (!finalTest) {
			list = BQuery.LoadAllQueries();
		}
		else {
			list = BQuery.LoadQueriesFromFile(BGlobalVar.Results + 
					"queryTest.txt");
		}
		BOptimize bop = new BOptimize(list, base, false, null);
		BQueryChainResult chain = bop.exec(list, best);
		chain.description = best.ExpansionInfo;
//		BRankLib ltr = new BRankLib();
//		ltr.Rerank(bop, list, chain, best, best.ExpansionInfo,  null);
		if (!finalTest) {
			BTrecEvalResult.RunTreceval(chain);
			BQueryChainResult.PrintChains(null, chain);
			showSummary(chain, true);
//			showTopDocs(chain);
		}
		else {
			BTrecEvalResult.PrepareTrecInputs(chain);
			BQueryChainResult.PrintTrecInputs(null, chain);
		}
	}

	private static void retrieve(List<String> cmd) throws Exception {
		StringBuilder sb = new StringBuilder();
		for (int ind = 1; ind < cmd.size(); ++ind){
			sb.append(cmd.get(ind) + " ");
		}
		List<BQuery> list = new ArrayList<BQuery>();
		list.add(new BQuery(1, sb.toString().trim()));
		BIRParam base = BIRParam.GetBest(true);
		BIRParam best = BIRParam.GetBest(false);
		BOptimize bop = new BOptimize(list, base, false, 21);
		BQueryChainResult chain = bop.exec(list, best);
		chain.description = best.ExpansionInfo;
		BRankLib ltr = new BRankLib();
		ltr.Rerank(bop, list, chain, best, best.ExpansionInfo, null, true);
		BQueryResult bqr = chain.queries.get(0);
		int count = Math.min(20, bqr.results.size());
		for (int rInd = 0; rInd < count; ++rInd) {
			BDocumentAtIndex bat = bqr.results.get(rInd);
			BLib.printlnForce(bat.scoreAtRetrieve);
			BLib.printlnForce(BStandardMethods.indexReader.
					document(bat.idAtIndex).get("DOCID"));
			BLib.printlnForce(BStandardMethods.indexReader.
					document(bat.idAtIndex).get("REPOSITORY"));
			BLib.printlnForce(BStandardMethods.indexReader.
					document(bat.idAtIndex).get("TITLE"));
			BLib.printlnForce(BIndexCreator.GetURL(
					BStandardMethods.indexReader.document(
					bat.idAtIndex).get("DOCID")));
			BLib.printlnForce(BStandardMethods.indexReader.
					document(bat.idAtIndex).get("BODY"));
		}
	}

	private static void rankTerms(String[] terms) throws Exception {
		BGoogleNGram bgn = new BGoogleNGram(1995, 2005, 10, false);
		Double[] res = bgn.getProbs(terms);
		ArrayList<BTerm> tList = new ArrayList<>();
		for (int ind = 0; ind < terms.length; ++ind) {
			BTerm bt = new BTerm(terms[ind]);
			bt.Weight = res[ind];
			tList.add(bt);
		}
		ArrayList<BTerm> sorted = BTerm.SortByWeight(tList, true);
		for (int ind = 0; ind < sorted.size(); ++ind) {
			BLib.println(
					String.format("%-30s", sorted.get(ind).Text) + 
					sorted.get(ind).Weight);
		}
	}
	
	private static void rank(List<String> cmd) throws Exception {
		ArrayList<String> terms = new ArrayList<>();
		for (int tInd = 1; tInd < cmd.size(); ++tInd) {
			terms.add(cmd.get(tInd));
		}
		BLib.println("1-grams:");
		rankTerms(terms.toArray(new String[0]));
		terms.clear();
		for (int tInd = 1; tInd < cmd.size(); ++tInd) {
			if (tInd + 1 < cmd.size())
				terms.add(cmd.get(tInd) + " " + cmd.get(tInd + 1));
		}
		BLib.println();
		BLib.println("2-grams:");
		rankTerms(terms.toArray(new String[0]));
	}
	
	private static void common(List<String> cmd) throws Exception {
		for (int tInd = 1; tInd < cmd.size(); ++tInd) {
			if (!BIndexCreator.IsCommonWord(cmd.get(tInd)))
				BLib.print(cmd.get(tInd) + " ");
		}		
	}
	
	private static void show(List<String> cmd) throws Exception {
		StringBuilder sb = new StringBuilder(
				"This document is related to these queries: ");
		if (BTrecEvalResult.DocQueryRels.containsKey(cmd.get(1))) {
			HashSet<Integer> relSet = 
					BTrecEvalResult.DocQueryRels.get(cmd.get(1));
			Integer[] vals = relSet.toArray(new Integer[0]);
			for (int rInd = 0; rInd < vals.length; ++rInd) {
				sb.append(vals[rInd] + ", ");
			}
		}
		else 
			sb.append("None");
		BLib.println(sb.toString());
		BLib.println();
		for (int i = 0; i < BTrecEvalResult.DocumentDOCIDs.size(); ++i) {
			if (BTrecEvalResult.DocumentDOCIDs.get(i).equals(cmd.get(1))) {
				BLib.println("REPOSITORY:");
				BLib.println(BStandardMethods.indexReader.
						document(i).get("REPOSITORY"));
				BLib.println();
				BLib.println("TITLE:");
				BLib.println(BStandardMethods.indexReader.
						document(i).get("TITLE"));
				BLib.println();
				BLib.println("URL:");
				BLib.println(BIndexCreator.GetURL(cmd.get(1)));
				BLib.println();
				BLib.println("TEXT:");
				BLib.println(BStandardMethods.indexReader.
						document(i).get("BODY"));
				break;
			}
		}
	}
	
	private static void optimize(List<String> cmd) throws Exception {
		BIRParam base = BIRParam.GetBest(true);
		BOptimize bcv = new BOptimize(BQuery.LoadAllQueries(), base, true, 21);
		bcv.overfit();
	}

	private static void ltr(List<String> cmd) throws Exception {
		BIRParam base = BIRParam.GetBest(true);
		BIRParam best = BIRParam.GetBest(false);
		List<BQuery> list = BQuery.LoadAllQueries();
		BOptimize bop = new BOptimize(list, base, false, 21);
		BQueryChainResult chain = bop.exec(list, best);
		BRankLib ltr = new BRankLib();
		ltr.Train(bop, list, chain, best, BLTRParam.GetBest(), 
				best.ExpansionInfo, null, true); ///////////////////////////// adjust BLTRParam
	}

	private static void lda(List<String> cmd) throws Exception {
		BMallet.CreateModel(Integer.parseInt(cmd.get(1)));
	}
	
	private static void test(List<String> cmd) throws Exception {
		BJournalExperiments.analyzeSteps();
		
//		BJournalExperiments.analyzeSteps();
//
//		String qq = "cancer hosptital blood skin food";
//		System.out.println(Arrays.toString(BMallet.getDistribution(qq)));
//		System.out.println(Arrays.toString(BMallet.getDistribution(qq)));
//
//		String path = "/home/paya/Desktop/Link to research/biocaddie/result/submitted/Untitled Folder/";
//		List<String> qrel = Files.readAllLines(Paths.get(path + "new_merged_qrels.txt"), Charset.forName("UTF-8"));
//		List<String> em = Files.readAllLines(Paths.get(path + "Emory-1.txt"), Charset.forName("UTF-8"));
//		Hashtable<String, HashSet<String>> qrelIDs = new Hashtable<>();
//		for (int ind = 0; ind < qrel.size(); ++ind) {
//			String[] tokens = qrel.get(ind).split("\t");
//			HashSet<String> docs = qrelIDs.get(tokens[0]);
//			if (docs == null) {
//				docs = new HashSet<>();
//				qrelIDs.put(tokens[0], docs);
//			}
//			docs.add(tokens[2]);
//		}
//		int missAll = 0;
//		int missUnder100 = 0;
//		int minRank = -1;
//		String minId = "";
//		StringBuilder sb = new StringBuilder();
//		for (int ind = 0; ind < em.size(); ++ind) {
//			String[] tokens = em.get(ind).split(" ");
//			String qid = tokens[0];
//			String docid = tokens[2];
//			int rank = Integer.parseInt(tokens[3]);			
//			HashSet<String> docs = qrelIDs.get(qid);
//			if (!docs.contains(docid)) {
//				++missAll;
//				if (rank <= 100) {
//					sb.append(em.get(ind) + "\n");
//					++missUnder100;
//				}
//				if (minRank == -1 || rank < minRank) {
//					minRank = rank;
//					minId = docid;
//				}
//			}
//		}
//		System.out.println(minRank + " > " + minId);
//		System.out.println(missAll);
//		System.out.println(missUnder100);
//		PrintStream ps = new PrintStream(path + "missing.txt");
//		ps.print(sb.toString());
//		ps.close();
	}
	
	private static void runcv(List<String> cmd) throws Exception {
		BIRParam base = BIRParam.GetBest(true);
		BOptimize bcv = new BOptimize(BQuery.LoadAllQueries(), base, true, 21);
		bcv.runcvIR("paper-experiments/ir/");
//		bcv.runcvLTR("paper-experiments/ltr/");
	}

}
