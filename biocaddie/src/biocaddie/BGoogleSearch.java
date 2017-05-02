package biocaddie;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class BGoogleSearch {
	
	public static ArrayList<BGResult> Search(String query, String optionalDomain) 
			throws Exception {
		ArrayList<BGResult> result = new ArrayList<>();
		String charset = "UTF-8";
		String encSearch = URLEncoder.encode(query, charset);
		String google = "https://www.google.com/search?hl=en&as_q=" + encSearch;
		if (optionalDomain != null) {
			google += "&as_sitesearch=" + optionalDomain;
		}
//		String userAgent = "Mozilla/5.0 (X11; Ubuntu; "
//				+ "Linux x86_64; rv:47.0) Gecko/20100101 Firefox/47.0"; 
		String userAgent = "ExampleBot 1.0 (+http://example.com/bot)"; 
		Elements links = Jsoup.connect(google).userAgent(userAgent).get().select(".g>.r>a");
		for (Element link : links) {
		    String title = link.text();
		    String url = link.absUrl("href"); // Google returns URLs in format "http://www.google.com/url?q=<url>&sa=U&ei=<someKey>".
		    url = URLDecoder.decode(url.substring(url.indexOf('=') + 1, url.indexOf('&')), "UTF-8");
		    if (!url.startsWith("http")) {
		        continue; // Ads/news/etc.
		    }
		    result.add(new BGResult(url, title));
		}
		return result;
	}
	
	public static String GetText(String urlAddr) throws Exception {
        StringBuilder result = new StringBuilder();
        try {
    		String userAgent = "Mozilla/5.0 (X11; Ubuntu; "
    				+ "Linux x86_64; rv:47.0) Gecko/20100101 Firefox/47.0"; 
            Document doc = Jsoup.connect(urlAddr).timeout(60000).
    				userAgent(userAgent).get();
            Elements paragraphs = doc.select("p");
            for(Element p : paragraphs)
            	result.append(p.text() + " ");
            return result.toString();
          } 
          catch (Exception err) {
        	  err.printStackTrace();
        	  return null;
          }
	}
	
}

class BGResult {
	public String URL;
	public String Title;
	
	public BGResult(String url, String title) {
		URL = url;
		Title = title;
	}
	
}
