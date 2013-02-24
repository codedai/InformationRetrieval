package query;

import invertidx.DocEntry;
import invertidx.InvertedIndex;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

/**
 * QueryResult: class for the result of a query
 * 
 * @author Zeyuan Li
 * */
public class QueryResult {

  public int qid;

  public String q0 = "Q0";

  public List<ResultEntry> reslist = null;

  public String runid;
  
  private static final int retSize = 100;

  public QueryResult() {
  }

  public QueryResult(int qid) {
    this.qid = qid;
    q0 = "Q0";
  }

  public QueryResult(int qid, String q0, List<ResultEntry> reslist) {
    this.qid = qid;
    this.q0 = q0;
    this.reslist = reslist;
  }

  public void intersect(InvertedIndex ii, int rankType) {
    if (ii == null)
      return;

    // first time
    if (reslist == null) {
      reslist = new ArrayList<ResultEntry>();

      Iterator<DocEntry> it = ii.docEntries.iterator();
      while (it.hasNext()) {
        DocEntry en = it.next();
        if (rankType == Util.TYPE_RANKED)
          reslist.add(new ResultEntry(en.docid, -1, en.tf));
        else if (rankType == Util.TYPE_UNRANKED)
          reslist.add(new ResultEntry(en.docid, -1, 1));
      }
    }
    // merge with other ii
    else {
      Iterator<DocEntry> itdoc = ii.docEntries.iterator();
      Iterator<ResultEntry> itres = reslist.iterator();
      List<ResultEntry> mergeres = new ArrayList<ResultEntry>();  // IMP: array list is much faster
      DocEntry docen = null;
      ResultEntry resen = null;
      
      while (itdoc.hasNext() && itres.hasNext()) {
        // IMP: linkedlist random access is slow!!!!
        if(docen == null && resen == null) {
          docen = itdoc.next();
          resen = itres.next();
        }
        
        if (docen.docid == resen.docid) {
          if (rankType == Util.TYPE_RANKED) {
            if (resen.score > docen.tf)
              resen.score = docen.tf;
          } else if (rankType == Util.TYPE_UNRANKED) {
            if (resen.score > 1)
              resen.score = 1;
          }
          
          mergeres.add(resen);
          
          docen = itdoc.next();
          resen = itres.next();
        } else if (docen.docid < resen.docid) {
          docen = itdoc.next();
        }
        // delete results from reslist
        else {
          resen = itres.next();
        }
      }
      
      // assign new list
      reslist = mergeres;
    }
  }

  /**
   * intersect with QueryResult
   * */
  public void intersect(QueryResult qro, int rankType) {
    if (qro == null)
      return;

    // first time
    if (reslist == null) {
      reslist = qro.reslist;
    }
    // merge with other QueryResult
    else {
      Iterator<ResultEntry> ito = qro.reslist.iterator();
      Iterator<ResultEntry> itres = reslist.iterator();
      List<ResultEntry> mergeres = new ArrayList<ResultEntry>();  // IMP: array list is much faster
      ResultEntry oen = null;
      ResultEntry resen = null;
      
      while (ito.hasNext() && itres.hasNext()) {
        if(oen == null && resen == null) {
          oen = ito.next();
          resen = itres.next();
        }
        
        if (oen.docid == resen.docid) {
          if (rankType == Util.TYPE_RANKED) {
            if (resen.score > oen.score)
              resen.score = oen.score;
          } else if (rankType == Util.TYPE_UNRANKED) {
            if (resen.score > 1)
              resen.score = 1;
          }
          mergeres.add(resen);
          
          oen = ito.next();
          resen = itres.next();
        } else if (oen.docid < resen.docid) {
          oen = ito.next();
        }
        // delete results from reslist
        else {
          resen = itres.next();
        }
      }
      
      // assign new list
      reslist = mergeres;
    }
  }

