package dataObject;

import org.apache.commons.lang3.StringUtils;

/**
 * A marker-enum that enables OutClass to change its HEX Setting in different version and still get correct data
 * 
 * @author Kunj
 *
 */
public enum HexVersion {
    SHA_256("SHA_256"),
    SHA_512("SHA_512");

    private final String tag;

    HexVersion(String tag) {
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
    public static HexVersion fromValue(String str) {
        str = StringUtils.trimToEmpty(str).toUpperCase();
        switch (str) {
        case "SHA_256":
            return HexVersion.SHA_256;
        case "SHA_512":
            return HexVersion.SHA_512;
        default:
            throw new RuntimeException("No HexVersion enum for value=" + str);
        }
    }
}
