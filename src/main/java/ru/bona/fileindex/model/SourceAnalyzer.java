package ru.bona.fileindex.model;

import ru.bona.fileindex.filesholder.FileInfo;
import ru.bona.fileindex.search.CharsPosition;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * SourceAnalyzer
 *
 * @author Kontsur Alex (bona)
 * @since 21.09.14
 */

public interface SourceAnalyzer {

    /*===========================================[ INTERFACE METHODS ]============*/

    void analyze(FileInfo fileInfo, AtomicBoolean terminate);

    List<CharsPosition> getCharsPositions(String target);

}