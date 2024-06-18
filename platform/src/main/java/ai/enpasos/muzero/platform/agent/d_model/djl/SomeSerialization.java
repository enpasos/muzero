package ai.enpasos.muzero.platform.agent.d_model.djl;
import ai.djl.ndarray.*;
import ai.djl.ndarray.types.*;

import java.io.*;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SomeSerialization {

    private static final Logger logger = Logger.getLogger(SomeSerialization.class.getName());
    private static final int FILE_VERSION = 1;

    /**
     * Saves an NDList to a file.
     *
     * @param list     the NDList to save
     * @param filename the file to save the NDList to
     */
    public static void saveNDList(NDList list, String filename) {
        if (list == null || filename == null || filename.isEmpty()) {
            throw new IllegalArgumentException("NDList and filename must not be null or empty");
        }

        try (RandomAccessFile aFile = new RandomAccessFile(filename, "rw");
             FileChannel channel = aFile.getChannel()) {

            // Write file version
            ByteBuffer versionBuffer = ByteBuffer.allocate(Integer.BYTES);
            versionBuffer.order(ByteOrder.LITTLE_ENDIAN);
            versionBuffer.putInt(FILE_VERSION);
            versionBuffer.flip();
            channel.write(versionBuffer);

            for (NDArray nd : list) {
                int dimensions = nd.getShape().dimension();
                ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES + Long.BYTES * dimensions + Integer.BYTES);
                buffer.order(ByteOrder.LITTLE_ENDIAN); // Ensure the buffer uses the correct byte order

                buffer.putInt(dimensions);
                for (long dim : nd.getShape().getShape()) {
                    buffer.putLong(dim);
                }
                buffer.putInt(nd.getDataType().ordinal());
                buffer.flip();
                channel.write(buffer);

                ByteBuffer dataBuffer = nd.toByteBuffer();
                dataBuffer.order(ByteOrder.LITTLE_ENDIAN);
                channel.write(dataBuffer);
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error while saving NDList", e);
        }
    }

    /**
     * Loads an NDList from a file.
     *
     * @param filename the file to load the NDList from
     * @param manager  the NDManager to create NDArrays
     * @return the loaded NDList
     */
    public static NDList loadNDList(String filename, NDManager manager) {
        if (filename == null || filename.isEmpty() || manager == null) {
            throw new IllegalArgumentException("Filename and NDManager must not be null or empty");
        }

        NDList list = new NDList();
        try (RandomAccessFile aFile = new RandomAccessFile(filename, "r");
             FileChannel channel = aFile.getChannel()) {

            ByteBuffer buffer = ByteBuffer.allocate((int) channel.size());
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            channel.read(buffer);
            buffer.flip();

            // Read file version
            int fileVersion = buffer.getInt();
            if (fileVersion != FILE_VERSION) {
                throw new IOException("Unsupported file version: " + fileVersion);
            }

            while (buffer.hasRemaining()) {
                try {
                    int dimensions = buffer.getInt();
                    long[] shape = new long[dimensions];

                    for (int i = 0; i < dimensions; i++) {
                        shape[i] = buffer.getLong();
                    }

                    DataType dataType = DataType.values()[buffer.getInt()];
                    NDArray array = manager.create(new Shape(shape), dataType);

                    long totalElements = array.getShape().size();
                    int bytesPerElement = dataType.getNumOfBytes();
                    int bufferSize = (int) (totalElements * bytesPerElement);

                    if (buffer.remaining() < bufferSize) {
                        logger.log(Level.WARNING, "Buffer underflow: insufficient data for NDArray. Expected {0} bytes, but only {1} bytes remaining.",
                                new Object[]{bufferSize, buffer.remaining()});
                        break;
                    }

                    byte[] tempArray = new byte[bufferSize];
                    buffer.get(tempArray);
                    ByteBuffer dataBuffer = ByteBuffer.wrap(tempArray);
                    dataBuffer.order(ByteOrder.LITTLE_ENDIAN);

                    array.set(dataBuffer);
                    list.add(array);
                } catch (BufferUnderflowException e) {
                    logger.log(Level.WARNING, "Buffer underflow while reading NDArray data. Potentially partial data. Skipping NDArray.", e);
                    break;
                }
            }

            logger.log(Level.INFO, "Finished reading NDList.");
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error while loading NDList", e);
        }
        return list;
    }


    public static void saveIntArray(int[] data, String filename) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < data.length - 1; i++) {
            sb.append(data[i]).append(",");
        }
        sb.append(data[data.length - 1]);
        try (FileWriter writer = new FileWriter(filename);) {
            writer.write(sb.toString());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static int[] loadIntArray(String filename)   {
        StringBuilder sb = new StringBuilder();
        try (FileReader reader =  new FileReader(filename)) {
            int ch;
            while ((ch = reader.read()) != -1) {
                sb.append((char) ch);
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        String[] stringArray = sb.toString().split(",");
        int[] data = new int[stringArray.length];
        for (int i = 0; i < data.length; i++) {
            data[i] = Integer.parseInt(stringArray[i]);
        }
        return data;
    }


    public static void saveBooleanArray(boolean[][][] data, String filename)  {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < data.length; i++) {
            for (int j = 0; j < data[i].length; j++) {
                for (int k = 0; k < data[i][j].length; k++) {
                    sb.append(data[i][j][k] ? "T" : "F").append(",");
                }
                sb.deleteCharAt(sb.length() - 1).append("|");
            }
            sb.deleteCharAt(sb.length() - 1).append("\n");
        }
        try (FileWriter writer = new FileWriter(filename);) {
            writer.write(sb.toString());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

   public static boolean[][][] loadBooleanArray(String filename)   {
        try (FileReader reader = new FileReader(filename);) {
            StringBuilder sb = new StringBuilder();
            int ch;
            while ((ch = reader.read()) != -1) {
                sb.append((char) ch);
            }
            reader.close();

            String[] lines = sb.toString().split("\n");
            int dim1 = lines.length;
            String[][] dataString = new String[dim1][];

            for (int i = 0; i < dim1; i++) {
                dataString[i] = lines[i].split("\\|");
            }

            int dim2 = dataString[0].length;
            boolean[][][] data = new boolean[dim1][dim2][];

            for (int i = 0; i < dim1; i++) {
                for (int j = 0; j < dim2; j++) {
                    String[] row = dataString[i][j].split(",");
                    data[i][j] = new boolean[row.length];
                    for (int k = 0; k < row.length; k++) {
                        data[i][j][k] = row[k].equals("T");
                    }
                }
            }

            return data;
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
