// In this example, I import data from a file, train a topic model, 
//and analyze the topic assignments of the first instance. I then create 
//a new instance, which is made up of the words from topic 0, and infer 
//a topic distribution for that instance. Note that this example requires 
//the latest development release, and will not compile under Mallet 2.0.6.

// An example input file is available: ap.txt. This is the same example 
//data set provided by David Blei with the lda-c package. The file 
//contains one document per line. Each line has three fields, separated 
//by commas. This is a standard Mallet format. For more information, 
//see the importing data guide. The first field is a name for the document. 
//The second field could contain a document label, as in a classification 
//task, but for this example we won't use that field. It is therefore set 
//to a meaningless placeholder value. The third field contains the full 
//text of the document, with no newline characters.

package biocaddie;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.Iterator;
import java.util.Locale;
import java.util.TreeSet;
import java.util.regex.Pattern;

import cc.mallet.types.*;
import weka.core.GlobalInfoJavadoc;
import cc.mallet.pipe.*;
import cc.mallet.pipe.iterator.*;
import cc.mallet.topics.*;

public class BMalletWWW {
	private static String LDATopicsFile = null;
	private static String LDADocsFile = null;
	private static String LDAmodelFile = null;
	private static String LDAInstancesFile = null;
	private static ParallelTopicModel MModel;
	private static InstanceList MInstances;
	private static Alphabet MDataAlphabet;
	private static TopicInferencer MInferencer;

	private static void malletTests(int numTopics) throws Exception {
		// Begin by importing documents from text to feature sequences
		ArrayList<Pipe> pipeList = new ArrayList<Pipe>();

		// Pipes: lowercase, tokenize, remove stopwords, map to features
//		pipeList.add( new CharSequenceLowercase() );
		pipeList.add( new CharSequence2TokenSequence(Pattern.compile(
				"\\p{L}[\\p{L}\\p{P}]+\\p{L}")) );
//		pipeList.add( new TokenSequenceRemoveStopwords(new File(
//				dirLDA + "en.txt"), "UTF-8", false, false, false) );
		pipeList.add( new TokenSequence2FeatureSequence() );

		InstanceList instances = new InstanceList (new SerialPipes(
				pipeList));

		Reader fileReader = new InputStreamReader(new FileInputStream(
				new File(LDADocsFile)), "UTF-8");
		instances.addThruPipe(new CsvIterator (fileReader, 
				Pattern.compile("^(\\S*)[\\s,]*(\\S*)[\\s,]*(.*)$"), 
				3, 2, 1)); // data, label, name fields

		// Create a model with 100 topics, alpha_t = 0.01, beta_w = 0.01
		//  Note that the first parameter is passed as the sum over topics, while
		//  the second is the parameter for a single dimension of the Dirichlet prior.
		ParallelTopicModel model = new ParallelTopicModel(
				numTopics, 1.0, 0.01);

		model.addInstances(instances);

		// Use two parallel samplers, which each look at one half the corpus and combine
		//  statistics after every iteration.
		model.setNumThreads(10);

		// Run the model for 50 iterations and stop (this is for testing only, 
		//  for real applications, use 1000 to 2000 iterations)
		model.setNumIterations(1500);
		model.estimate();

////////////////////////////////////////////////////////////////////		

		// Show the words and topics in the first instance

		// The data alphabet maps word IDs to strings
		Alphabet dataAlphabet = instances.getDataAlphabet();
		
		FeatureSequence tokens = (FeatureSequence) model.getData().
				get(0).instance.getData();
		LabelSequence topics = model.getData().get(0).topicSequence;
		
		Formatter out = new Formatter(new StringBuilder(), Locale.US);
		for (int pos = 0; pos < tokens.getLength(); pos++) {
			out.format("%s-%d ", 
				dataAlphabet.lookupObject(tokens.getIndexAtPosition(pos)), 
				topics.getIndexAtPosition(pos));
		}
		BLib.println("[[[ " + out + " ]]]");
		
		// Estimate the topic distribution of the first instance, 
		//  given the current Gibbs state.
		double[] topicDistribution = model.getTopicProbabilities(0);

		// Get an array of sorted sets of word ID/count pairs
		ArrayList<TreeSet<IDSorter>> topicSortedWords = 
				model.getSortedWords();
		
		// Show top 5 words in topics with proportions for the first document
		for (int topic = 0; topic < numTopics; topic++) {
			Iterator<IDSorter> iterator = topicSortedWords.get(topic).
					iterator();
			
			out = new Formatter(new StringBuilder(), Locale.US);
			out.format("%d\t%.3f\t", topic, topicDistribution[topic]);
			int rank = 0;
			while (iterator.hasNext() && rank < 5) {
				IDSorter idCountPair = iterator.next();
				out.format("%s (%.0f) ", 
						dataAlphabet.lookupObject(idCountPair.getID()), 
						idCountPair.getWeight());
				rank++;
			}
			BLib.println(out.toString());
		}
	
		// Create a new instance with high probability of topic 0
		StringBuilder topicZeroText = new StringBuilder();
		Iterator<IDSorter> iterator = topicSortedWords.get(0).iterator();

		int rank = 0;
		while (iterator.hasNext() && rank < 5) {
			IDSorter idCountPair = iterator.next();
			topicZeroText.append(dataAlphabet.lookupObject(
					idCountPair.getID()) + " ");
			rank++;
		}

		// Create a new instance named "test instance" with empty target and source fields.
		InstanceList testing = new InstanceList(instances.getPipe());
		testing.addThruPipe(new Instance(topicZeroText.toString(), null, 
				"test instance", null));

		TopicInferencer inferencer = model.getInferencer();
		double[] testProbabilities = inferencer.getSampledDistribution(
				testing.get(0), 10, 1, 5);
		BLib.println("0\t" + testProbabilities[0] + "\n" + 
				"1\t" + testProbabilities[1]);		
		
	    model.write(new File(LDAmodelFile));
	    instances.save(new File(LDAInstancesFile));
	}
	
