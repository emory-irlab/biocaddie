package biocaddie;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;

public class BRankLib {
	public static boolean LTRAllowState = false;
	public static int LTRstate = 0;
	private static boolean useLDA = false;
	private static int topCount = 100;
	private static int fieldTopDocCount = 50000;

	class DocFeatSet {
		public String DOCID;
		public String QNO;
		public int Relevance;
		public float BM25;
		public float BM25Title;
		public float BM25Text;
		public float BM25Meta;
		public float Gram1TFTitle;
		public float Gram1TFText;
		public float Gram1TFMeta;
		public float Gram1IDFTitle;
		public float Gram1IDFText;
		public float Gram1IDFMeta;
		public float Gram1TFIDFTitle;
		public float Gram1TFIDFText;
		public float Gram1TFIDFMeta;
		public float Gram1TF;
		public float Gram1IDF;
		public float Gram1TFIDF;
		public float Gram2TFTitle;
		public float Gram2TFText;
		public float Gram2TFMeta;
		public float Gram2TF;
		public float StartDistanceText;
		public float DomainWeight;
		public float LDASim;
		public float LDASimTop1;
		public float LDASimTop3;

		private int getEmptyFields(StringBuilder result, int fieldStartNumber, int count) {
			return fieldStartNumber;
//			for (int ind = 0; ind < count; ++ind) {
//				++fieldStartNumber;
//				result.append(fieldStartNumber + ":0 ");
//			}
//			return fieldStartNumber;
		}
		
		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append(Relevance);
			sb.append(" qid:");
			sb.append(QNO);
			sb.append(" ");
			int fInd = 0;
			if (!LTRAllowState || LTRstate != 1) {
				++fInd;
				sb.append(fInd + ":" + BM25 + " ");
				++fInd;
				sb.append(fInd + ":" + BM25Title + " ");
				++fInd;
				sb.append(fInd + ":" + BM25Text + " ");
				++fInd;
				sb.append(fInd + ":" + BM25Meta + " ");
			}
			else
				fInd = getEmptyFields(sb, fInd, 4);
			if (!LTRAllowState || LTRstate != 2) {
				++fInd;
				sb.append(fInd + ":" + Gram1TFTitle + " ");
				++fInd;
				sb.append(fInd + ":" + Gram1TFText + " ");
				++fInd;
				sb.append(fInd + ":" + Gram1TFMeta + " ");
			}
			else
				fInd = getEmptyFields(sb, fInd, 3);
			if (!LTRAllowState || LTRstate != 3) {
				++fInd;
				sb.append(fInd + ":" + Gram1IDFTitle + " ");
				++fInd;
				sb.append(fInd + ":" + Gram1IDFText + " ");
				++fInd;
				sb.append(fInd + ":" + Gram1IDFMeta + " ");
			}
			else
				fInd = getEmptyFields(sb, fInd, 3);
			if (!LTRAllowState || LTRstate != 4) {
				++fInd;
				sb.append(fInd + ":" + Gram1TFIDFTitle + " ");
				++fInd;
				sb.append(fInd + ":" + Gram1TFIDFText + " ");
				++fInd;
				sb.append(fInd + ":" + Gram1TFIDFMeta + " ");
			}
			else
				fInd = getEmptyFields(sb, fInd, 3);
			if (!LTRAllowState || LTRstate != 5) {
				++fInd;
				sb.append(fInd + ":" + Gram1TF + " ");
				++fInd;
				sb.append(fInd + ":" + Gram1IDF + " ");
				++fInd;
				sb.append(fInd + ":" + Gram1TFIDF + " ");
			}
			else
				fInd = getEmptyFields(sb, fInd, 3);
			if (!LTRAllowState || LTRstate != 6) {
				++fInd;
				sb.append(fInd + ":" + Gram2TFTitle + " ");
				++fInd;
				sb.append(fInd + ":" + Gram2TFText + " ");
				++fInd;
				sb.append(fInd + ":" + Gram2TFMeta + " ");
				++fInd;
				sb.append(fInd + ":" + Gram2TF + " ");
			}
			else
				fInd = getEmptyFields(sb, fInd, 4);
			if (!LTRAllowState || LTRstate != 7) {
				++fInd;
				sb.append(fInd + ":" + StartDistanceText + " ");
			}
			else
				fInd = getEmptyFields(sb, fInd, 1);
			if (!LTRAllowState || LTRstate != 8) {
				++fInd;
				sb.append(fInd + ":" + DomainWeight + " ");
			}
			else
				fInd = getEmptyFields(sb, fInd, 1);
			if (useLDA) {
				if (!LTRAllowState || LTRstate != 9) {
					++fInd;
					sb.append(fInd + ":" + LDASim + " ");
					++fInd;
					sb.append(fInd + ":" + LDASimTop1 + " ");
					++fInd;
					sb.append(fInd + ":" + LDASimTop3 + " ");
				}
				else
					fInd = getEmptyFields(sb, fInd, 3);
			}
			sb.append("# " + DOCID);
			return sb.toString();
		}

	}
	
	class QueryInfo {
		public BQuery Query;
		public ArrayList<String> OrigTerms;
		public ArrayList<String> NewTerms;
		public BQueryResult ResultTitle;
		public BQueryResult ResultText;
		public BQueryResult ResultMeta;
		public double[] LDAVec;
		public double[] LDAVecTop1;
		public double[] LDAVecTop3;
		
		public void clear() {
			ResultTitle = ResultText = ResultMeta = null;
		}
		
	}

	private QueryInfo loadQueryInfo(BQuery query, BIRParam param, 
			BOptimize bop) throws Exception {
		QueryInfo qi = new QueryInfo();
		qi.Query = query;
		qi.OrigTerms = BIndexCreator.Tokenize(query.title);
		String extWords = bop.newExtTerms(query, param);
		qi.NewTerms = BIndexCreator.Tokenize(extWords);
		String intWords = bop.newIntTerms(query, param);
		qi.NewTerms.addAll(BIndexCreator.Tokenize(intWords));
		float titleW = param.TitleWeight;
		float textW = param.TextWeight;
		float metaW = param.MetaWeight;
		
		int tempRelCount = BStandardMethods.relCount;
		BStandardMethods.relCount = fieldTopDocCount;
		param.TitleWeight = titleW;
		param.TextWeight = 0;
		param.MetaWeight = 0;
		qi.ResultTitle = bop.exec(query, param);

		param.TitleWeight = 0;
		param.TextWeight = textW;
		param.MetaWeight = 0;
		qi.ResultText = bop.exec(query, param);

		param.TitleWeight = 0;
		param.TextWeight = 0;
		param.MetaWeight = metaW;
		qi.ResultMeta = bop.exec(query, param);
		BStandardMethods.relCount = tempRelCount;

		param.TitleWeight = titleW;
		param.TextWeight = textW;
		param.MetaWeight = metaW;
		BQueryResult bqr = bop.exec(query, param);
		
		if (useLDA) {
			qi.LDAVec = BMallet.getDistribution(qi.Query.title);
			qi.LDAVecTop1 = BMallet.filterTop(qi.LDAVec, 1);
			qi.LDAVecTop3 = BMallet.filterTop(qi.LDAVec, 3);
		}
		return qi;
	}
	
	private float findDocScore(BQueryResult result, 
			BDocumentAtIndex doc) {
		for (int dInd = 0; dInd < result.results.size(); ++dInd) {
			if (result.results.get(dInd).idAtIndex == doc.idAtIndex)
				return result.results.get(dInd).scoreAtRetrieve;
		}
		return 0;
	}
	
	private float[] stat(QueryInfo query, 
			HashMap<String, BTerm> termVector) {
		float TF = 0;
		float IDF = 0;
		float TFIDF = 0;
		for (int oInd = 0; oInd < query.OrigTerms.size(); ++oInd) {
			if (termVector.containsKey(query.OrigTerms.get(oInd))) {
				BTerm bt = termVector.get(query.OrigTerms.get(oInd));
				TF += bt.TF;
				IDF += bt.IDF;
				TFIDF += bt.TF * bt.IDF;
			}
		}
		for (int nInd = 0; nInd < query.NewTerms.size(); ++nInd) {
			if (termVector.containsKey(query.NewTerms.get(nInd))) {
				BTerm bt = termVector.get(query.NewTerms.get(nInd));
				TF += bt.TF;
				IDF += bt.IDF;
				TFIDF += bt.TF * bt.IDF;
			}
		}
		return new float[] {TF, IDF, TFIDF};
	}
	
	private int find2Gram(QueryInfo query, ArrayList<String> list) {
		int result = 0;
		ArrayList<String> qs = query.OrigTerms;
		for (int lInd = 0; lInd < list.size() - 1; ++lInd) {
			for (int qInd = 0; qInd < qs.size() - 1; ++qInd) {
				if (list.get(lInd).equals(qs.get(qInd)) &&
						list.get(lInd + 1).equals(qs.get(qInd + 1))) {
					++result;
				}
			}
		}
		return result;
	}
	
	private int findStaDis(QueryInfo query, ArrayList<String> list) 
			throws Exception {
		for (int lInd = 0; lInd < list.size(); ++lInd) {
			for (int qInd = 0; qInd < query.OrigTerms.size(); ++qInd) {
				if (!BIndexCreator.IsCommonWord(
						query.OrigTerms.get(qInd)) && 
						list.get(lInd).equals(
								query.OrigTerms.get(qInd))) {
					return lInd;
				}
			}
			for (int qInd = 0; qInd < query.NewTerms.size(); ++qInd) {
				if (!BIndexCreator.IsCommonWord(
						query.NewTerms.get(qInd)) && 
						list.get(lInd).equals(
								query.NewTerms.get(qInd))) {
					return lInd;
				}
			}
		}
		return -1;
	}
	
	private DocFeatSet loadDocFeatures(BDocumentAtIndex doc,
			QueryInfo qInfo) throws Exception {
		doc.FetchFields();
		DocFeatSet feat = new DocFeatSet();
		feat.DOCID = doc.DOCID;
		feat.QNO = String.valueOf(qInfo.Query.id);
		HashMap<String, Integer> rels = BTrecEvalResult.QueryDocRels.get(
				qInfo.Query.id);
		if (rels != null && rels.containsKey(doc.DOCID))
			feat.Relevance = rels.get(doc.DOCID);
		feat.BM25 = doc.scoreAtRetrieve;
		feat.BM25Title = findDocScore(qInfo.ResultTitle, doc);
		feat.BM25Text = findDocScore(qInfo.ResultText, doc);
		feat.BM25Meta = findDocScore(qInfo.ResultMeta, doc);
		doc.FetchTermVector("TITLE", true);
		float[] vals = stat(qInfo, doc.termVector);
		feat.Gram1TFTitle = vals[0];
		feat.Gram1IDFTitle = vals[1];
		feat.Gram1TFIDFTitle = vals[2];
		doc.FetchTermVector("TEXT", true);
		vals = stat(qInfo, doc.termVector);
		feat.Gram1TFText = vals[0];
		feat.Gram1IDFText = vals[1];
		feat.Gram1TFIDFText = vals[2];
		doc.FetchTermVector("META", true);
		vals = stat(qInfo, doc.termVector);
		feat.Gram1TFMeta = vals[0];
		feat.Gram1IDFMeta = vals[1];
		feat.Gram1TFIDFMeta = vals[2];
		doc.FetchTermVector("TITLE", true);
		doc.FetchTermVector("TEXT", false);
		doc.FetchTermVector("META", false);
		vals = stat(qInfo, doc.termVector);
		feat.Gram1TF = vals[0];
		feat.Gram1IDF = vals[1];
		feat.Gram1TFIDF = vals[2];
		feat.Gram2TFTitle = find2Gram(qInfo, 
				BIndexCreator.Tokenize(doc.TITLE));
		feat.Gram2TFText = find2Gram(qInfo, 
				BIndexCreator.Tokenize(doc.TEXT));
		feat.Gram2TFMeta = find2Gram(qInfo, 
				BIndexCreator.Tokenize(doc.META));
		feat.Gram2TF = feat.Gram2TFTitle + 
				feat.Gram2TFText + 
				feat.Gram2TFMeta;
		feat.StartDistanceText = findStaDis(qInfo, 
				BIndexCreator.Tokenize(doc.TEXT));
		feat.DomainWeight = 
				(float)BIndexCreator.GetDomainCount(doc.URL) /
				BStandardMethods.DocCount;
		if (useLDA) {
			doc.FetchTITLEAndTEXT();
			double[] vec = BMallet.getDistribution(doc.TITLE + " " + doc.TEXT);
			feat.LDASim = (float)BMallet.getDistance(qInfo.LDAVec, vec, true);
			feat.LDASimTop1 = (float)BMallet.getDistance(qInfo.LDAVecTop1, 
					vec, false);
			feat.LDASimTop3 = (float)BMallet.getDistance(qInfo.LDAVecTop3, 
					vec, false);
			doc.clear();
		}
		return feat;
	}

	private HashSet<String> savedQueries = new HashSet<>();
	private Hashtable<String, String> savedVectors = new Hashtable<>();
	public String generateStringJournal(BOptimize opt, List<BQuery> queries, 
			BQueryChainResult chain, BIRParam param, boolean printProgress) throws Exception {
		StringBuilder sb = new StringBuilder();
		if (printProgress) 
			BLib.print("ltr");
		for (int qInd = 0; qInd < queries.size(); ++qInd) {
			BQueryResult bqr = chain.queries.get(qInd);
			QueryInfo qi = null;
			if (!savedQueries.contains(queries.get(qInd).id)) {
				qi = loadQueryInfo(queries.get(qInd), param, opt);
				savedQueries.add(queries.get(qInd).id + "");
			}
			for (int dInd = 0; 
					dInd < topCount && dInd < bqr.results.size(); 
					++dInd) {
				BDocumentAtIndex doc = bqr.results.get(dInd);
				String key = queries.get(qInd).id + ">" + doc.idAtIndex;
				if (!savedVectors.containsKey(key)) {
					DocFeatSet dfs = loadDocFeatures(doc, qi);
					savedVectors.put(key, dfs.toString());
				}
				sb.append(savedVectors.get(key).toString() + "\n");				
			}
			if (printProgress) 
				BLib.print(" q" + (qInd + 1));
		}
		if (printProgress) 
			BLib.println();
		return sb.toString();
	}
	
	public String generateString(BOptimize opt, List<BQuery> queries, 
			BQueryChainResult chain, BIRParam param, boolean printProgress) throws Exception {
		StringBuilder sb = new StringBuilder();
		if (printProgress) 
			BLib.print("ltr");
		for (int qInd = 0; qInd < queries.size(); ++qInd) {
			BQueryResult bqr = chain.queries.get(qInd);
			QueryInfo qi = null;
			if (queries.get(qInd).ltrTag == null) {
				queries.get(qInd).ltrTag = loadQueryInfo(queries.get(qInd), param, opt);
			}
			qi = (QueryInfo)queries.get(qInd).ltrTag;
			for (int dInd = 0; 
					dInd < topCount && dInd < bqr.results.size(); 
					++dInd) {
				BDocumentAtIndex doc = bqr.results.get(dInd);
				DocFeatSet dfs = loadDocFeatures(doc, qi);
				sb.append(dfs.toString() + "\n");
			}
			if (printProgress) 
				BLib.print(" q" + (qInd + 1));
		}
		if (printProgress) 
			BLib.println();
		return sb.toString();
	}
	
	public void Train(BOptimize opt, List<BQuery> queries, 
			BQueryChainResult chain, BIRParam param, 
			BLTRParam ltrParam, String modelName, 
			String optionalDirectory, boolean printProgress) 
					throws Exception {
		String sb = generateString(opt, queries, chain, param, printProgress);
		runRankLibTrain(ltrParam, sb, modelName, optionalDirectory);
	}

	private void runRankLibTrain(BLTRParam ltrParam, String text, 
			String modelName, String optionalDirectory) throws Exception {
		String dir = BLib.createDIR(optionalDirectory);
		String inFile = dir + modelName + "In.txt";
		String outFile = dir + "model-" + modelName + ".txt";
		PrintStream ps = new PrintStream(inFile);
		ps.print(text);
		ps.close();
		ProcessBuilder ranklibProcConfig = new ProcessBuilder(
				"java", "-jar", BGlobalVar.RankLibFile, "-train",
				inFile, "-ranker", "0", "-metric2t", "NDCG@100",
				"-save", outFile
				, "-leaf", ltrParam.LeafCount + ""
				, "-shrinkage", ltrParam.Shrinkage + ""
				);
		Process ranklibProc = ranklibProcConfig.start();
		BufferedReader stdIn = new BufferedReader(
				new InputStreamReader(ranklibProc.getInputStream()));
		List<String> output = new ArrayList<String>();
		String line = null;
		while ((line = stdIn.readLine()) != null){
			output.add(line + "\n");
		}
		stdIn.close();
		Files.delete(Paths.get(inFile));
	}

	public void Rerank(BOptimize opt, List<BQuery> queries, 
			BQueryChainResult chain, BIRParam param,
			String modelName, String optionalDirectory, 
			boolean printProgress) throws Exception {
		String sb = generateString(opt, queries, chain, param, printProgress);
		ArrayList<String> lines = runRankLibTest(sb, modelName, optionalDirectory);
		for (int lInd = 0; lInd < lines.size(); ++lInd) {
			String[] tokens = lines.get(lInd).split("\t");
			int qVal = Integer.parseInt(tokens[0]);
			int rVal = Integer.parseInt(tokens[1]);
			float sVal = Float.parseFloat(tokens[2]);
			int qInd = 0;
			for (qInd = 0; qInd < chain.queries.size(); ++qInd) {
				if (chain.queries.get(qInd).QueryNo == qVal) {
					break;
				}
			}
			chain.queries.get(qInd).results.get(rVal).
				scoreAtRetrieve = sVal;
		}
		for (int qInd = 0; qInd < chain.queries.size(); ++qInd) {
			ArrayList<BDocumentAtIndex> list = BDocumentAtIndex.Remove(
					chain.queries.get(qInd).results, 0, topCount);
			float baseScore = chain.queries.get(qInd).results.get(0).
					scoreAtRetrieve;
			list = BDocumentAtIndex.SortByScore(list, false);
			for (BDocumentAtIndex doc : list) {
				doc.scoreAtRetrieve += baseScore;
			}
			chain.queries.get(qInd).results.addAll(0, list);
		}
	}
	
	private ArrayList<String> runRankLibTest(String text, 
			String modelName, String optionalDirectory) throws Exception {
		String dir = BLib.createDIR(optionalDirectory);
		String inFile = dir + modelName + "TempIn.txt";
		String ouFile = dir + modelName + "TempOut.txt";
		String modelFile = dir + "model-" + modelName + ".txt";
		PrintStream ps = new PrintStream(inFile);
		ps.print(text);
		ps.close();
		ProcessBuilder ranklibProcConf = new ProcessBuilder(
				"java", "-jar", BGlobalVar.RankLibFile, "-load",
				modelFile, "-rank", inFile, "-score", ouFile);
		Process ranklibProc = ranklibProcConf.start();
		BufferedReader stdIn = new BufferedReader(
				new InputStreamReader(ranklibProc.getInputStream()));
		List<String> output = new ArrayList<String>();
		String line = null;
		while ((line = stdIn.readLine()) != null) {
			output.add(line + "\n");
		}
		stdIn.close();
		BufferedReader br = new BufferedReader(new FileReader(ouFile));
		ArrayList<String> result = new ArrayList<String>();
		line = null;
		while ((line = br.readLine()) != null) {
			result.add(line + "\n");
		}
		br.close();
		Files.delete(Paths.get(inFile));
		Files.delete(Paths.get(ouFile));
		return result;
	}

}
