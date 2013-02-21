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

public class QueryResult {

  public int qid;

  public String q0 = "Q0";

  public List<ResultEntry> reslist = null;

  public String runid;
  
  private static final int retSize = 100;

  public QueryResult() {
    // reslist = new LinkedList<ResultEntry>();
  }

  public QueryResult(int qid) {
    this.qid = qid;
    q0 = "Q0";
    // reslist = new ArrayList<ResultEntry>();
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
      // reslist = new ArrayList<ResultEntry>();
      reslist = new LinkedList<ResultEntry>();

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
      int i = 0, ires = 0, n = ii.docEntries.size(), nres = reslist.size();

      while (i < n && ires < nres) {
        DocEntry docen = ii.docEntries.get(i);
        ResultEntry resen = reslist.get(ires);

        if (docen.docid == resen.docid) {
          if (rankType == Util.TYPE_RANKED) {
            if (resen.score > docen.tf)
              resen.score = docen.tf;
          } else if (rankType == Util.TYPE_UNRANKED) {
            if (resen.score > 1)
              resen.score = 1;
          }
          i++;
          ires++;
        } else if (docen.docid < resen.docid) {
          i++;
        }
        // delete results from reslist
        else {
          reslist.remove(ires);
          nres--;
        }
      }

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
      int io = 0, ires = 0, no = qro.reslist.size(), nres = reslist.size();

      while (io < no && ires < nres) {
        ResultEntry oen = qro.reslist.get(io);
        ResultEntry resen = reslist.get(ires);

        if (oen.docid == resen.docid) {
          if (rankType == Util.TYPE_RANKED) {
            if (resen.score > oen.score)
              resen.score = oen.score;
          } else if (rankType == Util.TYPE_UNRANKED) {
            if (resen.score > 1)
              resen.score = 1;
          }
          io++;
          ires++;
        } else if (oen.docid < resen.docid) {
          io++;
        }
        // delete results from reslist
        else {
          reslist.remove(ires);
          nres--;
        }
      }
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
      int i = 0, ires = 0, n = ii.docEntries.size(), nres = reslist.size();

      while (i < n && ires < nres) {
        DocEntry docen = ii.docEntries.get(i);
        ResultEntry resen = reslist.get(ires);

        if (docen.docid == resen.docid) {
          if (rankType == Util.TYPE_RANKED) {
            if (resen.score < docen.tf)
              resen.score = docen.tf;
          } else if (rankType == Util.TYPE_UNRANKED) {
            if (resen.score < 1)
              resen.score = 1;
          }
          i++;
          ires++;
        } else if (docen.docid < resen.docid) {
          assert docen.docid < reslist.get(ires).docid : docen.docid + " " + reslist.get(ires).docid;
          
          if (rankType == Util.TYPE_RANKED)
            reslist.add(ires, new ResultEntry(docen.docid, -1, docen.tf));
          else if (rankType == Util.TYPE_UNRANKED)
            reslist.add(ires, new ResultEntry(docen.docid, -1, 1));

          i++;
          ires++;
          nres++; //IMP
        } else {
          ires++;
        }
      }

      // ii has elements left
      while (i < n) {
        DocEntry docen = ii.docEntries.get(i);
        //assert docen.docid > reslist.get(reslist.size()-1).docid : docen.docid +" " + reslist.get(reslist.size()-1).docid;
        
        if (rankType == Util.TYPE_RANKED)
          reslist.add(new ResultEntry(docen.docid, -1, docen.tf));
        else if (rankType == Util.TYPE_UNRANKED)
          reslist.add(new ResultEntry(docen.docid, -1, 1));
        i++;
      }
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
      int io = 0, ires = 0, no = qro.reslist.size(), nres = reslist.size();

      while (io < no && ires < nres) {
        ResultEntry oen = qro.reslist.get(io);
        ResultEntry resen = reslist.get(ires);

        if (oen.docid == resen.docid) {
          if (rankType == Util.TYPE_RANKED) {
            if (resen.score < oen.score)
              resen.score = oen.score;
          } else if (rankType == Util.TYPE_UNRANKED) {
            if (resen.score < 1)
              resen.score = 1;
          }
          io++;
          ires++;
        } else if (oen.docid < resen.docid) {
          assert oen.docid < reslist.get(ires).docid : oen.docid + " " + reslist.get(ires).docid;
          
          if (rankType == Util.TYPE_RANKED) 
            reslist.add(ires, new ResultEntry(oen.docid, -1, oen.score));
          else if (rankType == Util.TYPE_UNRANKED)
            reslist.add(ires, new ResultEntry(oen.docid, -1, 1));

          io++;
          ires++;
          nres++; //IMP
        } else {
          ires++;
        }
      }

      // other qr has elements left
      while (io < no) {
        ResultEntry eno = qro.reslist.get(io);
        assert eno.docid > reslist.get(reslist.size()-1).docid : eno.docid +" " + reslist.get(reslist.size()-1).docid;
        
        if (rankType == Util.TYPE_RANKED)
          reslist.add(new ResultEntry(eno.docid, -1, eno.score));
        else if (rankType == Util.TYPE_UNRANKED)
          reslist.add(new ResultEntry(eno.docid, -1, 1));
        io++;
      }
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
