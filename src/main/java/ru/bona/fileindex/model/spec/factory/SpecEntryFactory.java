package ru.bona.fileindex.model.spec.factory;

import ru.bona.fileindex.model.spec.SpecEntry;

/**
 * SpecEntryFactory
 *
 * @author Kontsur Alex (bona)
 * @since 27.09.14
 */
public interface SpecEntryFactory {

    /*===========================================[ CLASS METHODS ]================*/

    SpecEntry createSpecEntry(Character character);
}
