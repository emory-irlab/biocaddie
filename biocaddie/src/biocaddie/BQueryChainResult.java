package biocaddie;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.lucene.search.BooleanQuery;

public class BQueryChainResult {
	public List<BQueryResult> queries = new ArrayList<BQueryResult>();
	public BTrecEvalResult trec;
	public String name, description;
	
	public BQueryChainResult(String chainName, String chainDescription){
		name = chainName;
		description = chainDescription;
	}
	
	public BQueryChainResult(String chainName, String chainDescription, 
			BQueryResult... firstResults){
		name = chainName;
		description = chainDescription;
		queries.addAll(Arrays.asList(firstResults));
	}
	
	public static void PrintChains(String optionalDirectory, 
			BQueryChainResult... list) throws Exception {
		String Dir = BLib.createDIR(optionalDirectory);
		PrintTrecInputs(optionalDirectory, list);
		for (int i = 0; i < list.length; ++i) {
			PrintStream output = new PrintStream(Dir + "TrecOutput " + 
					list[i].name + " (" + list[i].description + ").txt");
			output.print(list[i].trec.trecEvalOutput);
			output.close();
		}
//		PrintAnalysisFile(Dir, list);
	}

	public static void PrintTrecInputs(String optionalDirectory, 
			BQueryChainResult... list) throws Exception{
		String Dir = BLib.createDIR(optionalDirectory);
		for (int i = 0; i < list.length; ++i) {
			PrintStream output = new PrintStream(Dir + "TrecInput " + 
					list[i].name + " (" + list[i].description + ").txt");
			output.print(list[i].trec.trecEvalInput);
			output.close();
		}
	}
	
	private static void PrintAnalysisFile(String Dir,
			BQueryChainResult... list) throws Exception {
		StringBuilder result = new StringBuilder();
		result.append(String.format("%40s", "") + "MAP\n");
		for (int runIndex = 0; runIndex < list.length; ++runIndex) {
			result.append(String.format("%-40s", list[runIndex].name) + 
					list[runIndex].trec.MAP + "\n");
		}
		result.append("\n");
		for (int queryIndex = 0; queryIndex < list[0].queries.size(); ++queryIndex){
			result.append("____________________\n");
			result.append("Q" + list[0].queries.get(queryIndex).QueryNo + "\n");
			for (int runIndex = 0; runIndex < list.length; ++runIndex){
				result.append(String.format("%-40s", list[runIndex].name) + 
						list[runIndex].queries.get(queryIndex).trec.MAP + "\n");
			}
			result.append("\n");
			for (int runIndex = 0; runIndex < list.length; ++runIndex){
				StringBuilder queryScores = new StringBuilder();
				queryScores.append(String.format("%-40s", list[runIndex].name));
				BooleanQuery bq = (BooleanQuery)list[runIndex].queries.get(
						queryIndex).query;
				for (int termIndex = bq.clauses().size() - 1; termIndex >= 0; --termIndex){
					queryScores.append(String.format("%-30s", bq.clauses().get(termIndex).
							getQuery().toString()));
				}
				result.append(queryScores.toString() + "\n");
			}
		}
		PrintStream resultFile = new PrintStream(Dir + "TrecOutput " + 
				list[0].name + " (analyze).txt");
		resultFile.print(result.toString());
		resultFile.close();		
	}
	
	@Override
	public String toString() {
		if (trec != null)
			return trec.toString();
		else
			return super.toString();
	}
	
}
