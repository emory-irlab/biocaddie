package biocaddie;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;

import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TermStatistics;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.DefaultSimilarity;
import org.apache.lucene.search.similarities.Similarity;

public class BOptimize {
	public static int bucketSize = 5;
	private List<BQuery> queries;
	private ArrayList<BIRParam> params;
	private ArrayList<ExpSett> settings;
	private Hashtable<String, Integer> wigFreqs = new Hashtable<>();
	
	class ExpSett {
		public ArrayList<BQuery> Train = new ArrayList<>();
		public ArrayList<BQuery> Test = new ArrayList<>();
		
		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append("train: ");
			for (int ind = 0; ind < Train.size(); ++ind)
				sb.append(Train.get(ind).id + ", ");
			sb.append("test: ");
			for (int ind = 0; ind < Test.size(); ++ind)
				sb.append(Test.get(ind).id + ", ");
			return sb.toString();
		}
	
	}
	
	class ExpRes {
		public BIRParam Param;
		public BLTRParam LTRParam;
		public BQueryChainResult Train;
		public BQueryChainResult Test;

		public ExpRes(BIRParam param, BLTRParam ltrParam) {
			Param = param;
			LTRParam = ltrParam;
		}

	}
	
	class ExpanSett {
		public ArrayList<BTerm> MainCandidTerms = new ArrayList<>();
		public ArrayList<BTerm> CandidTerms = new ArrayList<>();
		public BIRParam Param;
		public BQueryResult Result;
		public BBOW FeedTerms = new BBOW();
		public ArrayList<BTerm> FeedTermsSorted = new ArrayList<>();
	}
	
	public BOptimize(List<BQuery> list, BIRParam baseParam, 
			boolean prepareForTrain, Integer nullableExclusiveSplitBound) 
					throws Exception {
		queries = list;
//		BLib.println("base expansion and retrieval started");
		findNewTerms(queries);
		for (int lInd = 0; lInd < list.size(); ++lInd) {
			ExpanSett es = (ExpanSett)list.get(lInd).tag;			
			es.Result = execSimple(list.get(lInd), baseParam);
			for (int dInd = 0; dInd < BIRParam.MaxTopDocs; ++dInd) {
				es.Result.results.get(dInd).FetchTermVector("TITLE", true);
				es.Result.results.get(dInd).FetchTermVector("TEXT", false);
				es.Result.results.get(dInd).FetchTermVector("META", false);
			}
		}
		if (prepareForTrain) {
			params = BIRParam.GetParams();
			prepareSettings(list, nullableExclusiveSplitBound);
		}
	}
	
	public void prepareSettings(List<BQuery> list, 
			Integer nullableExclusiveSplitBound) {
		if (nullableExclusiveSplitBound == null)
			nullableExclusiveSplitBound = list.size();
		ArrayList<BQuery> mainList = new ArrayList<>();
		mainList.addAll(list.subList(0, nullableExclusiveSplitBound));
		ArrayList<BQuery> fixed = new ArrayList<>();
		fixed.addAll(list.subList(nullableExclusiveSplitBound, list.size()));
		
		settings = new ArrayList<>();
		int en = (int)Math.ceil(mainList.size() / (float)bucketSize);
		for (int bInd = 0; bInd < en; ++bInd) {
			ExpSett es = new ExpSett();
			settings.add(es);
			for (int qInd = 0; qInd < mainList.size(); ++qInd) {
				if (bInd * bucketSize <= qInd &&
						qInd < (bInd + 1) * bucketSize) {
					es.Test.add(mainList.get(qInd));
				}
				else {
					es.Train.add(mainList.get(qInd));
				}
			}
			es.Train.addAll(fixed);
		}
		int lastInd = settings.size() - 1;
		if (settings.get(lastInd).Test.size() < (bucketSize / 2)) {
			settings.get(lastInd - 1).Test.addAll(settings.get(lastInd).Test);
			settings.get(lastInd - 1).Train.removeAll(settings.get(lastInd).Test);
			settings.remove(lastInd);
		}
	}
	
	private void findNewTerms(List<BQuery> list) throws Exception {
		for (int qInd = 0; qInd < list.size(); ++qInd) {
			BQuery bq = list.get(qInd);
			ExpanSett exp = new ExpanSett();
			bq.tag = exp;
			BBOW bb = BExpansion.FindRelatedTerms(bq.title);
			exp.MainCandidTerms.addAll(BTerm.SortByTF(bb.getTerms(), false));
//			BLib.println("query " + (qInd + 1) + " finished");
		}
	}

	public void runcvLTR(String dir) throws Exception {
		StringBuilder sb = new StringBuilder();
		sb.append("_______________________\n");
		BRankLib ltr = new BRankLib();
		BIRParam best = BIRParam.GetBest(false);
		for (int sInd = 0; sInd < settings.size(); ++sInd) {
			StringBuilder tempSb = new StringBuilder("bucket: " + (sInd + 1) + "\n");

			ExpRes maxLTR = new ExpRes(null, BLTRParam.GetBest()); //null;
//			ArrayList<BLTRParam> ltrparams = BLTRParam.GetParams();
//			for (int pInd = 0; pInd < ltrparams.size(); ++pInd) {
//				ExpRes er = new ExpRes(null, ltrparams.get(pInd));
//				er.Train = exec(settings.get(sInd).Train, best);
//				ltr.Train(this, settings.get(sInd).Train, er.Train, best, 
//						er.LTRParam, best.ExpansionInfo, dir + (sInd + 1), true);
//				ltr.Rerank(this, settings.get(sInd).Train, er.Train, best, 
//						best.ExpansionInfo, dir + (sInd + 1), true);				
//				BTrecEvalResult.RunTreceval(er.Train);
//				if (maxLTR == null || 
//						maxLTR.Train.trec.NDCG < er.Train.trec.NDCG)
//					maxLTR = er;
//				if (pInd % 1 == 0) {
//					BLib.println("bucket: " + (sInd + 1) + "/" + settings.size()
//							+ " > " + (pInd + 1) + "/" + ltrparams.size()
//							+ " > " + maxLTR.Train
//							+ " > " + maxLTR.LTRParam);
//				}
//			}
			tempSb.append("@" + (sInd + 1) + " param> " + maxLTR.LTRParam + "\n");
			ExpRes maxIR = new ExpRes(best, null);
			maxIR.Train = exec(settings.get(sInd).Train, maxIR.Param);
			BTrecEvalResult.RunTreceval(maxIR.Train);
			tempSb.append("train exp> " + maxIR.Train + "\n");
			maxIR.Test = exec(settings.get(sInd).Test, maxIR.Param);
			BTrecEvalResult.RunTreceval(maxIR.Test);
			tempSb.append("test exp> " + maxIR.Test + "\n");

			ltr.Train(this, settings.get(sInd).Train, maxIR.Train, maxIR.Param, 
					maxLTR.LTRParam, maxIR.Param.ExpansionInfo, dir + (sInd + 1), true);
			ltr.Rerank(this, settings.get(sInd).Test, maxIR.Test, maxIR.Param, 
					maxIR.Param.ExpansionInfo, dir + (sInd + 1), true);
			BTrecEvalResult.RunTreceval(maxIR.Test);
			maxIR.Test.description = "LTR";
			BQueryChainResult.PrintChains(dir + (sInd + 1), maxIR.Test);
			tempSb.append("test ltr> " + maxIR.Test + "\n");

			tempSb.append("_______________________\n");
			BLib.print(tempSb.toString());
			sb.append(tempSb.toString());
		}
		BLib.println(sb.toString());
		PrintStream ps = new PrintStream(BGlobalVar.Results + dir + "summary.txt");
		ps.print(sb.toString());
		ps.close();
	}

	public void runcvIR(String dir) throws Exception {
		StringBuilder sb = new StringBuilder();
		sb.append("_______________________\n");
		BRankLib ltr = new BRankLib();
		for (int sInd = 0; sInd < settings.size(); ++sInd) {
			StringBuilder tempSb = new StringBuilder("bucket: " + (sInd + 1) + "\n");

			ExpRes max = null;
			for (int pInd = 0; pInd < params.size(); ++pInd) {
				ExpRes er = new ExpRes(params.get(pInd), null);
				er.Train = exec(settings.get(sInd).Train, er.Param);
				BTrecEvalResult.RunTreceval(er.Train);
				if (max == null || 
						max.Train.trec.NDCG < er.Train.trec.NDCG)
					max = er;
				if (pInd % 10 == 0) {
					BLib.println("bucket: " + (sInd + 1) + "/" + settings.size()
							+ " > " + (pInd + 1) + "/" + params.size()
							+ " > " + max.Train
							+ " > " + max.Param);
				}
			}
			tempSb.append("@" + (sInd + 1) + " param> " + max.Param + "\n");
			BQueryChainResult simTrain = execSimple(settings.get(sInd).Train, max.Param);
			BTrecEvalResult.RunTreceval(simTrain);
			tempSb.append("train sim> " + simTrain + "\n");
			tempSb.append("train exp> " + max.Train + "\n");

			BQueryChainResult simTest = execSimple(settings.get(sInd).Test, max.Param);
			BTrecEvalResult.RunTreceval(simTest);
			BQueryChainResult.PrintChains(dir + (sInd + 1), simTest);
			tempSb.append("test sim> " + simTest + "\n");
			max.Test = exec(settings.get(sInd).Test, max.Param);
			BTrecEvalResult.RunTreceval(max.Test);
			max.Test.description = "expand";
			BQueryChainResult.PrintChains(dir + (sInd + 1), max.Test);
			tempSb.append("test exp> " + max.Test + "\n");

			tempSb.append("_______________________\n");
			BLib.print(tempSb.toString());
			sb.append(tempSb.toString());
		}
		BLib.println(sb.toString());
		PrintStream ps = new PrintStream(BGlobalVar.Results + dir + "summary.txt");
		ps.print(sb.toString());
		ps.close();
	}
	
	private float getStd(BQueryChainResult bqcr) {
		float squared = 0;
		float mean = bqcr.trec.NDCG;
		for (int qInd = 0; qInd < bqcr.queries.size(); ++qInd) {
			squared += (bqcr.queries.get(qInd).trec.NDCG - mean) * 
					(bqcr.queries.get(qInd).trec.NDCG - mean);
		}
		squared /= bqcr.queries.size();
		return squared;
	}
	
	public void overfit() throws Exception {
		ArrayList<ExpRes> bests = new ArrayList<>();
		for (int pInd = 0; pInd < params.size(); ++pInd) {
			ExpRes er = new ExpRes(params.get(pInd), null);
			er.Train = exec(queries, er.Param);
			BTrecEvalResult.RunTreceval(er.Train);
			if (bests.size() == 0 || 
					bests.get(0).Train.trec.NDCG <= er.Train.trec.NDCG) {
				if (bests.size() == 0 || 
					bests.get(0).Train.trec.NDCG < er.Train.trec.NDCG) {
					bests.clear();
				}
				bests.add(er);
			}
			if (pInd % 5 == 0) {
				BLib.println(
						"param " + (pInd + 1) + "/" + params.size() + 
						" >   @param> " + bests.get(0).Param + 
						"   @result> " + bests.get(0).Train);
			}
		}
		int bestInd = 0;
		float bestStd = getStd(bests.get(0).Train);
		for (int ind = 1; ind < bests.size(); ++ind) {
			float std = getStd(bests.get(ind).Train);
			if (bestStd > std) {
				bestInd = ind;
				bestStd = std;
			}
		}
		BLib.println("@param> " + bests.get(bestInd).Param + "\n");
		BLib.println("@result> " + bests.get(bestInd).Train);
	}

	public BQueryChainResult execSimple(List<BQuery> list, 
			BIRParam param) throws Exception {
		Similarity sim = 
//				new DefaultSimilarity();				
				new BM25Similarity(param.BM25K1, param.BM25b);
		BQueryChainResult chain = new BQueryChainResult(sim.toString(), "");
		for (int qInd = 0; qInd < list.size(); ++qInd) {			
			BQueryResult bqr = execSimple(list.get(qInd), param);
			chain.queries.add(bqr);
		}
		return chain;
	}
	
	public BQueryChainResult exec(List<BQuery> list, 
			BIRParam param) throws Exception {
		Similarity sim = 
//				new DefaultSimilarity();				
				new BM25Similarity(param.BM25K1, param.BM25b);
		BQueryChainResult chain = new BQueryChainResult(sim.toString(), "");
		for (int qInd = 0; qInd < list.size(); ++qInd) {			
			BQueryResult bqr = exec(list.get(qInd), param);
			chain.queries.add(bqr);
		}
		return chain;
	}
	
	public BQueryResult execSimple(BQuery query, BIRParam param) 
			throws Exception {
		Similarity sim =
//				new DefaultSimilarity();
				new BM25Similarity(param.BM25K1, param.BM25b);
		String mainCmd = "exec" +
				" -ti " + param.TitleWeight + 
				" -te " + param.TextWeight +
				" -me " + param.MetaWeight +
				" -q " + query.id +
				" $ 1 " + query.title;
		List<String> tokens = Arrays.asList(mainCmd.split(" "));
		BQueryChainResult temp = BStandardMethods.retrieve(tokens, 
				sim, "t", "t");
		BQueryResult result = temp.queries.get(0);
		return result;
	}

	public BQueryResult exec(BQuery query, BIRParam param) 
			throws Exception {
		Similarity sim = 
//				new DefaultSimilarity();
				new BM25Similarity(param.BM25K1, param.BM25b);
		String mainCmd = "exec" +
				" -ti " + param.TitleWeight + 
				" -te " + param.TextWeight +
				" -me " + param.MetaWeight +
				" -q " + query.id;
		ExpanSett es = (ExpanSett)query.tag;
		if (es.Param == null || es.Param.TopDocs != param.TopDocs) {
			adjustExpansionTerms(query, param);			
		}
		es.Param = param;
		if (param.ExpansionIs) {
			BQueryResult t = execExpand(mainCmd, query, param, sim);
			return t;
		}
		else {
			BQueryResult t = execReweight(mainCmd, query, param, sim);
			return t;
		}
	}
	
	public String newExtTerms(BQuery query, BIRParam param) 
			throws Exception {
		StringBuilder sb = new StringBuilder();
		ExpanSett es = (ExpanSett)query.tag;
		if (es.Param == null || es.Param.TopDocs != param.TopDocs) {
			adjustExpansionTerms(query, param);			
		}
		if (es.CandidTerms.size() > 0 && es.CandidTerms.get(0).TF > 0) {
			int tInd = 0;
			int added = 0;
			while (added < param.ExtTerms && 
					tInd < es.CandidTerms.size()) {
				BTerm bt = es.CandidTerms.get(tInd);
				++tInd;
				if (bt.TF == 0)
					break;
				if (BIndexCreator.GetFrequency(bt.Text) <= 3)
					continue;
				if (sb.length() == 0)
					sb.append(" $ " + param.ExtTermsWeight);
				sb.append(" " + bt.Text);
				++added;
			}
		}
		return sb.toString();
	}
	
	public String newIntTerms(BQuery query, BIRParam param) throws Exception {
		StringBuilder sb = new StringBuilder();
		ExpanSett es = (ExpanSett)query.tag;
		if (es.Param == null || es.Param.TopDocs != param.TopDocs) {
			adjustExpansionTerms(query, param);	
		}
		int added = 0;
		int ind = 0;
		while (added < param.IntTerms && ind < es.FeedTermsSorted.size()) {
			BTerm term = es.FeedTermsSorted.get(ind);
			if (term.TF > 1 && !BIndexCreator.IsCommonWord(term.Text)) {
				sb.append(" " + term.Text);
				++added;
			}
			++ind;
		}
		if (sb.length() > 0) {
			sb.insert(0, " $ " + param.IntTermsWeight);
		}
		return sb.toString();
	}
	
	private float log2(float d){
		return (float)(Math.log(d) / Math.log(2));
	}

	private void setInnerWeights(StringBuilder sb, BQuery query,
			BIRParam param) throws Exception {
		ExpanSett es = (ExpanSett)query.tag;
		String[] terms = query.title.split(" ");
		IndexSearcher is = BStandardMethods.indexSearcher;
		for (int tInd = 0; tInd < terms.length; ++tInd) {
			String norm = BIndexCreator.Normalize(terms[tInd]).trim();
			long cFreq = BStandardMethods.TotalTermFreq / 10;
			if (!BIndexCreator.IsCommonWord(norm)) {
				if (!wigFreqs.containsKey(norm)) {
					Term lTerm = new Term("TEXT", norm);
					TermStatistics tStat = is.termStatistics(lTerm, 
							TermContext.build(is.getTopReaderContext(), 
									lTerm));
					cFreq = tStat.totalTermFreq();
					wigFreqs.put(norm, (int)cFreq);
				}
				cFreq = wigFreqs.get(norm);
			}
			float cLog = log2((float)(cFreq + 1) / 
					(BStandardMethods.TotalTermFreq + 1));
			BTerm bTerm = es.FeedTerms.Get(norm);
			int topFreq = bTerm != null ? (int)bTerm.TF + 1 : 1;
			float tLog = log2((float)(topFreq + 1) / 
					(es.FeedTerms.Length + 1));
			float w = (tLog - cLog) / (-cLog);
			if (w <= 0)
				w = 1;
			else
				w += 2;
			sb.append(" $ ");
			sb.append(w);
			sb.append(" ");
			sb.append(norm);
		}
	}
	
	private BQueryResult execReweight(String mainCmd, BQuery query,
			BIRParam param, Similarity sim) throws Exception {
		StringBuilder cmd = new StringBuilder(mainCmd);
		setInnerWeights(cmd, query, param);
		List<String> tokens = Arrays.asList(cmd.toString().split(" "));
		BQueryChainResult temp = BStandardMethods.retrieve(
				tokens, sim, "t", "t");
		return temp.queries.get(0);
	}
	
	private BQueryResult execExpand(String mainCmd, BQuery query,
			BIRParam param, Similarity sim) throws Exception {
		StringBuilder cmd = new StringBuilder(mainCmd);
		setInnerWeights(cmd, query, param);
		cmd.append(newExtTerms(query, param));
		cmd.append(newIntTerms(query, param));
//		BLib.println("_______________________");
		List<String> tokens = Arrays.asList(cmd.toString().split(" "));
		BQueryChainResult temp = BStandardMethods.retrieve(tokens, 
				sim, "t", "t");
		return temp.queries.get(0);
	}
	
	private void adjustExpansionTerms(BQuery query, BIRParam param) 
			throws Exception {
		ExpanSett es = (ExpanSett)query.tag;
		int st = 0;
		if (es.Param != null && es.Param.TopDocs < param.TopDocs)
			st = es.Param.TopDocs;
		else
			es.FeedTerms.Clear();
		int en = param.TopDocs;
		for (int ind = st; ind < en; ++ind) {
			es.FeedTerms.Add(es.Result.results.get(ind).termVector.values());
		}
		if (param.ExpansionIs) {
			es.FeedTerms.RemoveText(query.title);
			BTerm.SetTFIDF(es.FeedTerms.getTerms());
			es.FeedTermsSorted = BTerm.SortByWeight(es.FeedTerms.getTerms(), false);
			es.CandidTerms = es.FeedTerms.FindShared(es.MainCandidTerms);
		}
		es.Param = param;
	}
	
}
