package intextbooks.content.extraction.format;

import java.awt.Color;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.RenderedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.PrimitiveIterator.OfInt;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;
import java.util.logging.Logger;
import java.util.stream.IntStream;

import org.apache.commons.io.output.NullWriter;
import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDPageTree;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.color.PDColor;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.graphics.state.PDGraphicsState;
import org.apache.pdfbox.pdmodel.graphics.state.RenderingMode;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.PDFTextStripperPersonalized;
import org.apache.pdfbox.text.TextPosition;
import org.apache.pdfbox.util.Matrix;

import intextbooks.SystemLogger;
import intextbooks.content.extraction.buildingBlocks.format.CharacterBlock;
import intextbooks.content.extraction.buildingBlocks.format.Line;
import intextbooks.content.extraction.buildingBlocks.format.Page;
import intextbooks.content.extraction.buildingBlocks.format.ResourceUnit;
import intextbooks.content.extraction.buildingBlocks.format.Slide;
import intextbooks.content.extraction.buildingBlocks.format.Text;
import intextbooks.content.extraction.structure.TableOfContentsExtractor;
import intextbooks.tools.utility.ListOperations;

import org.apache.pdfbox.contentstream.operator.color.SetNonStrokingColor;
import org.apache.pdfbox.contentstream.operator.color.SetNonStrokingColorN;
import org.apache.pdfbox.contentstream.operator.color.SetNonStrokingColorSpace;
import org.apache.pdfbox.contentstream.operator.color.SetNonStrokingDeviceCMYKColor;
import org.apache.pdfbox.contentstream.operator.color.SetNonStrokingDeviceGrayColor;
import org.apache.pdfbox.contentstream.operator.color.SetNonStrokingDeviceRGBColor;
import org.apache.pdfbox.contentstream.operator.color.SetStrokingColor;
import org.apache.pdfbox.contentstream.operator.color.SetStrokingColorN;
import org.apache.pdfbox.contentstream.operator.color.SetStrokingColorSpace;
import org.apache.pdfbox.contentstream.operator.color.SetStrokingDeviceCMYKColor;
import org.apache.pdfbox.contentstream.operator.color.SetStrokingDeviceGrayColor;
import org.apache.pdfbox.contentstream.operator.color.SetStrokingDeviceRGBColor;
import org.apache.pdfbox.cos.COSArray;

public class PdfTextExtractor  {
	
	//Library modified from source code of PDFBox 2.x
	PDFTextStripperPersonalized textStripper;
	PDDocument document;

	private Vector <Text> words = new Vector <Text>();
	private Vector <ResourceUnit> units = new Vector <ResourceUnit>();
	HashMap<Integer,  List<RenderedImage>> images = new HashMap<Integer,  List<RenderedImage>>();
	private Map<String, PDFont> fonts = new HashMap<String, PDFont>();
	
	private float pageHeight=-1, pageWidth=-1;
	FormatReasoner reasoner;

	public final static char charReplacer = '☀';
	
