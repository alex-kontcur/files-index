package ru.bona.fileindex.search;

import org.apache.commons.lang.Validate;

import java.util.Arrays;

/**
 * CharsPosition
 *
 * @author Kontsur Alex (bona)
 * @since 27.09.14
 */
@SuppressWarnings("ReturnOfCollectionOrArrayField")
public class CharsPosition implements Comparable<CharsPosition> {

    /*===========================================[ INSTANCE VARIABLES ]===========*/

    private char[] chars;
    private int start;
    private int stop;
    private boolean isLexem;

    /*===========================================[ CONSTRUCTORS ]=================*/

    public CharsPosition(int start, int stop, char...chars) {
        Validate.notNull(chars);

        this.start = start;
        this.stop = stop;
        this.chars = new char[chars.length];
        System.arraycopy(chars, 0, this.chars, 0, chars.length);
    }

    /*===========================================[ GETTER/SETTER ]================*/

    public int getLenght() {
        return stop - start + 1;
    }

    @Override
    public int compareTo(CharsPosition o) {
        return Integer.valueOf(start).compareTo(o.start);
    }

    public char[] getChars() {
        return chars;
    }

    public int getStart() {
        return start;
    }

    public int getStop() {
        return stop;
    }

    public boolean isLexem() {
        return chars.length > 1;
    }

    @Override
    public String toString() {
        return String.valueOf(chars);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        CharsPosition that = (CharsPosition) obj;

        if (start != that.start) {
            return false;
        }
        if (stop != that.stop) {
            return false;
        }
        if (!Arrays.equals(chars, that.chars)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = Arrays.hashCode(chars);
        result = 31 * result + start;
        result = 31 * result + stop;
        return result;
    }
}
