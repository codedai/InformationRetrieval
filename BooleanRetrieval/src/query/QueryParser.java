package query;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

/**
 * QueryParser: parse a query into a query tree
 * 
 * @author Zeyuan Li
 * */
public class QueryParser {
  
  private HashSet<String> typeset = new HashSet<String>();
  private HashSet<String> stopwords = new HashSet<String>();
  private static String stopPath = "data/stoplist.txt";
  
  public QueryParser() {
    typeset.add(Util.AND);
    typeset.add(Util.OR);
    typeset.add(Util.NEAR);
    
    readStopwords();
  }
  
  public void readStopwords() {
    try {
      BufferedReader br = new BufferedReader(new FileReader(stopPath));
      String line;
      
      while((line = br.readLine()) != null) 
        stopwords.add(line);
      
      br.close();
    } catch (FileNotFoundException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    
  }
  
  private String getQuery(String qori, String type, String field) {
    StringBuilder sb = new StringBuilder();
    sb.append(type + " ( ");
    String[] tks = qori.split("\\s+");
    for(String s : tks)
      if(!stopwords.contains(s))
        sb.append(s + "+" + field + " ");
    sb.append(")");
    
    return sb.toString();
  }
  
  @Deprecated
  private String getQuery(String qori, String type) {
    StringBuilder sb = new StringBuilder();
    sb.append(type + " ( ");
    String[] tks = qori.split("\\s+");
    for(String s : tks)
      sb.append(s + " ");
    sb.append(")");
    
    return sb.toString();
  }
  
  public String getQueryBOWOr(Query q) {
    return getQuery(q.qori, Util.OR, q.field);
  }
  
  public String getQueryBOWAnd(Query q) {
    return getQuery(q.qori, Util.AND, q.field);
  }
  
  public String getQueryNear(Query q, int winsize) {
    return getQuery(q.qori, Util.NEAR + "/" + winsize, q.field);
  }
  
  // TODO: test
  /*public String getQueryStructured(Query q) {
    return getQueryNear(q, 5);
  }*/
  
  public QueryTreeNode parseOr(Query q) {
    String qstr = getQueryBOWOr(q);
    return parse(qstr);
  }
  
  public QueryTreeNode parseAnd(Query q) {
    String qstr = getQueryBOWAnd(q);
    return parse(qstr);
  }
  
  public QueryTreeNode parseStructured(Query q) {
    return parse(q.qori);
  }
  
  public QueryTreeNode parse(Query q) {
    if(q.queryType == Util.TYPE_AND)
      return parseAnd(q);
    else if(q.queryType == Util.TYPE_OR)
      return parseOr(q);
    else
      return parseStructured(q);
  }
  
  /**
   * parse normalized query string (qstr).
   * */
  private QueryTreeNode parse(String qstr) {
    String[] tks = qstr.split("\\s+");
    LinkedList<QueryTreeNode> stk = new LinkedList<QueryTreeNode>();
    
    for(int i = 0; i < tks.length; i++) {
      if(tks[i].equals(")")) {
        List<QueryTreeNode> childlist = new LinkedList<QueryTreeNode>();
        while(!stk.peekLast().val.equals("("))
          childlist.add(0, stk.removeLast());
        stk.removeLast(); // pop (
        
        stk.peekLast().children = childlist;
        // set parent
        for(QueryTreeNode node : childlist)
          node.par = stk.peekLast();
      }
      else {
        QueryTreeNode node = new QueryTreeNode(tks[i]);
        stk.add(node);
      }
    }
    
    return stk.removeLast();
  }
  
  // test
  public void dfs(QueryTreeNode root) {
    if(root == null)  return;
    System.out.print(root.val + " ");
    
    for(QueryTreeNode child : root.children)
      dfs(child);
  }

}
