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
  
  public String getQueryBM25Sum(Query q) {
    return getQuery(q.qori, Util.SUM, q.field);
  }
  
  public String getQueryUW(Query q, int winsize) {
    return getQuery(q.qori, Util.UW + "/" + winsize, q.field);
  }
  
  public String getQueryIndriCombine(Query q) {
    return getQuery(q.qori, Util.ANDINDRI, q.field);
  }
  
  public String getQueryIndriWAnd(Query q) {
    return getQuery(q.qori, Util.WEIGHT, q.field);
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
  
  public QueryTreeNode parseSumBM25(Query q) {
    String qstr = getQueryBM25Sum(q);
    return parse(qstr);
  }
  
  public QueryTreeNode parseCombine(Query q) {
    String qstr = getQueryIndriCombine(q);
    return parse(qstr);
  }
  
  public QueryTreeNode parseOr(Query q) {
    String qstr = getQueryBOWOr(q);
    return parse(qstr);
  }
  
  public QueryTreeNode parseAnd(Query q) {
    String qstr = getQueryBOWAnd(q);
    return parse(qstr);
  }
  
  public QueryTreeNode parseStructured(Query q) {
    if(q.qori.contains("+"))
      return parse(q.qori);
    // structured query doesn't have field
    else {
      StringBuilder sb = new StringBuilder();
      String[] strs = q.qori.split("\\s+");
      for(String s : strs) { 
        if(!s.contains(".") && !s.equals("(") && !s.equals(")") && !s.startsWith("#"))
          sb.append(s + "+" + Util.FIELD_DEFAULT + " ");
        else
          sb.append(s + " ");
      }
      return parse(sb.toString());
    }
  }
  
  public QueryTreeNode parse(Query q) {
    if(q.queryType == Util.TYPE_AND)
      return parseAnd(q);
    else if(q.queryType == Util.TYPE_OR)
      return parseOr(q);
    else if(q.queryType == Util.TYPE_SUM)
      return parseSumBM25(q);
    else if(q.queryType == Util.TYPE_ANDINDRI)
      return parseCombine(q);
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
        QueryTreeNode node = null;
        if(isNumber(tks[i])) {
          node = new QueryTreeNode(tks[i+1], Double.parseDouble(tks[i]));
          i++;
        }
        else
          node = new QueryTreeNode(tks[i]);
        stk.add(node);
      }
    }
    
    return stk.removeLast();
  }
  
  // test
  public void dfs(QueryTreeNode root) {
    if(root == null)  return;
    if(root.weight - 0.0 < 1e-4)
      System.out.print(root.val + " ");
    else 
      System.out.print(root.val + "_" + root.weight + " ");
    
    for(QueryTreeNode child : root.children)
      dfs(child);
  }
  
  public boolean isNumber(String s) {
    // Start typing your C/C++ solution below
    // DO NOT write int main() function
    if (s == null && s.length() == 0)
      return false;

    int i = 0, n = s.length();
    while (i < n && s.charAt(i) == ' ')
      i++;

    if (i<n && (s.charAt(i) == '+' || s.charAt(i) == '-'))
      i++;

    boolean eAppear = false;
    boolean dotAppear = false;
    boolean firstPart = false;
    boolean secondPart = false;
    boolean spaceAppear = false;
    while (i < n) {
      char ch = s.charAt(i);
      if (ch == '.') {
        if (dotAppear || eAppear || spaceAppear)
          return false;
        else
          dotAppear = true;
      } 
      else if (ch == 'e' || ch == 'E') {
        if (eAppear || !firstPart || spaceAppear)
          return false;
        else
          eAppear = true;
      } 
      else if (ch >= '0' && ch <= '9') {
        if (spaceAppear)
          return false;

        if (!eAppear)
          firstPart = true;
        else
          secondPart = true;
      } 
      else if (ch == '+' || ch == '-') {
        if (spaceAppear)
          return false;

        if (!eAppear || !(s.charAt(i - 1) == 'e' || s.charAt(i - 1) == 'E'))
          return false;
      } 
      else if (ch == ' ')
        spaceAppear = true;
      else
        return false;

      i++;
    }

    if (!firstPart)
      return false;
    else if (eAppear && !secondPart)
      return false;
    else
      return true;
  }

}
