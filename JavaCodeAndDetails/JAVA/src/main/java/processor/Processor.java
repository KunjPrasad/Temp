package processor;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;

import serDeUtils.CipherDecoratorUtils;
import serDeUtils.CompressDecoratorUtils;
import serDeUtils.CompressOption;
import serDeUtils.ListDeserHandlerDelegate;
import serDeUtils.ListSerHandlerDelegate;
import serDeUtils.SerDeOption;
import dataObject.DataClass;

/**
 * This is the main class solving PROBLEM_1
 * 
 * @author Kunj
 *
 */
public class Processor {

    // constant to decide if enable or disable cipher
    private static final boolean enableInCipher = false;
    private static final boolean enableOutCipher = false;

    /**
     * the main method solving PROBLEM_1
     * 
     * @param args
     */
    public static void main(String[] args) {
        // start log
        System.out.println("Starting Processor at " + new Date());
        // USER INPUT LOGIC
        // 1) confirm the input count
        basicCheckArgs(args);
        // 2) Do assignments based on logic. In the process, do validation
        String inFilePath = getInputFilePath(args[0]);
        SerDeOption inSerDeOption = SerDeOption.fromValue(args[1]);
        SerDeOption outSerDeOption = SerDeOption.fromValue(args[1]);
        CompressOption inCompressOption = CompressOption.fromValue(args[2]);
        CompressOption outCompressOption = CompressOption.fromValue(args[2]);
        if (args.length >= 4) {
            outSerDeOption = SerDeOption.fromValue(args[3]);
        }
        if (args.length >= 5) {
            outCompressOption = CompressOption.fromValue(args[4]);
        }
        // 3) Make additional data items based on input - use variables from #2 now, not the args[i] values
        // NOTE: Cipher-ing of stream is done even before compression, so compressed data is encrypted. This even
        // prevents someone from knowing the type of compression being used
        String outFilePath = getOutputFilePath(inFilePath);
        try (InputStream is = CompressDecoratorUtils.decorateInput(inCompressOption,
                CipherDecoratorUtils.decorateInput(enableInCipher,
                        new BufferedInputStream(new FileInputStream(inFilePath))));
                OutputStream os = CompressDecoratorUtils.decorateOutput(outCompressOption,
                        CipherDecoratorUtils.decorateOutput(enableOutCipher, new BufferedOutputStream(
                                new FileOutputStream(outFilePath))))) {
            // 3 - continued
            ListDeserHandlerDelegate<DataClass> deSer = new ListDeserHandlerDelegate<DataClass>(is, inSerDeOption);
            ListSerHandlerDelegate<DataClass> ser = new ListSerHandlerDelegate<DataClass>(os, outSerDeOption);
            // MAIN-PROCESSING LOGIC
            // 4) read data. Continue till it is not null
            DataClass varArg = new DataClass();
            DataClass data = deSer.getNextRecordAs(varArg);
            while (data != null) {
                // 5) write result to serializing destination
                ser.putNextRecord(data);
                // get data for next loop
                data = deSer.getNextRecordAs(varArg);
            }
            // 6) flush and close resource - they flush and close underlying streams
            deSer.close();
            ser.flush();
            os.flush();
            ser.close();
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Error reading/writing file");
        }
        // end log
        System.out.println("Ending Processor at " + new Date());
    }

    /**
     * This utility method checks if User has not given correct inputs. If not, then it throws exceptions
     * 
     * @param args
     */
    private static void basicCheckArgs(String[] args) {
        // check that there are at least 3 args
        if (args.length < 3) {
            throw new RuntimeException("minimum 3 inputs needed: path of input file, type of input SerDe, type of "
                    + "input compression. " + args.length + " prvided. Exitting!");
        }
    }

    /**
     * This function reads the argument and returns the path for the input file. Any necessary validations are also done
     * 
     * @param arg
     * @return
     */
    private static String getInputFilePath(String arg) {
        // No need to check for null because inputs are coming from console, so it cannot be null
        File f = new File(arg);
        if (!f.exists() || !f.isFile() || !f.canRead()) {
            throw new RuntimeException("Input file (" + arg + ") either does not exist, or is not a file, "
                    + "or cannot be read");
        }
        return arg.replace('\\', '/'); // to keep paths in unix format
    }

    // constant used in searching for file extension
    private static final String DOT = ".";
    private static final String FILE_SEP = "/";
    // constant used in making output fileName
    private static final String OUTFILE_SUFFIX = "_result";

    /**
     * This function identifies the output filename based on input filename given by user. Any necessary validations are
     * also done
     * 
     * @param arg
     * @return
     */
    private static String getOutputFilePath(String arg) {
        // get fileName from input file name, excluding the extension
        int dotIndx = arg.lastIndexOf(DOT);
        int fileSepIndx = arg.lastIndexOf(FILE_SEP);
        if (dotIndx != -1 && dotIndx > fileSepIndx) { // dotIndx>fileSepIndx means dot is in fileName not some previous
                                                      // folder
            return arg.substring(0, dotIndx) + OUTFILE_SUFFIX + arg.substring(dotIndx, arg.length());
        } else {
            return arg + OUTFILE_SUFFIX;
        }
    }
}