	/*
	 * Preferred constructor
	 */	
	public PdfTextExtractor() {
		textStripper = null;
		try {
			textStripper = new PDFTextStripperPersonalized();
			textStripper.setSortByPosition(true);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}	
	
	public void processText(String resourceID, PDDocument document)  throws IOException{
		PDPageTree allPages = document.getPages();
		this.processText(resourceID, document, 0, allPages.getCount());
	}
	
	/**This method process every page to get the words and lines of each page. The reasoner (FormatReasoner) is
	 * used to get the fonts used in each page and in general for the book, the text body font size.
	 * 
	 * @param resourceID
	 * @param document
	 * @return
	 * @throws IOException
	 */
	public void processText(String resourceID, PDDocument document, int startP, int endP)  throws IOException{
		
		SystemLogger.getInstance().log("Resource text process");
		
		this.document = document;
				
		PDPageTree allPages = document.getPages();
		
		int samplePage =20;
		
		if(allPages.getCount()<20){
			
			samplePage = allPages.getCount()/2;
		}		
		
		PDPage p = allPages.get(samplePage);
		
		PDRectangle pageSize = p.getMediaBox();
		this.pageHeight = pageSize.getHeight();
		this.pageWidth  = pageSize.getWidth();
		
		reasoner = new FormatReasoner();
		this.units = this.textStripper.getDocAsResourceUnits(document);
		List<Integer> toNull = new ArrayList<Integer>();
		int index = 0;
		for(ResourceUnit unit: this.units) {
			if(unit != null) {
				unit.extractLines();
				List<Line> lines = unit.getLines();
				Iterator<Line> iterator = lines.iterator();

				trimWords(lines);
				reasoner.checkFontSize(unit.getWords());
				removeBeyondScopeLines(lines);
				while(iterator.hasNext()) {
					Line l =iterator.next();
					if(l.size() == 0) {
						iterator.remove();
					}
				}
				unit.extractText();
				if(unit.getLines().size() == 0) {
					toNull.add(index);
				}

			}
			index++;
		}
		
		for(Integer indexPos: toNull) {
			this.units.set(indexPos, null);
		}
		
		SystemLogger.getInstance().log("Resource text process....Done");
	}
	
	private List<RenderedImage> getImagesFromResources(PDResources resources) throws IOException {
	    List<RenderedImage> images = new ArrayList<>();

	    for (COSName xObjectName : resources.getXObjectNames()) {
	        PDXObject xObject = resources.getXObject(xObjectName);

	        if (xObject instanceof PDFormXObject) {
	            images.addAll(getImagesFromResources(((PDFormXObject) xObject).getResources()));
	        } else if (xObject instanceof PDImageXObject) {
	            images.add(((PDImageXObject) xObject).getImage());
	        }
	    }

	    return images;
	}
	
	public void closeDocument() {
		try {
			this.document.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	protected void processRepeatedLines() {
		//FIRST: Find if there are repeated lines (first or last)
		int total = this.units.size();
		//Initial check
		if(total <= 1) {
			return;
		}
		Random random = new Random();
		OfInt is = random.ints(0, total).iterator();
		//get a initial page to compare with. Do a while until a null page with lines is found
		int page0 = is.nextInt();
		String firstLine = null;
		String lastLine = null;
		int totalChecked = 1;
		while(true) {
			ResourceUnit ru = this.units.get(page0);
			if(ru != null && ru.getLines().size() >= 2) {
				firstLine = ru.getLineAt(0).getText();
				lastLine = ru.getLineAt(ru.getLines().size()-1).getText();
				break;
			}
			page0 =  is.nextInt();
			totalChecked++;
			if(totalChecked >= total) {
				return;
			}
		}
		//get a array of samples pages
		int sample = is.nextInt();
		int[] samplePages = new int[sample];
		for(int i = 0; i < sample; i++) {
			samplePages[i] = is.nextInt();
		}
		//check for first and lastLines
		int equalFirstLines = 0;
		int equalLastLines = 0;
		for(int i = 0; i < sample; i++) {
			int pageNumber = samplePages[i];
			if(this.units.get(pageNumber) == null) {
				continue;
			}
			if(firstLine.equals(this.units.get(pageNumber).getLineAt(0).getText())){
				equalFirstLines++;
			}
			if(firstLine.equals(this.units.get(pageNumber).getLineAt(this.units.get(pageNumber).getLines().size()-1).getText())){
				equalLastLines++;
			}
		}
		
		//check and remove first lines
		if((double)equalFirstLines / (double)sample >= 0.75) {
			for(ResourceUnit ru : this.units) {
				if(ru != null && ru.getLines().size() > 0 && firstLine.equals(ru.getLineAt(0).getText())) {
					ru.removeLineAt(0);
				}
			}
		}
		
		//check and remove last lines
		if((double)equalLastLines / (double)sample >= 0.75) {
			for(ResourceUnit ru : this.units) {
				if (ru != null && ru.getLines().size() > 0) {
					int last = ru.getLines().size()-1;
					if(lastLine.equals(ru.getLineAt(last).getText())) {
						ru.removeLineAt(last);
					}
				}
			}
		}
		
//		/*TESTING*/
//		int i = 0;
//		for(ResourceUnit ru : this.units) {
//			System.out.println("-------------------");
//			System.out.println("Page: " + i++);
//			String text = "";
//			if(ru != null) {
//				for(Line l : ru.getLines()) {
//					text += l.getText() + "\n";
//				}
//			}
//			System.out.println(text);
//			
//		}
//		/*TESTING*/
		
	}	


	
	
	private List<Line> trimWords(List<Line> lines){
		Iterator<Line> iterate = lines.iterator();
		
		while(iterate.hasNext()){
			
			Line test = iterate.next();
			
			//trim first words
			for(int i = 0; i < test.size(); i++){
				if(test.getWordAt(i).getText().trim().length() == 0 || (test.getWordAt(i).getText().length() == 1 && test.getWordAt(i).getText().charAt(0) == 160)) {
					test.removeWordAt(i);
					i--;
				} else {
					break;
				}
			}
			//trim last words
			for(int i = test.size()-1; i >= 0; i--){
				if(test.getWordAt(i).getText().trim().length() == 0 || (test.getWordAt(i).getText().length() == 1 && test.getWordAt(i).getText().charAt(0) == 160)) {
					test.removeWordAt(i);
				} else {
					break;
				}
			}
		}	
		return lines;
	}
	
	/**
	 * 
	 * @param lines
	 * @return
	 */
	private List<Line> removeBeyondScopeLines(List<Line> lines){
		
		Iterator<Line> iterate = lines.iterator();
		
		while(iterate.hasNext()){
			
			Line test = iterate.next();
			
			if(test.getPositionY()<0 || test.getPositionY()>this.pageHeight || test.getPositionY()>this.pageHeight - 10 ){
				if(test.size() >= 1 && !StringUtils.isNumeric(test.getText()) && !test.getText().toUpperCase().matches(TableOfContentsExtractor.regexRomanNumber) 
						&& !StringUtils.isNumeric(test.getWordAt(0).getText()) && !StringUtils.isNumeric(test.getLastWordText())) {
					iterate.remove();	
				}
			}			
		}
		
		return lines;
	}
	
	public Vector <Page> convertToPage(){
		
		Vector <Page> result = new Vector<Page> ();
		
		for(int i = 0; i<units.size(); i++){
			if(units.get(i)!=null){
				result.add(units.get(i).convertToPage());
			}
			else{
				result.add(null);
			}
		}
		
		return result;
	}
	
	public String getRawText(int start, int end) {
		String text = "";
		for(int i = start; i<=end; i++){
			text += units.get(i).getText() + " ";
		}
		text = text.trim().replace("\n", " ");
		System.out.println("******");
		System.out.println(text);
		System.exit(0);
		return text;
	}
	
	public String getRawText() {
		String text = "";
		for(int i = 0; i < units.size(); i++){
			if(units.get(i) != null)
					text += units.get(i).getText() + " ";
		}
		text = text.trim().replace("\n", " ");
		return text;
	}
	
	public Vector <ResourceUnit> getResourceUnits(){
		return units;
	}
	
	public float getHeight(){
		return this.pageHeight;
	}
	
	public float getWidth(){
		return this.pageWidth;
	}
	
	public float getTextBodyFontSize() {
		return reasoner.getTextBodyFontSize();
	}
	
	public Map<String, PDFont> getFonts(){
		return this.fonts;
	}
	
	public HashMap<Integer,  List<RenderedImage>> getImages() {
		return this.images;
	}
	
	public List<RenderedImage> getImages(int pageN) {
		return this.images.get(pageN);
	}
}
