package biocaddie;

import java.util.ArrayList;
import java.util.HashMap;

import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.util.BytesRef;

public class BQueryResult {
	public ArrayList<BDocumentAtIndex> results = 
			new ArrayList<BDocumentAtIndex>();
	public BTrecEvalResult trec;
	public int QueryNo;
	public Query query;
	
	public BQueryResult(int queryNo, TopDocs queryResult, 
			int topCount, Query execQuery) {
		QueryNo = queryNo;
		for (int i = 0; i < topCount && i < queryResult.scoreDocs.length; 
				++i) {
			BDocumentAtIndex retDoc = new BDocumentAtIndex(
					queryResult.scoreDocs[i].doc);
			retDoc.scoreAtRetrieve = queryResult.scoreDocs[i].score;
			results.add(retDoc);
			query = execQuery;
		}
	}
	
	private int loadTopDoc(BDocumentAtIndex doc, 
			HashMap<String, BTerm> map) throws Exception {
		int docLength = 0;
		String[] fields = new String[] {"TEXT", "TITLE", "META"};
		for (int fInd = 0; fInd < fields.length; ++fInd) {
			Terms terms = BStandardMethods.indexReader.getTermVector(
					doc.idAtIndex, fields[fInd]);
			if (terms != null) {
				TermsEnum iterator = terms.iterator(null);
				BytesRef br;
				while ((br = iterator.next()) != null){
					String word = br.utf8ToString();
					BTerm term = null;
					if (!map.containsKey(word)) {
						term = new BTerm(word);
						map.put(word, term);
					}
					else {
						term = map.get(word);
					}
					term.TF += iterator.totalTermFreq();
					docLength += iterator.totalTermFreq();
				}
			}
		}
		return docLength;
	}
	
	public BBOW LoadTopDocs(int startInd, int endInd) 
			throws Exception {
		BBOW result = new BBOW();
		for (int ind = startInd; ind < endInd && ind < results.size(); 
				++ind) {
			result.Length += loadTopDoc(results.get(ind), result.Map);
		}
		return result;
	}
	
}
