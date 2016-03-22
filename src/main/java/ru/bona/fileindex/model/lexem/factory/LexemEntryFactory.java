package ru.bona.fileindex.model.lexem.factory;

import ru.bona.fileindex.model.lexem.LexemEntry;

/**
 * LexemEntryFactory
 *
 * @author Kontsur Alex (bona)
 * @since 27.09.14
 */
public interface LexemEntryFactory {

    /*===========================================[ CLASS METHODS ]================*/

    LexemEntry createLexemEntry(String lexem);
}
