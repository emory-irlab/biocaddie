# biocaddie
- Apache Lucene 4.8.1 was used for indexing and retrieval, Mallet 2.0.8 was used for topic modelling, jsoup 1.10 api used for crawling the web, and RankLib 2.1 was also used for LTR. Some functionalities of Apache commons 3.5 were also used to facilitate coding. Weka 3.8.0 api was also used for clustering (currently clustering is not active in the code).
- For efficiency reasons the standards of Java coding was violated in some cases, e.g. the getter/setter methods were not used and the attributes were made directly accessible.
- To make the coding ready for rapid changes and also have fewer lines of codes some standards of soding was violated, e.g. the number of "public static" methods is higher than usual projects.

+ BBOW.java: 
To manipulate a hashtable of BTerms.

+ BDocumentAtIndex.java: 
To store and manipulate indexed lucene documents.

+ BExpansion.java: 
To load and save expansion resources (HGNC, KEGG, ...).

+ BGlobalVar.java: 
To store the project globab constants.

+ BGoogleNGram.java: 
Extract information from google ngrams.

+ BGoogleSearch.java: 
Search through google web interface.

+ BIRParam.java: 
To store the parameters for optimizations.

+ BIndexCreator.java: 
To create lucene index.

+ BJournalExperiments.java: 
We used this class to do additional experiments for submitted paper.

+ BLTRParam.java: 
To store the parameters for LTR optimizations (it is not used in the code).

+ BLib.java: 
public functionalities used in the project.

+ BMainThread.java: 
Main class

+ BMallet.java: 
To find the topic distributions.

+ BOptimize.java: 
To tune the system (the parts of the code is in this class)

+ BQuery.java: 
To store and manipulate the official queries.

+ BQueryChainResult.java: 
To store a series of queries.

+ BQueryResult.java: 
To store the hit list retrieved using Lucene.

+ BRankLib.java: 
To re-rank the top documents in alist using LTR.

+ BStandardMethods.java: 
A facade for Lucene API (the Lucene API is not directly accessed in other classes).

+ BTerm.java: 
To store a term information in a set od documents (or collection).

+ BTrecEvalResult.java: 
A facade to run/parse the treceval results.

+ BWeka.java: 
A facade for Weka API.