	private static void malletTests2() throws Exception {
		ParallelTopicModel model = ParallelTopicModel.read(new File(
				LDAmodelFile));
		InstanceList instances = InstanceList.load(new File(
				LDAInstancesFile));
		Alphabet dataAlphabet = instances.getDataAlphabet();
		
		// Get an array of sorted sets of word ID/count pairs
		ArrayList<TreeSet<IDSorter>> topicSortedWords = 
				model.getSortedWords();
		
		// Create a new instance with high probability of topic 0
		StringBuilder topicZeroText = new StringBuilder();
		Iterator<IDSorter> iterator = topicSortedWords.get(0).iterator();

		int rank = 0;
		while (iterator.hasNext() && rank < 5) {
			IDSorter idCountPair = iterator.next();
			topicZeroText.append(dataAlphabet.lookupObject(
					idCountPair.getID()) + " ");
			rank++;
		}

		// Create a new instance named "test instance" with empty target and source fields.
		InstanceList testing = new InstanceList(instances.getPipe());
		testing.addThruPipe(new Instance(topicZeroText.toString(), null, 
				"test instance", null));
		
		TopicInferencer inferencer = model.getInferencer();
		double[] testProbabilities = inferencer.getSampledDistribution(
				testing.get(0), 10, 1, 5);
		BLib.println("0\t" + testProbabilities[0] + "\n" + 
				"1\t" + testProbabilities[1]);		
	}
	
	private static void CreateFile() throws Exception {
		PrintStream ps = new PrintStream(LDADocsFile);
		for (int ind = 0; ind < BStandardMethods.DocCount; ++ind) {
			ps.print(ind);
			ps.print("\tX\t");
			BDocumentAtIndex doc = new BDocumentAtIndex(ind);
			doc.FetchTITLEAndTEXT();
			StringBuilder sb = new StringBuilder();
			sb.append(doc.TITLE);
			sb.append(" ");
			sb.append(doc.TEXT);
			ArrayList<String> words = BIndexCreator.Tokenize(sb.toString());
			words = BIndexCreator.RemoveCommonWords(words);
			for (String wd : words) {
				ps.print(wd + " ");
			}
			ps.println();
			if (ind % 10000 == 0) {
				BLib.println(ind);
			}
		}
		ps.close();
	}
	
