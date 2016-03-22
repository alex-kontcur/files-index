package ru.bona.fileindex.search.searcher;

import ru.bona.fileindex.search.CharsPosition;

/**
 * SRange
 *
 * @author Kontsur Alex (bona)
 * @since 28.09.14
 */
public class SRange implements Comparable<SRange> {

    /*===========================================[ INSTANCE VARIABLES ]===========*/

    private long start;
    private long stop;
    private CharsPosition charsPosition;

    /*===========================================[ CONSTRUCTORS ]=================*/

    public SRange(long start, long stop, CharsPosition charsPosition) {
        this.start = start;
        this.stop = stop;
        this.charsPosition = charsPosition;
    }

    /*===========================================[ CLASS METHODS ]================*/

    @Override
    public String toString() {
        return charsPosition.toString() + "(" + start + " : " + stop + ")";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        SRange sRange = (SRange) obj;

        if (start != sRange.start) {
            return false;
        }
        if (stop != sRange.stop) {
            return false;
        }
        if (!charsPosition.equals(sRange.charsPosition)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = (int) (start ^ (start >>> 32));
        result = 31 * result + (int) (stop ^ (stop >>> 32));
        result = 31 * result + charsPosition.hashCode();
        return result;
    }

    /*===========================================[ INTERFACE METHODS ]============*/

    @Override
    public int compareTo(SRange o) {
        return Long.valueOf(start).compareTo(o.start);
    }

    /*===========================================[ GETTER/SETTER ]================*/

    public long getStart() {
        return start;
    }

    public long getStop() {
        return stop;
    }

    public CharsPosition getCharsPosition() {
        return charsPosition;
    }
}
