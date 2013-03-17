package test;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import javax.naming.spi.DirectoryManager;

import eval.QueryExecutor;

import query.Query;
import query.QueryParser;
import query.QueryResult;
import query.QueryTreeNode;
import query.Util;

/**
 * TestQuery: driver program for the whole boolean retrieval system
 * 
 * @author Zeyuan Li
 * */
public class TestQuery {

  //private String qpath = "data/handin_queries.txt", qStruPath = "data/handin_stru_queries.txt";
  private String qpath = "data/queries.txt", qStruPath = "data/stru_queries.txt";

  private String outpathPrefix = "data/result/";

  private String logpath = "data/result/log.txt";

  private static String runid = "run-";

  private int[] paraRank = { Util.TYPE_RANKED, Util.TYPE_BM25, Util.TYPE_INDRI };

  private String[] paraRankS = { "_r", "_bm25", "_indri" };
  
  private int[] paraType = { Util.TYPE_OR, Util.TYPE_SUM, Util.TYPE_ANDINDRI };

  private String[] paraTypeS = { "_or", "_sum", "_combine" };

  public List<String> qs, qsStru;

  public QueryParser parser;

  public QueryExecutor executor;

  public TestQuery() {
    File resdir = new File(outpathPrefix);
    if (!resdir.exists())
      resdir.mkdir();

    qs = readQuery(qpath);
    qsStru = readQuery(qStruPath);
    parser = new QueryParser();
    executor = new QueryExecutor();
  }

  public void runOneQuery(int cnt, int type, int rank) throws IOException {
    String curRunId = runid + "" + cnt;
    String curRunIdStr = runid + "" + cnt + paraTypeS[cnt-1] + paraRankS[cnt-1];
    BufferedWriter br = new BufferedWriter(new FileWriter(logpath, true));

    String outpath = outpathPrefix + curRunIdStr + ".txt";
    File res = new File(outpath);
    if (res.exists())
      res.delete();

    System.out.println("Running test: " + curRunId);
    long startTime = System.currentTimeMillis();

    for (int i = 0; i < qs.size(); i++) {
      // get query
      Query q = new Query(qs.get(i), rank, type);
      // parse query
      QueryTreeNode root = parser.parse(q);
      q.setRoot(root);
      parser.dfs(root);
      System.out.println("");

      QueryResult qres = executor.execQuery(q);
      qres.runid = curRunId;

      // serialize
      qres.serialize(outpath);

      System.out.println("Done: " + q.qid);
    }
    long endTime = System.currentTimeMillis();
    System.out.println("Finised test: id:" + curRunId + " idStr:" + curRunIdStr + " in "
            + ((endTime - startTime) / 1000) + " secs");
    br.write("Finised test: id:" + curRunId + " idStr:" + curRunIdStr + " in "
            + ((endTime - startTime) / 1000) + " secs\n");

    br.close();
  }

  public void runOneStruQuery(int cnt, int rank) throws IOException {
    String curRunId = runid + "" + cnt;
    String curRunIdStr = runid + "" + cnt + paraTypeS[cnt-1]
            + paraRankS[cnt-1];
    BufferedWriter br = new BufferedWriter(new FileWriter(logpath, true));

    String outpath = outpathPrefix + curRunIdStr + ".txt";
    File res = new File(outpath);
    if (res.exists())
      res.delete();

    System.out.println("Running test: " + curRunId);
    long startTime = System.currentTimeMillis();

    for (int i = 0; i < qsStru.size(); i++) {
      // get query
      Query q = new Query(qsStru.get(i), rank, Util.TYPE_STRUCTURE);
      // parse query
      QueryTreeNode root = parser.parse(q);
      q.setRoot(root);
      parser.dfs(root);
      System.out.println("");

      QueryResult qres = executor.execQuery(q);
      qres.runid = curRunId;

      // serialize
      qres.serialize(outpath);

      System.out.println("Done: " + q.qid);
    }
    long endTime = System.currentTimeMillis();
    System.out.println("Finised test: id:" + curRunId + " idStr:" + curRunIdStr + " in "
            + ((endTime - startTime) / 1000) + " secs");
    br.write("Finised test: id:" + curRunId + " idStr:" + curRunIdStr + " in "
            + ((endTime - startTime) / 1000) + " secs\n");

    br.close();
  }

  public void runQuery() {
    int cnt = 1;

    try {
      for(int i = 0; i < paraType.length; i++) {
        int type = paraType[i], rank = paraRank[i];

        runOneQuery(cnt, type, rank);

        cnt++;
      }

      // run structed queries
//      for (int pj = 0; pj < paraRank.length; pj++) {
//        int rank = paraRank[pj];
//        runOneStruQuery(cnt, rank);
//        cnt++;
//      }

    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

  }

  public List<String> readQuery(String qpath) {
    ArrayList<String> qs = new ArrayList<String>();
    try {
      BufferedReader br = new BufferedReader(new FileReader(qpath));
      String line;
      while ((line = br.readLine()) != null)
        qs.add(line);
    } catch (IOException e) {
      e.printStackTrace();
    }
    return qs;
  }

  public void testDup() {
    HashSet<String> set = new HashSet<String>();
    try {
      BufferedReader br = new BufferedReader(new FileReader("data/result/run-1_st_r2.txt"));
      String line;

      while ((line = br.readLine()) != null) {
        String term = line.split(" ")[2];
        if (set.contains(term))
          System.out.println(line);
        else
          set.add(term);
      }

      br.close();
    } catch (FileNotFoundException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  /**
   * @param args
   */
  public static void main(String[] args) {
    // TODO Auto-generated method stub
    TestQuery tq = new TestQuery();
    int cnt = 3;
    int type = Util.TYPE_ANDINDRI;
    int rank = Util.TYPE_INDRI;
    
//     try {
////     tq.runOneStruQuery(cnt, rank);
//     
////       tq.runOneQuery(cnt, type, rank);
//     } catch (IOException e) {
//     // TODO Auto-generated catch block
//     e.printStackTrace();
//     }
     

    tq.runQuery();
  }

}
