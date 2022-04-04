package br.com.wpivotto.filebox.index;

import java.io.Reader;
import java.util.Set;

import org.apache.lucene.analysis.util.CharTokenizer;
import org.apache.lucene.util.Version;

public class CustomTokenizer extends CharTokenizer {

	private Set<Integer> delimiters;
	
	public CustomTokenizer(Version matchVersion, Reader in, Set<Integer> delimiters) {
		super(matchVersion, in);
		this.delimiters = delimiters;
	}

	@Override
	protected boolean isTokenChar(int c) {
		if (delimiters != null && !delimiters.isEmpty()) {
			return !Character.isWhitespace(c) && !delimiters.contains(c);
		}
		return !Character.isWhitespace(c);
	}

}
