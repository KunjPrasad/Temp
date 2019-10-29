package serDeUtils;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

/**
 * This interface identifies that a class can be converted to JSON.
 * 
 * @author Kunj
 *
 */
public interface JSONExternalizable<T> {

    /**
     * This method returns the Class object from JSON record
     * 
     * @param jr
     * @return
     */
    T fromJson(JsonReader jr);

    /**
     * This method writes the object to the JSON representation
     * 
     * @param jw
     * @return
     */
    void toJson(JsonWriter jw);
}
