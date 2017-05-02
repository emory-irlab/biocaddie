package biocaddie;

import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;

import org.apache.commons.lang3.time.StopWatch;

public class BLib {
	public static StopWatch stopW = new StopWatch();

	public static void println(Object text) {
		print(false, text, true);
	}

	public static void println() {
		print(false, null, true);
	}
	
	public static void print(Object text) {
		print(false, text, false);
	}

	public static void printlnForce(Object text) {
		print(true, text, true);
	}

	public static void printlnForce() {
		print(true, null, true);
	}
	
	public static void printForce(Object text) {
		print(true, text, false);
	}

	private static void print(boolean force, Object text, boolean newLine) {
//		if (force)
		{
			if (newLine) {
				if (text != null) {
					System.out.println(text);
				}
				else {
					System.out.println();
				}
			}
			else {
				System.out.print(text);
			}
		}
	}
	
	public static void JudgmentConverter() throws Exception {
		Integer[] qs = new Integer[] {3, 7, 9, 11, 12, 15};
		Integer[] keys = BTrecEvalResult.QueryDocRels.keySet().toArray(new Integer[0]);
		StringBuilder result = new StringBuilder();
		for (int ind = 0; ind < keys.length; ++ind) {
			if (Arrays.asList(qs).contains(keys[ind])) {
				HashMap<String, Integer> rels = BTrecEvalResult.QueryDocRels.get(keys[ind]);
				String[] docs = rels.keySet().toArray(new String[0]);
				int ones = 0;
				int twos = 0;
				for (int dInd = 0; dInd < docs.length; ++dInd) {
					if (rels.get(docs[dInd]) == 1 && ones < 40) {
						result.append(String.format("%03d", keys[ind]) + " 0 " + docs[dInd] + " 1\n");
						++ones;
					}
					else if (rels.get(docs[dInd]) == 2 && twos < 20) {
						result.append(String.format("%03d", keys[ind]) + " 0 " + docs[dInd] + " 2\n");
						++twos;
					}
				}
			}
		}
		PrintStream ps = new PrintStream(BGlobalVar.Results + "Judgments2.txt");
		ps.print(result.toString());
		ps.close();
	}
	
	public static String createDIR(String optionalDirectory) throws Exception {
		String Dir = BGlobalVar.Results;
		if (optionalDirectory != null){
			Dir += optionalDirectory + "/";
			if (!Files.exists(Paths.get(Dir)))
				Files.createDirectory(Paths.get(Dir));
		}
		return Dir;
	}
	
	public static void startWatch() {
		stopW.start();
	}
	
	public static void stopWatch() {
		stopW.stop();
	}

	public static void resumeWatch() {
		if (stopW.isSuspended())
			stopW.resume();
		else
			stopW.start();
	}
	
	public static void suspendWatch() {
		stopW.suspend();
	}
	
	public static String getElapsedTime() {
		return stopW.toString();
	}
	
	
	
}
