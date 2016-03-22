package ru.bona.fileindex.search.searcher;

import ru.bona.fileindex.model.range.LongRange;
import ru.bona.fileindex.model.range.Range;
import ru.bona.fileindex.model.spec.synctree.SyncTree;
import ru.bona.fileindex.search.CharsPosition;

import java.util.*;

/**
 * AnalyzingEntry
 *
 * @author Kontsur Alex (bona)
 * @since 28.09.14
 */
public class AnalyzingEntry {

    /*===========================================[ INSTANCE VARIABLES ]===========*/

    private String source;

    private MatchState state;

    private long fsStart;
    private int fsCount;

    private int tokenSize;
    private Number fileNum;
    private SortedSet<CharsPosition> charsPositions;
    private Map<CharsPosition, SortedSet<Range>> ranges;
    private Map<CharsPosition, Range> indexRanges;
    private Map<CharsPosition, SyncTree> trees;

    private List<SortedSet<Range>> foundLexemesRanges;

    /*===========================================[ CONSTRUCTORS ]=================*/

    public AnalyzingEntry(String source, Number fileNum, SortedSet<CharsPosition> charsPositions, Map<CharsPosition,
                          SortedSet<Range>> ranges, Map<CharsPosition, Range> indexRanges, Map<CharsPosition,
        SyncTree> trees, int tokenSize) {
        this.source = source;
        this.fileNum = fileNum;
        this.tokenSize = tokenSize;
        this.charsPositions = new TreeSet<>(charsPositions);
        this.ranges = new HashMap<>(ranges);
        this.indexRanges = new HashMap<>(indexRanges);
        this.trees = new HashMap<>(trees);

        foundLexemesRanges = new ArrayList<>();

        analyzeMatchState();
    }

    public String getSource() {
        return source;
    }

    public Map<CharsPosition, SyncTree> getTrees() {
        return Collections.unmodifiableMap(trees);
    }

    public int specsCount() {
        return trees.size();
    }

    public List<SortedSet<Range>> getFoundLexemesRanges() {
        return Collections.unmodifiableList(foundLexemesRanges);
    }

    public SortedSet<Range> getLexemesStartStopRanges() {
        SortedSet<Range> ranges = new TreeSet<>();
        int lexemsCount = getLexemSize();
        for (SortedSet<Range> sortedSet : foundLexemesRanges) {
            int cnt = 0;
            List<Range> tempRanges = new ArrayList<>();
            for (Range range : sortedSet) {
                tempRanges.add(range);
                if (++cnt == lexemsCount) {

                    long start = tempRanges.get(0).getStart().longValue();
                    long stop = tempRanges.get(tempRanges.size() - 1).getStop().longValue();

                    if (stop - start + 1 == source.length()) {
                        LongRange longRange = new LongRange();
                        longRange.setStart(start);
                        longRange.setStop(stop);
                        ranges.add(longRange);
                    }

                    tempRanges = new ArrayList<>();
                    cnt = 0;
                }
            }
        }
        return ranges;
    }

    public int getLexemSize() {
        int count = 0;
        for (CharsPosition position : charsPositions) {
            if (position.isLexem()) {
                count++;
            }
        }
        return count;
    }

    private void analyzeMatchState() {
        List<CharsPosition> list = new ArrayList<>(charsPositions);
        int len = list.size();
        if (lexemsExists()) {
            Map<CharsPosition, SortedSet<Range>> matchedMap = new HashMap<>();
            fillMatchedRanges(list, matchedMap);
            for (SortedSet<Range> set : matchedMap.values()) {
                if (set.isEmpty()) {
                    state = MatchState.NOT_MATCHED;
                    return;
                }
            }
            List<CharsPosition> list2 = new ArrayList<>(new TreeSet<>(matchedMap.keySet()));

            int k = 0;
            int count = list2.size();
            SortedSet<SRange> treeSet = sortRanges(matchedMap, len, list);
            SortedSet<Range> lexemesRangeSet = new TreeSet<>();
            for (SRange sRange : treeSet) {
                CharsPosition charsPosition = list2.get(k);
                if (sRange.getCharsPosition().equals(charsPosition)) {
                    lexemesRangeSet.addAll(ranges.get(charsPosition));
                    if (++k == count) {
                        foundLexemesRanges.add(lexemesRangeSet);
                        lexemesRangeSet = new TreeSet<>();
                        k = 0;
                    }
                }
            }

            if (foundLexemesRanges.isEmpty()) {
                state = MatchState.NOT_MATCHED;
            } else {
                if (specExists()) {
                    state = MatchState.MATCH_CANDIDATE;
                } else {
                    state = MatchState.MATCHED;
                    for (Map.Entry<CharsPosition, SortedSet<Range>> entry : matchedMap.entrySet()) {
                        CharsPosition charsPosition = entry.getKey();
                        if (charsPosition.getLenght() > tokenSize) {
                            state = MatchState.MATCH_CANDIDATE;
                            break;
                        }
                    }
                }
            }
        } else {
            if (specExists()) {
                state = MatchState.MATCH_CANDIDATE;
            } else {
                state =  MatchState.NOT_MATCHED; // todo - implement
            }
        }
    }

    private static SortedSet<SRange> sortRanges(Map<CharsPosition, SortedSet<Range>> matchedMap,
                                                int len, List<CharsPosition> list) {

        SortedSet<SRange> treeSet = new TreeSet<>();
        for (int i = 0; i < len; i++) {
            CharsPosition c = list.get(i);
            SortedSet<Range> ranges = matchedMap.get(c);
            for (Range range : ranges) {
                treeSet.add(new SRange(range.getStart().longValue(), range.getStop().longValue(), c));
            }
        }
        return treeSet;
    }

    private void fillMatchedRanges(List<CharsPosition> list, Map<CharsPosition, SortedSet<Range>> matchedMap) {
        for (CharsPosition c1 : list) {
            if (c1.isLexem()) {
                int start = c1.getStart();
                int stop = c1.getStop();
                SortedSet<Range> matched = getMatchedRanges(start, stop, ranges.get(c1));
                matchedMap.put(c1, matched);
            }
        }
    }

    private static SortedSet<Range> getMatchedRanges(int start, int stop, SortedSet<Range> lexemRanges) {
        SortedSet<Range> matched = new TreeSet<>();
        for (Range lexemRange : lexemRanges) {
            int rangeStart = lexemRange.getStart().intValue();
            int rangeStop = lexemRange.getStop().intValue();
            if (rangeStop - rangeStart == stop - start) {
                matched.add(lexemRange);
            }
        }
        return matched;
    }

    /*===========================================[ GETTER/SETTER ]================*/

    public Number getFileNum() {
        return fileNum;
    }

    public MatchState getState() {
        return state;
    }

    private boolean lexemsExists() {
        for (CharsPosition chp : charsPositions) {
            if (chp.isLexem()) {
                return true;
            }
        }
        return false;
    }

    private boolean specExists() {
        for (CharsPosition chp : charsPositions) {
            if (!chp.isLexem()) {
                return true;
            }
        }
        return false;
    }
}
