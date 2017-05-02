package biocaddie;

public final class BGlobalVar {

	public static String BasePath() {
		return "/mnt/01D266CBD57B0850/Research/";
	}
	
    public static String TrecEvalFile = BasePath() + "Shared/tools/trec_eval.9.0/trec_eval";
    public static String RankLibFile = BasePath() + "Shared/tools/ranklib/RankLib-2.1-patched.jar";
    public static String Project = BasePath() + "biocaddie/";
    public static String Dataset = Project + "new data/update_xml_folder/";
    public static String StopWords = Project + "StopWords.txt";
    public static String CommonWords = Project + "CommonWords.txt"; 
    public static String MedicalWords = Project + "MedicalWords.txt"; 
    public static String Judgments = Project + "Judgments.txt";
    public static String Queries = Project + "query.txt";
    public static String DocumentDOCID = Project + "DocumentDOCID.txt";
    public static String Index = Project + "index/";
    public static String Results = Project + "result/";
    public static String URLs = Project + "URLs.txt";
    public static String KEGG = Project + "KEGG.py";
    public static String KEGGSaved = Project + "KEGGSaved.txt";
    public static String HGNCSaved = Project + "HGNCSaved.txt";
    public static String WebSaved = Project + "WebSaved.txt";
    public static String Meta = Project + "meta.txt";
    public static String Domains = Project + "domains.txt";
    public static String LDAModel = Project + "LDAmodel.bin";
    public static String LDAInstances = Project + "LDAInstances.bin";
}

//public final class BGlobalVar {
//
//	public static String BasePath(){
////		return "/home/payam/biocaddie/";
//		return "/home/payam/bio2/";
//	}
//	
//	public static String Corpus = BasePath();
//	public static String TrecEvalFile = Corpus + "trec_eval";
//	public static String RankLibFile = Corpus + "RankLib-2.1-patched.jar";
//	public static String Dataset = Corpus + "update_xml_folder/";
//	public static String CommonWords = Corpus + "CommonWords.txt"; 
//	public static String MedicalWords = Corpus + "MedicalWords.txt"; 
//	public static String Judgments = Corpus + "Judgments.txt";
//	public static String Queries = Corpus + "query.txt";
//	public static String DocumentDOCID = Corpus + "DocumentDOCID.txt";
//	public static String Index = Corpus + "index/";
//	public static String Results = Corpus + "result/";
//	public static String URLs = Corpus + "URLs.txt";
//	public static String KEGG = Corpus + "KEGG.py";
//	public static String KEGGSaved = Corpus + "KEGGSaved.txt";
//	public static String HGNCSaved = Corpus + "HGNCSaved.txt";
//	public static String WebSaved = Corpus + "WebSaved.txt";
//	public static String Meta = Corpus + "meta.txt";
//	public static String Domains = Corpus + "domains.txt";
//	public static String LDAModel = Corpus + "LDAmodel.bin";
//	public static String LDAInstances = Corpus + "LDAInstances.bin";
//}
