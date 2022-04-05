package br.com.wpivotto.filebox.index;

import java.io.Reader;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.pattern.PatternReplaceCharFilter;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.util.Version;

public class CustomAnalyzer extends Analyzer {

  private CharArraySet stopWords =
      CharArraySet.copy(Version.LUCENE_45, StandardAnalyzer.STOP_WORDS_SET);
  private Set<Integer> delimiters = new HashSet<>();

  public CustomAnalyzer(Configs configs) {
    for (String word : configs.getStopwords()) {
      stopWords.add(word);
    }
    delimiters.addAll(configs.getCustomDelimiters());
  }

  @Override
  protected TokenStreamComponents createComponents(String fieldName, Reader reader) {
    Tokenizer tokenizer = new CustomTokenizer(Version.LUCENE_45, reader, delimiters);
    TokenStream filter = new LowerCaseFilter(Version.LUCENE_45, tokenizer);
    filter = new StopFilter(Version.LUCENE_45, filter, stopWords);
    return new TokenStreamComponents(tokenizer, filter);
  }

  @Override
  protected Reader initReader(String fieldName, Reader reader) {
    CharFilter cf = new PatternReplaceCharFilter(Pattern.compile("\\["), "", reader);
    cf = new PatternReplaceCharFilter(Pattern.compile("\\]"), "", cf);
    cf = new PatternReplaceCharFilter(Pattern.compile("\\)"), "", cf);
    cf = new PatternReplaceCharFilter(Pattern.compile("\\("), "", cf);
    return cf;
  }
}
