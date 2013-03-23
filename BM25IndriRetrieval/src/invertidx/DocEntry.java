package invertidx;

import java.util.List;

/**
 * DocEntry: document entry fields contained in inverted list
 * 
 * @author Zeyuan Li
 * */
public class DocEntry {
  
  public int docid;
  public int tf;
  public int doclen;
  public List<Integer> pos;
  
  public DocEntry() {}
  
  public DocEntry(int docid, int tf, int doclen, List<Integer> pos) {
    this.docid = docid;
    this.tf = tf;
    this.doclen = doclen;
    this.pos = pos;
  }

}
