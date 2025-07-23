package org.example;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class PDFOutlineExtractor extends PDFTextStripper {

    private static final String INPUT_DIR = System.getenv("PDF_INPUT_DIR") != null ?
            System.getenv("PDF_INPUT_DIR") : "D:/Adobe_Hackathone/Adobe_1A/input";

    private static final String OUTPUT_DIR = System.getenv("PDF_OUTPUT_DIR") != null ?
            System.getenv("PDF_OUTPUT_DIR") : "D:/Adobe_Hackathone/Adobe_1A/output";
    private final List<TextLine> textLines = new ArrayList<>();
    private int currentPageNum;

    public PDFOutlineExtractor() throws IOException {
        super.setSortByPosition(true);
    }

    public static class TextLine {
        public String text;
        public float y;
        public float x;
        public float maxX;
        public float fontSize;
        public boolean isBold;
        public int page;

        public TextLine(String text, float y, float x, float maxX, float fontSize, boolean isBold, int page) {
            this.text = text.trim();
            this.y = y;
            this.x = x;
            this.maxX = maxX;
            this.fontSize = fontSize;
            this.isBold = isBold;
            this.page = page;
        }

        @Override
        public String toString() {
            return String.format("Page %d, Y=%.2f, X=%.2f, Size=%.2f, Bold=%b: \"%s\"", page, y, x, fontSize, isBold, text);
        }
    }

    @Override
    protected void writeString(String text, List<TextPosition> textPositions) throws IOException {
        if (!textPositions.isEmpty()) {
            TextPosition firstChar = textPositions.get(0);
            TextPosition lastChar = textPositions.get(textPositions.size() - 1);

            float currentY = firstChar.getYDirAdj();
            float minX = firstChar.getXDirAdj();
            float maxX = lastChar.getEndX();
            float fontSize = firstChar.getFontSizeInPt();

            String fontName = null;
            if (firstChar.getFont() != null) {
                fontName = firstChar.getFont().getName();
            }
            boolean isBold = (fontName != null) && fontName.toLowerCase().contains("bold");

            if (!textLines.isEmpty()) {
                TextLine lastLine = textLines.get(textLines.size() - 1);
                if (lastLine.page == currentPageNum &&
                        Math.abs(lastLine.y - currentY) < 1.0 &&
                        (minX - lastLine.maxX < 5)
                ) {
                    lastLine.text += text;
                    lastLine.maxX = maxX;
                    lastLine.fontSize = Math.max(lastLine.fontSize, fontSize);
                    lastLine.isBold = lastLine.isBold || isBold;
                    return;
                }
            }
            textLines.add(new TextLine(text, currentY, minX, maxX, fontSize, isBold, currentPageNum));
        }
    }


    @Override
    protected void startPage(PDPage page) throws IOException {
        currentPageNum = getCurrentPageNo();
        super.startPage(page);
    }

    public List<OutlineData> extractHeadings(PDDocument document) throws IOException {
        textLines.clear();
        super.document = document;
        super.startPage(document.getPages().get(0)); // Process first page for initial font info
        super.getText(document); // Process all pages

        textLines.sort(Comparator
                .comparingInt((TextLine line) -> line.page)
                .thenComparingDouble(line -> line.y));

        List<OutlineData> extractedHeadings = new ArrayList<>();

        float avgBodyFontSize = calculateAverageBodyFontSize(textLines);
        if (avgBodyFontSize == 0) avgBodyFontSize = 10.0f;

        float h1MinSize = avgBodyFontSize * 1.6f;
        float h2MinSize = avgBodyFontSize * 1.3f;
        float h3MinSize = avgBodyFontSize * 1.1f;

        for (TextLine line : textLines) {
            if (line.isBold) {
                if (line.fontSize >= h1MinSize) {
                    extractedHeadings.add(new OutlineData("H1", line.text, line.page));
                } else if (line.fontSize >= h2MinSize) {
                    extractedHeadings.add(new OutlineData("H2", line.text, line.page));
                } else if (line.fontSize >= h3MinSize) {
                    extractedHeadings.add(new OutlineData("H3", line.text, line.page));
                }
            } else {
                if (line.fontSize > h1MinSize * 1.2) {
                    extractedHeadings.add(new OutlineData("H1", line.text, line.page));
                } else if (line.fontSize > h2MinSize * 1.2) {
                    extractedHeadings.add(new OutlineData("H2", line.text, line.page));
                }
            }
        }
        return extractedHeadings;
    }

    private float calculateAverageBodyFontSize(List<TextLine> lines) {
        List<Float> bodyFontSizes = lines.stream()
                                         .filter(line -> line.fontSize >= 8.0f && line.fontSize <= 14.0f)
                                         .map(line -> line.fontSize)
                                         .collect(Collectors.toList());

        if (bodyFontSizes.isEmpty()) {
            return 0;
        }

        Map<Float, Long> fontSizeCounts = bodyFontSizes.stream()
                                                       .collect(Collectors.groupingBy(s -> s, Collectors.counting()));

        return fontSizeCounts.entrySet().stream()
                             .max(Map.Entry.comparingByValue())
                             .map(Map.Entry::getKey)
                             .orElse(0.0f);
    }

    private String extractTitle(PDDocument document) throws IOException {
        PDDocumentInformation info = document.getDocumentInformation();
        if (info != null && info.getTitle() != null && !info.getTitle().trim().isEmpty()) {
            return info.getTitle().trim();
        }

        if (!textLines.isEmpty()) {
            float maxFontSize = 0;
            String heuristicTitle = "Untitled Document";
            for (TextLine line : textLines) {
                if (line.page == 1) {
                    if (line.fontSize > maxFontSize) {
                        maxFontSize = line.fontSize;
                        heuristicTitle = line.text;
                    }
                } else {
                    break;
                }
            }
            return heuristicTitle;
        }

        return "Untitled Document";
    }

    public static void main(String[] args) {
        System.out.println("Starting PDF Outline Extractor...");

        Path inputPath = Paths.get(INPUT_DIR);
        Path outputPath = Paths.get(OUTPUT_DIR);

        try {
            if (!Files.exists(outputPath)) {
                Files.createDirectories(outputPath);
            }
        } catch (IOException e) {
            System.err.println("Error creating output directory: " + e.getMessage());
            e.printStackTrace();
            return;
        }

        try {
            List<Path> pdfFiles = Files.walk(inputPath)
                                       .filter(Files::isRegularFile)
                                       .filter(p -> p.toString().toLowerCase().endsWith(".pdf"))
                                       .collect(Collectors.toList());

            if (pdfFiles.isEmpty()) {
                System.out.println("No PDF files found in " + INPUT_DIR);
                return;
            }

            System.out.println("Found " + pdfFiles.size() + " PDF(s) to process.");

            ObjectMapper mapper = new ObjectMapper();
            mapper.enable(SerializationFeature.INDENT_OUTPUT);

            for (Path pdfFile : pdfFiles) {
                String fileName = pdfFile.getFileName().toString();
                String outputFileName = fileName.substring(0, fileName.lastIndexOf(".")) + ".json";
                Path outputFile = outputPath.resolve(outputFileName);

                System.out.println("Processing: " + fileName);

                PDDocument document = null;
                try {
                    document = PDDocument.load(pdfFile.toFile());

                    if (document.getNumberOfPages() > 50) {
                        System.out.println("Skipping " + fileName + ": Document exceeds 50 pages.");
                        continue;
                    }

                    PDFOutlineExtractor extractor = new PDFOutlineExtractor();

                    // Call getText on the extractor to populate textLines before extracting title/headings
                    // This is important because extractTitle and extractHeadings rely on the populated textLines
                    extractor.getText(document);

                    String title = extractor.extractTitle(document);
                    List<OutlineData> headings = extractor.extractHeadings(document);

                    DocumentOutline docOutline = new DocumentOutline(title, headings);
                    mapper.writeValue(outputFile.toFile(), docOutline);

                    System.out.println("Successfully processed " + fileName + " -> " + outputFileName);

                } catch (IOException e) {
                    System.err.println("Error processing " + fileName + ": " + e.getMessage());
                    e.printStackTrace();
                } finally {
                    if (document != null) {
                        try {
                            document.close();
                        } catch (IOException e) {
                            System.err.println("Error closing document " + fileName + ": " + e.getMessage());
                        }
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error listing PDF files or general I/O error: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("PDF Outline Extractor finished.");
    }

    // Data models for JSON output
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class OutlineData {
        public String level;
        public String text;
        public Integer page;

        public OutlineData() {} // Default constructor

        public OutlineData(String level, String text, Integer page) {
            this.level = level;
            this.text = text;
            this.page = page;
        }
    }

    public static class DocumentOutline {
        public String title;
        public List<OutlineData> outline = new ArrayList<>();

        public DocumentOutline() {}

        public DocumentOutline(String title, List<OutlineData> outline) {
            this.title = title;
            this.outline = outline;
        }
    }
}
