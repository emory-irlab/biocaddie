package biocaddie;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.lucene.document.Document;

import biocaddie.BOptimize.ExpSett;

public class BJournalExperiments {
	
	enum Mode {
		bas,
		Sim,
		Wig,
		Int,
		Ext,
		Ful
	}
	
	private static class ExpSett {
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

	private static class StatSteps {
		
		private BTrecEvalResult basR = new BTrecEvalResult(),
			simR = new BTrecEvalResult(), 
			wigR = new BTrecEvalResult(), 
			intR = new BTrecEvalResult(), 
			extR = new BTrecEvalResult(), 
			fulR = new BTrecEvalResult();
		
		public void sum(BTrecEvalResult trec, Mode mode) {
			if (mode == Mode.bas) {
				basR.sum(trec);
			}
			else if (mode == Mode.Sim) {
				simR.sum(trec);
			}
			else if (mode == Mode.Wig) {
				wigR.sum(trec);
			}
			else if (mode == Mode.Int) {
				intR.sum(trec);
			}
			else if (mode == Mode.Ext) {
				extR.sum(trec);
			}
			else if (mode == Mode.Ful) {
				fulR.sum(trec);
			}
		}

		public void ave(int count) {
			basR.ave(count);
			simR.ave(count);
			wigR.ave(count);
			intR.ave(count);
			extR.ave(count);
			fulR.ave(count);
		}

		@Override
		public String toString() {
			return 
					"bas : " + basR.toStringFull() + "\n" +
					"sim : " + simR.toStringFull() + "\n" +
					"wig : " + wigR.toStringFull() + "\n" +
					"int : " + intR.toStringFull() + "\n" +
					"ext : " + extR.toStringFull() + "\n" +
					"ful : " + fulR.toStringFull() + "\n";
		}
		
	}

	private static class StatLtr {
		
		public ArrayList<Float> 
				MAP = new ArrayList<>(), 
				NDCG = new ArrayList<>(), 
				PAt10 = new ArrayList<>();
		
		public void sum(BTrecEvalResult trec, int index) {
			while (index >= MAP.size()) {
				MAP.add(0f);
				NDCG.add(0f);
				PAt10.add(0f);
			}
			MAP.set(index, MAP.get(index) + trec.MAP);
			NDCG.set(index, NDCG.get(index) + trec.NDCG);
			PAt10.set(index, PAt10.get(index) + trec.PAt10);
		}
		
		public void ave(int count) {
			for (int ind = 0; ind < MAP.size(); ++ind) {
				MAP.set(ind, MAP.get(ind) / count);
				NDCG.set(ind, NDCG.get(ind) / count);
				PAt10.set(ind, PAt10.get(ind) / count);
			}
		}
		
		@Override
		public String toString() {
			StringBuilder result = new StringBuilder();
			for (int ind = 0; ind < MAP.size(); ++ind) {
				result.append((ind + 1) + 
						" NDCG: " + NDCG.get(ind) + 
						" MAP: " + MAP.get(ind) + 
						" P@10: " + PAt10.get(ind) + "\n");
			}
			return result.toString();
		}
		
	}
	
	public static void analyzeSteps() throws Exception {
		ArrayList<BIRParam> params = loadParamsFromSummary(
				BGlobalVar.Results + 
				"paper-experiments/ir/summary.txt");
		List<BQuery> queries = BQuery.LoadAllQueries();
		ArrayList<ExpSett> qSet = prepareSettings(queries, 5, 21);
		BIRParam base = BIRParam.GetBest(true);
		StatSteps sts = new StatSteps();
		for (int qInd = 0; qInd < params.size(); ++qInd) {
			BIRParam pp = params.get(qInd);
			ExpSett qs = qSet.get(qInd);
			BOptimize bop = new BOptimize(qs.Test, base, false, null);
			BIRParam tempParam = null;
			BQueryChainResult chain = null;

			chain = bop.execSimple(qs.Test, pp);
			BTrecEvalResult.RunTreceval(chain);
			sts.sum(chain.trec, Mode.Sim);
			System.out.println((qInd + 1) + " simp > " + chain);
			
			tempParam = pp.Copy();
			tempParam.IntTermsWeight = tempParam.ExtTermsWeight = 
					tempParam.IntTerms = tempParam.ExtTerms = 0;
			chain = bop.exec(qs.Test, tempParam);
			BTrecEvalResult.RunTreceval(chain);
			sts.sum(chain.trec, Mode.Wig);
			System.out.println((qInd + 1) + " +wig > " + chain);
			
			tempParam = pp.Copy();
			tempParam.ExtTermsWeight = tempParam.ExtTerms = 0;
			chain = bop.exec(qs.Test, tempParam);
			BTrecEvalResult.RunTreceval(chain);
			sts.sum(chain.trec, Mode.Int);
			System.out.println((qInd + 1) + " +int > " + chain);
			
			tempParam = pp.Copy();
			tempParam.IntTermsWeight = tempParam.IntTerms = 0;
			chain = bop.exec(qs.Test, tempParam);
			BTrecEvalResult.RunTreceval(chain);
			sts.sum(chain.trec, Mode.Ext);
			System.out.println((qInd + 1) + " +ext > " + chain);

			chain = bop.exec(qs.Test, pp);
			BTrecEvalResult.RunTreceval(chain);
			sts.sum(chain.trec, Mode.Ful);
			System.out.println((qInd + 1) + " +ful > " + chain);
			
			System.out.println();
		}
		sts.ave(params.size());
		System.out.println("average:");
		System.out.println(sts.toString());
	}

