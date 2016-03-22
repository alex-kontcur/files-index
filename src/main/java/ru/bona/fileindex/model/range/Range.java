package ru.bona.fileindex.model.range;

import java.nio.ByteBuffer;

/**
 * Range
 *
 * @author Kontsur Alex (a.kontsur)
 * @since 24.09.14
 */
public abstract class Range<N extends Number> implements Comparable<Range> {

    /*===========================================[ INSTANCE VARIABLES ]===========*/

    protected N start;
    protected N stop;

    /*===========================================[ CLASS METHODS ]================*/

    public abstract int getSize();
    public abstract ByteBuffer dumpRange();
    public abstract void readRange(ByteBuffer buffer);


    @SuppressWarnings("SubtractionInCompareTo")
    @Override
    public int compareTo(Range o) {
        if (start == null || stop == null || o.start == null || o.stop == null) {
            return 0;
        }
        return start.equals(o.start) ? (int) (stop.longValue() - o.stop.longValue()) : (int) (start.longValue() - o.start.longValue());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        Range range = (Range) obj;
        if (start != null ? !start.equals(range.start) : range.start != null) {
            return false;
        }
        return !(stop != null ? !stop.equals(range.stop) : range.stop != null);
    }

    @Override
    public int hashCode() {
        int result = start != null ? start.hashCode() : 0;
        result = 31 * result + (stop != null ? stop.hashCode() : 0);
        return result;
    }

    /*===========================================[ GETTER/SETTER ]================*/

    public N getStart() {
        return start;
    }

    public N getStop() {
        return stop;
    }

    public abstract void setStart(Number start);
    public abstract void setStop(Number stop);

}