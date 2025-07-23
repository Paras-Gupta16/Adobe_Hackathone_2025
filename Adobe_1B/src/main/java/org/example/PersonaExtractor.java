package org.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;

import java.io.IOException;
import java.nio.file.*;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class PersonaExtractor {

    private static final String INPUT_DIR = System.getenv("PDF_INPUT_DIR") != null ?
            System.getenv("PDF_INPUT_DIR") : "D:/Adobe_Hackathone/Adobe_1B/input";

    private static final String OUTPUT_DIR = System.getenv("PDF_OUTPUT_DIR") != null ?
            System.getenv("PDF_OUTPUT_DIR") : "D:/Adobe_Hackathone/Adobe_1B/output";
    private final List<String> taskKeywords;

    public PersonaExtractor(String jobToBeDone) {
        this.taskKeywords = extractKeywords(jobToBeDone);
    }

    private List<String> extractKeywords(String text) {
        return Arrays.asList(text.toLowerCase().replaceAll("[^a-zA-Z0-9 ]", "").split("\\s+"));
    }

    private int scoreSection(String sectionText) {
        String lowerText = sectionText.toLowerCase();
        int score = 0;
        for (String keyword : taskKeywords) {
            if (lowerText.contains(keyword)) {
                score++;
            }
        }
        return score;
    }

    private List<RankedSection> rankSections(List<OutlineData> sections, String documentName) {
        List<RankedSection> rankedSections = new ArrayList<>();

        for (OutlineData section : sections) {
            int relevanceScore = scoreSection(section.text);
            if (relevanceScore > 0) {
                rankedSections.add(new RankedSection(
                        documentName,
                        section.page,
                        section.text,
                        relevanceScore
                ));
            }
        }

        rankedSections.sort(Comparator.comparingInt(s -> -s.importance_rank));
        return rankedSections;
    }

    public static void main(String[] args) throws IOException {
        String persona = "PhD Researcher in Computational Biology";
        String jobToBeDone = "Prepare a comprehensive literature review focusing on methodologies, datasets, and performance benchmarks";

        PersonaExtractor extractor = new PersonaExtractor(jobToBeDone);

        List<RankedSection> allRankedSections = new ArrayList<>();
        List<String> processedDocuments = new ArrayList<>();

        Path inputPath = Paths.get(INPUT_DIR);
        Path outputPath = Paths.get(OUTPUT_DIR);

        if (!Files.exists(outputPath)) {
            Files.createDirectories(outputPath);
        }

        List<Path> pdfFiles = Files.walk(inputPath)
                                   .filter(Files::isRegularFile)
                                   .filter(p -> p.toString().toLowerCase().endsWith(".pdf"))
                                   .collect(Collectors.toList());

        for (Path pdfFile : pdfFiles) {
            System.out.println("Processing: " + pdfFile.getFileName());

            processedDocuments.add(pdfFile.getFileName().toString());

            PDDocument document = null;
            try {
                document = PDDocument.load(pdfFile.toFile());
                List<OutlineData> headings = extractHeadings(document);
                List<RankedSection> rankedSections = extractor.rankSections(headings, pdfFile.getFileName().toString());
                allRankedSections.addAll(rankedSections);

            } finally {
                if (document != null) {
                    document.close();
                }
            }
        }

        Metadata metadata = new Metadata(processedDocuments, persona, jobToBeDone);

        Map<String, Object> outputJson = new HashMap<>();
        outputJson.put("metadata", metadata);
        outputJson.put("extracted_sections", allRankedSections);

        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);

        Path outputFile = outputPath.resolve("round1b_output.json");
        mapper.writeValue(outputFile.toFile(), outputJson);

        System.out.println("Round 1B output written to: " + outputFile.getFileName());
    }

    public static class Metadata {
        public List<String> documents;
        public String persona;
        public String job_to_be_done;
        public String processing_timestamp;

        public Metadata(List<String> documents, String persona, String jobToBeDone) {
            this.documents = documents;
            this.persona = persona;
            this.job_to_be_done = jobToBeDone;
            this.processing_timestamp = ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        }
    }

    public static class RankedSection {
        public String document;
        public int page_number;
        public String section_title;
        public int importance_rank;

        public RankedSection(String document, int pageNumber, String sectionTitle, int importanceRank) {
            this.document = document;
            this.page_number = pageNumber;
            this.section_title = sectionTitle;
            this.importance_rank = importanceRank;
        }
    }

    public static class OutlineData {
        public String level;
        public String text;
        public int page;

        public OutlineData(String level, String text, int page) {
            this.level = level;
            this.text = text;
            this.page = page;
        }
    }

    private static List<OutlineData> extractHeadings(PDDocument document) throws IOException {
        List<TextLine> textLines = new ArrayList<>();

        PDFTextStripper stripper = new PDFTextStripper() {
            int currentPageNum;

            @Override
            protected void startPage(PDPage page) throws IOException {
                currentPageNum = getCurrentPageNo();
                super.startPage(page);
            }

            @Override
            protected void writeString(String text, List<TextPosition> textPositions) throws IOException {
                if (text.trim().isEmpty()) return;
                TextPosition firstChar = textPositions.get(0);
                float y = firstChar.getYDirAdj();
                float x = firstChar.getXDirAdj();
                float fontSize = firstChar.getFontSizeInPt();
                boolean isBold = firstChar.getFont().getName().toLowerCase().contains("bold");

                textLines.add(new TextLine(text.trim(), y, x, fontSize, isBold, currentPageNum));
            }
        };

        stripper.setSortByPosition(true);
        stripper.getText(document);

        textLines.sort(Comparator
                .comparingInt((TextLine line) -> line.page)
                .thenComparingDouble(line -> line.y));

        float avgFontSize = calculateAverageFontSize(textLines);

        float h1Min = avgFontSize * 1.6f;
        float h2Min = avgFontSize * 1.3f;
        float h3Min = avgFontSize * 1.1f;

        List<OutlineData> result = new ArrayList<>();
        for (TextLine line : textLines) {
            if (line.fontSize >= h1Min) {
                result.add(new OutlineData("H1", line.text, line.page));
            } else if (line.fontSize >= h2Min) {
                result.add(new OutlineData("H2", line.text, line.page));
            } else if (line.fontSize >= h3Min) {
                result.add(new OutlineData("H3", line.text, line.page));
            }
        }
        return result;
    }

    private static float calculateAverageFontSize(List<TextLine> textLines) {
        List<Float> fontSizes = textLines.stream()
                                         .map(line -> line.fontSize)
                                         .filter(size -> size >= 8.0f && size <= 14.0f)
                                         .collect(Collectors.toList());

        if (fontSizes.isEmpty()) return 10.0f;

        float sum = 0;
        for (float size : fontSizes) sum += size;
        return sum / fontSizes.size();
    }

    public static class TextLine {
        public String text;
        public float y;
        public float x;
        public float fontSize;
        public boolean isBold;
        public int page;

        public TextLine(String text, float y, float x, float fontSize, boolean isBold, int page) {
            this.text = text;
            this.y = y;
            this.x = x;
            this.fontSize = fontSize;
            this.isBold = isBold;
            this.page = page;
        }
    }
}