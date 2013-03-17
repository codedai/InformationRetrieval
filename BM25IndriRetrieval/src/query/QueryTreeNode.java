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
  public double weight;
  
  public QueryTreeNode() {
    val = "";
    children = new LinkedList<QueryTreeNode>();
    par = null;
    weight = 0.0;
  }
  
  public QueryTreeNode(String val) {
    this.val = val;
    children = new LinkedList<QueryTreeNode>();
    par = null;
    weight = 0.0;
  }
  
  public QueryTreeNode(String val, double w) {
    this.val = val;
    children = new LinkedList<QueryTreeNode>();
    par = null;
    weight = w;
  }
  
  public boolean isLeaf() {
    if(children.size() == 0)
      return true;
    else
      return false;
  }
  
  public int getNumLeafChildren() {
    if(isLeaf())
      return 1;
    else {
      int num = 0;
      for(QueryTreeNode node : children)
        num += node.getNumLeafChildren();
      return num;
    }
  }
  
  public double getAllChildrenWeight() {
    if(isLeaf())
      return weight;
    else {
      double totalWeight = 0.0;
      for(QueryTreeNode node : children)
        totalWeight += node.getAllChildrenWeight();
      return totalWeight;
    }
  }
  
}
