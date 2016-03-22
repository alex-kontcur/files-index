package ru.bona.fileindex.lexeme;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import ru.bona.fileindex.AbstractIndexTest;
import ru.bona.fileindex.model.lexem.LexemeAnalyzer;
import ru.bona.fileindex.model.range.Range;
import ru.bona.fileindex.model.spec.SpecRegistry;
import ru.bona.fileindex.utils.Helper;

import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

/**
 * LexemeAnalyzerTest
 *
 * @author Kontsur Alex (bona)
 * @since 26.09.14
 */
public class LexemeAnalyzerTest extends AbstractIndexTest {


    private Map<String, SortedSet<Range>> lexemeMap;
    private LexemeAnalyzer.TokenCollector collector;

    /*===========================================[ CLASS METHODS ]================*/

    @Before
    public void init() {
        lexemeMap = new ConcurrentHashMap<>();

        collector = new LexemeAnalyzer.TokenCollector() {
            @Override
            public void collect(String token, long start, long stop, long fileSize) {
                SortedSet<Range> infos = lexemeMap.get(token);
                if (infos == null) {
                    infos = new TreeSet<>();
                    lexemeMap.put(token, infos);
                }
                Range lexemRange = Helper.makeRange(fileSize);
                lexemRange.setStart(start);
                lexemRange.setStop(stop);
                infos.add(lexemRange);
            }
        };
    }

    @Test
    public void tokenspickedCorrectly() {
        String source = SpecRegistry.cleanSource("uyf 6");
        lexemeAnalyzer.pickTokens(collector, source, 0, true, 100, 2);
        Assert.assertEquals(3, lexemeMap.size());
        SortedSet<Range> sortedSet = lexemeMap.get("uyf");
        Assert.assertNotNull(sortedSet);
        Assert.assertEquals(0, sortedSet.first().getStart().intValue());
        lexemeMap.clear();

        source = SpecRegistry.cleanSource("       77      000)))))))");
        lexemeAnalyzer.pickTokens(collector, source, 0, true, 100, 2);
        Assert.assertEquals(4, lexemeMap.size());
        sortedSet = lexemeMap.get("000");
        Assert.assertNotNull(sortedSet);
        Range range = sortedSet.first();
        Assert.assertEquals(15, range.getStart().intValue());
        Assert.assertEquals(17, range.getStop().intValue());
        lexemeMap.clear();

        source = SpecRegistry.cleanSource("      ");
        lexemeAnalyzer.pickTokens(collector, source, 0, true, 100, 2);
        Assert.assertEquals(0, lexemeMap.size());
        sortedSet = lexemeMap.get("uyf");
        Assert.assertNull(sortedSet);
        lexemeMap.clear();

        source = SpecRegistry.cleanSource("       t.y");
        lexemeAnalyzer.pickTokens(collector, source, 0, true, 100, 2);
        Assert.assertEquals(1, lexemeMap.size());
        sortedSet = lexemeMap.get("ty");
        Assert.assertNotNull(sortedSet);
        lexemeMap.clear();
    }
}
