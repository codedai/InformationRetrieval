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
  
  public void sum(InvertedIndex ii, int rankType, int qtf) {
    if (ii == null)
      return;

    // first time
    if (reslist == null) {
      reslist = new ArrayList<ResultEntry>();

      Iterator<DocEntry> it = ii.docEntries.iterator();
      while (it.hasNext()) {
        DocEntry en = it.next();
        if (rankType == Util.TYPE_BM25) {
          double rsv = Util.getRSV(ii, en, qtf);
          reslist.add(new ResultEntry(en.docid, -1, rsv));
        }
      }
    }
    // merge with other ii
    else {
      int idoc = 0, ires = 0, ndoc = ii.docEntries.size(), nres = reslist.size();
      // IMP: linked list add is also slow!!!!
      ArrayList<ResultEntry> mergeres = new ArrayList<ResultEntry>();
      
      while (idoc < ndoc && ires < nres) {
        DocEntry docen = ii.docEntries.get(idoc);
        ResultEntry resen = reslist.get(ires);

        if (docen.docid == resen.docid) {
          if (rankType == Util.TYPE_BM25) {
            double rsv = Util.getRSV(ii, docen, qtf);
            resen.score += rsv;
            
            mergeres.add(resen);
          }
          
          idoc++;
          ires++;
        } 
        else if (docen.docid < resen.docid) {
          if (rankType == Util.TYPE_BM25) {
            double rsv = Util.getRSV(ii, docen, qtf);
            mergeres.add(new ResultEntry(docen.docid, -1, rsv));
          }

          idoc++;
        } 
        else {
          if (rankType == Util.TYPE_BM25)
            mergeres.add(resen);
          
          ires++;
        }
      }
      
      // ii has elements left
      while (idoc < ndoc) {
        DocEntry docen = ii.docEntries.get(idoc);
        if (rankType == Util.TYPE_BM25) {
          double rsv = Util.getRSV(ii, docen, qtf);
          mergeres.add(new ResultEntry(docen.docid, -1, rsv));
        }
        idoc++;
      }
      // reslist has elements left
      while (ires < nres) {
        ResultEntry resen = reslist.get(ires);
        if (rankType == Util.TYPE_BM25)
          mergeres.add(resen);
        ires++;
      }
      
      // assign new list
      reslist = mergeres;
    }
  }
  
  public void sum(QueryResult qro, int rankType) {
    if (qro.reslist == null)
      return;

    // first time
    if (reslist == null) {
      reslist = qro.reslist;
    }
    // merge with other ii
    else {
      int io = 0, ires = 0, no = qro.reslist.size(), nres = reslist.size();
      ArrayList<ResultEntry> mergeres = new ArrayList<ResultEntry>();
      
      while (io < no && ires < nres) {
        ResultEntry oen = qro.reslist.get(io);
        ResultEntry resen = reslist.get(ires);
        
        if (oen.docid == resen.docid) {
          resen.score += oen.score;
          mergeres.add(resen);
          
          io++;
          ires++;
        } 
        else if (oen.docid < resen.docid) {
          mergeres.add(oen);
          io++;
        } else {
          mergeres.add(resen);
          ires++;
        }
      }

      // other qr has elements left
      while (io < no) {
        mergeres.add(qro.reslist.get(io));
        io++;
      }
      while(ires < nres) {
        mergeres.add(reslist.get(ires));
        ires++;
      }
      
      // assign new list
      reslist = mergeres;
    }
  }
  
  // like intersect
  public void combine(InvertedIndex ii, int rankType, int qlen) {
    if (ii == null)
      return;

    // first time
    if (reslist == null) {
      reslist = new ArrayList<ResultEntry>();

      Iterator<DocEntry> it = ii.docEntries.iterator();
      while (it.hasNext()) {
        DocEntry en = it.next();
        if (rankType == Util.TYPE_INDRI) {
          double Pqd = Util.getTwoStageSmoothScore(ii, en);
          reslist.add(new ResultEntry(en.docid, -1, Math.pow(Pqd, 1.0/qlen)));
        }
      }
    }
    // merge with other ii
    else {
      int idoc = 0, ires = 0, ndoc = ii.docEntries.size(), nres = reslist.size();
      ArrayList<ResultEntry> mergeres = new ArrayList<ResultEntry>();  // IMP: array list is much faster
      
      while (idoc < ndoc && ires < nres) {
        // IMP: linkedlist random access is slow!!!!
        DocEntry docen = ii.docEntries.get(idoc);
        ResultEntry resen = reslist.get(ires);
        
        if (docen.docid == resen.docid) {
          if (rankType == Util.TYPE_INDRI) {
            double tss = Util.getTwoStageSmoothScore(ii, docen);
            resen.score *= Math.pow(tss, 1.0/qlen);
            mergeres.add(resen);
          } 
          
          idoc++;
          ires++;
        } else if (docen.docid < resen.docid) {
          idoc++;
        }
        // delete results from reslist
        else {
          ires++;
        }
      }
      
      // assign new list
      reslist = mergeres;
    }
  }
  
  public void combine(QueryResult qro, int rankType, int qlen) {
    if (qro.reslist == null)
      return;

    // first time
    if (reslist == null) {
      reslist = qro.reslist;
    }
    // merge with other QueryResult
    else {
      int io = 0, ires = 0, no = qro.reslist.size(), nres = reslist.size();
      List<ResultEntry> mergeres = new ArrayList<ResultEntry>();  // IMP: array list is much faster
      
      while (io < no && ires < nres) {
        ResultEntry oen = qro.reslist.get(io);
        ResultEntry resen = reslist.get(ires);
        
        if (oen.docid == resen.docid) {
          if (rankType == Util.TYPE_INDRI) {
            resen.score += Math.pow(oen.score, 1.0/qlen);
            mergeres.add(resen);
          } 
          
          io++;
          ires++;
        } else if (oen.docid < resen.docid) {
          io++;
        }
        else {
          ires++;
        }
      }
      
      // assign new list
      reslist = mergeres;
    }
  }
  
