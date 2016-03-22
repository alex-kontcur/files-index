package ru.bona.fileindex.search;

import java.util.Collection;

/**
 * SearchCompleteListener
 *
 * @author Kontsur Alex (bona)
 * @since 27.09.14
 */
public interface SearchCompleteListener {

    /*===========================================[ INTERFACE METHODS ]============*/

    void onSearchComplete(Collection<FileSearchInfo> matchedFileInfos);

    void onFailure(Throwable t);
}
