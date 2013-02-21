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
import query.Util;

public class QueryExecutor {

  public HashMap<String, String> pathmap;

  private HashSet<String> typeset = new HashSet<String>();

  private String iipath = "data/index/"; // inverted index root path

  private String qfield; // query field
  
  private boolean loadIndexMem = true;
  
  public HashMap<String, InvertedIndex> iimap = new HashMap<String, InvertedIndex>();

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
        } else {
          String key = s.split("\\.")[0] + "+" + Util.FIELD_BODY;
          pathmap.put(key, pathPrefix + s);
        }
      }
    }
  }

  public QueryResult execQuery(Query q) {
    // set query field
    qfield = q.field;

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
    // do operations in non-leaf nodes
    if (root.val.equals(Util.AND)) {
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
          qres.intersect(ii, rankType);
        }
        else  //non leaf intersect with QueryResult
          qres.intersect(childres.get(i), rankType);
      }
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
          for (DocEntry en : doclist)
            en.tf = 1;
        } else {
          iipr.near(ii, k);
        }
      }

      // union NEAR result
      qres.union(iipr, Util.TYPE_RANKED);
    }
    
    return qres;
  }

  

  /**
   * @param args
   */
  public static void main(String[] args) {
    // TODO Auto-generated method stub

  }

}
