package biocaddie;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;

import weka.clusterers.SimpleKMeans;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;

public class BWeka {
	
	public BWeka() throws Exception {
	}
	
	public void ClassifyIndex() throws Exception {
		ArrayList<BDocumentAtIndex> docs = new ArrayList<>();
		for (int dInd = 0; dInd < BStandardMethods.DocCount; ++dInd) {
			BDocumentAtIndex d = new BDocumentAtIndex(dInd);
			d.FetchDOCIDAndURL();
			docs.add(d);
			if (dInd % 50000 == 0) {
				BLib.println("url loaded " + dInd);
			}
		}
		ArrayList<String> domains = BIndexCreator.GetDomainList();
		for (int bInd = 0; bInd < domains.size(); ++bInd) {
			BLib.println(bInd + "> " + domains.get(bInd) + " started");
			classifyDomain(domains.get(bInd), docs);
		}
	}
	
	private BBOW extractTerms(String domain, 
			ArrayList<BDocumentAtIndex> allDocs,
			ArrayList<BDocumentAtIndex> docs) throws Exception {
		BBOW bow = new BBOW();
		for (int aInd = 0; aInd < allDocs.size(); ++aInd) {
			BDocumentAtIndex dTemp = allDocs.get(aInd);
			String dd = BIndexCreator.getDomain(dTemp.URL);
			if (domain.equals(dd)) {
				docs.add(dTemp);
				BBOW bTemp = new BBOW();
				dTemp.tag = bTemp;
				dTemp.FetchTITLEAndTEXT();
				bTemp.AddText(dTemp.TITLE);
				bTemp.AddText(dTemp.TEXT);
				bow.Add(bTemp.getTerms());
			}
		}
		return bow;
	}
	
	private FastVector getFeatures(BBOW bow, 
			HashMap<String, Integer> featInds) throws Exception {
		FastVector result = new FastVector();
		ArrayList<BTerm> terms = bow.getTerms();
		terms = BTerm.SortByTF(terms, false);
		for (int ind = 0; ind < terms.size() && terms.get(ind).TF > 1; 
				++ind){
			String t = terms.get(ind).Text;
			if (!BIndexCreator.IsCommonWord(t)) {
				result.addElement(new Attribute(t));
				featInds.put(t, result.size() - 1);
			}
		}
		return result;
	}
	
	private void loadData(ArrayList<BDocumentAtIndex> docs, 
			Instances data, FastVector feats, 
			HashMap<String, Integer> featInd) {
		for (int dInd = 0; dInd < docs.size(); ++dInd){
			BBOW bow = (BBOW)docs.get(dInd).tag;
			ArrayList<BTerm> terms = bow.getTerms();
			double[] values = new double[data.numAttributes()];
			for (int tInd = 0; tInd < terms.size(); ++tInd){
				BTerm t = terms.get(tInd);
				if (featInd.containsKey(t.Text)) {
					int ind = featInd.get(t.Text);
					values[ind] = t.TF;
				}
			}
			data.add(new Instance(1, values));
		}
	}
	
	private void saveData(String domain, Instances data, 
			ArrayList<BDocumentAtIndex> docs, SimpleKMeans clus) 
					throws Exception {
		StringBuilder feats = new StringBuilder();
		for (int fInd = 0; fInd < data.numAttributes(); ++fInd) {
			feats.append(data.attribute(fInd).name() + "\n");
		}
		PrintStream ps = new PrintStream(BGlobalVar.Results + 
				domain + "-feats.txt");
		ps.print(feats.toString());
		ps.close();
		
		StringBuilder results = new StringBuilder();
		for (int tInd = 0; tInd < clus.getAssignments().length; ++tInd) {
			results.append(docs.get(tInd).DOCID + "\t" + 
					clus.getAssignments()[tInd] + "\n");
		}
		ps = new PrintStream(BGlobalVar.Results + 
				domain + "-results.txt");
		ps.print(results.toString());
		ps.close();

		StringBuilder centroids = new StringBuilder();
		for (int cInd = 0; cInd < clus.getClusterSizes().length; 
				++cInd) {
			centroids.append(clus.getClusterSizes()[cInd] + "\t" + 
					clus.getClusterCentroids().instance(cInd).
					toString() + "\n");
		}
		ps = new PrintStream(BGlobalVar.Results + 
				domain + "-centroids.txt");
		ps.print(centroids.toString());
		ps.close();
	}
	
	private void classifyDomain(String domain, 
			ArrayList<BDocumentAtIndex> allDocs) throws Exception {
		ArrayList<BDocumentAtIndex> docs = new ArrayList<>();
		BBOW bow = extractTerms(domain, allDocs, docs);
		BLib.println("feats extracted");
		HashMap<String, Integer> featInds = new HashMap<>();
		FastVector feats = getFeatures(bow, featInds);
		Instances data = new Instances("mem", feats, 0);
		loadData(docs, data, feats, featInds);
		BLib.println("data loaded");
		SimpleKMeans clus = new SimpleKMeans();
		long approxClusterSize = (long)Math.ceil(docs.size() / 20.0);
		long itrCount = (long)Math.ceil(docs.size() / 20.0);
		clus.setOptions(weka.core.Utils.splitOptions(
				"-N " + approxClusterSize + 
				" -I " + itrCount + " -O"));
		clus.buildClusterer(data);
		saveData(domain, data, docs, clus);
		BDocumentAtIndex.RemoveTags(docs);
		BDocumentAtIndex.RemoveFieldsExceptDOCID(docs);
	}
	
}
