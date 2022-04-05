package br.com.wpivotto.filebox.pdf;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.pdfbox.io.IOUtils;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.multipdf.Splitter;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.encoding.WinAnsiEncoding;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;

public class PDFUtils {

  private static final int MAX_CHARS = 4700;

  public static File extractPage(String filename, int page) throws Exception {

    PDDocument document = null;

    try {

      PDFMergerUtility merger = new PDFMergerUtility();
      File pdf = File.createTempFile("search", "pdf");
      merger.setDestinationFileName(pdf.getAbsolutePath());
      File file = new File(filename);
      Splitter splitter = new Splitter();
      splitter.setStartPage(page);
      splitter.setEndPage(page);
      document = PDDocument.load(file);

      for (PDDocument part : splitter.split(document)) {
        File tempFile = File.createTempFile("part", "pdf");
        tempFile.deleteOnExit();
        part.save(tempFile);
        merger.addSource(tempFile);
      }

      document.close();
      merger.mergeDocuments();

      return pdf;

    } catch (Exception e) {
      throw e;
    } finally {
      IOUtils.closeQuietly(document);
    }
  }

  public static File getPage(String doc, int page) {
    try {
      return extractPage(doc, page);
    } catch (Exception e) {
      e.printStackTrace();
      return toPDF(e);
    }
  }

  public static File buildPDF(String content) {

    PDDocument document = null;

    try {

      File pdf = File.createTempFile("empty", "pdf");
      document = new PDDocument();
      PDPage page = new PDPage();
      document.addPage(page);
      PDPageContentStream contentStream = new PDPageContentStream(document, page);

      int fontSize = 10;
      PDRectangle mediaBox = page.getMediaBox();
      float marginY = 80;
      float marginX = 60;
      float width = mediaBox.getWidth() - 2 * marginX;
      float x = mediaBox.getLowerLeftX() + marginX;
      float y = mediaBox.getUpperRightY() - marginY;

      contentStream.beginText();
      addParagraph(
          content, x, y, width, page, contentStream, PDType1Font.HELVETICA, fontSize, true);
      contentStream.endText();

      contentStream.close();
      document.save(pdf);
      document.close();

      return pdf;

    } catch (Exception e) {
      e.printStackTrace();
      return null;
    } finally {
      IOUtils.closeQuietly(document);
    }
  }

  public static File buildPDF(SearchMatch match) {

    PDDocument document = null;

    try {

      if (match.isPDF()) return match.getFile();

      document = new PDDocument();

      File pdf = File.createTempFile("empty", "pdf");
      PDPage page = new PDPage();
      document.addPage(page);
      PDPageContentStream contentStream = new PDPageContentStream(document, page);

      int fontSize = 10;
      PDRectangle mediaBox = page.getMediaBox();
      float marginY = 80;
      float marginX = 60;
      float width = mediaBox.getWidth() - 2 * marginX;
      float x = mediaBox.getLowerLeftX() + marginX;
      float y = mediaBox.getUpperRightY() - marginY;

      String html = match.getHtml();
      String content = removeHighlight(html);
      int len = html.length();

      if (len >= MAX_CHARS) {
        int pos = getHighlightPosition(html);
        int start = Math.max((pos - (MAX_CHARS / 2)), 0);
        int end = Math.min((pos + (MAX_CHARS / 2)), (len - 1));
        content = StringUtils.substring(content, start, end);
        content = "..." + content + "...";
      }

      contentStream.beginText();
      addParagraph(
          match.getFilename(),
          x,
          y,
          width,
          page,
          contentStream,
          PDType1Font.HELVETICA_BOLD,
          fontSize,
          true);
      addParagraph(
          content,
          0f,
          -fontSize,
          width,
          page,
          contentStream,
          PDType1Font.HELVETICA,
          fontSize,
          true);
      contentStream.endText();
      contentStream.close();

      document.save(pdf);

      return pdf;

    } catch (Exception e) {
      e.printStackTrace();
      return null;
    } finally {
      IOUtils.closeQuietly(document);
    }
  }

  private static String removeHighlight(String html) {

    String content = html;

    Pattern pattern =
        Pattern.compile("<span class='MatchedText'>(.*?)<\\/span>", Pattern.MULTILINE);
    Matcher matcher = pattern.matcher(content);

    while (matcher.find()) {
      content = content.replace(matcher.group(0), matcher.group(1));
    }

    return content;
  }

  private static int getHighlightPosition(String html) {

    Pattern pattern =
        Pattern.compile("<span class='MatchedText'>(.*?)<\\/span>", Pattern.MULTILINE);
    Matcher matcher = pattern.matcher(html);

    while (matcher.find()) {
      return matcher.start(0);
    }

    return 0;
  }

  public static File toPDF(Throwable e) {
    return buildPDF(getRootCause(e));
  }

  private static String getRootCause(Throwable throwable) {
    String[] causes = ExceptionUtils.getRootCauseStackTrace(throwable);
    StringBuffer erro = new StringBuffer();
    for (String cause : causes) {
      erro.append(cause).append("\n");
    }
    return erro.toString();
  }

  public static BufferedImage getPNG(PDDocument document, int page) throws IOException {
    PDFRenderer renderer = new PDFRenderer(document);
    return renderer.renderImageWithDPI(page - 1, 300, ImageType.RGB);
  }

  private static void addParagraph(
      String text,
      float x,
      float y,
      float width,
      PDPage page,
      PDPageContentStream contentStream,
      PDFont font,
      int fontSize,
      boolean justify)
      throws IOException {
    List<String> lines = parseLines(text, width, font, fontSize);
    contentStream.setFont(font, fontSize);
    contentStream.newLineAtOffset(x, y);
    for (String line : lines) {
      float charSpacing = 0;
      if (justify) {
        if (line.length() > 1) {
          float size = fontSize * font.getStringWidth(line) / 1000;
          float free = width - size;
          if (free > 0 && !lines.get(lines.size() - 1).equals(line)) {
            charSpacing = free / (line.length() - 1);
          }
        }
      }
      contentStream.setCharacterSpacing(charSpacing);
      contentStream.showText(line);
      contentStream.newLineAtOffset(0, -1.5f * fontSize);
    }
  }

  private static List<String> parseLines(String text, float width, PDFont font, int fontSize)
      throws IOException {
    text = normalizeText(text);
    List<String> lines = new ArrayList<String>();
    int lastSpace = -1;
    while (text.length() > 0) {
      int spaceIndex = text.indexOf(' ', lastSpace + 1);
      if (spaceIndex < 0) spaceIndex = text.length();
      String subString = text.substring(0, spaceIndex);
      float size = fontSize * font.getStringWidth(subString) / 1000;
      if (size > width) {
        if (lastSpace < 0) {
          lastSpace = spaceIndex;
        }
        subString = text.substring(0, lastSpace);
        lines.add(subString);
        text = text.substring(lastSpace).trim();
        lastSpace = -1;
      } else if (spaceIndex == text.length()) {
        lines.add(text);
        text = "";
      } else {
        lastSpace = spaceIndex;
      }
    }
    return lines;
  }

  private static String normalizeText(String text) {

    StringBuilder string = new StringBuilder();

    for (int i = 0; i < text.length(); i++) {
      if (WinAnsiEncoding.INSTANCE.contains(text.charAt(i))) {
        string.append(text.charAt(i));
      } else {
        string.append(" ");
      }
    }

    return string.toString();
  }
}
