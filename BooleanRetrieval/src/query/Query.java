package query;

import java.util.ArrayList;
import java.util.List;

public class Query {
  
  public int qid;   // query id
  public int rankType;  // rank type
  public int queryType;  // query type
  public String qori; // query original string
  public String field;  //query field
  private QueryTreeNode root;  // query tree root

  public Query(String qline) {  // input string line 
    String[] strs = qline.split(":");
    qid = Integer.parseInt(strs[0]);
    qori = strs[1];
    field = Util.FIELD_BODY;
    rankType = Util.TYPE_RANKED;
    queryType = Util.TYPE_AND;
  }
  
  public Query(String qline, int rankType, int queryType) {  // input string line 
    String[] strs = qline.split(":");
    qid = Integer.parseInt(strs[0]);
    qori = strs[1];
    field = Util.FIELD_BODY;
    this.rankType = rankType;
    this.queryType = queryType;
  }
  
  public Query(int qid, String qori, String field) {
    this.qid = qid;
    this.qori = qori;
    this.field = field;
    rankType = Util.TYPE_RANKED;
    queryType = Util.TYPE_AND;
  }
  
  // get multiple queries
  public List<Query> splitBigramQuery() {
    List<Query> qlist = new ArrayList<Query>();
    String[] qs = qori.split("\\s+");
    
    if(qs.length <= 1) {
      qlist.add(this);
      return qlist;
    }
    
    for(int i = 0; i < qs.length-1; i++) {
      String prefix = qid + ":";
      String qline = prefix + qs[i] + " " + qs[i+1];
      Query q = new Query(qline, rankType, queryType);
      qlist.add(q);
    }
    
    return qlist;
  }

  public QueryTreeNode getRoot() {
    return root;
  }

  public void setRoot(QueryTreeNode root) {
    this.root = root;
  }
  
  
}
