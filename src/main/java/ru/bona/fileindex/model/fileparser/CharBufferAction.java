package ru.bona.fileindex.model.fileparser;

import ru.bona.fileindex.filesholder.FileInfo;
import ru.bona.fileindex.model.BufferInfo;

/**
 * CharBufferAction
 *
 * @author Kontsur Alex (bona)
 * @since 22.09.14
 */
public interface CharBufferAction {

    /*===========================================[ INTERFACE METHODS ]============*/

    void processBuffer(BufferInfo bufferInfo, FileInfo fileInfo);

}
