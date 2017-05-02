package biocaddie;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;


import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TermStatistics;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import biocaddie.BGlobalVar;;

public class BIndexCreator {
	public static Analyzer analyzer;
	public static HashSet<String> stopWords = new HashSet<>();
	private static HashSet<String> commonWords = new HashSet<>();
	private static HashSet<String> medicalWords = new HashSet<>();
	private static Hashtable<String, String> urls = new Hashtable<>();
	private static Hashtable<String, Integer> domains = new Hashtable<>();
	private static Hashtable<String, Long> cFreqs = new Hashtable<>();
	
	static {
		analyzer = new org.apache.lucene.analysis.en.EnglishAnalyzer(
				Version.LUCENE_48);
		try {
			urls = loadURLs();
			BufferedReader br = new BufferedReader(new FileReader(
					BGlobalVar.CommonWords));
			String line = null;
			int count = 2000;
			int ind = 0;
			while (ind < count && (line = br.readLine()) != null) {
				String temp = Normalize(line.toLowerCase());
				if (!commonWords.contains(temp))
					commonWords.add(temp);
				++ind;
			}
			br.close();
			
			br = new BufferedReader(new FileReader(
					BGlobalVar.StopWords));
			line = null;
			while ((line = br.readLine()) != null) {
				String temp = Normalize(line.toLowerCase());
				if (!stopWords.contains(temp))
					stopWords.add(temp);
			}
			br.close();
			
			br = new BufferedReader(new FileReader(
					BGlobalVar.MedicalWords));
			line = null;
			while ((line = br.readLine()) != null) {
				String temp = Normalize(line.toLowerCase());
				if (!medicalWords.contains(temp))
					medicalWords.add(temp);
			}
			br.close();
			
			br = new BufferedReader(new FileReader(
					BGlobalVar.Domains));
			line = null;
			while ((line = br.readLine()) != null) {
				String[] tokens = line.split("\t");
				domains.put(tokens[0], Integer.parseInt(tokens[1]));
			}
			br.close();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	Hashtable<String, String> metas = new Hashtable<>();
	
	public void Create(String dataPath, Similarity similarity) 
			throws Exception {
		List<String> ms = Files.readAllLines(Paths.get(
				BGlobalVar.Meta), StandardCharsets.UTF_8);
		for (int lInd = 0; lInd < ms.size(); lInd += 2) {
			metas.put(ms.get(lInd), ms.get(lInd + 1));
		}

		IndexWriterConfig config = new IndexWriterConfig(
				Version.LUCENE_48, BIndexCreator.analyzer);
		config.setSimilarity(similarity);
		IndexWriter indexWriter = new IndexWriter(FSDirectory.open(
				new File("index")), config);
		indexWriter.deleteAll();
		File dir = new File(BGlobalVar.Dataset);
		File[] fileList = dir.listFiles();
		int docIndexedCount = 0;
		int docFailedCount = 0;
		for (int fInd = 0; fInd < fileList.length; ++fInd) {
			if (!getFileExtention(fileList[fInd]).equals(".xml")) 
				continue;
			List<String> lines = Files.readAllLines(
					fileList[fInd].toPath(), StandardCharsets.UTF_8);
			org.apache.lucene.document.Document doc = getDoc(lines);
			if (doc != null) {
				indexWriter.addDocument(doc);
				++docIndexedCount;
			}
			else {
				++docFailedCount;
			}
			if (docIndexedCount % 10000 == 0) {
				BLib.println(docIndexedCount + " Indexed, " + 
						docFailedCount + " Failed.");
			}				
		}
		BLib.println(docIndexedCount + " Indexed, " + 
				docFailedCount + " Failed.");
		indexWriter.commit();
		indexWriter.close();
		createDocumentDOCID();
		BLib.println("index created.");
	}

	String getFileExtention(File file){
		int dotPos = file.getName().lastIndexOf(".");
		String ext =  file.getName().substring(dotPos);
		return ext;
	}
	
	int getLineIndexByStart(List<String> lines, String start) {
		String line = null;
		int lInd = 0;
		try {
			for (lInd = 0; lInd < lines.size(); ++lInd) {
				line = lines.get(lInd).trim();
				if (line.length() >= start.length()) {
					line = line.substring(0, start.length());
					if (line.equals(start)) {
						return lInd;
					}
				}
			}
		}
		catch (Exception err) {
		}
		return -1;
	}
	
	String extractTextFromLine(String line, String startTag, String endTag) {
		String nl = line.trim();
		String result = nl.substring(startTag.length(), 
				nl.length() - endTag.length());
		return result;
	}
	
	String extractTextByLineTag(List<String> lines, String startTag, 
			String endTag) {
		int lInd = getLineIndexByStart(lines, startTag);
		if (lInd >= 0) {
			return extractTextFromLine(lines.get(lInd), startTag, endTag);
		}
		return null;
	}
	
	String abstractLine(String line) {
		StringBuilder result = new StringBuilder();
		int ind = 0;
		while (ind < line.length()) {
			if (line.charAt(ind) == '"') {
				int colPos = line.indexOf("\":", ind + 1);
				if (colPos >= 0) {
					int itr = ind + 1;
					while (itr < colPos &&
							(Character.isLetter(line.charAt(itr)) ||
									line.charAt(itr) == '_')) {
						++itr;
					}
					if (itr == colPos) {
						ind = colPos + 2;
						continue;
					}
				}
			}
			if (Character.isLetter(line.charAt(ind)) ||
					Character.isDigit(line.charAt(ind)) ||
					line.charAt(ind) == '-' ||
					line.charAt(ind) == '_') {
				result.append(line.charAt(ind));
			}
			else {
				result.append(" ");
			}
			++ind;
		}
		return result.toString();
	}
	
	Document getDoc(List<String> lines) 
			throws Exception {
		String DOCNO = extractTextByLineTag(lines, "<DOCNO>", "</DOCNO>");
		String TITLE = extractTextByLineTag(lines, "<TITLE>", "</TITLE>");
		String REPOSITORY = extractTextByLineTag(lines, "<REPOSITORY>", "</REPOSITORY>");
		String METADATA = extractTextByLineTag(lines, "<METADATA>", "</METADATA>");
		String TEXT = abstractLine(METADATA);
		org.apache.lucene.document.Document luceneDoc = 
				new org.apache.lucene.document.Document();
		
		FieldType luceneFieldType = new FieldType();
		luceneFieldType.setStored(true);
		Field luceneField = new Field("DOCID", DOCNO, luceneFieldType);
		luceneDoc.add(luceneField);
		
		luceneFieldType = new FieldType();
		luceneFieldType.setIndexed(true);
		luceneFieldType.setStored(true);
		luceneFieldType.setTokenized(true);
		luceneFieldType.setStoreTermVectors(true);
		luceneField = new Field("TITLE", TITLE, luceneFieldType);
		luceneDoc.add(luceneField);
		
		luceneFieldType = new FieldType();
		luceneFieldType.setIndexed(true);
		luceneFieldType.setStored(true);
		luceneFieldType.setTokenized(true);
		luceneFieldType.setStoreTermVectors(true);
		luceneField = new Field("TEXT", TEXT, luceneFieldType);
		luceneDoc.add(luceneField);

		luceneFieldType = new FieldType();
		luceneFieldType.setStored(true);
		luceneField = new Field("REPOSITORY", REPOSITORY, luceneFieldType);
		luceneDoc.add(luceneField);

		luceneFieldType = new FieldType();
		luceneFieldType.setStored(true);
		luceneField = new Field("BODY", METADATA, luceneFieldType);
		luceneDoc.add(luceneField);
		
		String url = urls.get(DOCNO);
		String domain = getDomain(url);
		String META = "";
		if (metas.containsKey(domain)) {
			META = metas.get(domain);
		}
		luceneFieldType = new FieldType();
		luceneFieldType.setIndexed(true);
		luceneFieldType.setStored(true);
		luceneFieldType.setTokenized(true);
		luceneFieldType.setStoreTermVectors(true);
		luceneField = new Field("META", META, luceneFieldType);
		luceneDoc.add(luceneField);
		
		return luceneDoc;
	}
	
	void createDocumentDOCID() throws Exception {
		IndexReader indexReader = DirectoryReader.open(
				FSDirectory.open(new File("index")));
		StringBuilder result = new StringBuilder();
		for (int i = 0; i < indexReader.maxDoc(); ++i) {
			result.append(indexReader.document(i).get("DOCID") + "\n");
		}
		indexReader.close();
		PrintStream ps = new PrintStream("DocumentDOCID.txt");
		ps.print(result.toString());
		ps.close();
	}
	
	public static String getDomain(String url) {
		if (url == null || url.equals("") || url.equals("None"))
			return url;
		int st = url.indexOf("://");
		int en = url.indexOf("/", st + 3);
		String domain = url.substring(st + 3, en);
		domain = domain.startsWith("www.") ? domain.substring(4) : domain;
		return domain;
	}
	
	private static Hashtable<String, String> loadURLs() 
			throws Exception {
		Hashtable<String, String> result = 
				new Hashtable<String, String>();
		BufferedReader br = new BufferedReader(new FileReader(
				BGlobalVar.URLs));
		List<String> lines = new ArrayList<>();
		String line = null;
		while ((line = br.readLine()) != null) {
			lines.add(line);
		}
		br.close();
		for (int lInd = 0; lInd < lines.size(); ++lInd) {
			String[] terms = lines.get(lInd).split("\t");
			if (terms.length == 2)
				result.put(terms[0], terms[1]);
			else
				result.put(terms[0], "None");
		}
		return result;
	}
	
	public static ArrayList<String> Tokenize(String text) 
			throws Exception {
		TokenStream tokenStream = BIndexCreator.analyzer.tokenStream(
				"TEXT", text);
		CharTermAttribute charTermAttribute = tokenStream.addAttribute(
				CharTermAttribute.class);
//		OffsetAttribute offsetAttribute = tokenStream.addAttribute(
//				OffsetAttribute.class);
		tokenStream.reset();
		ArrayList<String> result = new ArrayList<>();
		while (tokenStream.incrementToken()) {
//		    int startOffset = offsetAttribute.startOffset();
//		    int endOffset = offsetAttribute.endOffset();
		    String word = charTermAttribute.toString();
		    result.add(word);
		}
		tokenStream.close();
		return result;
	}
	
	public static String Normalize(String word) throws Exception {
		ArrayList<String> list = Tokenize(word);
		if (list.size() > 0)
			return list.get(0);
		return word;
	}
	
	public static boolean IsCommonWord(String word) throws Exception {
		String temp = Normalize(word);
		if (temp == "" || temp == null)
			return true;
		if (!medicalWords.contains(temp))
			return commonWords.contains(temp);
		return false;
	}
	
	public static String GetURL(String DOCID) throws Exception {
		return urls.get(DOCID);
	}
	
	public static int GetDomainCount(String url) {
		String domain = getDomain(url);
		return domains.get(domain);
	}
	
	public static ArrayList<String> GetDomainList() {
		ArrayList<String> result = new ArrayList<>(domains.keySet());
		return result;
	}
	
	public static void CreateYantingFiles() throws Exception {
		File dir = new File("/home/paya/Desktop/Link to research/"
				+ "biocaddie/Yanting/feed");
		File[] fileList = dir.listFiles();
		StringBuilder qs = new StringBuilder();
		StringBuilder judge = new StringBuilder();
		qs.append("<list>\n\n");
		for (int fInd = 0; fInd < fileList.length; ++fInd) {
			BLib.println(fileList[fInd].getPath());
			List<String> ms = Files.readAllLines(Paths.get(
					fileList[fInd].getPath()), StandardCharsets.UTF_8);
			qs.append("<QUERY>\n");
			qs.append("<ID>" + (fInd + 1) + "</ID>\n");
			qs.append("<TITLE>" + ms.get(0) + "</TITLE>\n");
			qs.append("<DESCRIPTION></DESCRIPTION>\n<NARRATIVE></NARRATIVE>\n");
			qs.append("</QUERY>\n\n");
			for (int lInd = 2; lInd < ms.size(); ++lInd) {
				String[] terms = ms.get(lInd).split(" ");
				if (!terms[1].equals("0")) {
					judge.append(String.format("%03d", fInd + 1) + 
							" 0 " + terms[0] + " " + terms[1] + "\n");
				}
			}
		}
		qs.append("</list>");
		PrintStream ps = new PrintStream(BGlobalVar.Results + "qqq.txt");
		ps.print(qs.toString());
		ps.close();
		ps = new PrintStream(BGlobalVar.Results + "jjj.txt");
		ps.print(judge.toString());
		ps.close();
	}
	
	public static long GetFrequency(String word) throws Exception {
		String temp = Normalize(word);
		if (temp == "" || temp == null)
			return 0;
		if (cFreqs.containsKey(temp))
			return cFreqs.get(temp);
		Term lTerm = new Term("TEXT", temp);
		IndexSearcher is = BStandardMethods.indexSearcher;
		TermStatistics tStat = is.termStatistics(lTerm, 
				TermContext.build(is.getTopReaderContext(), lTerm));
		long cFreq = tStat.totalTermFreq();
		cFreqs.put(temp, cFreq);
		return cFreq;
	}
	
	public static ArrayList<String> RemoveCommonWords(List<String> list) 
			throws Exception {
		ArrayList<String> result = new ArrayList<>();
		for (int ind = 0; ind < list.size(); ++ind) {
			if (!IsCommonWord(list.get(ind))) {
				result.add(list.get(ind));
			}
		}
		return result;
	}

	public static String GetString(ArrayList<String> words) {
		StringBuilder result = new StringBuilder();
		for (int ind = 0; ind < words.size(); ++ind) {
			result.append(words.get(ind) + " ");
		}
		return result.toString();
	}
	
}
