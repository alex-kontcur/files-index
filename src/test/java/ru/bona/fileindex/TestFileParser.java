package ru.bona.fileindex;

import ru.bona.fileindex.model.fileparser.FileParser;

/**
 * TestFileParser
 *
 * @author Kontsur Alex (bona)
 * @since 05.10.14
 */
public class TestFileParser extends FileParser {

    /*===========================================[ CLASS METHODS ]================*/

    @Override
    public int getCharBufferSize() {
        return 3;
    }
}
