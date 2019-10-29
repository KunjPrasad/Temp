package dataObject;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.codec.digest.DigestUtils;

/**
 * This class contains static utilities for the DataClass class
 * 
 * @author Kunj
 *
 */
public class DataClassUtils {

    private DataClassUtils() {
        throw new AssertionError("Cannot be instatiated");
    }

    /**
     * Utility to get hex string for corresponding data
     * 
     * @param data
     * @return
     */
    public static String getHexForDataAndHexVersion(String data, HexVersion hexVersion) {
        if (data == null) {
            return null;
        }
        switch (hexVersion) {
        case SHA_256:
            return DigestUtils.sha256Hex(data);
        case SHA_512:
            return DigestUtils.sha512Hex(data);
        default:
            throw new RuntimeException("Wrong hexVersion provided. HexVersion=" + hexVersion);
        }
    }

    /**
     * Utility to get character-count map from a string
     * 
     * @param data
     * @return
     */
    public static Map<Character, Integer> getCharCountMapForString(String data) {
        if (data == null) {
            // return empty map if data is null, not a null map. This prevents NullPointerException
            return new HashMap<>();
        }
        Map<Character, Integer> charCountMap = new HashMap<>();
        for (char ch : data.toCharArray()) {
            Character key = new Character(ch);
            if (!charCountMap.containsKey(key)) {
                charCountMap.put(key, new Integer(0));
            }
            charCountMap.put(key, new Integer(charCountMap.get(key) + 1));
        }
        return charCountMap;
    }
}
