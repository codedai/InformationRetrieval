package query;

import java.util.LinkedList;
import java.util.List;

/**
 * QueryTreeNode: TreeNode for query tree
 * 
 * @author Zeyuan Li
 * */
public class QueryTreeNode {
  
  public String val;
  public List<QueryTreeNode> children;
  public QueryTreeNode par;
  
  
  public QueryTreeNode() {
    val = "";
    children = new LinkedList<QueryTreeNode>();
    par = null;
  }
  
  public QueryTreeNode(String val) {
    this.val = val;
    children = new LinkedList<QueryTreeNode>();
    par = null;
  }
  
  public boolean isLeaf() {
    if(!val.equals(Util.AND) && !val.equals(Util.OR) && !val.startsWith(Util.NEAR))
      return true;
    else
      return false;
  }
  
}
