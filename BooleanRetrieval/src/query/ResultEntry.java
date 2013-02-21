package query;

public class ResultEntry implements Comparable {

  public int docid;
  public int rank;
  public double score;
  
  public ResultEntry(int docid, int rank, double score) {
    this.docid = docid;
    this.rank = rank;
    this.score = score;
  }

  @Override
  public int compareTo(Object arg0) {
    ResultEntry re = (ResultEntry)arg0;
    if(Double.compare(re.score, score) == 0) {
      if(docid < re.docid)  return -1;
      else if(docid == re.docid)  return 0;
      else  return 1;
    }

    // decreasing order
    return Double.compare(re.score, score);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + docid;
    result = prime * result + rank;
    long temp;
    temp = Double.doubleToLongBits(score);
    result = prime * result + (int) (temp ^ (temp >>> 32));
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    ResultEntry other = (ResultEntry) obj;
    if (docid != other.docid)
      return false;
    if (rank != other.rank)
      return false;
    if (Double.doubleToLongBits(score) != Double.doubleToLongBits(other.score))
      return false;
    return true;
  }

  
  
}
