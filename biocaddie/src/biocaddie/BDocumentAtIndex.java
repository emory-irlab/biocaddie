package biocaddie;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.util.BytesRef;

public class BDocumentAtIndex {
	public int idAtIndex;
	public float scoreAtRetrieve;
	public HashMap<String, BTerm> termVector = new HashMap<String, BTerm>();
	public float weight;
	public Boolean isRelevant = false;
	public int length;
	public Object tag;
	
	public String DOCID;
	public String TITLE;
	public String TEXT;
	public String META;
	public String REPOSITORY;
	public String URL;
	
	public BDocumentAtIndex(int index){
		idAtIndex = index;
	}
	
	public void FetchTermVector(String field, 
			boolean reset) throws Exception{
		if (reset) {
			termVector.clear();
			length = 0;
		}
		Terms terms = BStandardMethods.indexReader.getTermVector(
				idAtIndex, field);
		if (terms != null) {
			TermsEnum iterator = terms.iterator(null);
			BytesRef br;
			while ((br = iterator.next()) != null){
				BTerm term = null;
				if (termVector.containsKey(br.utf8ToString())) {
					term = termVector.get(br.utf8ToString());
				}
				else {
					term = new BTerm(br.utf8ToString());
					term.IDF = BStandardMethods.defaultSim.idf(
							BStandardMethods.indexReader.docFreq(
									new Term(field, term.Text)),
							BStandardMethods.indexReader.maxDoc());
					termVector.put(term.Text, term);
				}
				term.TF += iterator.totalTermFreq();
				length += term.TF;
			}
		}
	}
	
	public void FetchFields() throws Exception {
		DOCID = BStandardMethods.indexReader.document(idAtIndex).
				get("DOCID");
		TITLE = BStandardMethods.indexReader.document(idAtIndex).
				get("TITLE");
		TEXT = BStandardMethods.indexReader.document(idAtIndex).
				get("TEXT");
		META = BStandardMethods.indexReader.document(idAtIndex).
				get("META");
		REPOSITORY = BStandardMethods.indexReader.document(idAtIndex).
				get("REPOSITORY");
		URL = BIndexCreator.GetURL(DOCID);
	}
	
	public void FetchDOCIDAndURL() throws Exception {
		DOCID = BStandardMethods.indexReader.document(idAtIndex).
				get("DOCID");
		URL = BIndexCreator.GetURL(DOCID);
	}
	
	public void FetchTITLEAndTEXT() throws Exception {
		TITLE = BStandardMethods.indexReader.document(idAtIndex).
				get("TITLE");
		TEXT = BStandardMethods.indexReader.document(idAtIndex).
				get("TEXT");
	}

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();
		for (BTerm term : termVector.values()) {
			result.append(term.toString() + " /");
		}
		return result.toString();
	}

	public void clear() {
		termVector.clear();
		length = 0;
		DOCID = TITLE = TEXT = META = REPOSITORY = URL = "";
	}
	
	public static void RemoveTags(List<BDocumentAtIndex> list) {
		for (BDocumentAtIndex q : list) {
			q.tag = null;
		}
	}

	public static void RemoveFieldsExceptDOCID(
			List<BDocumentAtIndex> list) {
		for (int lInd = 0; lInd < list.size(); ++lInd) {
			list.get(lInd).TITLE = null;
			list.get(lInd).TEXT = null;
			list.get(lInd).META = null;
			list.get(lInd).REPOSITORY = null;
			list.get(lInd).URL = null;
		}
	}
	
	public static ArrayList<BDocumentAtIndex> SortByScore(
			List<BDocumentAtIndex> list, boolean asc) {
		List<BDocumentAtIndex> cList = 
				new ArrayList<BDocumentAtIndex>(list);
		ArrayList<BDocumentAtIndex> sorted = new ArrayList<>();
		while (cList.size() > 0) {
			int tarInd = 0;
			for (int ind = 1; ind < cList.size(); ++ind) {
				if ((asc && cList.get(ind).scoreAtRetrieve < 
						cList.get(tarInd).scoreAtRetrieve) ||
						(!asc && cList.get(ind).scoreAtRetrieve > 
						cList.get(tarInd).scoreAtRetrieve))
					tarInd = ind;
			}
			sorted.add(cList.get(tarInd));
			cList.remove(tarInd);
		}
		return sorted;
	}
	
	public static ArrayList<BDocumentAtIndex> Remove(
			ArrayList<BDocumentAtIndex> list, int start, int end) {
		ArrayList<BDocumentAtIndex> result = new ArrayList<>();
		int count = end - start;
		for (int ind = 0; ind < count; ++ind) {
			result.add(list.get(start));
			list.remove(start);
		}
		return result;
	}

}
