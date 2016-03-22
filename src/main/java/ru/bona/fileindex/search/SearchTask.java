package ru.bona.fileindex.search;

/**
 * SearchTask
 *
 * @author Kontsur Alex (bona)
 * @since 27.09.14
 */
@SuppressWarnings("ReturnOfCollectionOrArrayField")
public class SearchTask {

    /*===========================================[ INSTANCE VARIABLES ]===========*/

    private SearchCompleteListener listener;
    private SearchRecord searchRecord;
    private String source;

    /*===========================================[ CONSTRUCTORS ]=================*/

    public SearchTask(String source, SearchRecord searchRecord, SearchCompleteListener listener) {
        this.source = source;
        this.listener = listener;
        this.searchRecord = searchRecord;
    }

    /*===========================================[ GETTER/SETTER ]================*/

    public SearchCompleteListener getListener() {
        return listener;
    }

    public SearchRecord getSearchRecord() {
        return searchRecord;
    }

    public String getSource() {
        return source;
    }
}
