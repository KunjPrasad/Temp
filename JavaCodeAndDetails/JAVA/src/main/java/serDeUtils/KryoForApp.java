package serDeUtils;

import java.io.Flushable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.pool.KryoFactory;
import com.esotericsoftware.kryo.pool.KryoPool;

/**
 * This class provides Kryo instance to be used for the operation. It does so by creating a pool of Kryo objects so it
 * can be reused. The "ForApp" Suffix is added to prevent ambiguity with Kryo's KroFactory class
 * 
 * @author Kunj
 *
 */
// The code is derived from that provided on Github page
public class KryoForApp {

    // static KryoPool utility containing threads
    private static final KryoPool kryoPool;

    static {
        KryoFactory factory = new KryoFactory() {
            public Kryo create() {
                Kryo kryo = new Kryo();
                // configure kryo instance, customize settings
                return kryo;
            }
        };
        // Build pool "with SoftReferences" enabled (optional)
        kryoPool = new KryoPool.Builder(factory).softReferences().build();
    }

    // KRYO-READER
    public static class Reader implements AutoCloseable {
        private Kryo kryo;
        private Input in;

        public Reader(InputStream is) {
            this.in = new Input(is);
            this.kryo = kryoPool.borrow();
        }

        @Override
        public void close() throws Exception {
            kryoPool.release(this.kryo);
            in.close();
        }
    }

    // KRYO-WRITER
    public static class Writer implements AutoCloseable, Flushable {
        private Kryo kryo;
        private Output out;

        public Writer(OutputStream os) {
            this.out = new Output(os);
            this.kryo = kryoPool.borrow();
        }

        @Override
        public void flush() throws IOException {
            out.flush();
        }

        @Override
        public void close() throws IOException {
            kryoPool.release(this.kryo);
            out.close();
        }
    }
}
