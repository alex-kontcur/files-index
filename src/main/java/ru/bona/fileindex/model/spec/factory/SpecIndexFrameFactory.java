package ru.bona.fileindex.model.spec.factory;

import ru.bona.fileindex.model.spec.SpecIndexFrame;
import ru.bona.fileindex.model.spec.synctree.SyncTree;

import java.util.Map;

/**
 * SpecIndexFrameFactory
 *
 * @author Kontsur Alex (bona)
 * @since 27.09.14
 */
public interface SpecIndexFrameFactory {

    /*===========================================[ CLASS METHODS ]================*/

    SpecIndexFrame createSpecIndexFrame( Number fileNum, Map<Character, SyncTree> treeMap);
}
