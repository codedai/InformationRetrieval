package invertidx;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeSet;

import query.QueryResult;
import query.ResultEntry;
import query.Util;

/**
 * InvertedIndex: class for inverted index (posting lists)
 * 
 * @author Zeyuan Li
 * */
public class InvertedIndex {

  // public String path;
  public String term;

  public String termStem;

  public double ctf; // collection term frequency

  public double ttc; // total term count

  public List<DocEntry> docEntries; // a list of doc entries

  public InvertedIndex() {
    docEntries = new LinkedList<DocEntry>();
  }

  public InvertedIndex(String path) {
    docEntries = new LinkedList<DocEntry>();
    readInvertedIndex(path);
  }

  public void readInvertedIndex(String path) {
    try {
      BufferedReader br = new BufferedReader(new FileReader(path));
      String line = br.readLine();
      String[] strs = line.split("\\s+");
      if(strs.length != 4)  // bad line
        return;
      
      term = strs[0];
      termStem = strs[1];
      ctf = Double.parseDouble(strs[2]);

      assert strs.length == 4 : path + " " + line;
      ttc = Double.parseDouble(strs[3]);

      while ((line = br.readLine()) != null) {
        strs = line.split("\\s+");
        ArrayList<Integer> poslist = new ArrayList<Integer>();

        for (int i = 3; i < strs.length; i++)
          poslist.add(Integer.parseInt(strs[i]));

        DocEntry de = new DocEntry(Integer.parseInt(strs[0]), Integer.parseInt(strs[1]),
                Integer.parseInt(strs[2]), poslist);
        docEntries.add(de);
      }
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }


  /**
   * NEAR/k query
   * */
  public void near(InvertedIndex ii, int k, int rankType) {
    if (ii == null)
      return;

    Iterator<DocEntry> ito = ii.docEntries.iterator();
    Iterator<DocEntry> itdoc = docEntries.iterator();
    DocEntry en = null, eno = null;
    List<DocEntry> mergeres = new ArrayList<DocEntry>();
    
    while (ito.hasNext() && itdoc.hasNext()) {
      if(en == null && eno == null) {
        en = itdoc.next();
        eno = ito.next();
      }

      if (en.docid == eno.docid) {
        List<Integer> p1 = en.pos, p2 = eno.pos;
        List<Integer> pres = new ArrayList<Integer>(); // possible result positions
        
        // near score is the freq of doc that match the NEAR operator
        for (int j1 = 0; j1 < p1.size(); j1++) {
          for (int j2 = 0; j2 < p2.size(); j2++) {
            if (p2.get(j2) - p1.get(j1) <= j2 && p2.get(j2) - p1.get(j1) >= 0) {
              // contains duplicates for weighting (cnt can propagate through the last query term)
              //// save distinct posid but cnt should be total # of matches 
              pres.add(p2.get(j2)); 
            } 
            else if (p2.get(j2) - p1.get(j1) > j2)
              break;
          }
        }
        
        if(pres.size() > 0) {
          en.pos = pres;
          if(rankType == Util.TYPE_RANKED)
            en.tf = pres.size();
          else 
            en.tf = 1;
          mergeres.add(en);
        }
        
        en = itdoc.next();
        eno = ito.next();
      } else if (en.docid < eno.docid) {
        en = itdoc.next();
      } else
        eno = ito.next();
    }// end while
    
    // assign new list
    docEntries = mergeres;
  }

}
