package com.pisearch.pi.search.services;

public interface PiSearch {
    int searchInPi(String pdfFilePath, String pattern);

    String generatePi(int digits);
}
