package com.example.demo.spring.boot.util;

import org.springframework.http.MediaType;

/**
 * Custom media type for csv
 * 
 * @author KunjPrasad
 *
 */
public class CsvMediaType extends MediaType {

    private static final long serialVersionUID = -8689964102975870822L;

    public static final String CSV_TYPE = "text";
    public static final String CSV_SUBTYPE = "csv";

    public CsvMediaType() {
        super(CSV_TYPE, CSV_SUBTYPE);
    }
}
