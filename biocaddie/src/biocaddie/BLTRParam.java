package biocaddie;

import java.util.ArrayList;

public class BLTRParam {
	public int LeafCount;
	public static int[] LeafCountValues = new int[]
//			{4, 5, 6};
			{10};

	public float Shrinkage;
	public static float[] ShrinkageValues = new float[]
//			{0.005f, 0.01f, 0.05f};
			{0.1f};

	public BLTRParam(int leafCount, float shrinkage) {
		LeafCount = leafCount;
		Shrinkage = shrinkage;
	}

	@Override
	public String toString() {
		return "LeafCount: " + LeafCount + 
				" Shrinkage: " + Shrinkage;
	}
	
	public static ArrayList<BLTRParam> GetParams() {
		ArrayList<BLTRParam> result = new ArrayList<BLTRParam>();
		for (int leaf = 0; leaf < LeafCountValues.length; ++leaf) {
			for (int shrin = 0; shrin < ShrinkageValues.length; ++shrin) {
				BLTRParam bp = new BLTRParam(
						LeafCountValues[leaf], 
						ShrinkageValues[shrin]);
				result.add(bp);
			}
		}
		return result;
	}
	
	public static BLTRParam GetBest() {
		BLTRParam best = new BLTRParam(10, 0.1f);
		
		return best;
	}
	
}
