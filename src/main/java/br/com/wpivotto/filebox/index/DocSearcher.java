package br.com.wpivotto.filebox.index;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.sandbox.queries.regex.JavaUtilRegexCapabilities;
import org.apache.lucene.sandbox.queries.regex.RegexCapabilities;
import org.apache.lucene.sandbox.queries.regex.RegexQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.highlight.Formatter;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.InvalidTokenOffsetsException;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.SimpleHTMLFormatter;
import org.apache.lucene.search.highlight.SimpleSpanFragmenter;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.Version;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.multipdf.Splitter;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;

import br.com.wpivotto.filebox.pdf.Highlight;
import br.com.wpivotto.filebox.pdf.IndexEntry;
import br.com.wpivotto.filebox.pdf.PDFTextExtractor;
import br.com.wpivotto.filebox.pdf.PDFUtils;
import br.com.wpivotto.filebox.pdf.SearchMatch;
import br.com.wpivotto.filebox.pdf.SearchMatches;

@ApplicationScoped
public class DocSearcher {

  private static final int DEFAULT_RESULT_SIZE = 500;
  private Configs configs;
  private DocIndexer indexer;

  @Inject
  public DocSearcher(DocIndexer indexer, Configs configs) {
    this.indexer = indexer;
    this.configs = configs;
  }

  public SearchMatches search(String queryString) {

    List<SearchMatch> results = new ArrayList<SearchMatch>();

    IndexSearcher searcher = null;
    Analyzer analyzer = null;
    QueryParser parser = null;

    try {

      Directory dir = indexer.getIndexDir();
      searcher = new IndexSearcher(DirectoryReader.open(dir));
      analyzer = new CustomAnalyzer(configs);
      parser = new QueryParser(Version.LUCENE_45, IndexEntry.CONTENT, analyzer);

      Query query = parser.parse(escape(queryString));
      ScoreDoc[] queryResults = searcher.search(query, DEFAULT_RESULT_SIZE).scoreDocs;

      for (ScoreDoc hits : queryResults) {

        Document doc = searcher.doc(hits.doc);

        Path path = Paths.get(configs.getDocsFolder() + File.separator + doc.get(IndexEntry.PATH));

        SearchMatch result = new SearchMatch();
        result.setDocument(hits.doc);
        result.setPath(path.normalize().toString());
        result.setPage(doc.get(IndexEntry.PAGE));
        // result.setReference(doc.get(IndexEntry.REFERENCE));
        result.setTerm(queryString);
        result.setHtml(
            getHighlightedField(query, analyzer, IndexEntry.CONTENT, doc.get(IndexEntry.CONTENT)));
        results.add(result);
      }

      Collections.sort(results);

      return new SearchMatches(results);

    } catch (Exception e) {
      e.printStackTrace();
      return new SearchMatches();

    } finally {
      if (analyzer != null) analyzer.close();
    }
  }

  private String getHighlightedField(
      Query query, Analyzer analyzer, String fieldName, String fieldValue)
      throws IOException, InvalidTokenOffsetsException {
    Formatter formatter = new SimpleHTMLFormatter("<span class='MatchedText'>", "</span>");
    QueryScorer queryScorer = new QueryScorer(query, fieldName);
    Highlighter highlighter = new Highlighter(formatter, queryScorer);
    highlighter.setTextFragmenter(new SimpleSpanFragmenter(queryScorer, Integer.MAX_VALUE));
    highlighter.setMaxDocCharsToAnalyze(Integer.MAX_VALUE);
    return highlighter.getBestFragment(analyzer, fieldName, fieldValue);
  }

  public byte[] searchFor(String term) {

    try {
      SearchMatches matches = search(term);
      if (matches.found()) {
        matches.print();
        return Files.readAllBytes(merge(matches).toPath());
      } else {
        File file = PDFUtils.buildPDF("Term " + term + " not found");
        return Files.readAllBytes(file.toPath());
      }
    } catch (Exception e) {
      e.printStackTrace();
      File file = PDFUtils.toPDF(e);
      try {
        return Files.readAllBytes(file.toPath());
      } catch (IOException e1) {
        return null;
      }
    }
  }

