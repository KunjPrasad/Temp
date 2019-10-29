package serDeUtils;

import java.io.Reader;
import java.io.Writer;
import java.util.Map;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

/**
 * This class contains methods to handle the read/write of json files
 * 
 * @author Kunj
 *
 */
public class JSONSerDeHandlerFactory {

    private JSONSerDeHandlerFactory() {
        throw new AssertionError("Cannot be instantiated");
    }

    /**
     * utility method to return jsonReader bound to given reader and provided options. While there is a separate method
     * asking for options, currently the extra options are not used. Both overloaded methods have same logic. The method
     * does not reuse any existing object binding a new reader to it, but creates new object.
     * 
     * @param rd
     * @param options
     * @return
     */
    public static final JsonReader getReaderWithOptions(Reader rd, Map<String, String> options) {
        if (options == null || options.isEmpty()) {
            return getReaderWithOptions(rd);
        }
        return new JsonReader(rd);
    }

    public static final JsonReader getReaderWithOptions(Reader rd) {
        return new JsonReader(rd);
    }

    /**
     * utility method to return jsonWriter bound to given reader and provided options. While there is a separate method
     * asking for options, currently the extra options are not used. Both overloaded methods have same logic. The method
     * does not reuse any existing object binding a new reader to it, but creates new object.
     * 
     * @param wt
     * @param options
     * @return
     */
    public static final JsonWriter getWriterWithOptions(Writer wt, Map<String, String> options) {
        if (options == null || options.isEmpty()) {
            return getWriterWithOptions(wt);
        }
        return new JsonWriter(wt);
    }

    public static final JsonWriter getWriterWithOptions(Writer wt) {
        return new JsonWriter(wt);
    }
}