  public void union(InvertedIndex ii, int rankType) {
    if (ii == null)
      return;

    // first time
    if (reslist == null) {
      reslist = new ArrayList<ResultEntry>();

      Iterator<DocEntry> it = ii.docEntries.iterator();
      while (it.hasNext()) {
        DocEntry en = it.next();
        if (rankType == Util.TYPE_RANKED)
          reslist.add(new ResultEntry(en.docid, -1, en.tf));
        if (rankType == Util.TYPE_UNRANKED)
          reslist.add(new ResultEntry(en.docid, -1, 1));
      }
    }
    // merge with other ii
    else {
      Iterator<DocEntry> itdoc = ii.docEntries.iterator();
      Iterator<ResultEntry> itres = reslist.iterator();
      DocEntry docen = null;
      ResultEntry resen = null;
      // IMP: linked list add is also slow!!!!
      List<ResultEntry> mergeres = new ArrayList<ResultEntry>();
      
      while (itdoc.hasNext() && itres.hasNext()) {
        if(docen == null && resen == null) {
          docen = itdoc.next();
          resen = itres.next();
        }

        if (docen.docid == resen.docid) {
          if (rankType == Util.TYPE_RANKED) {
            if (resen.score < docen.tf)
              resen.score = docen.tf;
          } else if (rankType == Util.TYPE_UNRANKED) {
            if (resen.score < 1)
              resen.score = 1;
          }
          
          mergeres.add(resen);
          
          docen = itdoc.next();
          resen = itres.next();
        } else if (docen.docid < resen.docid) {
          //assert docen.docid < reslist.get(ires).docid : docen.docid + " " + reslist.get(ires).docid;
          
          if (rankType == Util.TYPE_RANKED)
            mergeres.add(new ResultEntry(docen.docid, -1, docen.tf));
          else if (rankType == Util.TYPE_UNRANKED)
            mergeres.add(new ResultEntry(docen.docid, -1, 1));

          docen = itdoc.next();
        } else {
          if (rankType == Util.TYPE_RANKED)
            mergeres.add(resen);
          else if (rankType == Util.TYPE_UNRANKED)
            mergeres.add(resen);
          
          resen = itres.next();
        }
      }

      // ii has elements left
      while (itdoc.hasNext()) {
        docen = itdoc.next();
        //assert docen.docid > reslist.get(reslist.size()-1).docid : docen.docid +" " + reslist.get(reslist.size()-1).docid;
        
        if (rankType == Util.TYPE_RANKED)
          mergeres.add(new ResultEntry(docen.docid, -1, docen.tf));
        else if (rankType == Util.TYPE_UNRANKED)
          mergeres.add(new ResultEntry(docen.docid, -1, 1));
      }
      
      // assign new list
      reslist = mergeres;
    }
    
  }
  
  /**
   * union with oter QueryResult
   * */
  public void union(QueryResult qro, int rankType) {
    if (qro == null)
      return;

    // first time
    if (reslist == null) {
      reslist = qro.reslist;
    }
    // merge with other ii
    else {
      Iterator<ResultEntry> ito = qro.reslist.iterator();
      Iterator<ResultEntry> itres = reslist.iterator();
      ResultEntry oen = null;
      ResultEntry resen = null;
      ArrayList<ResultEntry> mergeres = new ArrayList<ResultEntry>();
      
      
      while (ito.hasNext() && itres.hasNext()) {
        if(oen == null && resen == null) {
          oen = ito.next();
          resen = itres.next();
        }
        
        if (oen.docid == resen.docid) {
          if (rankType == Util.TYPE_RANKED) {
            if (resen.score < oen.score)
              resen.score = oen.score;
          } else if (rankType == Util.TYPE_UNRANKED) {
            if (resen.score < 1)
              resen.score = 1;
          }
          
          mergeres.add(resen);
          
          oen = ito.next();
          resen = itres.next();
        } else if (oen.docid < resen.docid) {
          //assert oen.docid < reslist.get(ires).docid : oen.docid + " " + reslist.get(ires).docid;
          if (rankType == Util.TYPE_RANKED) 
            mergeres.add(new ResultEntry(oen.docid, -1, oen.score));
          else if (rankType == Util.TYPE_UNRANKED)
            mergeres.add(new ResultEntry(oen.docid, -1, 1));

          oen = ito.next();
        } else {
          mergeres.add(resen);
          
          resen = itres.next();
        }
      }

      // other qr has elements left
      while (ito.hasNext()) {
        ResultEntry eno = ito.next();
        //assert eno.docid > reslist.get(reslist.size()-1).docid : eno.docid +" " + reslist.get(reslist.size()-1).docid;
        
        mergeres.add(eno);
      }
      
      // assign new list
      reslist = mergeres;
    }
    
  }
  
  public void rankResult() {
    Collections.sort(reslist);
    for (int i = 1; i <= reslist.size(); i++)
      reslist.get(i - 1).rank = i;
  }

  public void serialize(String path) {
    if (path == null || path.length() == 0)
      return;

    try {
      BufferedWriter bw = new BufferedWriter(new FileWriter(path, true));

      for (int i = 0; i < retSize && i < reslist.size(); i++) {
        ResultEntry en = reslist.get(i);
        bw.write(String
                .format("%d %s %d %d %.1f %s\n", qid, q0, en.docid, en.rank, en.score, runid));
      }

      bw.close();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }
  
  public void checkDup() {
    HashSet<Integer> set = new HashSet<Integer>();
    ArrayList<Integer> reslistval = new ArrayList<Integer>();
    
    for(int i = 0; i < reslist.size(); i++) {
      int id = reslist.get(i).docid;
      reslistval.add(id);
      
      if(set.contains(id))
        System.out.println("DUP: " + id);
      else
        set.add(id);
    }
  }
}