  public Set<String> regexSearch(String regex) {

    Set<String> results = new TreeSet<>();

    IndexSearcher searcher = null;
    Analyzer analyzer = null;

    try {

      Directory dir = indexer.getIndexDir();
      searcher = new IndexSearcher(DirectoryReader.open(dir));
      analyzer = new CustomAnalyzer(configs);

      RegexQuery query = new RegexQuery(new Term(IndexEntry.CONTENT, regex));
      RegexCapabilities capability =
          new JavaUtilRegexCapabilities(
              JavaUtilRegexCapabilities.FLAG_CASE_INSENSITIVE
                  | JavaUtilRegexCapabilities.FLAG_DOTALL);
      query.setRegexImplementation(capability);
      query.setBoost(1.0f);
      ScoreDoc[] queryResults = searcher.search(query, Integer.MAX_VALUE).scoreDocs;

      for (ScoreDoc hits : queryResults) {
        Document doc = searcher.doc(hits.doc);
        SearchMatch result = new SearchMatch();
        result.setHtml(
            getHighlightedField(query, analyzer, IndexEntry.CONTENT, doc.get(IndexEntry.CONTENT)));
        results.addAll(result.getMatches(regex));
      }

    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      if (analyzer != null) analyzer.close();
    }

    return results;
  }

  private String escape(String s) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      if (c == '\\' || c == '+' || c == '-' || c == '!' || c == '\"' || c == '^' || c == '['
          || c == ']' || c == '{' || c == '}' || c == '~' || c == '*' || c == '?' || c == '|'
          || c == '&' || c == '/') {
        sb.append('\\');
      }
      sb.append(c);
    }
    return sb.toString();
  }

  public File merge(List<SearchMatch> matches) throws Exception {

    PDFMergerUtility merger = new PDFMergerUtility();

    File pdf = File.createTempFile("search", "pdf");

    merger.setDestinationFileName(pdf.getAbsolutePath());

    int i = 0;

    for (SearchMatch match : matches) {

      PDDocument document = match.toPDF();

      Splitter splitter = new Splitter();
      splitter.setStartPage(match.getPageNo());
      splitter.setEndPage(match.getPageNo());

      for (PDDocument part : splitter.split(document)) {
        File tempFile = File.createTempFile("part_" + i, "pdf");
        tempFile.deleteOnExit();
        part.save(tempFile);
        merger.addSource(tempFile);
        i++;
      }

      document.close();
    }

    merger.mergeDocuments();

    return pdf;
  }

  private File merge(SearchMatches matches) throws Exception {

    PDFMergerUtility merger = new PDFMergerUtility();

    File pdf = File.createTempFile("search", "pdf");

    merger.setDestinationFileName(pdf.getAbsolutePath());

    int i = 0;

    List<SearchMatch> values = matches.groupedByPage();

    for (SearchMatch match : values) {

      PDDocument document = match.toPDF();

      Splitter splitter = new Splitter();
      splitter.setStartPage(match.getPageNo());
      splitter.setEndPage(match.getPageNo());

      for (PDDocument part : splitter.split(document)) {
        File tempFile = File.createTempFile("part_" + i, "pdf");
        tempFile.deleteOnExit();
        if (configs.highlightEnabled()) {
          highlight(part, match.getMatches());
        }
        part.save(tempFile);
        merger.addSource(tempFile);
        i++;
      }

      document.close();
    }

    merger.mergeDocuments();

    return pdf;
  }

  private void highlight(PDDocument document, Set<String> terms) throws IOException {

    PDFTextExtractor extractor = new PDFTextExtractor();
    Set<Highlight> highlights = extractor.getHighlights(terms, document);
    for (Highlight highlight : highlights) {
      List<PDAnnotation> annotations = document.getPage(highlight.getPageNo() - 1).getAnnotations();
      annotations.add(highlight.getAnnotation());
    }
  }
}
