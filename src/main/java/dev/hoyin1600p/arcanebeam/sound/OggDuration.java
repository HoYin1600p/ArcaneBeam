package dev.hoyin1600p.arcanebeam.sound;

/**
 * Reads the playing time of an Ogg Vorbis file from its container, without decoding it: the sample rate is in the
 * identification header of the first page and the length is the granule position (the sample count) of the last
 * page. Used to size a sound's lifetime from the file itself, so resource pack sounds of any length play to the end.
 */
public final class OggDuration {
    private static final int PAGE_HEADER_SIZE = 27;

    private OggDuration() {
    }

    /** Duration in seconds, or -1 when the data is not a readable Ogg Vorbis stream. */
    public static double seconds(byte[] data) {
        if (data == null) {
            return -1.0D;
        }
        long sampleRate = 0L;
        long lastGranule = -1L;
        int offset = 0;
        while (offset + PAGE_HEADER_SIZE <= data.length) {
            if (data[offset] != 'O' || data[offset + 1] != 'g' || data[offset + 2] != 'g' || data[offset + 3] != 'S') {
                return -1.0D;
            }
            long granule = readLongLittleEndian(data, offset + 6);
            int segmentCount = data[offset + 26] & 0xFF;
            if (offset + PAGE_HEADER_SIZE + segmentCount > data.length) {
                break;
            }
            int bodySize = 0;
            for (int segment = 0; segment < segmentCount; segment++) {
                bodySize += data[offset + PAGE_HEADER_SIZE + segment] & 0xFF;
            }
            int bodyStart = offset + PAGE_HEADER_SIZE + segmentCount;
            if (sampleRate == 0L && bodySize >= 16 && bodyStart + 16 <= data.length && data[bodyStart] == 1
                    && data[bodyStart + 1] == 'v' && data[bodyStart + 2] == 'o' && data[bodyStart + 3] == 'r') {
                sampleRate = readIntLittleEndian(data, bodyStart + 12) & 0xFFFFFFFFL;
            }
            if (granule >= 0L) {
                lastGranule = granule;
            }
            offset = bodyStart + bodySize;
        }
        if (sampleRate <= 0L || lastGranule < 0L) {
            return -1.0D;
        }
        return (double) lastGranule / (double) sampleRate;
    }

    private static int readIntLittleEndian(byte[] data, int offset) {
        return (data[offset] & 0xFF) | (data[offset + 1] & 0xFF) << 8 | (data[offset + 2] & 0xFF) << 16 | (data[offset + 3] & 0xFF) << 24;
    }

    private static long readLongLittleEndian(byte[] data, int offset) {
        long value = 0L;
        for (int index = 7; index >= 0; index--) {
            value = value << 8 | (data[offset + index] & 0xFF);
        }
        return value;
    }
}
