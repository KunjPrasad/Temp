package dataObject;

import io.protostuff.LinkedBuffer;
import io.protostuff.ProtostuffIOUtil;
import io.protostuff.Schema;
import io.protostuff.runtime.RuntimeSchema;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import serDeUtils.CSVExternalizable;
import serDeUtils.JSONExternalizable;
import serDeUtils.KryoExternalizable;
import serDeUtils.ProtoBufExternalizable;
import serDeUtils.ProtoStuffExternalizable;
import utils.HashUtils;

/**
 * This is the data class for PROBLEM_1. It is in form of this class that the input/output should be made
 * 
 * @author Kunj
 *
 */
public class DataClass implements CSVExternalizable<DataClass>, JSONExternalizable<DataClass>,
        ProtoStuffExternalizable<DataClass>, ProtoBufExternalizable<DataClass>, KryoExternalizable<DataClass> {

    // A marker that enables DataClass to change its HEX Setting in different version and still get correct data
    // Making it final enables it to be set only once
    private final HexVersion hexVersion;
    private String data;
    // All following values are derivable from "data" member. So they should not play a role in any other logic - like,
    // toString, equals, hash, etc
    private transient String hex;
    private transient Map<Character, Integer> charCountMap;
    private transient Lock mapLock;

    // this constant helps in versioning the DataClass and be flexible to choose different hash implementations
    private static final HexVersion HEX_USED = HexVersion.SHA_256;

    public DataClass() {
        this(HEX_USED, null);
    }

    public DataClass(String data) {
        this(HEX_USED, data);
    }

    public DataClass(HexVersion hexVersion, String data) {

        this.mapLock = new ReentrantLock(); // new lock object defined - do first because map-lock operation is
                                            // undefined unless this object is made
        this.hexVersion = hexVersion; // set hex version before setting data because it is used by latter
        this.setData(data);// using setter - because it has logic to automatically fill other fields
    }

    // LOCK WRAPPING
    // these internal constants define how long a thread blocks when requesting for a lock
    private static final long LOCK_TRY_TIME = 10;
    private static final TimeUnit LOCK_TRY_TIME_UNIT = TimeUnit.SECONDS;
    // these constants define how long a thread will wait before attempting to get lock again
    private static final long LOCK_TRY_FAIL_TIMEOUT = 5000; // in milliseconds

    // this method accepts a function and argument and wraps the method call with logic to obtain lock
    // do realize that once issue with this process is that autoboxing is necessary and can end up creating slowdowns
    private <T, R> R doWithMapLock(Function<T, R> f, T t) {
        boolean locked = false;
        try {
            // try to get lock with timeout. If lock not achieved, sleep and try again
            while (!locked) {
                locked = mapLock.tryLock(LOCK_TRY_TIME, LOCK_TRY_TIME_UNIT);
                if (!locked) {
                    Thread.sleep(LOCK_TRY_FAIL_TIMEOUT);
                }
            }
            // lock achieved - now do logic
            return f.apply(t);
        } catch (InterruptedException e) {
            // for now just write the stacktrace
            e.printStackTrace();
            throw new RuntimeException("Error in obtaining lock on map object");
        } finally {
            if (locked) {
                mapLock.unlock();
            }
        }
    }

    // constants used to label field names when serializing/deserializing
    private static final String HEX_VERSION_STR = "hexVersion";
    private static final String DATA_STR = "data";

    // INTERFACE METHODS
    // ---- utility methods to change to/from serialization proxy
    public static Class<?> getSerDeProxyClass() {
        return DataClassSerDeProxy.class;
    }

    public static DataClass fromSerDeProxy(DataClassSerDeProxy dcProxy) {
        return new DataClass(HexVersion.fromValue(dcProxy.getHexVersionStr()), dcProxy.getData());
    }

    public static DataClass fromTypeErasedSerDeProxy(Object obj) {
        if (obj instanceof DataClassSerDeProxy) {
            DataClassSerDeProxy dcProxy = (DataClassSerDeProxy) obj;
            return new DataClass(HexVersion.fromValue(dcProxy.getHexVersionStr()), dcProxy.getData());
        } else {
            throw new RuntimeException("Provided object is not serialization-proxy of DataClass");
        }
    }

    public DataClassSerDeProxy toSerDeProxy() {
        return new DataClassSerDeProxy(this.hexVersion.toString(), this.data);
    }

    // ---- CSV
    @Override
    public DataClass fromCsv(String[] csvArr) {
        if (csvArr.length == 1) {
            return new DataClass(HEX_USED, csvArr[0]);
        }
        if (csvArr.length == 2) {
            return new DataClass(HexVersion.fromValue(csvArr[0]), csvArr[1]);
        }
        // if none of the above cases, then throw error
        throw new RuntimeException("Cannot deserialize from csv unless there are 1 or 2 entries");
    }

    @Override
    public String[] getCsvHeader() {
        return new String[] { HEX_VERSION_STR, DATA_STR };
    }

    @Override
    public String[] toCsv() {
        return new String[] { HEX_USED.toString(), this.getData() };
    }

    // ---- JSON
    @Override
    public DataClass fromJson(JsonReader jr) {
        String data = null; // default
        String hexVersionStr = HEX_USED.toString(); // default
        // start reading values from json
        try {
            jr.beginObject();
            while (jr.hasNext()) {
                String name = jr.nextName();
                if (name.equals(DATA_STR)) {
                    data = jr.nextString();
                    continue;
                }
                if (name.equals(HEX_VERSION_STR)) {
                    hexVersionStr = jr.nextString();
                    continue;
                }
                // if there os some other field except the above 2, throw exception of unknwon json format
                throw new RuntimeException("Obtained unidentified field " + name + " in json.");
            }
            jr.endObject();
            return new DataClass(HexVersion.fromValue(hexVersionStr), data);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Unable to parse json");
        }
    }

    @Override
    public void toJson(JsonWriter jw) {
        try {
            jw.beginObject();
            jw.name(HEX_VERSION_STR).value(hexVersion.toString());
            jw.name(DATA_STR).value(data);
            jw.endObject();
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Unable to write json entry");
        }
    }

    // ---- PROTOSTUFF
    private static Schema<DataClassSerDeProxy> schema = RuntimeSchema.getSchema(DataClassSerDeProxy.class);

    @Override
    public DataClass fromProtoStuff(InputStream is) {
        DataClassSerDeProxy proxy = schema.newMessage();
        try {
            ProtostuffIOUtil.mergeDelimitedFrom(is, proxy, schema);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Cannot deserialize from ProtoStuff");
        }
        return fromSerDeProxy(proxy);
    }

    @Override
    public void toProtoStuff(OutputStream os) {
        LinkedBuffer buffer = LinkedBuffer.allocate();
        try {
            ProtostuffIOUtil.writeDelimitedTo(os, toSerDeProxy(), schema, buffer);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Cannot serialize from ProtoStuff");
        }
    }

    // ---- PROTOBUF
    @Override
    public DataClass fromProtoBuf(InputStream is) {
        DataClassProtoBufProxy.DataClass.Builder bld = DataClassProtoBufProxy.DataClass.newBuilder();
        try {
            bld.mergeDelimitedFrom(is);
            DataClassProtoBufProxy.DataClass varObj = bld.build();
            if (varObj.getHexVersion().equals("")) {
                return new DataClass();
            }
            return new DataClass(HexVersion.fromValue(varObj.getHexVersion()), varObj.getData());
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Cannot deserialize from protocol-buffer");
        }
    }

    @Override
    public void toProtoBuf(OutputStream os) {
        DataClassProtoBufProxy.DataClass.Builder bld = DataClassProtoBufProxy.DataClass.newBuilder();
        bld.setHexVersion(this.hexVersion.toString());
        bld.setData(this.data);
        try {
            bld.build().writeDelimitedTo(os);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Cannot serialize to protocol-buffer");
        }
    }

    // ---- KRYO
    @Override
    public Class<?> getRegisterClass() {
        return getSerDeProxyClass();
    }

    @Override
    public DataClass fromKryo(Kryo kryo, Input in) {
        return fromTypeErasedSerDeProxy(kryo.readObject(in, getRegisterClass()));
    }

    @Override
    public void toKryo(Kryo kryo, Output out) {
        kryo.writeObject(out, toSerDeProxy());
    }

    // OTHER METHODS
    @Override
    public String toString() {
        return data; // since the response does not depend on map, so no need to hold lock.
    }

    // constants used in defining the method toStringWithInternals()
    private static final String FIELD_SEP = ",";
    private static final String MAP_ENTRY_SEP = ";";
    private static final String MAP_KEY_SEP = "=";

    // This is different method than toString() --> toString() only showing the main string that decides values for all
    // class members
    public String toStringWithInternals() {
        return doWithMapLock(new Function<Void, String>() {
            @Override
            public String apply(Void t) {
                StringBuilder stbl = new StringBuilder();
                charCountMap.entrySet().stream()
                        .map((entry) -> entry.getKey() + MAP_KEY_SEP + entry.getValue() + MAP_ENTRY_SEP)
                        .forEach((str) -> stbl.append(str));
                return data + FIELD_SEP
                        + hex + FIELD_SEP
                        // to exclude last MAP_ENTRY_SEP character, if string data was non-empty
                        + (stbl.length() == 0 ? "" : stbl.substring(0, stbl.length() - 1));
            }
        }, null);
    }

    // this is the hash value for the class even when the data object is null. This prevents it from being confused with
    // some other class that may also have only 1 string member. Even when that member is null, the 2 classes should be
    // distinct
    // NOTE: Once can use "ThreadLocalRandom.current().nextInt()", but then it means that same class running at
    // different time or on different VM(s) will be dissimilar
    private static final int DEFAULT_HASHCODE = 1000;

    // hashcode method - only based on string content
    @Override
    public int hashCode() {
        return doWithMapLock(new Function<Void, Integer>() {
            @Override
            public Integer apply(Void t) {
                int hashResult = DEFAULT_HASHCODE;
                hashResult = HashUtils.updateHash(hashResult, (data == null) ? HashUtils.NULL_HASH : data.hashCode());
                return hashResult;
            }
        }, null);
    }

    // equals method - only based on string content
    @Override
    public boolean equals(Object o) {
        return doWithMapLock(new Function<Object, Boolean>() {
            @Override
            public Boolean apply(Object t) {
                if (!(t instanceof DataClass)) {
                    return false;
                }
                DataClass t2 = (DataClass) t;
                if ((data == null && t2.data != null) || (!data.equals(t2.data))) {
                    return false;
                }
                return true;
            }
        }, null);
    }

    // making the setter as final since it has logic used by constructor
    public final void setData(String data) {
        doWithMapLock(new Function<String, Void>() {
            @Override
            public Void apply(String t) {
                DataClass.this.data = t;
                DataClass.this.hex = DataClassUtils.getHexForDataAndHexVersion(t, DataClass.this.hexVersion);
                // no need to copy the returned map from util-class to new one before storing reference since a new,
                // unique map is returned on call
                DataClass.this.charCountMap = DataClassUtils.getCharCountMapForString(t);
                return null;
            }
        }, data);
    }

    // GETTERS
    public String getData() {
        return data;
    }

    // Only getter, no setter - since it is done automatically from string
    public String getHex() {
        return hex;
    }

    public Map<Character, Integer> getCharCountMap() {
        return doWithMapLock(new Function<Void, Map<Character, Integer>>() {
            @Override
            public Map<Character, Integer> apply(Void t) {
                Map<Character, Integer> copyMap = new HashMap<>();
                for (Entry<Character, Integer> entry : charCountMap.entrySet()) {
                    copyMap.put(entry.getKey(), new Integer(entry.getValue().intValue()));
                }
                return copyMap;
            }
        }, null);
    }

    // BUILDER CLASS - NOTE, it keeps default hex, and a utility method from main class to get Builder instance
    // accessing builder visa utility creates more abstraction hiding the actual builder class from user
    public DataClass.Builder builder() {
        return new DataClass.Builder();
    }

    public static class Builder {

        private HexVersion hexVersion;
        private String data;

        private Builder() {
            hexVersion = HEX_USED;
            data = null;
        }

        public Builder hexVersion(HexVersion hexVersion) {
            this.hexVersion = hexVersion;
            return this;
        }

        public Builder data(String data) {
            this.data = data;
            return this;
        }

        public DataClass build() {
            return new DataClass(hexVersion, data);
        }
    }
}
