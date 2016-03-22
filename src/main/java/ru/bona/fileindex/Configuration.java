package ru.bona.fileindex;

/**
 * Configuration
 *
 * @author Kontsur Alex (bona)
 * @since 27.09.14
 */
public interface Configuration {

    /*===========================================[ CLASS METHODS ]================*/

    @SuppressWarnings("AnonymousInnerClassWithTooManyMethods")
    Configuration DEFAULT = new Configuration() {
        @Override
        public int getTokenSize() {
            return 3;
        }
        @Override
        public int getSimIndexThreads() {
            return 10;
        }
        @Override
        public int getSimSearchThreads() {
            return 50;
        }
        @Override
        public String getIndexPath() {
            return System.getProperty("user.dir") + "/index/";
        }
    };

    int getTokenSize();

    int getSimIndexThreads();

    int getSimSearchThreads();

    String getIndexPath();

}
