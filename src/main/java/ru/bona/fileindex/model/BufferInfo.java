package ru.bona.fileindex.model;

import java.nio.CharBuffer;

/**
* BufferInfo
*
* @author Kontsur Alex (bona)
* @since 22.09.14
*/
public class BufferInfo implements Comparable<BufferInfo> {

    /*===========================================[ INSTANCE VARIABLES ]===========*/

    private String sourcePart;
    private boolean last;
    private long start;
    private long stop;

    /*===========================================[ CONSTRUCTORS ]=================*/

    public BufferInfo(CharBuffer buffer, long start, long stop) {
        this.start = start;
        this.stop = stop;

        sourcePart = buffer.toString().replace("\u0000", "");
    }

    /*===========================================[ GETTER/SETTER ]================*/

    @Override
    public int compareTo(BufferInfo o) {
        int cmp = Long.valueOf(start).compareTo(o.start);
        return cmp == 0 ? Long.valueOf(stop).compareTo(o.stop) : cmp;
    }

    public String getSourcePart() {
        return sourcePart;
    }

    public long getStart() {
        return start;
    }

    public long getStop() {
        return stop;
    }

    public boolean isLast() {
        return last;
    }

    public void setLast(boolean last) {
        this.last = last;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        BufferInfo bufferInfo = (BufferInfo) obj;
        if (start != bufferInfo.start) {
            return false;
        }
        if (stop != bufferInfo.stop) {
            return false;
        }
        if (!sourcePart.equals(bufferInfo.sourcePart)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int result = sourcePart.hashCode();
        result = 31 * result + (int) (start ^ (start >>> 32));
        result = 31 * result + (int) (stop ^ (stop >>> 32));
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("BufferInfo");
        sb.append("{sourcePart='").append(sourcePart).append('\'');
        sb.append(", start=").append(start);
        sb.append(", stop=").append(stop);
        sb.append('}');
        return sb.toString();
    }
}
