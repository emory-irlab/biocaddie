package biocaddie;

import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class BQuery {
	public int id;
	public String title;
	public Object tag;
	public Object ltrTag;
	
	public BQuery(int queryID, String queryTitle) {
		id = queryID;
		title = queryTitle;
	}
	
	public static int QueryCount() throws Exception{
		DocumentBuilder xmlBuilder = DocumentBuilderFactory.
				newInstance().newDocumentBuilder();
		Document fileStructure = xmlBuilder.parse(BGlobalVar.Queries);
		NodeList nodeList = fileStructure.getElementsByTagName("QUERY");
		return nodeList.getLength();
	}
	
	public static List<BQuery> LoadQueriesExcludeRange(int startIndex, 
			int exclusiveEndIndex) throws Exception{
		List<BQuery> result = new ArrayList<BQuery>();
		DocumentBuilder xmlBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		Document fileStructure = xmlBuilder.parse(BGlobalVar.Queries);
		NodeList nodeList = fileStructure.getElementsByTagName("QUERY");
		Element xmlQuery;
		for (int docIndex = 0; docIndex < nodeList.getLength(); ++docIndex){
			if (!(startIndex <= docIndex && docIndex < exclusiveEndIndex)){
				xmlQuery = (Element)nodeList.item(docIndex);
				result.add(new BQuery(Integer.parseInt(
						xmlQuery.getElementsByTagName("ID").item(0).
						getTextContent()),
						xmlQuery.getElementsByTagName("TITLE").item(0).
						getTextContent()));
			}
		}
		return result;
	}
	
	public static List<BQuery> LoadQueriesIncludeRange(int startIndex, 
			int exclusiveEndIndex) throws Exception{
		List<BQuery> result = new ArrayList<BQuery>();
		DocumentBuilder xmlBuilder = DocumentBuilderFactory.newInstance().
				newDocumentBuilder();
		Document fileStructure = xmlBuilder.parse(BGlobalVar.Queries);
		NodeList nodeList = fileStructure.getElementsByTagName("QUERY");
		Element xmlQuery;
		for (int docIndex = startIndex; docIndex < exclusiveEndIndex; 
				++docIndex){
			xmlQuery = (Element)nodeList.item(docIndex);
			result.add(new BQuery(Integer.parseInt(
					xmlQuery.getElementsByTagName("ID").item(0).
					getTextContent()),
					xmlQuery.getElementsByTagName("TITLE").item(0).
					getTextContent()));
		}
		return result;
	}
	
	public static List<BQuery> LoadAllQueries() throws Exception {
		List<BQuery> result = new ArrayList<BQuery>();
		DocumentBuilder xmlBuilder = DocumentBuilderFactory.newInstance().
				newDocumentBuilder();
		Document fileStructure = xmlBuilder.parse(BGlobalVar.Queries);
		NodeList nodeList = fileStructure.getElementsByTagName("QUERY");
		Element xmlQuery;
		for (int docIndex = 0; docIndex < nodeList.getLength(); 
				++docIndex){
			xmlQuery = (Element)nodeList.item(docIndex);
			result.add(new BQuery(Integer.parseInt(
					xmlQuery.getElementsByTagName("ID").item(0).
					getTextContent()),
					xmlQuery.getElementsByTagName("TITLE").item(0).
					getTextContent()));
		}
		return result;
	}
	
	public static List<BQuery> LoadQueriesFromFile(String filePath) 
			throws Exception {
		List<BQuery> result = new ArrayList<BQuery>();
		List<String> lines = Files.readAllLines(Paths.get(filePath), 
				Charset.forName("UTF-8"));
		for (int lInd = 0; lInd < lines.size(); ++lInd) {
			String[] terms = lines.get(lInd).split("\t");
			int qid = Integer.parseInt(terms[0]);
			String title = terms[1].replace("(", "").replace(")", "");
			result.add(new BQuery(qid, title));
		}
		return result;
	}
	
	public static void RemoveTags(List<BQuery> list) {
		for (BQuery q : list) {
			q.tag = null;
		}
	}

	@Override
	public String toString() {
		return id + "> " + title;
	}
	
}
