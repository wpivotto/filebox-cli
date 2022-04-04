package br.com.wpivotto.filebox.index;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;

import com.google.common.io.Closeables;
import com.google.common.io.Flushables;

import br.com.wpivotto.filebox.pdf.PDFTextExtractor;

@ApplicationScoped
public class ContentExtractor {
	
	
	public ByteArrayInputStream getContent(File file) throws IOException {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		FileInputStream input = new FileInputStream(file);
		IOUtils.copy(input, output);
		Flushables.flushQuietly(output);
		Closeables.closeQuietly(input);
		return new ByteArrayInputStream(output.toByteArray());
	}
	
	private boolean isPDF(InputStream input) throws IOException {
		String mimetype = new Tika().detect(input);
		return mimetype.equals("application/pdf");
	}
	
	public List<String> getText(InputStream input) throws Exception {
		if (isPDF(input))
			return extractPDFContent(input);
		else
			return extractContent(input);
	}
	
	private List<String> extractContent(InputStream input) throws IOException, TikaException {
		List<String> content = new ArrayList<String>();
		String text = new Tika().parseToString(input);
		content.add(text);
		return content;
	}

	private List<String> extractPDFContent(InputStream input) {

		PDDocument document = null;
		List<String> content = new ArrayList<String>();
		
		try {
			
			document = PDDocument.load(input);
			int pageSize = document.getNumberOfPages();

			for (int pageNo = 0; pageNo < pageSize; pageNo++) {
				PDFTextExtractor reader = new PDFTextExtractor();
				String text = reader.getText(document, pageNo + 1);
				content.add(text);
			}
		
			return content;

		} catch (Exception e) {
			e.printStackTrace();
			return new ArrayList<String>();
		} finally {
			try {
				if (document != null) document.close();
			} catch (IOException e) {}
		}

	}


}
