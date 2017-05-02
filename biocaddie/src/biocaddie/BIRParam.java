package biocaddie;

import java.util.ArrayList;

public class BIRParam {
	public float TitleWeight;
	public static float[] TitleWeightValues = new float[]
			{0.3f, 0.5f, 0.7f};
//			{0.1f};
	
	public float TextWeight;
	public static float[] TextWeightValues = new float[]
			{0.3f, 0.5f, 0.7f};
//			{0.3f};
	
	public float MetaWeight;
	public static float[] MetaWeightValues = new float[]
			{0.3f, 0.5f, 0.7f};
//			{0.5f};

	public float BM25K1;
	public static float[] BM25K1Values = new float[]
			{0.5f, 1f, 1.5f};
//			{1.8f};

	public float BM25b;
	public static float[] BM25bValues = new float[]
			{0.3f, 0.6f, 0.9f};
//			{0.7f};

	public int TopDocs;
	private static int[] TopDocsValues = new int[]
//			{5, 10, 30};
			{0};

	public int ExtTerms;
	private static int[] ExtTermsValues = new int[] 
//			{5, 10, 30};
			{0};
	
	public float ExtTermsWeight;
	private static float[] ExtTermsWeightValues = new float[]
//			{0.1f, 0.3f, 0.5f};
			{0};

	public int IntTerms;
	private static int[] IntTermsValues = new int[] 
//			{5, 10, 30};
			{0};

	public float IntTermsWeight;
	private static float[] IntTermsWeightValues = new float[]
//			{0.1f, 0.3f, 0.5f};
			{0};

	public boolean ExpansionIs;
	public String ExpansionInfo;
	public static int MaxTopDocs = 50;
	
	public BIRParam(float titleWeight, float textWeight, 
			float metaWeight, float bm25K1, float bm25b, 
			int topDocs, int extTerms, float extTermsWeight, 
			int intTerms, float intTermsWeight) {
		TitleWeight = titleWeight;
		TextWeight = textWeight;
		MetaWeight = metaWeight;
		BM25K1 = bm25K1;
		BM25b = bm25b;
		TopDocs = topDocs;
		ExtTerms = extTerms;
		ExtTermsWeight = extTermsWeight;
		IntTerms = intTerms;
		IntTermsWeight = intTermsWeight;

		if (ExtTerms == 0 && IntTerms == 0) {
			ExpansionIs = false;
			ExpansionInfo = "original";
		}
		else {
			ExpansionIs = true;
			ExpansionInfo = "expanded";
		}
	}

	@Override
	public String toString() {
		return "title: " + TitleWeight + 
				" text: " + TextWeight +
				" meta: " + MetaWeight +
				" k1: " + BM25K1 + 
				" b: " + BM25b +
				" topDoc: " + TopDocs +
				" extTerms: " + ExtTerms +
				" extTerWei: " + ExtTermsWeight +
				" intTerms: " + IntTerms +
				" intTerWei: " + IntTermsWeight;
	}
	
	public String toStringCmd() {
		return "-ti " + TitleWeight +
				" -te " + TextWeight +
				" -me " + MetaWeight;
	}

	public boolean IsEqualInBase(BIRParam param) {
		if (this.TitleWeight == param.TitleWeight &&
				this.TextWeight == param.TextWeight &&
				this.MetaWeight == param.MetaWeight &&
				this.BM25K1 == param.BM25K1 &&
				this.BM25b == param.BM25b)
			return true;
		return false;
	}
	
	public BIRParam Copy() {
		BIRParam result = new BIRParam(
				this.TitleWeight, this.TextWeight, this.MetaWeight, 
				this.BM25K1, this.BM25b, this.TopDocs, 
				this.ExtTerms, this.ExtTermsWeight, this.IntTerms, 
				this.IntTermsWeight);
		return result;
	}
	
