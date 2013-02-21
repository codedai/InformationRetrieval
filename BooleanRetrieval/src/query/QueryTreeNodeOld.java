package query;

import java.util.LinkedList;
import java.util.List;

public class QueryTreeNodeOld {

  public static final int TYPE_LEAF = 0;
  public static final int TYPE_AND = 1;
  public static final int TYPE_OR = 2;
  public static final int TYPE_NEAR = 3;
  
  public int type;
  public String val;
  public List<QueryTreeNodeOld> children;
  
  
  public QueryTreeNodeOld() {
    type = TYPE_LEAF;
    val = "";
    children = new LinkedList<QueryTreeNodeOld>();
  }
  
  public QueryTreeNodeOld(String val) {
    this.type = TYPE_LEAF;
    this.val = val;
    children = new LinkedList<QueryTreeNodeOld>();
  }
  
  public QueryTreeNodeOld(int type) {
    this.type = type;
    this.val = "";
    children = new LinkedList<QueryTreeNodeOld>();
  }
  
}
