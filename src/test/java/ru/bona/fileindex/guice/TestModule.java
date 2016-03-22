package ru.bona.fileindex.guice;

import com.google.inject.Scopes;
import ru.bona.fileindex.Configuration;
import ru.bona.fileindex.channelworker.ChannelWorkerProvider;
import ru.bona.fileindex.channelworker.bytes.BytesWorkerProvider;

/**
 * TestModule
 *
 * @author Kontsur Alex (bona)
 * @since 27.09.14
 */
public class TestModule extends BaseModule {

    /*===========================================[ CONSTRUCTORS ]=================*/

    public TestModule() {
        super(Configuration.DEFAULT);
    }

    @Override
    protected void bindChannelProvider() {
        bind(ChannelWorkerProvider.class).to(BytesWorkerProvider.class);
        bind(BytesWorkerProvider.class).in(Scopes.SINGLETON);
    }

}
