package ru.bona.fileindex.utils;

/**
* SplittedRunnable
*
* @author Kontsur Alex (bona)
* @since 04.10.14
*/
public interface SplittedRunnable<T> {

    /*===========================================[ INTERFACE METHODS ]============*/

    void run(T element);
    
}