	public static ArrayList<BIRParam> GetParams() {
ArrayList<BIRParam> result = new ArrayList<BIRParam>();
for (int title = 0; title < TitleWeightValues.length; ++title) {
for (int text = 0; text < TextWeightValues.length; ++text) {
for (int meta = 0; meta < MetaWeightValues.length; ++meta) {
for (int k1 = 0; k1 < BM25K1Values.length; ++k1) {
for (int b = 0; b < BM25bValues.length; ++b) {
for (int topDocs = 0; topDocs < TopDocsValues.length; ++topDocs) {
for (int extTerms = 0; extTerms < ExtTermsValues.length; ++extTerms) {
for (int extTermsWei = 0; extTermsWei < ExtTermsWeightValues.length; ++extTermsWei) {
for (int intTerms = 0; intTerms < IntTermsValues.length; ++intTerms) {
for (int intTermsWei = 0; intTermsWei < IntTermsWeightValues.length; ++intTermsWei) {
	BIRParam bp = new BIRParam(
			TitleWeightValues[title], 
			TextWeightValues[text], 
			MetaWeightValues[meta], 
			BM25K1Values[k1], 
			BM25bValues[b],
			TopDocsValues[topDocs],
			ExtTermsValues[extTerms],
			ExtTermsWeightValues[extTermsWei],
			IntTermsValues[intTerms],
			IntTermsWeightValues[intTermsWei]);
	result.add(bp);
}}}}}}}}}}
return result;
	}
	
	public static BIRParam Average(ArrayList<BIRParam> params) {
		BIRParam ave = new BIRParam(0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		for (int pInd = 0; pInd < params.size(); ++pInd) {
			ave.TitleWeight += params.get(pInd).TitleWeight;
			ave.TextWeight += params.get(pInd).TextWeight;
			ave.MetaWeight += params.get(pInd).MetaWeight;
			ave.BM25K1 += params.get(pInd).BM25K1;
			ave.BM25b += params.get(pInd).BM25b;
			ave.TopDocs += params.get(pInd).TopDocs;
			ave.ExtTerms += params.get(pInd).ExtTerms;
			ave.ExtTermsWeight += params.get(pInd).ExtTermsWeight;
			ave.IntTerms += params.get(pInd).IntTerms;
			ave.IntTermsWeight += params.get(pInd).IntTermsWeight;
		}
		ave.TitleWeight /= params.size();
		ave.TextWeight /= params.size();
		ave.MetaWeight /= params.size();
		ave.BM25K1 /= params.size();
		ave.BM25b /= params.size();
		ave.TopDocs /= params.size();
		ave.ExtTerms /= params.size();
		ave.ExtTermsWeight /= params.size();
		ave.IntTerms /= params.size();
		ave.IntTermsWeight /= params.size();
		return ave;
	}

	public static BIRParam GetBest(boolean isBase) {
		BIRParam best = null;
		if (isBase) {
//			Yanting Base
//			best = new BParam(0.3f, 0.7f, 0.3f, 1.8f, 0.7f, 0, 0, 0, 0, 0);
//			MAP: 0.5766   NDCG: 0.7045

			best = new BIRParam(0.1f, 0.3f, 0.5f, 1.8f, 0.7f, 0, 0, 0, 0, 0);
//			MAP: 0.1489   NDCG: 0.4936
		}
		else {
//			Yanting
//			best = new BParam(0.1f, 0.5f, 0.1f, 1.4f, 0.9f, 50, 0, 0, 0, 0);
//			MAP: 0.6137   NDCG: 0.7598
//			MAP: 0.9171   NDCG: 0.9477		LTR

//			best = new BParam(0.1f, 0.5f, 0.9f, 1.8f, 0.9f, 30, 0, 0, 0, 0);
//			MAP: 0.2015   NDCG: 0.5563
//			MAP: 0.7412   NDCG: 0.8508		LTR			(MAP: 0.7102   NDCG: 0.8387 with LDA)
//			MAP: 0.1663   NDCG: 0.5321		Yanting				(before lda)
//			MAP: 0.0925   NDCG: 0.4678		Yanting LTR		(before lda)

//			best = new BParam(0.1f, 0.3f, 0.7f, 1.6f, 0.9f, 5, 30, 0.1f, 30, 0.1f);
//			MAP: 0.2357   NDCG: 0.5871
//			MAP: 0.7551   NDCG: 0.8623		LTR			(MAP: 0.6318   NDCG: 0.7945 with LDA)
//			MAP: 0.5883   NDCG: 0.801		Merge LTR			(MAP: 0.5706   NDCG: 0.7944 with LDA)
//																										MAP: 0.5481   NDCG: 0.7875 + tops

			// for journal:
			best = new BIRParam(0.1f, 0.3f, 0.5f, 1.8f, 0.7f, 5, 10, 0.5f, 5, 0.1f); // for journal, selected based on most frequent params using CV
//			NDCG: 0.5746 MAP: 0.2993 P@10: 0.6333
		}
		return best;
	}

}

