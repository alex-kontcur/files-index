package ru.bona.fileindex.guice;

import ch.qos.logback.classic.LoggerContext;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.InitializationError;
import org.nnsoft.guice.junice.JUniceRunner;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * GuiceTestRunner
 *
 * @author Kontsur Alex (bona)
 * @since 27.09.14
 */
public class GuiceTestRunner extends JUniceRunner {

    /*===========================================[ INSTANCE VARIABLES ]=========*/

    private Injector injector;

    /*===========================================[ CONSTRUCTORS ]===============*/

    public GuiceTestRunner(Class<?> clazz) throws InitializationError {
        super(clazz);
    }

    /*===========================================[ CLASS METHODS ]==============*/

    @Override
    protected Injector createInjector(List<Module> modules) {
        Collection<Module> allModules = new ArrayList<>(modules);
        allModules.add(new TestModule());
        injector = Guice.createInjector(allModules);
        return injector;
    }

    protected Injector getInjector() {
        return injector;
    }

    @Override
    public void run(RunNotifier notifier) {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        loggerContext.stop();

        super.run(notifier);
    }

}