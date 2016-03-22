package ru.bona.fileindex.model.lexem.factory;

import ru.bona.fileindex.model.lexem.LexemeIndexFrame;
import ru.bona.fileindex.model.range.Range;

import java.util.SortedSet;

/**
 * LexemeIndexFrameFactory
 *
 * @author Kontsur Alex (bona)
 * @since 27.09.14
 */
public interface LexemeIndexFrameFactory {

    /*===========================================[ CLASS METHODS ]================*/

    LexemeIndexFrame createLexemeIndexFrame(String lexeme, Number fileNum, SortedSet<Range> lexemRanges);
    
}
