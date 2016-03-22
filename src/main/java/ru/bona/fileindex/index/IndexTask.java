package ru.bona.fileindex.index;

import org.apache.commons.lang.Validate;
import ru.bona.fileindex.filesholder.FileInfo;

/**
 * IndexTask
 *
 * @author Kontsur Alex (bona)
 * @since 25.09.14
 */
public class IndexTask {

    /*===========================================[ INSTANCE VARIABLES ]===========*/

    private IndexingListener listener;
    private FileInfo fileInfo;

    /*===========================================[ CONSTRUCTORS ]=================*/

    public IndexTask(FileInfo fileInfo) {
        this(fileInfo, null);
    }

    public IndexTask(FileInfo fileInfo, IndexingListener listener) {
        Validate.notNull(fileInfo);
        
        this.fileInfo = fileInfo;
        this.listener = listener;
    }

    /*===========================================[ CLASS METHODS ]================*/

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        IndexTask indexTask = (IndexTask) obj;
        return fileInfo.equals(indexTask.fileInfo);
    }

    @Override
    public int hashCode() {
        return fileInfo.hashCode();
    }

    /*===========================================[ GETTER/SETTER ]================*/

    public FileInfo getFileInfo() {
        return fileInfo;
    }

    public IndexingListener getListener() {
        return listener;
    }
}
