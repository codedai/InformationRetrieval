package eval;

import invertidx.DocEntry;
import invertidx.InvertedIndex;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;

import query.Query;
import query.QueryResult;
import query.QueryTreeNode;
import query.ResultEntry;
import query.Util;

/**
 * QueryExector: class for execute a query
 * 
 * @author Zeyuan Li
 * */
public class QueryExecutor {

  public HashMap<String, String> pathmap;

  private HashSet<String> typeset = new HashSet<String>();

  private String iipath = "data/index/"; // inverted index root path

  private String qfield; // query field
  
  private boolean loadIndexMem = false;
  
  public HashMap<String, InvertedIndex> iimap = new HashMap<String, InvertedIndex>();

  public HashMap<String, Integer> qtfmap = new HashMap<String, Integer>();
  
  public QueryExecutor() {
    pathmap = new HashMap<String, String>();

    typeset.add(Util.AND);
    typeset.add(Util.OR);
    typeset.add(Util.NEAR);

    buildIIPathMap();
    if(loadIndexMem)
      loadIndexInMem();
  }
  
  public void loadIndexInMem() {
    System.out.println("Loading inverted index...");
    for(Entry<String, String> en : pathmap.entrySet()) {
      InvertedIndex ii = new InvertedIndex(en.getValue());
      iimap.put(en.getKey(), ii);
    }
    System.out.println("Loaded inverted index");
  }

  /**
   * build <inverted index name, ii file path> map
   * */
  public void buildIIPathMap() {
    File file = new File(iipath);
    String[] subdirs = file.list(new FilenameFilter() {
      @Override
      public boolean accept(File dir, String name) {
        return new File(dir, name).isDirectory();
      }
    });

    for (String dir : subdirs) {
      File dirfile = new File(file.getPath() + File.separator + dir);
      String pathPrefix = dirfile.getPath() + File.separator;
      String[] iis = dirfile.list();

      for (String s : iis) {
        if (s.endsWith(Util.IISuffix_TITLE)) {
          String key = s.split("\\.")[0] + "+" + Util.FIELD_TITLE;
          pathmap.put(key, pathPrefix + s);
        } 
        else if(s.endsWith(Util.IISuffix_BODY)){
          String key = s.split("\\.")[0] + "+" + Util.FIELD_BODY;
          pathmap.put(key, pathPrefix + s);
        }
        else if(s.endsWith(Util.IISuffix_INLINK)){
          String key = s.split("\\.")[0] + "+" + Util.FIELD_INLINK;
          pathmap.put(key, pathPrefix + s);
        }
        else if(s.endsWith(Util.IISuffix_URL)){
          String key = s.split("\\.")[0] + "+" + Util.FIELD_URL;
          pathmap.put(key, pathPrefix + s);
        }
        else {
          String key = s.split("\\.")[0] + "+" + Util.FIELD_DEFAULT;
          pathmap.put(key, pathPrefix + s);
        }
      }
    }
  }

  public QueryResult execQuery(Query q) {
    // set query field
    qfield = q.field;
    qtfmap = q.qtfmap;
    
    QueryResult qres = exec(q.getRoot(), q.rankType);
    qres.qid = q.qid;
    qres.rankResult();

    return qres;
  }
  
