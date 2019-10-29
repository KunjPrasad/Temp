package serDeUtils;

import org.apache.commons.lang3.StringUtils;

/**
 * This enumeration lists the various data compression options allowed
 * 
 * @author Kunj
 *
 */
public enum CompressOption {
    NONE("NONE"),
    SNAPPY("SNAPPY"),
    GZIP("GZIP"),
    LZ4("LZ4"),
    LZO("LZO");

    private final String tag;

    CompressOption(String tag) {
        this.tag = tag;
    }

    @Override
    public String toString() {
        return this.tag;
    }

    /**
     * This method returns the Enum based on string tag value
     * 
     * @param str
     * @return
     */
    public static CompressOption fromValue(String str) {
        str = StringUtils.trimToEmpty(str).toUpperCase();
        switch (str) {
        case "NONE":
            return CompressOption.NONE;
        case "SNAPPY":
            return CompressOption.SNAPPY;
        case "GZIP":
            return CompressOption.GZIP;
        case "LZ4":
            return CompressOption.LZ4;
        case "LZO":
            return CompressOption.LZO;
        default:
            throw new RuntimeException("No CompressOptions enum for value=" + str);
        }
    }
}
