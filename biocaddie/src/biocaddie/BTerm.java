package biocaddie;

import java.util.ArrayList;
import java.util.List;

public class BTerm {
	public String Text;
	public String Meta;
	public float TF, IDF, DocFreq, CollectionProbab;
	public double Weight;
	
	public BTerm(String text){
		Text = text;
	}

	@Override
	public String toString() {
		return Text + "> " +
				" TF:" + TF +
				" W:" + Weight;
	}
	
	public BTerm Copy() {
		BTerm result = new BTerm(this.Text);
		result.Meta = this.Meta;
		result.TF = this.TF;
		result.IDF = this.IDF;
		result.DocFreq = this.DocFreq;
		result.CollectionProbab = this.CollectionProbab;
		result.Weight = this.Weight;
		return result;
	}
	
	public static ArrayList<BTerm> SortByWeight(List<BTerm> list,
			boolean asc) {
		List<BTerm> cList = new ArrayList<BTerm>(list);
		ArrayList<BTerm> sorted = new ArrayList<>();
		while (cList.size() > 0) {
			int tarInd = 0;
			for (int ind = 1; ind < cList.size(); ++ind) {
				if ((asc && cList.get(ind).Weight < cList.get(tarInd).Weight) ||
						(!asc &&cList.get(ind).Weight > cList.get(tarInd).Weight))
					tarInd = ind;
			}
			sorted.add(cList.get(tarInd));
			cList.remove(tarInd);
		}
		return sorted;
	}
	
	public static ArrayList<BTerm> SortByTF(List<BTerm> list, 
			boolean asc) {
		List<BTerm> cList = new ArrayList<BTerm>(list);
		ArrayList<BTerm> sorted = new ArrayList<>();
		while (cList.size() > 0) {
			int tarInd = 0;
			for (int ind = 1; ind < cList.size(); ++ind) {
				boolean ascStat = asc && (cList.get(ind).TF < cList.get(tarInd).TF || 
						(cList.get(ind).TF == cList.get(tarInd).TF &&
						cList.get(tarInd).Text.compareTo(cList.get(ind).Text) > 0 ));
				boolean descStat = !asc && (cList.get(ind).TF > cList.get(tarInd).TF ||
						(cList.get(ind).TF == cList.get(tarInd).TF &&
						cList.get(tarInd).Text.compareTo(cList.get(ind).Text) < 0 ));
				if (ascStat || descStat)
					tarInd = ind;
			}
			sorted.add(cList.get(tarInd));
			cList.remove(tarInd);
		}
		return sorted;
	}
	
	public static void SetTFIDF(List<BTerm> list) {
		for (int ind  = 0; ind < list.size(); ++ind) {
			BTerm bt = list.get(ind);
			bt.Weight = bt.TF * bt.IDF;
		}
	}
	
}
