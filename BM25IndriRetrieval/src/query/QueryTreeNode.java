package query;

import invertidx.InvertedIndex;

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
  public InvertedIndex iicur; // only effective when node is #NEAR or #UW
  public double dftScore = 1.0; // default score
  
  public QueryTreeNode() {
    val = "";
    children = new LinkedList<QueryTreeNode>();
    par = null;
    weight = 0.0;
    iicur = null;
  }
  
  public QueryTreeNode(String val) {
    this.val = val;
    children = new LinkedList<QueryTreeNode>();
    par = null;
    weight = 0.0;
    iicur = null;
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
  
  public boolean isNearOrUW() {
    if(val.startsWith(Util.NEAR) || val.startsWith(Util.UW))
      return true;
    else
      return false;
  }
  
  
  public double getAllChildrenWeight() {
    double totalWeight = 0.0;
    for(QueryTreeNode node : children)
      totalWeight += node.weight;
    return totalWeight;
  }
  
}
