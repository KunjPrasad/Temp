package serDeUtils;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * This interface identifies that a class can be converted using protoStuff (https://www.protostuff.io/)
 * 
 * @author Kunj
 *
 */
public interface ProtoStuffExternalizable<T> {

    /**
     * This method returns the Class object from ProtoStuff record
     * 
     * @param is
     * @return
     */
    T fromProtoStuff(InputStream is);

    /**
     * This method writes the object to the ProtoStuff representation
     * 
     * @param os
     * @return
     */
    void toProtoStuff(OutputStream os);
}
