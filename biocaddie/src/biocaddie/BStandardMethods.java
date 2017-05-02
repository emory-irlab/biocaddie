package biocaddie;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.CollectionStatistics;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.similarities.DefaultSimilarity;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.NIOFSDirectory;
import org.apache.lucene.util.Version;

public class BStandardMethods {
	public static int relCount = 1000;
	public static IndexReader indexReader;
	public static IndexSearcher indexSearcher;
	public static DefaultSimilarity defaultSim;
	public static long TotalTermFreq = 0;
	public static long DocCount = 0;
	
	static {
		try {
			indexReader = DirectoryReader.open(
					FSDirectory.open(new File(BGlobalVar.Index)));
			indexSearcher = new IndexSearcher(indexReader);
			defaultSim = new DefaultSimilarity();
			
			CollectionStatistics cStat = indexSearcher.collectionStatistics("TEXT");
			TotalTermFreq = cStat.sumTotalTermFreq();
			DocCount = indexReader.maxDoc();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static org.apache.lucene.search.Query getQuery(
			List<String> cmd, QueryParser qp) throws Exception {
		BooleanQuery result = null;
		if (cmd.indexOf("$") < 0) {
			int qPos = cmd.indexOf("-q") + 1;
			int qInd = Integer.parseInt(cmd.get(qPos)) - 1;			
			List<BQuery> queries = BQuery.LoadAllQueries();
			result = (BooleanQuery)qp.parse(queries.get(qInd).title);
		}
		else {
			int ind = cmd.indexOf("$");
			float weight = 0;
			result = new BooleanQuery();
			while (ind < cmd.size()) {
				if (cmd.get(ind).equals("$")) {
					weight = Float.parseFloat(cmd.get(ind + 1));
					ind += 2;
				}
				BooleanClause clause = new BooleanClause(
						qp.parse(cmd.get(ind)), Occur.SHOULD);
				clause.getQuery().setBoost(weight);
				result.add(clause);
				++ind;
			}
		}
		return result;
	}

	private static float searchModifier(List<String> cmd, 
			String modifier) {
		float w = 0;
		if (cmd.indexOf(modifier) > 0) {
			int wInd = cmd.indexOf(modifier) + 1;
			w = Float.parseFloat(cmd.get(wInd));
		}
		return w;
	}
	
	public static BQueryChainResult retrieve(List<String> cmd, 
			Similarity similarity, String chainName, 
			String chainDescription) throws Exception {
		float tiWei = searchModifier(cmd, "-ti");
		float teWei = searchModifier(cmd, "-te");
		float meWei = searchModifier(cmd, "-me");
		indexSearcher.setSimilarity(similarity);
		QueryParser tiQp = new QueryParser(Version.LUCENE_48, "TITLE", 
				BIndexCreator.analyzer);
		QueryParser teQp = new QueryParser(Version.LUCENE_48, "TEXT", 
				BIndexCreator.analyzer);
		QueryParser meQp = new QueryParser(Version.LUCENE_48, "META", 
				BIndexCreator.analyzer);
		BQueryChainResult chain = new BQueryChainResult(chainName, 
				chainDescription);
		if (cmd.indexOf("-q") < 0) {
			List<BQuery> queries = BQuery.LoadAllQueries();
			for (int ind = 0; ind < queries.size(); ++ind){
				BooleanQuery bq = new BooleanQuery();
				Query tiQ = tiQp.parse(queries.get(ind).title);
				tiQ.setBoost(tiWei);
				bq.add(tiQ, Occur.SHOULD);
				Query teQ = teQp.parse(queries.get(ind).title);
				teQ.setBoost(teWei);
				bq.add(teQ, Occur.SHOULD);
				Query meQ = meQp.parse(queries.get(ind).title);
				meQ.setBoost(meWei);
				bq.add(meQ, Occur.SHOULD);
				TopDocs ran = indexSearcher.search(bq, relCount);
				BQueryResult result = new BQueryResult(
						queries.get(ind).id, ran, relCount, tiQ);
				chain.queries.add(result);
			}
		}
		else {
			BooleanQuery bq = new BooleanQuery();
			Query tiQ = getQuery(cmd, tiQp);
			tiQ.setBoost(tiWei);
			bq.add(tiQ, Occur.SHOULD);
			Query teQ = getQuery(cmd, teQp);
			teQ.setBoost(teWei);
			bq.add(teQ, Occur.SHOULD);
			Query meQ = getQuery(cmd, meQp);
			meQ.setBoost(meWei);
			bq.add(meQ, Occur.SHOULD);
			TopDocs ran = indexSearcher.search(bq, relCount);
			int qPos = cmd.indexOf("-q") + 1;
			int qNo = Integer.parseInt(cmd.get(qPos));
			BQueryResult result = new BQueryResult(qNo, ran, 
					relCount, tiQ);
			chain.queries.add(result);
		}
		return chain;
	}	
	
}
