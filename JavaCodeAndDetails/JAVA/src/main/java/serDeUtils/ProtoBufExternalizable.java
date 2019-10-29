package serDeUtils;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * This interface identifies that a class can be converted using protocol-buffer (but not using ProtoStuff)
 * 
 * @author Kunj
 *
 */
public interface ProtoBufExternalizable<T> {

    /**
     * This method returns the Class object from protocol-buffer record
     * 
     * @param is
     * @return
     */
    T fromProtoBuf(InputStream is);

    /**
     * This method writes the object to the protocol-buffer representation
     * 
     * @param os
     * @return
     */
    void toProtoBuf(OutputStream os);
}
