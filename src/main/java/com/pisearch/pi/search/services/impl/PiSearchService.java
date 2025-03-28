package com.pisearch.pi.search.services.impl;

import com.pisearch.pi.search.services.PiSearch;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

@Service
public class PiSearchService implements PiSearch {

    private static class SearchResult {
        final int foundIndex;
        final int newJ;

        SearchResult(int foundIndex, int newJ) {
            this.foundIndex = foundIndex;
            this.newJ = newJ;
        }
    }

    // Optimization constants
    private static final int INITIAL_AVG_PAGE_DIGITS = 1000; // Conservative initial estimate
    private int totalDigitsProcessed = 0;
    private int pagesProcessed = 0;

    public int searchInPi(String pdfFilePath, String pattern) {
        try {
            ClassPathResource resource = new ClassPathResource(pdfFilePath);
            InputStream inputStream = resource.getInputStream();
            PDDocument document = PDDocument.load(inputStream);

            try {
                PDFTextStripper stripper = new PDFTextStripper();
                int globalIndex = 0;
                int j = 0; // KMP state carried across pages
                final int patternLength = pattern.length();

                // Reset tracking for new search
                resetTracking();

                for (int pageNum = 1; pageNum <= document.getNumberOfPages(); pageNum++) {
                    stripper.setStartPage(pageNum);
                    stripper.setEndPage(pageNum);
                    String pageText = stripper.getText(document);

                    // Preprocess text: remove all non-digits
                    pageText = preprocessPageText(pageText );

                    if (pageText.isEmpty()) continue;

                    SearchResult result = kmpSearchWithState(pattern, pageText, j);
                    if (result.foundIndex != -1) {
                        return globalIndex + result.foundIndex;
                    }

                    // Update tracking metrics
                    updateTracking(pageText.length());

                    // Update global index and carry over KMP state
                    globalIndex += pageText.length();
                    j = result.newJ;

                    // Smart reset check - only if we have remaining pages
                    if (pageNum < document.getNumberOfPages()) {
                        int avgPageDigits = getCurrentAvgPageDigits();
                        int remainingPossibleDigits = (document.getNumberOfPages() - pageNum) * avgPageDigits;

                        if (remainingPossibleDigits < (patternLength - j)) {
                            j = 0; // Safe to reset state
                        }
                    }

                    // Early exit if remaining pages can't contain the pattern
                    if (j == 0 && (globalIndex + getCurrentAvgPageDigits() < patternLength)) {
                        break;
                    }
                }
            } finally {
                document.close();
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to search in PDF: " + pdfFilePath, e);
        }
        return -1;
    }

    private void resetTracking() {
        totalDigitsProcessed = 0;
        pagesProcessed = 0;
    }

    private void updateTracking(int newDigits) {
        totalDigitsProcessed += newDigits;
        pagesProcessed++;
    }

    private int getCurrentAvgPageDigits() {
        return pagesProcessed > 0 ? totalDigitsProcessed / pagesProcessed : INITIAL_AVG_PAGE_DIGITS;
    }

    private String preprocessPageText(String pageText) {
        // Remove all non-digit characters (handles various PDF formatting)
        String processed = pageText.replaceAll("[^0-9.]", "");

        return processed;
    }

    private SearchResult kmpSearchWithState(String pattern, String text, int initialJ) {
        int[] lps = computeLPS(pattern);
        int i = 0;
        int j = initialJ;
        final int M = pattern.length();
        final int N = text.length();

        while (i < N) {
            if (pattern.charAt(j) == text.charAt(i)) {
                j++;
                i++;
            }
            if (j == M) {
                return new SearchResult(i - j, 0); // Reset state after full match
            } else if (i < N && pattern.charAt(j) != text.charAt(i)) {
                if (j != 0) j = lps[j - 1];
                else i++;
            }
        }

        return new SearchResult(-1, j); // Carry partial match state
    }

    private int[] computeLPS(String pattern) {
        int[] lps = new int[pattern.length()];
        int j = 0;

        for (int i = 1; i < pattern.length();) {
            if (pattern.charAt(i) == pattern.charAt(j)) {
                lps[i++] = ++j;
            } else {
                if (j != 0) j = lps[j - 1];
                else lps[i++] = 0;
            }
        }
        return lps;
    }

    public String generatePi(int digits) {
        MathContext mc = new MathContext(digits + 5, RoundingMode.HALF_UP); // Extra precision
        BigDecimal C = new BigDecimal("426880").multiply(sqrt(new BigDecimal("10005"), mc));
        BigDecimal K = BigDecimal.ZERO;
        BigDecimal M = BigDecimal.ONE;
        BigDecimal X = BigDecimal.ONE;
        BigDecimal L = new BigDecimal("13591409");
        BigDecimal S = L;

        for (int k = 1; k <= digits / 14; k++) {
            K = new BigDecimal(6 * k - 5)
                    .multiply(new BigDecimal(2 * k - 1))
                    .multiply(new BigDecimal(6 * k - 1));
            M = M.multiply(K).divide(new BigDecimal(k * k * k), mc);
            X = X.multiply(new BigDecimal("-262537412640768000"), mc);
            L = L.add(new BigDecimal("545140134"));
            S = S.add(M.multiply(L).divide(X, mc));
        }

        return C.divide(S, mc).setScale(digits, RoundingMode.DOWN).toString();

    }

    private static BigDecimal sqrt(BigDecimal value, MathContext mc) {
        BigDecimal x0 = BigDecimal.ZERO;
        BigDecimal x1 = value;
        while (!x0.equals(x1)) {
            x0 = x1;
            x1 = value.divide(x0, mc).add(x0).divide(BigDecimal.valueOf(2), mc);
        }
        return x1;
    }
}