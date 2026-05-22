package com.crawler.transdep.eticket.parser;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility class for parsing HTML documents and extracting data
 */
public class HtmlParser {
    private static final Logger logger = LoggerFactory.getLogger(HtmlParser.class);
    
    private Document document;

    public HtmlParser(Document document) {
        this.document = document;
    }

    /**
     * Extract text content by CSS selector
     */
    public String getText(String selector) {
        Element element = document.selectFirst(selector);
        return element != null ? element.text() : null;
    }

    /**
     * Extract attribute value by CSS selector
     */
    public String getAttributeValue(String selector, String attributeName) {
        Element element = document.selectFirst(selector);
        return element != null ? element.attr(attributeName) : null;
    }

    /**
     * Extract multiple elements and return as list of text content
     */
    public List<String> getTextList(String selector) {
        List<String> results = new ArrayList<>();
        Elements elements = document.select(selector);
        elements.forEach(el -> results.add(el.text()));
        return results;
    }

    /**
     * Extract multiple elements and return as list of maps (attributes + text)
     */
    public List<Map<String, String>> getElementList(String selector) {
        List<Map<String, String>> results = new ArrayList<>();
        Elements elements = document.select(selector);
        
        elements.forEach(el -> {
            Map<String, String> item = new HashMap<>();
            item.put("text", el.text());
            item.put("html", el.html());
            el.attributes().forEach(attr -> item.put(attr.getKey(), attr.getValue()));
            results.add(item);
        });
        
        return results;
    }

    /**
     * Extract table data by rows and columns
     */
    public List<Map<String, String>> parseTable(String tableSelector, String headerSelector, String rowSelector) {
        List<Map<String, String>> tableData = new ArrayList<>();
        
        Element table = document.selectFirst(tableSelector);
        if (table == null) {
            logger.warn("Table not found with selector: {}", tableSelector);
            return tableData;
        }

        // Extract headers
        List<String> headers = new ArrayList<>();
        Elements headerCells = table.select(headerSelector);
        headerCells.forEach(th -> headers.add(th.text()));

        // Extract rows
        Elements rows = table.select(rowSelector);
        rows.forEach(row -> {
            Elements cells = row.select("td, th");
            Map<String, String> rowData = new HashMap<>();
            
            for (int i = 0; i < cells.size() && i < headers.size(); i++) {
                rowData.put(headers.get(i), cells.get(i).text());
            }
            
            tableData.add(rowData);
        });

        return tableData;
    }

    /**
     * Extract form data (input fields)
     */
    public Map<String, String> parseForm(String formSelector) {
        Map<String, String> formData = new HashMap<>();
        Element form = document.selectFirst(formSelector);
        
        if (form == null) {
            logger.warn("Form not found with selector: {}", formSelector);
            return formData;
        }

        Elements inputs = form.select("input, select, textarea");
        inputs.forEach(input -> {
            String name = input.attr("name");
            String value = input.attr("value");
            if (!name.isEmpty()) {
                formData.put(name, value);
            }
        });

        return formData;
    }

    /**
     * Check if element exists
     */
    public boolean hasElement(String selector) {
        return document.selectFirst(selector) != null;
    }

    /**
     * Get raw HTML of an element
     */
    public String getHtml(String selector) {
        Element element = document.selectFirst(selector);
        return element != null ? element.html() : null;
    }

    /**
     * Get the document for direct access
     */
    public Document getDocument() {
        return document;
    }
}