//like intersect
 public void weight(InvertedIndex ii, int rankType, double normWeight) {
   if (ii == null)
     return;

   // first time
   if (reslist == null) {
     reslist = new ArrayList<ResultEntry>();

     Iterator<DocEntry> it = ii.docEntries.iterator();
     while (it.hasNext()) {
       DocEntry en = it.next();
       if (rankType == Util.TYPE_INDRI) {
         double Pqd = Util.getTwoStageSmoothScore(ii, en);
         reslist.add(new ResultEntry(en.docid, -1, Math.pow(Pqd, normWeight)));
       }
     }
   }
   // merge with other ii
   else {
     int idoc = 0, ires = 0, ndoc = ii.docEntries.size(), nres = reslist.size();
     ArrayList<ResultEntry> mergeres = new ArrayList<ResultEntry>();  // IMP: array list is much faster
     
     while (idoc < ndoc && ires < nres) {
       // IMP: linkedlist random access is slow!!!!
       DocEntry docen = ii.docEntries.get(idoc);
       ResultEntry resen = reslist.get(ires);
       
       if (docen.docid == resen.docid) {
         if (rankType == Util.TYPE_INDRI) {
           double tss = Util.getTwoStageSmoothScore(ii, docen);
           resen.score *= Math.pow(tss, normWeight);
           mergeres.add(resen);
         } 
         
         idoc++;
         ires++;
       } 
       else if (docen.docid < resen.docid) {
         idoc++;
       }
       // delete results from reslist
       else {
         ires++;
       }
     }
     
     // assign new list
     reslist = mergeres;
   }
 }
 
 public void weight(QueryResult qro, int rankType, double normWeight) {
   if (qro.reslist == null)
     return;

   // first time
   if (reslist == null) {
     reslist = qro.reslist;
   }
   // merge with other QueryResult
   else {
     int io = 0, ires = 0, no = qro.reslist.size(), nres = reslist.size();
     List<ResultEntry> mergeres = new ArrayList<ResultEntry>();  // IMP: array list is much faster
     
     while (io < no && ires < nres) {
       ResultEntry oen = qro.reslist.get(io);
       ResultEntry resen = reslist.get(ires);
       
       if (oen.docid == resen.docid) {
         if (rankType == Util.TYPE_INDRI) {
           resen.score += Math.pow(oen.score, normWeight);
           mergeres.add(resen);
         } 
         
         io++;
         ires++;
       } else if (oen.docid < resen.docid) {
         io++;
       }
       else {
         ires++;
       }
     }
     
     // assign new list
     reslist = mergeres;
   }
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
      int idoc = 0, ires = 0, ndoc = ii.docEntries.size(), nres = reslist.size();
      ArrayList<ResultEntry> mergeres = new ArrayList<ResultEntry>();  // IMP: array list is much faster
      
      while (idoc < ndoc && ires < nres) {
        // IMP: linkedlist random access is slow!!!!
        DocEntry docen = ii.docEntries.get(idoc);
        ResultEntry resen = reslist.get(ires);
        
        if (docen.docid == resen.docid) {
          if (rankType == Util.TYPE_RANKED) {
            if (resen.score > docen.tf)
              resen.score = docen.tf;
          } else if (rankType == Util.TYPE_UNRANKED) {
            if (resen.score > 1)
              resen.score = 1;
          }
          
          mergeres.add(resen);
          
          idoc++;
          ires++;
        } else if (docen.docid < resen.docid) {
          idoc++;
        }
        // delete results from reslist
        else {
          ires++;
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
    if (qro.reslist == null)
      return;

    // first time
    if (reslist == null) {
      reslist = qro.reslist;
    }
    // merge with other QueryResult
    else {
      int io = 0, ires = 0, no = qro.reslist.size(), nres = reslist.size();
      List<ResultEntry> mergeres = new ArrayList<ResultEntry>();  // IMP: array list is much faster
      
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
          mergeres.add(resen);
          
          io++;
          ires++;
        } else if (oen.docid < resen.docid) {
          io++;
        }
        // delete results from reslist
        else {
          ires++;
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
      int idoc = 0, ires = 0, ndoc = ii.docEntries.size(), nres = reslist.size();
      // IMP: linked list add is also slow!!!!
      ArrayList<ResultEntry> mergeres = new ArrayList<ResultEntry>();
      
      while (idoc < ndoc && ires < nres) {
        DocEntry docen = ii.docEntries.get(idoc);
        ResultEntry resen = reslist.get(ires);

        if (docen.docid == resen.docid) {
          if (rankType == Util.TYPE_RANKED) {
            if (resen.score < docen.tf)
              resen.score = docen.tf;
          } else if (rankType == Util.TYPE_UNRANKED) {
            if (resen.score < 1)
              resen.score = 1;
          }
          
          mergeres.add(resen);
          
          idoc++;
          ires++;
        } else if (docen.docid < resen.docid) {
          //assert docen.docid < reslist.get(ires).docid : docen.docid + " " + reslist.get(ires).docid;
          
          if (rankType == Util.TYPE_RANKED)
            mergeres.add(new ResultEntry(docen.docid, -1, docen.tf));
          else if (rankType == Util.TYPE_UNRANKED)
            mergeres.add(new ResultEntry(docen.docid, -1, 1));

          idoc++;
        } else {
          if (rankType == Util.TYPE_RANKED)
            mergeres.add(resen);
          else if (rankType == Util.TYPE_UNRANKED)
            mergeres.add(resen);
          
          ires++;
        }
      }
      
      
      // ii has elements left
      while (idoc < ndoc) {
        DocEntry docen = ii.docEntries.get(idoc);
        if (rankType == Util.TYPE_RANKED)
          mergeres.add(new ResultEntry(docen.docid, -1, docen.tf));
        else if (rankType == Util.TYPE_UNRANKED)
          mergeres.add(new ResultEntry(docen.docid, -1, 1));
        idoc++;
      }
      // reslist has elements left
      while (ires < nres) {
        ResultEntry resen = reslist.get(ires);
        if (rankType == Util.TYPE_RANKED)
          mergeres.add(resen);
        else if (rankType == Util.TYPE_UNRANKED)
          mergeres.add(resen);
        ires++;
      }
      
      // assign new list
      reslist = mergeres;
    }
    
  }

  
  /**
   * union with oter QueryResult
   * */
  public void union(QueryResult qro, int rankType) {
    if (qro.reslist == null)
      return;

    // first time
    if (reslist == null) {
      reslist = qro.reslist;
    }
    // merge with other ii
    else {
      int io = 0, ires = 0, no = qro.reslist.size(), nres = reslist.size();
      ArrayList<ResultEntry> mergeres = new ArrayList<ResultEntry>();
      
      
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
          
          mergeres.add(resen);
          
          io++;
          ires++;
        } else if (oen.docid < resen.docid) {
          //assert oen.docid < reslist.get(ires).docid : oen.docid + " " + reslist.get(ires).docid;
          if (rankType == Util.TYPE_RANKED) 
            mergeres.add(new ResultEntry(oen.docid, -1, oen.score));
          else if (rankType == Util.TYPE_UNRANKED)
            mergeres.add(new ResultEntry(oen.docid, -1, 1));

          io++;
        } else {
          mergeres.add(resen);
          
          ires++;
        }
      }

      // other qr has elements left
      while (io < no) {
        mergeres.add(qro.reslist.get(io));
        io++;
      }
      while(ires < nres) {
        mergeres.add(reslist.get(ires));
        ires++;
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
                .format("%d %s %d %d %f %s\n", qid, q0, en.docid, en.rank, en.score, runid));
      }

      bw.close();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }
  
//  public void serialize(String path) {
//    if (path == null || path.length() == 0)
//      return;
//
//    try {
//      BufferedWriter bw = new BufferedWriter(new FileWriter(path, true));
//
//      for (int i = 0; i < retSize && i < reslist.size(); i++) {
//        ResultEntry en = reslist.get(i);
//        bw.write(String
//                .format("%d %s %d %d %.1f %s\n", qid, q0, en.docid, en.rank, en.score, runid));
//      }
//
//      bw.close();
//    } catch (IOException e) {
//      // TODO Auto-generated catch block
//      e.printStackTrace();
//    }
//  }
  
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
