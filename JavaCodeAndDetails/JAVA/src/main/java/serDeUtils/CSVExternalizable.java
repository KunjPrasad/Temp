package serDeUtils;

/**
 * This interface identifies that a class can be converted to CSV
 * 
 * @author Kunj
 *
 */
public interface CSVExternalizable<T> {

    /**
     * This method returns the Class object from CSV record
     * 
     * @param csvArr
     * @return
     */
    T fromCsv(String[] csvArr);

    /**
     * This method returns the CSV header to be used
     * 
     * @return
     */
    String[] getCsvHeader();

    /**
     * This method returns the CSV representation of the object
     * 
     * @return
     */
    String[] toCsv();
}
