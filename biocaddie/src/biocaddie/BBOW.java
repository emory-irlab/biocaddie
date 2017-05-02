package biocaddie;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

public class BBOW {
	public HashMap<String, BTerm> Map = new HashMap<>();
	public int Length;
	
	public BBOW() {
		
	}
	
	public BBOW(String text) throws Exception {
		AddText(text);
	}
	
	public void AddText(String text) throws Exception {
		ArrayList<String> list = BIndexCreator.Tokenize(text);
		AddWords(list);
	}
	
	public void AddWords(ArrayList<String> terms) {
		for (int ind = 0; ind < terms.size(); ++ind) {
			String word = terms.get(ind);
		    BTerm term = null;
			if (!Map.containsKey(word)) {
				term = new BTerm(word);
				Map.put(word, term);
			}
			else {
				term = Map.get(word);
			}
			term.TF += 1;
			Length += 1;
		}
	}
	
	public void AddTerm(BTerm term) {
		BTerm bt = null;
		if (Map.containsKey(term.Text)) {
			bt = Map.get(term.Text);
		}
		else {
			bt = new BTerm(term.Text);
			bt.IDF = term.IDF;
			Map.put(bt.Text, bt);
		}
		bt.TF = term.TF;
		Length += term.TF;
	}
	
	public void Add(ArrayList<BTerm> terms) {
		for (int tInd = 0; tInd < terms.size(); ++tInd) {
			AddTerm(terms.get(tInd));
		}
	}
	
	public void Add(Collection<BTerm> terms) {
		Add(new ArrayList<BTerm>(terms));
	}
	
	public ArrayList<BTerm> getTerms() {
		ArrayList<BTerm> temp = new ArrayList<>(Map.values());
		return temp;
	}
	
	public ArrayList<String> getWords() {
		ArrayList<BTerm> temp = new ArrayList<>(Map.values());
		ArrayList<String> result = new ArrayList<>();
		for (int tInd = 0; tInd < temp.size(); ++tInd) {
			result.add(temp.get(tInd).Text);
		}
		return result;
	}
	
	public BTerm Get(String word) {
		BTerm result = null;
		if (Map.containsKey(word)) {
			result = Map.get(word).Copy();
		}
		return result;
	}
	
	public void Remove(String tokenizedWord) {
		if (Map.containsKey(tokenizedWord)) {
			BTerm tt = Map.get(tokenizedWord);
			Map.remove(tokenizedWord);
			Length -= tt.TF;
		}
	}
	
	public void RemoveText(String text) throws Exception {
		ArrayList<String> ts = BIndexCreator.Tokenize(text);
		for (int tInd = 0; tInd < ts.size(); ++tInd) {
			Remove(ts.get(tInd));
		}
	}
	
	@Override
	public String toString() {
		ArrayList<BTerm> temp = new ArrayList<>(Map.values());
		List<BTerm> list = BTerm.SortByTF(temp, false);
		StringBuilder sb = new StringBuilder();
		sb.append("Len:" + Length);
		for (int tInd = 0; tInd < 10 && tInd < list.size(); ++tInd) {
			sb.append("\n" + list.get(tInd).Text + 
					" [" + list.get(tInd).TF + "]");
		}
		return sb.toString();
	}
	
	public ArrayList<BTerm> FindShared(ArrayList<BTerm> terms) {
		ArrayList<BTerm> result = new ArrayList<>();
		for (int tInd = 0; tInd < terms.size(); ++tInd) {
			if (Map.containsKey(terms.get(tInd).Text))
				result.add(terms.get(tInd));
		}
		return result;
	}

	public void Clear() {
		Map.clear();
		Length = 0;
	}
	
}
