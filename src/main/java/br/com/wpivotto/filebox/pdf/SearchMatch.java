package br.com.wpivotto.filebox.pdf;

import java.io.File;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FilenameUtils;
import org.apache.pdfbox.pdmodel.PDDocument;

public class SearchMatch implements Comparable<SearchMatch> {

  private Integer document;
  private String path;
  private String page;
  private String term;
  private String html;

  public int getDocument() {
    return document;
  }

  public void setDocument(Integer document) {
    this.document = document;
  }

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public String getPage() {
    return page;
  }

  public int getPageNo() {
    return Integer.parseInt(page);
  }

  public void setPage(String page) {
    this.page = page;
  }

  public String getTerm() {
    return term;
  }

  public void setTerm(String term) {
    this.term = term;
  }

  public String getHtml() {
    return html;
  }

  public void setHtml(String html) {
    this.html = html;
  }

  public String getFiletype() {
    return FilenameUtils.getExtension(path);
  }

  public Set<String> getMatches() {

    Pattern pattern =
        Pattern.compile("<span class='MatchedText'>(.*?)<\\/span>", Pattern.MULTILINE);
    Matcher matcher = pattern.matcher(html);
    Set<String> matches = new HashSet<>();

    while (matcher.find()) {
      matches.add(matcher.group(1));
    }

    return matches;
  }

  public Set<String> getMatches(String regex) {

    Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    Set<String> matches = getMatches();
    Set<String> results = new HashSet<>();

    for (String match : matches) {
      try {
        Matcher matcher = pattern.matcher(match);
        while (matcher.find()) {
          results.add(matcher.group());
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    return results;
  }

  public boolean isPDF() {
    return "pdf".equalsIgnoreCase(getFiletype());
  }

  @Override
  public String toString() {
    return "Found [term = " + term + ", path = " + path + ", page = " + page + "]";
  }

  public int compareTo(SearchMatch other) {
    return Comparator.comparing(SearchMatch::getPath)
        .thenComparing(SearchMatch::getPage)
        .thenComparing(SearchMatch::getTerm)
        .compare(this, other);
  }

  public boolean match(Integer page) {
    return this.page.equals(page + 1);
  }

  public String getBookmark() {
    // return getTerm() + "_Page_" + page;
    return "Pag_" + page;
  }

  public File getFile() {
    return new File(path);
  }

  public String getFilename() {
    return getFile().getName();
  }

  public PDDocument toPDF() throws Exception {
    File file = PDFUtils.buildPDF(this);
    return PDDocument.load(file);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((page == null) ? 0 : page.hashCode());
    result = prime * result + ((path == null) ? 0 : path.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    SearchMatch other = (SearchMatch) obj;
    if (page == null) {
      if (other.page != null) return false;
    } else if (!page.equals(other.page)) return false;
    if (path == null) {
      if (other.path != null) return false;
    } else if (!path.equals(other.path)) return false;
    return true;
  }
}
