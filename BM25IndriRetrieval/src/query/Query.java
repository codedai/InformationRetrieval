package query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Query: place holder for all the information related to a query
 * 
 * @author Zeyuan Li
 * */
public class Query {
  
  public int qid;   // query id
  public int rankType;  // rank type
  public int queryType;  // query type
  public String qori; // query original string
  public String field;  //query field
  private QueryTreeNode root;  // query tree root
  public HashMap<String, Integer> qtfmap;
  

  public Query(String qline, int rankType, int queryType) {  // input string line 
    String[] strs = qline.split(":");
    qid = Integer.parseInt(strs[0]);
    qori = strs[1];
    field = Util.FIELD_DEFAULT;
    this.rankType = rankType;
    this.queryType = queryType;
    
    // build query term frequency
    qtfmap = new HashMap<String, Integer>();
    String[] terms = qori.split("\\s+");
    for(String t : terms) {
      if(qtfmap.containsKey(t))
        qtfmap.put(t, qtfmap.get(t) + 1);
      else
        qtfmap.put(t, 1);
    }
  }
  
  public QueryTreeNode getRoot() {
    return root;
  }

  public void setRoot(QueryTreeNode root) {
    this.root = root;
  }
  
  
}
