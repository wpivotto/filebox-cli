package br.com.wpivotto.filebox.pdf;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.io.IOUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.apache.pdfbox.util.Matrix;

public class PDFTextExtractor {

	public String getText(PDDocument document, int page) throws IOException {

		Writer output = null;
		try {

			output = new StringWriter(); 
			
			int startPage = page;
			int endPage = Math.min(page, document.getNumberOfPages());

			FilteredTextStripper stripper = new FilteredTextStripper(null, null);
			stripper.setSortByPosition(false);
			stripper.setShouldSeparateByBeads(true);

			extractPages(startPage, endPage, stripper, document, output);
			
			return output.toString();

		} finally {
			IOUtils.closeQuietly(output);
		}

	}
	
	public Set<Highlight> getHighlights(Set<String> terms, PDDocument document) throws IOException {

		Writer output = null;
		Set<Highlight> highlights = new HashSet<>();
		
		try {

			output = new StringWriter(); 
			FilteredTextStripper stripper = new FilteredTextStripper(terms, highlights);
			stripper.setSortByPosition(false);
			stripper.setShouldSeparateByBeads(true);
			
			int i = 1;
			for (PDPage page : document.getPages()) {
				int startPage = i;
				int endPage = Math.min(i, document.getNumberOfPages());
				extractPages(startPage, endPage, stripper, document, output);
				i++;	
			}
			
			return highlights;

		} finally {
			IOUtils.closeQuietly(output);
		}

	}

	private void extractPages(int startPage, int endPage, PDFTextStripper stripper, PDDocument document, Writer output) {
		for (int p = startPage; p <= endPage; ++p) {
			stripper.setStartPage(p);
			stripper.setEndPage(p);
			try {
				PDPage page = document.getPage(p - 1);
				int rotation = page.getRotation();
				page.setRotation(0);
				AngleCollector angleCollector = new AngleCollector();
				angleCollector.setStartPage(p);
				angleCollector.setEndPage(p);
				angleCollector.writeText(document, new NullWriter());
				for (int angle : angleCollector.getAngles()) {
		
					PDPageContentStream cs = new PDPageContentStream(document, page, PDPageContentStream.AppendMode.PREPEND, false);
					cs.transform(Matrix.getRotateInstance(-Math.toRadians(angle), 0, 0));
					cs.close();
					stripper.writeText(document, output);
					((COSArray) page.getCOSObject().getItem(COSName.CONTENTS)).remove(0);
				}
				page.setRotation(rotation); 
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
	}

}

/**
 * Collect all angles while doing text extraction. Angles are in degrees and
 * rounded to the closest integer (to avoid slight differences from floating
 * point arithmethic resulting in similarly angled glyphs being treated
 * separately). This class must be constructed for each page so that the angle
 * set is initialized.
 * Source: https://github.com/topobyte/pdfbox-tools/blob/master/src/main/java/org/apache/pdfbox/tools/ExtractText.java
 */
class AngleCollector extends PDFTextStripper {
	private final Set<Integer> angles = new TreeSet<Integer>();

	AngleCollector() throws IOException {
	}

	Set<Integer> getAngles() {
		return angles;
	}

	@Override
	protected void processTextPosition(TextPosition text) {
		Matrix m = text.getTextMatrix();
		m.concatenate(text.getFont().getFontMatrix());
		int angle = (int) Math.round(Math.toDegrees(Math.atan2(m.getShearY(), m.getScaleY())));
		angle = (angle + 360) % 360;
		angles.add(angle);
	}
}

/**
 * TextStripper that only processes glyphs that have angle 0.
 */
class FilteredTextStripper extends PDFTextStripper {
	
	Set<String> terms;
	Set<Highlight> highlights;
	
	FilteredTextStripper(Set<String> terms, Set<Highlight> highlights) throws IOException {
		this.terms = terms;
		this.highlights = highlights;
	}

	@Override
	protected void processTextPosition(TextPosition text) {
		Matrix m = text.getTextMatrix();
		m.concatenate(text.getFont().getFontMatrix());
		int angle = (int) Math.round(Math.toDegrees(Math.atan2(m.getShearY(), m.getScaleY())));
		if (angle == 0) {
			super.processTextPosition(text);
		}
	}
	
	@Override
	protected void writeString(String string, List<TextPosition> textPositions) throws IOException {
		super.writeString(string, textPositions);
		if (terms != null) {
			for (String term : terms) {
				if (term != null) {
					//System.out.println("Processando: " + string);
					String[] wordsInStream = string.split(" ");
					if (wordsInStream != null) {
						for (String word : wordsInStream) {
							if (!word.trim().equals("") && word.contains(term)) {
								Map<Integer, List<TextPosition>> positions = extractPositions(word, textPositions);
								for (List<TextPosition> pos : positions.values()) {
									highlights.add(new Highlight(word, pos, getCurrentPage(), getCurrentPageNo()));
								}
							}
						}
					}
				}
			}
		}
	}
	
	//pode encontrar a mesma word varias vezes na mesma linha, extrai a posição de cada ocorrência
	private Map<Integer, List<TextPosition>> extractPositions(String word, List<TextPosition> textPositions) {
		Map<Integer, List<TextPosition>> map = new TreeMap<>();
		List<TextPosition> positions = new ArrayList<TextPosition>();
		List<TextPosition> temp = new ArrayList<TextPosition>();
		String candidate = "";
		int index = 0; 
		for (TextPosition text : textPositions) {
			if (text.getUnicode().equals(" ")) {
				if (candidate.equals(word)) {
					positions.addAll(temp);
					map.put(index, positions);
					positions = new ArrayList<TextPosition>(); 
					candidate = "";
					temp.clear();
					index++;
				} else {
					candidate = "";
					temp.clear();
				}
			} else {
				candidate += text.getUnicode();
				temp.add(text);
			}
		}
		
		if (candidate.equals(word)) {
			positions.addAll(temp);
			map.put(index, positions);
		} 
		
		return map;
	}
	
}

/**
 * Dummy output.
 */
class NullWriter extends Writer {
	@Override
	public void write(char[] cbuf, int off, int len) throws IOException {
		// do nothing
	}

	@Override
	public void flush() throws IOException {
		// do nothing
	}

	@Override
	public void close() throws IOException {
		// do nothing
	}
}
