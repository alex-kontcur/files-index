/*
 * Copyright (c) 2013, i-Free. All Rights Reserved.
 * Use is subject to license terms.
 */

package ru.bona.fileindex.index.builder;

import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.bona.fileindex.filesholder.FileInfo;
import ru.bona.fileindex.guice.SourceAnalyzers;
import ru.bona.fileindex.model.SourceAnalyzer;
import ru.bona.fileindex.utils.Helper;
import ru.bona.fileindex.utils.SplittedRunnable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * IndexBuilder
 *
 * @author Kontsur Alex (bona)
 * @since 18.09.14
 */
public class IndexBuilder {

    /*===========================================[ STATIC VARIABLES ]=============*/

    private static final Logger logger = LoggerFactory.getLogger(IndexBuilder.class);

    /*===========================================[ INSTANCE VARIABLES ]===========*/

    private Collection<SourceAnalyzer> sourceAnalyzers;
    private AtomicBoolean interrupted;

    /*===========================================[ CLASS METHODS ]================*/

    @Inject
    protected void init(@SourceAnalyzers Collection<SourceAnalyzer> sourceAnalyzers) {
        this.sourceAnalyzers = new ArrayList<>(sourceAnalyzers);

        interrupted = new AtomicBoolean(false);
    }

    public void setInterrupted(boolean value) {
        interrupted.set(value);
    }

    public void processFile(final FileInfo fileInfo) throws Exception {
        if (sourceAnalyzers.isEmpty()) {
            throw new IllegalStateException("No analyzers regitered");
        }

        long fileSize = fileInfo.getFileSize();
        String encoding = fileInfo.getEncoding();

        Helper.splitThread(sourceAnalyzers, new SplittedRunnable<SourceAnalyzer>() {
            @Override
            public void run(SourceAnalyzer element) {
                element.analyze(fileInfo, interrupted);
            }
        });
    }

}
