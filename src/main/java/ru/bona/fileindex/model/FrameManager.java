package ru.bona.fileindex.model;

import ru.bona.fileindex.model.range.IntRange;

import java.util.Map;
import java.util.SortedMap;

/**
 * FrameManager
 *
 * @author Kontsur Alex (bona)
 * @since 22.09.14
 */
public interface FrameManager<L extends Loadable, S extends Storable> {

    /*===========================================[ CLASS METHODS ]================*/

    L loadFromIndexByRange(Map<Number, IntRange> frameInfoMap, char... c);

    L loadFromIndex(char... c);

    SortedMap<Number, IntRange> loadFromRegistry(char...c);

    boolean deleteFromIndex(Number fileNum);

    void store(S indexFrame);

}