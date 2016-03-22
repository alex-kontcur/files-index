package ru.bona.fileindex.search;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * SearchRecord
 *
 * @author Kontsur Alex (bona)
 * @since 27.09.14
 */
public class SearchRecord {

    /*===========================================[ INSTANCE VARIABLES ]===========*/

    private List<CharsPosition> lexems;
    private List<CharsPosition> specChars;

    /*===========================================[ CONSTRUCTORS ]=================*/

    public SearchRecord(List<CharsPosition> lexems, List<CharsPosition> specChars) {
        this.lexems = new ArrayList<>(lexems);
        this.specChars = new ArrayList<>(specChars);
    }

    /*===========================================[ CLASS METHODS ]================*/

    public List<CharsPosition> getLexems() {
        return Collections.unmodifiableList(lexems);
    }

    public List<CharsPosition> getSpecChars() {
        return Collections.unmodifiableList(specChars);
    }
}
