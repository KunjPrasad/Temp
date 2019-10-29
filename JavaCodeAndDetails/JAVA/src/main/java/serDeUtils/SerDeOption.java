package serDeUtils;

import org.apache.commons.lang3.StringUtils;

/**
 * This enumeration lists the various serde options allowed
 * 
 * @author Kunj
 *
 */
public enum SerDeOption {
    CSV("CSV"),
    JSON("JSON"),
    PROTOSTUFF("PROTOSTUFF"),
    PROTOBUF("PROTOBUF"),
    KRYO("KRYO");

    private final String tag;

    SerDeOption(String tag) {
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
    public static SerDeOption fromValue(String str) {
        str = StringUtils.trimToEmpty(str).toUpperCase();
        switch (str) {
        case "CSV":
            return SerDeOption.CSV;
        case "JSON":
            return SerDeOption.JSON;
        case "PROTOSTUFF":
            return SerDeOption.PROTOSTUFF;
        case "PROTOBUF":
            return SerDeOption.PROTOBUF;
        case "KRYO":
            return SerDeOption.KRYO;
        default:
            throw new RuntimeException("No SerDeOptions enum for value=" + str);
        }
    }
}
