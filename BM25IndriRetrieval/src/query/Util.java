package query;

import invertidx.DocEntry;
import invertidx.InvertedIndex;

/**
 * Util: Utillity class and constant defination
 * 
 * @author Zeyuan Li
 * */
public class Util {
  public static final String AND = "#AND";
  public static final String OR = "#OR";
  public static final String NEAR = "#NEAR";
  public static final String SUM = "#SUM";
  public static final String UW = "#UW";
  public static final String ANDINDRI = "#AND";
  public static final String WEIGHT = "#WEIGHT";
  
  public static final String FIELD_TITLE = "title";
  public static final String FIELD_BODY = "body";
  public static final String FIELD_INLINK = "inlink";
  public static final String FIELD_URL = "url";
  public static final String FIELD_DEFAULT = "default";
  
  public static final String IISuffix_TITLE = ".title.inv";
  public static final String IISuffix_INLINK = ".anchor.inv";
  public static final String IISuffix_BODY = ".body.inv";
  public static final String IISuffix_URL = ".url.inv";
  
  public static final int TYPE_UNRANKED = 10;
  public static final int TYPE_RANKED = 11;
  
  public static final int TYPE_ORRANK = 20;
  public static final int TYPE_BM25 = 21;
  public static final int TYPE_INDRI = 22;
  
  public static final int TYPE_OR = 0;
  public static final int TYPE_AND = 1;
  public static final int TYPE_STRUCTURE = 2;
  public static final int TYPE_SUM = 3;
  public static final int TYPE_ANDINDRI = 4;
  public static final int TYPE_WEIGHT = 5;
  
  public static final int OFFSET = 20;
  
  public static final double avg_doc_size = 1301.0;
  public static final double voca_size = 4073034.0;
  public static final double total_doc_size = 890630.0;
  public static final double total_word_size = 1158815080.0;
  
  // in two stage smoothing
  public static double mu=2500, lambda=0.4;
  //public static double mu=2500, lambda=0.6;
  
  public static double getRSV(InvertedIndex ii, DocEntry en, int qtf) {
    double k1=1.2, b=0.75, k3=0;
    //double k1=1.2, b=0.75, k3=-0.8;
    
    double df = ii.df;
    int tf = en.tf;
    
    double idfScore = Math.log((total_doc_size-df+0.5) / (df+0.5));
    double dfScore = tf / (tf + k1*((1-b) + b*en.doclen/avg_doc_size) );
    double userScore = ((k3+1)*qtf) / (k3+qtf);
    
    return idfScore * dfScore * userScore;
  }
  
  public static double getTwoStageSmoothScore(InvertedIndex ii, DocEntry en) {
    double tf = en.tf;
    // defalut Pqc
    double Pqc = ii.ctf / total_word_size;
    // alternative Pqc
    //double Pqc = ii.df / total_doc_size;
    
    double dirprior = lambda * (tf + mu*Pqc) / (en.doclen+mu);  //tf
    double mixture = (1-lambda) * Pqc;  //idf
    
    return dirprior + mixture;
  }
  
  // assigne default score
  public static double getTwoStageSmoothScoreDefault(double ctf) {
    double tf = 0;
    // defalut Pqc
    double Pqc = ctf / total_word_size;
    // alternative Pqc
    //double Pqc = ii.df / total_doc_size;
    
    double dirprior = lambda * (tf + mu*Pqc) / (avg_doc_size+mu);
    double mixture = (1-lambda) * Pqc;
    
    return dirprior + mixture;
  }
}
