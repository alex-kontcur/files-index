package ru.bona.fileindex.exceptions;

/**
 * StoppedException
 *
 * @author Kontsur Alex (bona)
 * @since 25.09.14
 */
public class StoppedException extends RuntimeException {

    /*===========================================[ STATIC VARIABLES ]=============*/

    private static final long serialVersionUID = -6326519088506013032L;

    /*===========================================[ CONSTRUCTORS ]=================*/

    public StoppedException(String message) {
        super(message);
    }
}
