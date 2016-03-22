package ru.bona.fileindex.search;

import org.apache.commons.lang.Validate;
import ru.bona.fileindex.model.range.Range;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;

/**
 * FileSearchInfo
 *
 * @author Kontsur Alex (bona)
 * @since 02.10.14
 */
public class FileSearchInfo {

    /*===========================================[ INSTANCE VARIABLES ]===========*/

    private String source;
    private Map<File, SortedSet<Range>> fileMap;

    /*===========================================[ CONSTRUCTORS ]=================*/

    public FileSearchInfo(String source, Map<File, SortedSet<Range>> fileMap) {
        Validate.notNull(source);
        this.source = source;
        this.fileMap = new HashMap<>(fileMap);
    }

    /*===========================================[ CLASS METHODS ]================*/

    public Map<File, SortedSet<Range>> getFileMap() {
        return Collections.unmodifiableMap(fileMap);
    }

    /*===========================================[ GETTER/SETTER ]================*/

    public String getSource() {
        return source;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        FileSearchInfo searchInfo = (FileSearchInfo) obj;
        return source.equals(searchInfo.source);
    }

    @Override
    public int hashCode() {
        return source.hashCode();
    }
}
