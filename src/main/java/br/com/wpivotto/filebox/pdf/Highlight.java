package br.com.wpivotto.filebox.pdf;

import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.color.PDColor;
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceRGB;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationTextMarkup;
import org.apache.pdfbox.text.TextPosition;

public class Highlight {
	
	private String word;
	private List<TextPosition> textPositions;
	private int pageNo;
	private PDPage page;
	private Point2D point;
	
	public Highlight(String word, List<TextPosition> textPositions, PDPage page, int pageNo) {
		this.word = word;
		this.textPositions = textPositions;
		this.page = page;
		this.pageNo = pageNo;
		this.point = getPoint();
	}

	public void print() {
		Point2D p = getPoint();
	    System.out.println("Palavra: " + word + " [(X = " + p.getX() + ", Y = " + p.getY() + "]");
		for (TextPosition text : textPositions) {
			printPosition(text);
		}
		System.out.println("");
	}
	
	private float minX() {
		float min = 999999999;
		for (TextPosition text : textPositions) {
			if (text.getTextMatrix().getTranslateX() < min) {
				min = text.getTextMatrix().getTranslateX();
			}
		}
		return min;
	}
	
	public void printPosition(TextPosition text) {
		System.out.println(text.getUnicode()+ " [(X = " + text.getXDirAdj() + ", Y = " +
				text.getYDirAdj() + ") height = " + text.getHeightDir() + " width = " +
				text.getWidthDirAdj() + "]");
	}
	
	public boolean inside(Rectangle area) {
		return area.contains(point);
	}
	
	public double getX() {
		return point.getX();
	}
	
	public double getY() {
		return point.getY();
	}
	
	public String getText() {
		return word.trim();
	}
	
	public int getPageNo() {
		return pageNo;
	}

	private Point2D getPoint() {
		PDRectangle cropBox = page.getCropBox();
	    float x = minX() + cropBox.getLowerLeftX();
	    float y = textPositions.get(0).getTextMatrix().getTranslateY() + cropBox.getLowerLeftY();
	    return new Point2D.Float(x, y);
	}

	public PDAnnotation getAnnotation() {
		
		PDAnnotationTextMarkup markup = new PDAnnotationTextMarkup(PDAnnotationTextMarkup.SUB_TYPE_HIGHLIGHT);
		
		float posXInit = 0, posXEnd = 0, posYInit = 0, posYEnd = 0, height = 0;
		posXInit = textPositions.get(0).getXDirAdj();
        posXEnd  = textPositions.get(textPositions.size() - 1).getXDirAdj() + textPositions.get(textPositions.size() - 1).getWidth();
        posYInit = textPositions.get(0).getPageHeight() - textPositions.get(0).getYDirAdj();
        posYEnd  = textPositions.get(0).getPageHeight() - textPositions.get(textPositions.size() - 1).getYDirAdj();
        height   = textPositions.get(0).getHeightDir();
        
        PDRectangle position = new PDRectangle();
        position.setLowerLeftX(posXInit);
        position.setLowerLeftY(posYEnd);
        position.setUpperRightX(posXEnd);
        position.setUpperRightY(posYEnd + height);
        
        float yOffset = 3.5f;
        float xOffset = 1f;
        float quadPoints[] = {posXInit - xOffset, posYEnd + height + yOffset, posXEnd + xOffset, posYEnd + height + yOffset, posXInit - xOffset, posYInit - yOffset, posXEnd + xOffset, posYEnd - yOffset};
        
		markup.setRectangle(position);
		markup.setQuadPoints(quadPoints);
		markup.setColor(new PDColor(new float[]{ 152 / 255F, 193 / 255F, 218 / 255F }, PDDeviceRGB.INSTANCE));
		return markup;
		
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((point == null) ? 0 : point.hashCode());
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
		Highlight other = (Highlight) obj;
		if (point == null) {
			if (other.point != null)
				return false;
		} else if (!point.equals(other.point))
			return false;
		return true;
	}
	

}
