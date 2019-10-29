package utils;

/**
 * This is a utility class that contains methods used to obtain or update hash values
 * 
 * @author Kunj
 *
 */
public class HashUtils {

    /**
     * Hash value of null objects
     */
    public static final int NULL_HASH = 0;

    // throwing error prevents creating an object via reflection
    private HashUtils() {
        throw new AssertionError("Cannot be instatiated");
    }

    // An implementation-specific constants
    private static final int HASH_MULT = 32;

    /**
     * This method takes an oldhash value and updates it to include contributions from a new hash value. The returned
     * values emulates hash of a new object that consists of all members used to make "oldHash" and new-member
     * 
     * @return
     */
    public static int updateHash(int oldObjectsHash, int newObjectHash) {
        return (HASH_MULT * oldObjectsHash) + newObjectHash;
    }
}
