package serDeUtils;

import java.io.Reader;
import java.io.Writer;
import java.util.Map;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;

/**
 * This class contains methods to handle the read/write of csv files
 * 
 * @author Kunj
 *
 */
public class CSVSerDeHandlerFactory {

    private CSVSerDeHandlerFactory() {
        throw new AssertionError("Cannot be instantiated");
    }

    // name of various options that can be provided to customize the behavior of handler
    // Also provided are default values used for the tags - unless otherwise specified
    public static final String CSV_SEPARATOR_TAG = "CSV_SEPARATOR";
    private static final char CSV_SEPARATOR_VAL = ','; // CSVWriter.DEFAULT_SEPARATOR;
    public static final String CSV_QUOTE_TAG = "CSV_QUOTE";
    private static final char CSV_QUOTE_VAL = '"'; // CSVWriter.DEFAULT_QUOTE_CHARACTER;
    public static final String CSV_QUOTE_ESCAPE_TAG = "CSV_QUOTE_ESCAPE";
    private static final char CSV_QUOTE_ESCAPE_VAL = '/'; // CSVWriter.DEFAULT_ESCAPE_CHARACTER;
    public static final String CSV_SKIPLINE_TAG = "CSV_SKIPLINE";
    private static final int CSV_SKIPLINE_VAL = 1; // causes skip of first line

    /**
     * utility method to return csvReader bound to given reader and provided options. The method is overloaded to return
     * reader with default settings if no options are given. The static method does not reuses same csvReader object but
     * creates a new one for each call. Thus it should be used with caution to prevent creating multiple garbage. It is
     * the responsibility of calling method to not modify the options-map while a new reader is being constructed.
     * 
     * @param rd
     * @param options
     * @return
     */
    public static final CSVReader getReaderWithOptions(Reader rd, Map<String, String> options) {
        if (options == null || options.isEmpty()) {
            return getReaderWithOptions(rd);
        }
        char separator = getCsvSeparatorFromOptions(options);
        char quote = getCsvQuoteFromOptions(options);
        char quoteEsc = getCsvQuoteEscapeFromOptions(options);
        int skipLine = getCsvSkipLineFromOptions(options);
        return new CSVReader(rd, separator, quote, quoteEsc, skipLine);
    }

    public static final CSVReader getReaderWithOptions(Reader rd) {
        return new CSVReader(rd, CSV_SEPARATOR_VAL, CSV_QUOTE_VAL, CSV_QUOTE_ESCAPE_VAL, CSV_SKIPLINE_VAL);
    }

    /**
     * utility method to return csvWriter bound to given writer and provided options. The method is overloaded to return
     * writer with default settings if no options are given. The static method does not reuses same csvWriter object but
     * creates a new one for each call. Thus it should be used with caution to prevent creating multiple garbage. It is
     * the responsibility of calling method to not modify the options-map while a new reader is being constructed.
     * 
     * @param wt
     * @param options
     * @return
     */
    public static final CSVWriter getWriterWithOptions(Writer wt, Map<String, String> options) {
        if (options == null || options.isEmpty()) {
            return getWriterWithOptions(wt);
        }
        char separator = getCsvSeparatorFromOptions(options);
        char quote = getCsvQuoteFromOptions(options);
        char quoteEsc = getCsvQuoteEscapeFromOptions(options);
        return new CSVWriter(wt, separator, quote, quoteEsc);
    }

    public static final CSVWriter getWriterWithOptions(Writer wt) {
        return new CSVWriter(wt, CSV_SEPARATOR_VAL, CSV_QUOTE_VAL, CSV_QUOTE_ESCAPE_VAL);
    }

    // UTILITY METHODS
    // Method to get csv separator from options map
    private static char getCsvSeparatorFromOptions(Map<String, String> options) {
        if (options.containsKey(CSV_SEPARATOR_TAG)) {
            String val = options.get(CSV_SEPARATOR_TAG);
            if (val.length() != 1) {
                throw new RuntimeException("Wrong option-value passed for CSV separator. "
                        + "Separator should be 1 character only, Provided value=" + val);
            } else {
                return val.charAt(0);
            }
        } else {
            return CSV_SEPARATOR_VAL;
        }
    }

    // Method to get csv quote character from options map
    private static char getCsvQuoteFromOptions(Map<String, String> options) {
        if (options.containsKey(CSV_QUOTE_TAG)) {
            String val = options.get(CSV_QUOTE_TAG);
            if (val.length() != 1) {
                throw new RuntimeException("Wrong option-value passed for CSV quote. "
                        + "Quote should be 1 character only, Provided value=" + val);
            } else {
                return val.charAt(0);
            }
        } else {
            return CSV_QUOTE_VAL;
        }
    }

    // Method to get csv quote-escape character from options map
    private static char getCsvQuoteEscapeFromOptions(Map<String, String> options) {
        if (options.containsKey(CSV_QUOTE_ESCAPE_TAG)) {
            String val = options.get(CSV_QUOTE_ESCAPE_TAG);
            if (val.length() != 1) {
                throw new RuntimeException("Wrong option-value passed for CSV quote-escape. "
                        + "Quote-escape should be 1 character only, Provided value=" + val);
            } else {
                return val.charAt(0);
            }
        } else {
            return CSV_QUOTE_ESCAPE_VAL;
        }
    }

    // Method to get the number of lines to skip from options map
    private static int getCsvSkipLineFromOptions(Map<String, String> options) {
        if (options.containsKey(CSV_SKIPLINE_TAG)) {
            String val = options.get(CSV_SKIPLINE_TAG);
            try {
                return Integer.parseInt(val);
            } catch (NumberFormatException e) {
                throw new RuntimeException("Wrong option-value passed for CSV skip-line. "
                        + "Skip-line should be numeric, Provided value=" + val);
            }
        } else {
            return CSV_SKIPLINE_VAL;
        }
    }
}
