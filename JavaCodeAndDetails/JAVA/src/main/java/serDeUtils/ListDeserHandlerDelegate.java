package serDeUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.Map;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.google.gson.stream.JsonReader;

import au.com.bytecode.opencsv.CSVReader;

/**
 * This class provides utilities for deserialization of "list" of records
 * 
 * @author Kunj
 *
 */
public class ListDeserHandlerDelegate<T> implements AutoCloseable {

    private SerDeOption serDeOption;
    private CSVReader csvRd;
    private JsonReader jsonRd;
    private InputStream protoStuffIs; // used by protoStuff
    private InputStream protoBufIs; // used by protocol-buffer
    private Kryo kryo;
    private Input kryoIn;
    // this flag is used to do some operation at start, like register Kryo
    private boolean justOpenFlag;

    /**
     * Defining the overloaded constructors
     * 
     * @param is
     * @param serDeOption
     */
    public ListDeserHandlerDelegate(InputStream is, SerDeOption serDeOption) {
        this(is, serDeOption, null);
    }

    public ListDeserHandlerDelegate(InputStream is, SerDeOption serDeOption, Map<String, String> options) {
        this.serDeOption = serDeOption;
        this.justOpenFlag = true;
        switch (serDeOption) {
        case CSV: {
            Reader rd;
            try {
                rd = new InputStreamReader(is, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                throw new RuntimeException("Coding error. Output encoding unknown");
            }
            csvRd = CSVSerDeHandlerFactory.getReaderWithOptions(rd, options);
            break;
        }
        case JSON: {
            Reader rd;
            try {
                rd = new InputStreamReader(is, "UTF-8");
            } catch (UnsupportedEncodingException e1) {
                e1.printStackTrace();
                throw new RuntimeException("Coding error. Output encoding unknown");
            }
            jsonRd = JSONSerDeHandlerFactory.getReaderWithOptions(rd, options);
            break;
        }
        case PROTOSTUFF: {
            this.protoStuffIs = is;
            break;
        }
        case PROTOBUF: {
            this.protoBufIs = is;
            break;
        }
        case KRYO: {
            this.kryo = new Kryo();
            this.kryoIn = new Input(is);
            break;
        }
        default: {
            throw new RuntimeException("ListDeserHandlerDelegate not defined for option=" + serDeOption.toString());
        }
        }
    }

    // Defining close() method
    @Override
    public void close() throws IOException {
        switch (serDeOption) {
        case CSV: {
            csvRd.close();
            break;
        }
        case JSON: {
            jsonRd.close();
            break;
        }
        case PROTOSTUFF: {
            protoStuffIs.close();
            break;
        }
        case PROTOBUF: {
            protoBufIs.close();
            break;
        }
        case KRYO: {
            kryoIn.close();
            break;
        }
        default:
            throw new AssertionError("Code Integration Error. This should not have been invoked");
        }
    }

    /**
     * Defining the method to get next record from deserialization. The method delegates deserialization process to
     * corresponding processor
     * 
     * @param obj
     * @return
     */
    @SuppressWarnings("unchecked")
    public T getNextRecordAs(T obj) {
        switch (this.serDeOption) {
        case CSV: {
            if (obj instanceof CSVExternalizable) {
                return getNextCsvDeserailizedRecordAs((CSVExternalizable<T>) obj);
            } else {
                throw new RuntimeException("Provided object-type cannot be deserialized from csv");
            }
        }
        case JSON: {
            if (obj instanceof JSONExternalizable) {
                return getNextJsonDeserailizedRecordAs((JSONExternalizable<T>) obj);
            } else {
                throw new RuntimeException("Provided object-type cannot be deserialized from json");
            }
        }
        case PROTOSTUFF: {
            if (obj instanceof ProtoStuffExternalizable) {
                return getNextProtoStuffDeserailizedRecordAs((ProtoStuffExternalizable<T>) obj);
            } else {
                throw new RuntimeException("Provided object-type cannot be deserialized from Protostuff");
            }
        }
        case PROTOBUF: {
            if (obj instanceof ProtoBufExternalizable) {
                return getNextProtoBufDeserailizedRecordAs((ProtoBufExternalizable<T>) obj);
            } else {
                throw new RuntimeException("Provided object-type cannot be deserialized from Protocol-buffer");
            }
        }
        case KRYO: {
            if (obj instanceof KryoExternalizable) {
                return getNextKryoDeserailizedRecordAs((KryoExternalizable<T>) obj);
            } else {
                throw new RuntimeException("Provided object-type cannot be deserialized from Kryo");
            }
        }
        default: {
            throw new RuntimeException("ListDeserHandlerDelegate not defined for option=" + serDeOption.toString());
        }
        }
    }

    // Utility method to get deserialized object from csv record
    private T getNextCsvDeserailizedRecordAs(CSVExternalizable<T> obj) {
        justOpenFlag = false;
        String[] arr;
        try {
            arr = csvRd.readNext();
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Cannot read from csv file");
        }
        if (arr != null) {
            return obj.fromCsv(arr);
        } else {
            return null;
        }
    }

    // Utility method to get deserialized object from json record
    // See: https://sites.google.com/site/gson/streaming
    private T getNextJsonDeserailizedRecordAs(JSONExternalizable<T> obj) {
        if (justOpenFlag) {
            try {
                jsonRd.beginArray(); // also open array so data can start getting read
            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException("Unable to read json file");
            }
            this.justOpenFlag = false;
        }
        try {
            if (jsonRd.hasNext()) {
                return obj.fromJson(jsonRd);
            } else {
                jsonRd.endArray(); // close array after all data is read
                return null;
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Unable to read next json entry");
        }
    }

    // Utility method to get deserialized object from protostuff record
    // See: https://www.protostuff.io/documentation/runtime-schema/
    private T getNextProtoStuffDeserailizedRecordAs(ProtoStuffExternalizable<T> obj) {
        justOpenFlag = false;
        try {
            if (protoStuffIs.available() > 0) {
                return obj.fromProtoStuff(protoStuffIs);
            } else {
                return null;
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Cannot read from input file");
        }
    }

    // Utility method to get deserialized object from protocol-buffer record
    private T getNextProtoBufDeserailizedRecordAs(ProtoBufExternalizable<T> obj) {
        justOpenFlag = false;
        try {
            if (protoBufIs.available() > 0) {
                return obj.fromProtoBuf(protoBufIs);
            } else {
                return null;
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Cannot read from input file");
        }
    }

    // Utility method to get deserialized object from Kryo record
    private T getNextKryoDeserailizedRecordAs(KryoExternalizable<T> obj) {
        if (justOpenFlag) {
            kryo.register(obj.getRegisterClass());
            justOpenFlag = false;
        }
        try {
            if (kryoIn.available() > 0) {
                return obj.fromKryo(kryo, kryoIn);
            } else {
                return null;
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Cannot read from input file");
        }
    }
}
