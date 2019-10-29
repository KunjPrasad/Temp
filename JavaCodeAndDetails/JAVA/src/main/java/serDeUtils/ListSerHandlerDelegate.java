package serDeUtils;

import java.io.Flushable;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.Map;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Output;
import com.google.gson.stream.JsonWriter;

import au.com.bytecode.opencsv.CSVWriter;

/**
 * This class provides utilities for serialization of "list" of records
 * 
 * @author Kunj
 *
 */
public class ListSerHandlerDelegate<T> implements Flushable, AutoCloseable {

    private SerDeOption serDeOption;
    private CSVWriter csvWt;
    private JsonWriter jsonWt;
    private OutputStream protoStuffOs; // used by protoStuff
    private OutputStream protoBufOs; // used by protocol-buffer
    private Kryo kryo; // this is definitely not thread safe!!
    private Output kryoOut;
    // this flag is used to do some operation at start, like csv-header
    private boolean justOpenFlag;

    /**
     * Defining the overloaded constructors
     * 
     * @param os
     * @param serDeOption
     */
    public ListSerHandlerDelegate(OutputStream os, SerDeOption serDeOption) {
        this(os, serDeOption, null);
    }

    public ListSerHandlerDelegate(OutputStream os, SerDeOption serDeOption, Map<String, String> options) {
        this.serDeOption = serDeOption;
        this.justOpenFlag = true;
        switch (serDeOption) {
        case CSV: {
            Writer wt;
            try {
                wt = new OutputStreamWriter(os, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                throw new RuntimeException("Coding error. Output encoding unknown");
            }
            csvWt = CSVSerDeHandlerFactory.getWriterWithOptions(wt, options);
            break;
        }
        case JSON: {
            Writer wt;
            try {
                wt = new OutputStreamWriter(os, "UTF-8");
            } catch (UnsupportedEncodingException e1) {
                e1.printStackTrace();
                throw new RuntimeException("Coding error. Output encoding unknown");
            }
            jsonWt = JSONSerDeHandlerFactory.getWriterWithOptions(wt, options);
            try {
                jsonWt.beginArray(); // also open array so data can start getting writes
            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException("Unable to write json file");
            }
            break;
        }
        case PROTOSTUFF: {
            this.protoStuffOs = os;
            break;
        }
        case PROTOBUF: {
            this.protoBufOs = os;
            break;
        }
        case KRYO: {
            this.kryo = new Kryo();
            this.kryoOut = new Output(os);
            break;
        }
        default: {
            throw new RuntimeException("ListSerHandlerDelegate not defined for option=" + serDeOption.toString());
        }
        }
    }

    // Defining flush() method
    @Override
    public void flush() throws IOException {
        switch (serDeOption) {
        case CSV: {
            csvWt.flush();
            break;
        }
        case JSON: {
            jsonWt.flush();
            break;
        }
        case PROTOSTUFF: {
            protoStuffOs.flush();
            break;
        }
        case PROTOBUF: {
            protoBufOs.flush();
            break;
        }
        case KRYO: {
            kryoOut.flush();
            break;
        }
        default:
            throw new AssertionError("Code Integration Error. This should not have been invoked");
        }
    }

    // Defining close() method
    @Override
    public void close() throws IOException {
        switch (serDeOption) {
        case CSV: {
            csvWt.close();
            break;
        }
        case JSON: {
            jsonWt.endArray();
            jsonWt.flush();
            jsonWt.close();
            break;
        }
        case PROTOSTUFF: {
            protoStuffOs.close();
            break;
        }
        case PROTOBUF: {
            protoBufOs.close();
            break;
        }
        case KRYO: {
            kryo = null;
            kryoOut.close();
            break;
        }
        default:
            throw new AssertionError("Code Integration Error. This should not have been invoked");
        }
    }

    /**
     * Defining the method to get next record from serialization. The method delegates serialization process to
     * corresponding processor
     * 
     * @param obj
     * @return
     */
    @SuppressWarnings("unchecked")
    public void putNextRecord(T obj) {
        switch (this.serDeOption) {
        case CSV: {
            if (obj instanceof CSVExternalizable) {
                putNextCsvDeserailizedRecord((CSVExternalizable<T>) obj);
            } else {
                throw new RuntimeException("Provided object-type cannot be serialized to csv");
            }
            break;
        }
        case JSON: {
            if (obj instanceof JSONExternalizable) {
                putNextJsonDeserailizedRecord((JSONExternalizable<T>) obj);
            } else {
                throw new RuntimeException("Provided object-type cannot be serialized to json");
            }
            break;
        }
        case PROTOSTUFF: {
            if (obj instanceof ProtoStuffExternalizable) {
                putNextProtoStuffDeserailizedRecord((ProtoStuffExternalizable<T>) obj);
            } else {
                throw new RuntimeException("Provided object-type cannot be serialized to protostuff");
            }
            break;
        }
        case PROTOBUF: {
            if (obj instanceof ProtoBufExternalizable) {
                putNextProtoBufDeserailizedRecord((ProtoBufExternalizable<T>) obj);
            } else {
                throw new RuntimeException("Provided object-type cannot be serialized to protocol-buffer");
            }
            break;
        }
        case KRYO: {
            if (obj instanceof KryoExternalizable) {
                putNextKryoDeserailizedRecord((KryoExternalizable<T>) obj);
            } else {
                throw new RuntimeException("Provided object-type cannot be serialized to Kryo");
            }
            break;
        }
        default: {
            throw new RuntimeException("ListSerHandlerDelegate not defined for option=" + serDeOption.toString());
        }
        }
    }

    // Utility method to put object into csv record
    private void putNextCsvDeserailizedRecord(CSVExternalizable<T> obj) {
        if (justOpenFlag) {
            csvWt.writeNext(obj.getCsvHeader());
            justOpenFlag = false;
        }
        csvWt.writeNext(obj.toCsv());
    }

    // Utility method to put object into json record
    // See: https://sites.google.com/site/gson/streaming
    private void putNextJsonDeserailizedRecord(JSONExternalizable<T> obj) {
        // even though justOpenFlag is of not used for JSON - still setting it to false for consistency
        justOpenFlag = false;
        // main processing - writing json
        obj.toJson(jsonWt);
    }

    // Utility method to put object into protostuff record
    // See: https://www.protostuff.io/documentation/runtime-schema/
    private void putNextProtoStuffDeserailizedRecord(ProtoStuffExternalizable<T> obj) {
        // even though justOpenFlag is of not used for ProtoStuff - still setting it to false for consistency
        justOpenFlag = false;
        // main processing - writing protostuff
        obj.toProtoStuff(protoStuffOs);
    }

    // Utility method to put object into protocol-buffer record
    private void putNextProtoBufDeserailizedRecord(ProtoBufExternalizable<T> obj) {
        // even though justOpenFlag is of not used for Protocol-buffer - still setting it to false for consistency
        justOpenFlag = false;
        // main processing - writing protobuf
        obj.toProtoBuf(protoBufOs);
    }

    // Utility method to put object into kryo record
    private void putNextKryoDeserailizedRecord(KryoExternalizable<T> obj) {
        if (justOpenFlag) {
            kryo.register(obj.getRegisterClass());
            justOpenFlag = false;
        }
        // main processing - writing kryo
        obj.toKryo(kryo, kryoOut);
    }
}