  /**
   * dfs (post-order) on query tree to execute query
   * */
  private QueryResult exec(QueryTreeNode root, int rankType) {
    if (root == null)
      return null;

    List<QueryResult> childres = new ArrayList<QueryResult>();
    for (QueryTreeNode child : root.children) {
      QueryResult cres = exec(child, rankType);
      // allow null
      childres.add(cres);
    }

    // System.out.println(root.val);
    
    // current node final result
    QueryResult qres = new QueryResult();
    double dftScore = 1.0;
    
    // do operations in non-leaf nodes
    if (root.val.equals(Util.AND)) {
      // For indri only: calc query length (# of leaf nodes)
      int qlen = root.children.size();
      
      for (int i = 0; i < root.children.size(); i++) {
        QueryTreeNode child = root.children.get(i);
        // leaf uses inverted index to get result, while nonleaf uses QueryResult to get result
        if (child.isLeaf()) {
          InvertedIndex ii = null;
          if(loadIndexMem) {
            if(iimap.containsKey(child.val))
              ii = iimap.get(child.val);
          }
          else {
            if (pathmap.containsKey(child.val))
              ii = new InvertedIndex(pathmap.get(child.val));
          }
          if(rankType == Util.TYPE_INDRI)
            qres.combine(ii, rankType, qlen, dftScore);  // include ctf from previous inverted list
          else
            qres.intersect(ii, rankType);
          // calc default score
          if(ii != null)
            dftScore *= Math.pow(Util.getTwoStageSmoothScoreDefault(ii.ctf), 1.0/qlen);
        }
        else if(child.isNearOrUW()) {
          if(rankType == Util.TYPE_INDRI)
            qres.combine(child.iicur, rankType, qlen, dftScore); 
          
          dftScore *= Math.pow(Util.getTwoStageSmoothScoreDefault(child.iicur.ctf), 1.0/qlen);
        }
        else {  //non leaf intersect with QueryResult
          if(rankType == Util.TYPE_INDRI)
            qres.combine(childres.get(i), rankType, qlen, dftScore, child.dftScore); 
          else
            qres.intersect(childres.get(i), rankType);
          
          dftScore *= Math.pow(child.dftScore, 1.0/qlen);
        } 
      }
      
      root.dftScore = dftScore;
    }
    else if (root.val.equals(Util.OR)) {
      for (int i = 0; i < root.children.size(); i++) {
        QueryTreeNode child = root.children.get(i);
        
        if (child.isLeaf()) {
          InvertedIndex ii = null;
          if(loadIndexMem) {
            if(iimap.containsKey(child.val))
              ii = iimap.get(child.val);
          }
          else {
            if (pathmap.containsKey(child.val))
              ii = new InvertedIndex(pathmap.get(child.val)); 
          }
          qres.union(ii, rankType);
        }
        else
          qres.union(childres.get(i), rankType);
      }
      
      root.dftScore = dftScore;
    } 
    else if (root.val.startsWith(Util.NEAR)) {
      InvertedIndex iipr = null;
      int k = Integer.parseInt(root.val.split("/")[1]);

      for (int i = 0; i < root.children.size(); i++) {
        String key = root.children.get(i).val;
        InvertedIndex ii = null;
        if(loadIndexMem) {
          if(iimap.containsKey(key))
            ii = iimap.get(key);
        }
        else {
          if (pathmap.containsKey(key))
            ii = new InvertedIndex(pathmap.get(key));
        }
        
        if (iipr == null) {
          iipr = ii;
          // reset freq (near score is the freq of doc matched the NEAR operator)
          List<DocEntry> doclist = iipr.docEntries;
        } 
        else {
          iipr.near(ii, k, rankType);
        }
      }

      root.iicur = iipr;
      // transform inverted list to score list
      // TODO: add type bm25
      int qlen = root.children.size();
      if(rankType == Util.TYPE_INDRI)
        qres.combine(iipr, rankType, qlen, dftScore);
    }
    else if (root.val.equals(Util.SUM)) {
      for (int i = 0; i < root.children.size(); i++) {
        QueryTreeNode child = root.children.get(i);
        
        if (child.isLeaf()) {
          InvertedIndex ii = null;
          if(loadIndexMem) {
            if(iimap.containsKey(child.val))
              ii = iimap.get(child.val);
          }
          else {
            if (pathmap.containsKey(child.val))
              ii = new InvertedIndex(pathmap.get(child.val)); 
          }
          qres.sum(ii, rankType, qtfmap.get(ii.term));
        }
        else
          qres.sum(childres.get(i), rankType);
      }
    } 
    else if (root.val.startsWith(Util.UW)) {
      InvertedIndex iipr = new InvertedIndex();
      int k = Integer.parseInt(root.val.split("/")[1]);

      ArrayList<InvertedIndex> iis = new ArrayList<InvertedIndex>();
      for (int i = 0; i < root.children.size(); i++) {
        String key = root.children.get(i).val;
        InvertedIndex ii = null;
        if(loadIndexMem) {
          if(iimap.containsKey(key))
            ii = iimap.get(key);
        }
        else {
          if (pathmap.containsKey(key))
            ii = new InvertedIndex(pathmap.get(key));
        }
        iis.add(ii);
      }
      
      iipr.uw(iis, k, rankType);

      root.iicur = iipr;
      // transform inverted list to score list
      // TODO: add type bm25
      int qlen = root.children.size();
      if(rankType == Util.TYPE_INDRI)
        qres.combine(iipr, rankType, qlen, dftScore);
      
    }
    else if (root.val.equals(Util.WEIGHT)) {
      // calc query length (# of leaf nodes)
      double totalWeight = root.getAllChildrenWeight();
      
      for (int i = 0; i < root.children.size(); i++) {
        QueryTreeNode child = root.children.get(i);
        
        if (child.isLeaf()) {
          InvertedIndex ii = null;
          if(loadIndexMem) {
            if(iimap.containsKey(child.val))
              ii = iimap.get(child.val);
          }
          else {
            if (pathmap.containsKey(child.val))
              ii = new InvertedIndex(pathmap.get(child.val)); 
          }
          
          if(child.weight > 0)
            qres.weight(ii, rankType, child.weight / totalWeight, dftScore);
          // calc default score
          if(ii != null)
            dftScore *= Math.pow(Util.getTwoStageSmoothScoreDefault(ii.ctf), child.weight/totalWeight);
        }
        else {
          if(child.weight > 0)
            qres.weight(childres.get(i), rankType, child.weight / totalWeight, dftScore, child.dftScore);
          
          dftScore *= Math.pow(child.dftScore, child.weight / totalWeight);
        }
      }
      
      root.dftScore = dftScore;
    } 
    
    return qres;
  }
  
}
