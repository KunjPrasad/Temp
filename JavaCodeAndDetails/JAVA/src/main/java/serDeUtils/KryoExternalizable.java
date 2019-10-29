package serDeUtils;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

/**
 * This interface identifies that a class can be converted to Kryo
 * 
 * @author Kunj
 *
 */
public interface KryoExternalizable<T> {

    /**
     * This method returns the class that should be registered in Kryo
     * 
     * @return
     */
    Class<?> getRegisterClass();

    /**
     * This method returns the Class object from Kryo record
     * 
     * @param kryo
     * @param in
     * @return
     */
    T fromKryo(Kryo kryo, Input in);

    /**
     * This method writes the object to the Kryo representation
     * 
     * @param kryo
     * @param out
     * @return
     */
    void toKryo(Kryo kryo, Output out);
}