	public static void averageLtr() throws Exception {
		List<BQuery> queries = BQuery.LoadAllQueries();
		ArrayList<ExpSett> qSet = prepareSettings(queries, 5, 21);
		BIRParam base = BIRParam.GetBest(true);
		BIRParam best = BIRParam.GetBest(false);
		BOptimize bop = new BOptimize(queries, base, false, null);
		BTrecEvalResult expR = new BTrecEvalResult();
		BTrecEvalResult ltrR = new BTrecEvalResult();
		for (int qind = 0; qind < qSet.size(); ++qind) {
			BQueryChainResult chain = bop.exec(qSet.get(qind).Test, best);
			BTrecEvalResult.RunTreceval(chain);
			expR.sum(chain.trec);
			BRankLib ltr = new BRankLib();
			ltr.Rerank(bop, qSet.get(qind).Test, chain, best, best.ExpansionInfo, 
					"paper-experiments/ltr/" + (qind + 1), true);
			BTrecEvalResult.RunTreceval(chain);
			System.out.println("> " + chain);
			ltrR.sum(chain.trec);
		}
		expR.ave(qSet.size());
		ltrR.ave(qSet.size());
		System.out.println("test exp " + expR.toStringFull());
		System.out.println("test ltr " + ltrR.toStringFull());
	}
	
	public static void analyzeLtr() throws Exception {
		String dir = BGlobalVar.Results + "paper-experiments/ltr-yan/";
		List<BQuery> queries = BQuery.LoadAllQueries();
		ArrayList<ExpSett> qSet = prepareSettings(queries, 5, 21);
		BIRParam base = BIRParam.GetBest(true);
		BIRParam best = BIRParam.GetBest(false);
		BOptimize bop = new BOptimize(queries, base, false, null);
		StatLtr sts = new StatLtr();
		for (int qind = 0; qind < qSet.size(); ++qind) {
			for (int sind = 1; sind <= 8; ++sind) {
				BRankLib.LTRAllowState = true;
				BRankLib.LTRstate = sind;
				BQueryChainResult chain = bop.exec(qSet.get(qind).Train, best);
				BRankLib ltr = new BRankLib();
				ltr.Train(bop, qSet.get(qind).Train, chain, best, 
						BLTRParam.GetBest(), best.ExpansionInfo, null, false); ///////////////////////////// adjust BLTRParam
				chain = bop.exec(qSet.get(qind).Test, best);
				ltr.Rerank(bop, qSet.get(qind).Test, chain, best, 
						best.ExpansionInfo, null, false);
				BTrecEvalResult.RunTreceval(chain);
				sts.sum(chain.trec, sind - 1);
				System.out.println("F" + (qind + 1) + " L" + sind + " > " + chain);				
			}
		}
		sts.ave(qSet.size());
		System.out.println("average:");
		System.out.println(sts.toString());
	}

	public static void gatherIndexStat() throws Exception {
		System.out.println(">>> doc count: " + BStandardMethods.DocCount);
		long titleLen = 0;
		long textLen = 0;
		for (int cind = 0; cind < BStandardMethods.DocCount; ++cind) {
			Document doc = BStandardMethods.indexReader.document(cind);
			titleLen += doc.get("TITLE").length();
			textLen += doc.get("TEXT").length();
			if (cind % 50000 == 0) {
				System.out.println(cind + "/" + BStandardMethods.DocCount);
			}
		}
		System.out.println(">>> title ave:" + (titleLen / (float)BStandardMethods.DocCount) );
		System.out.println(">>> text ave:" + (textLen / (float)BStandardMethods.DocCount) );
	}
	
	public static ArrayList<BIRParam> loadParamsFromSummary(
			String filePath) throws Exception {
		ArrayList<BIRParam> result = new ArrayList<>();
		List<String> lines = Files.readAllLines(Paths.get(filePath));
		for (int ind = 0; ind < lines.size(); ++ind) {
			String line = lines.get(ind);
			if (line.length() > 0 && line.charAt(0) == '@') {
				String[] tokens = line.split(" ");
				BIRParam prm = new BIRParam(
						Float.parseFloat(tokens[3]), Float.parseFloat(tokens[5]), 
						Float.parseFloat(tokens[7]), Float.parseFloat(tokens[9]), 
						Float.parseFloat(tokens[11]), Integer.parseInt(tokens[13]),
						Integer.parseInt(tokens[15]), Float.parseFloat(tokens[17]),
						Integer.parseInt(tokens[19]), Float.parseFloat(tokens[21]));
				result.add(prm);
			}
		}
		return result;
	}

	private static float[] loadLineNumbers(String line) {
		String[] tokens = line.split(" ");
		float ndcg = Float.parseFloat(tokens[3]);
		float map = Float.parseFloat(tokens[5]);
		float pat10 = Float.parseFloat(tokens[7]);
		return new float[] {ndcg, map, pat10};
	}

	private static ArrayList<ExpSett>  prepareSettings(List<BQuery> list, 
			int bucketSize, Integer nullableExclusiveSplitBound) {
		ArrayList<ExpSett> settings = new ArrayList<>();
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
		return settings;
	}

}
