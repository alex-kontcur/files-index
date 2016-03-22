package ru.bona.fileindex.model;

import com.google.gag.annotation.remark.Facepalm;
import ru.bona.fileindex.filesholder.FileInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * SourceContext
 *
 * @author Kontsur Alex (bona)
 * @since 22.09.14
 */
public class SourceContext {

    /*===========================================[ STATIC VARIABLES ]=============*/

    private static final String[] some_lang_identificators = {
        "public","package","module","def","begin","import","define","friend","struct","template","namespace",
        "protected","private","virtual","class","interface","Procedure","Property","Record", "Var", "With",
        "assert", "except","finally","global","lambda","nonlocal","return","INDEX","DELETE","INSERT","SELECT",
        "ROWID","SYSDATE","CURSOR","CREATE","TABLE","COLUMN","VARCHAR","static"
    };

    /*===========================================[ INSTANCE VARIABLES ]===========*/

    private FileInfo fileInfo;
    private Map<String, AtomicInteger> words;

    private boolean repeatedWordsStatsticsMatched;
    private boolean someWordsOccurencesStatisticsMatched;

    /*===========================================[ CONSTRUCTORS ]=================*/

    public SourceContext(FileInfo fileInfo, Map<String, AtomicInteger> words) {
        this.fileInfo = fileInfo;
        this.words = new HashMap<>(words);

        repeatedWordsStatsticsMatched = repeatedWordsStatsticsMatched();
        someWordsOccurencesStatisticsMatched = someWordsOccurencesStatisticsMatched();
    }

    /*===========================================[ CLASS METHODS ]================*/

    public FileInfo getFileInfo() {
        return fileInfo;
    }

    public boolean isRepeatedWordsStatsticsMatched() {
        return repeatedWordsStatsticsMatched;
    }

    public boolean isSomeWordsOccurencesStatisticsMatched() {
        return someWordsOccurencesStatisticsMatched;
    }

    @Facepalm("Here we can plugin some dictionaries or another logic for defining of words occurences")
    private boolean repeatedWordsStatsticsMatched() {
        List<Integer> counters = new ArrayList<>();
        for (Map.Entry<String, AtomicInteger> entry : words.entrySet()) {
            int counter = entry.getValue().get();
            if (counter > 5) {
                counters.add(counter);
            }
        }
        return !counters.isEmpty();
    }

    @Facepalm("Here we can plugin some dictionaries or another logic for defining of words occurences")
    private boolean someWordsOccurencesStatisticsMatched() {
        int counter = 0;
        for (String value : some_lang_identificators) {
            for (String word : words.keySet()) {
                if (word.equalsIgnoreCase(value)) {
                    counter++;
                }
            }
        }
        return words.size() > 100 ? counter > 5 : counter > 1;
    }
}
