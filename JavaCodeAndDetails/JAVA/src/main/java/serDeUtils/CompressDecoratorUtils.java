package serDeUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import net.jpountz.lz4.LZ4BlockInputStream;
import net.jpountz.lz4.LZ4BlockOutputStream;

import org.anarres.lzo.LzoAlgorithm;
import org.anarres.lzo.LzoCompressor;
import org.anarres.lzo.LzoDecompressor;
import org.anarres.lzo.LzoInputStream;
import org.anarres.lzo.LzoLibrary;
import org.anarres.lzo.LzoOutputStream;
import org.xerial.snappy.SnappyInputStream;
import org.xerial.snappy.SnappyOutputStream;

/**
 * This class provided static utilities to decorate the input and output stream based on compression option
 * 
 * @author Kunj
 *
 */
public class CompressDecoratorUtils {

    // private constant used to identify LZO algorithm
    private static final LzoAlgorithm LZO_ALGO = LzoAlgorithm.LZO1X;

    /**
     * The method decorates inputStream to add corresponding compression, or no-compression if it is selected. The
     * method throws exception because it is likely to be called in try-with-resources block - Thus there would already
     * be a catch(IOException) clause which will handle this failure
     * 
     * @param option
     * @param is
     * @return
     * @throws IOException
     */
    public static InputStream decorateInput(CompressOption option, InputStream is) throws IOException {
        switch (option) {
        case NONE: {
            return is;
        }
        case SNAPPY: {
            return new SnappyInputStream(is);
        }
        case GZIP: {
            return new GZIPInputStream(is);
        }
        case LZ4: { // customize it with bigger blocks when making proper code use
            return new LZ4BlockInputStream(is);
        }
        case LZO: {
            LzoDecompressor decompressor = LzoLibrary.getInstance().newDecompressor(LZO_ALGO, null);
            return new LzoInputStream(is, decompressor);
        }
        default: {
            throw new RuntimeException("Invalid compressOption provided to decorate "
                    + "inputStream. CompressOption=" + option.toString());
        }
        }
    }

    /**
     * The method decorates outputStream to add corresponding compression, or no-compression if it is selected. The
     * method throws exception because it is likely to be called in try-with-resources block - Thus there would already
     * be a catch(IOException) clause which will handle this failure
     * 
     * @param option
     * @param os
     * @return
     * @throws IOException
     */
    public static OutputStream decorateOutput(CompressOption option, OutputStream os) throws IOException {
        switch (option) {
        case NONE: {
            return os;
        }
        case SNAPPY: {
            return new SnappyOutputStream(os);
        }
        case GZIP: {
            return new GZIPOutputStream(os);
        }
        case LZ4: { // customize it with bigger blocks when making proper code use
            return new LZ4BlockOutputStream(os);
        }
        case LZO: {
            LzoCompressor compressor = LzoLibrary.getInstance().newCompressor(LZO_ALGO, null);
            return new LzoOutputStream(os, compressor, 256);
        }
        default: {
            throw new RuntimeException("Invalid compressOption provided to decorate "
                    + "outputStream. CompressOption=" + option.toString());
        }
        }
    }
}