	public static void GetTopics(int numTopics) throws Exception {
		if (MModel == null) {
			MModel = ParallelTopicModel.read(new File(LDAmodelFile));
			MInstances = InstanceList.load(new File(LDAInstancesFile));
			MDataAlphabet = MInstances.getDataAlphabet();
			MInferencer = MModel.getInferencer();
		}
		ArrayList<String> result = new ArrayList<>();
		ArrayList<TreeSet<IDSorter>> topicSortedWords = 
				MModel.getSortedWords();
		for (int topic = 0; topic < MModel.numTopics; topic++) {
			StringBuilder sb = new StringBuilder();
			Iterator<IDSorter> iterator = topicSortedWords.get(topic).
					iterator();			
			int rank = 0;
			while (iterator.hasNext() && rank < 10) {
				IDSorter idCountPair = iterator.next();
				sb.append(MDataAlphabet.lookupObject(idCountPair.getID()));
				sb.append("(" + idCountPair.getWeight() + ") ");
				rank++;
			}
			result.add(sb.toString());
		}
		PrintStream ps = new PrintStream(LDATopicsFile);
		for (String str : result) {
			ps.println(str.toString());
		}
		ps.close();
	}
	
	public static void CreateModel(int numTopics) throws Exception {
		setAddr(numTopics);
		CreateFile();
		malletTests(numTopics);
		malletTests2();
		GetTopics(numTopics);
	}

	public static double[] getDistribution(String text) throws Exception {
		if (MModel == null) {
			MModel = ParallelTopicModel.read(new File(LDAmodelFile));
			MInstances = InstanceList.load(new File(LDAInstancesFile));
			MDataAlphabet = MInstances.getDataAlphabet();
			MInferencer = MModel.getInferencer();
		}
		ArrayList<String> words = BIndexCreator.Tokenize(text);
		words = BIndexCreator.RemoveCommonWords(words);
		String body = BIndexCreator.GetString(words);
		InstanceList testing = new InstanceList(MInstances.getPipe());
		testing.addThruPipe(new Instance(body, null, "test", null));
		double[] result = MInferencer.getSampledDistribution(
				testing.get(0), 20, 1, 5);
		return result;
	}
	
	public static double getDistance(double[] vec1, double[] vec2, 
			boolean normalize) {
		double mult = 0;
		double len1 = 0;
		double len2 = 0;
		for (int ind = 0; ind < vec1.length; ++ind) {
			mult += vec1[ind] * vec2[ind];
			len1 += vec1[ind] * vec1[ind];
			len2 += vec2[ind] * vec2[ind];
		}
		if (normalize) {
			len1 = Math.sqrt(len1);
			len2 = Math.sqrt(len2);
			double result = mult / (len1 * len2);
			return result;
		}
		else {
			return mult;
		}
	}
	
	public static double[] filterTop(double[] list, int topCount) {
		double[] result = new double[list.length];
		for (int tInd = 0; tInd < topCount; ++tInd) {
			int maxInd = -1;
			for (int ind = 0;  ind < list.length; ++ind) {
				if (result[ind] == 0 && 
						(maxInd == -1 || list[maxInd] < list[ind])) {
					maxInd = ind;
				}
			}
			result[maxInd] = list[maxInd];
		}
		return result;
	}
	
	private static void setAddr(int numTopics) 
			throws Exception {
		String Dir = BGlobalVar.Results + "LDA" + numTopics + "/";
		if (!Files.exists(Paths.get(Dir)))
			Files.createDirectory(Paths.get(Dir));
		LDATopicsFile = Dir + "LDATopics.txt";
		LDADocsFile = Dir + "LDADocs.txt";
		LDAmodelFile = Dir + "LDAmodel.bin";
		LDAInstancesFile = Dir + "LDAInstances.bin";
	}
	
}
