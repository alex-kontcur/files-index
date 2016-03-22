package ru.bona.fileindex.search.searcher;

/**
 * IndexSearcherFactory
 *
 * @author Kontsur Alex (bona)
 * @since 28.09.14
 */
public interface IndexSearcherFactory {

    /*===========================================[ CLASS METHODS ]================*/

    IndexSearcher createIndexSearcher();    
    
}
