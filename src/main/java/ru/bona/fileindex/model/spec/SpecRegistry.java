package ru.bona.fileindex.model.spec;

import com.google.inject.Inject;
import com.google.inject.Injector;
import ru.bona.fileindex.Configuration;
import ru.bona.fileindex.channelworker.ChannelWorkerProvider;
import ru.bona.fileindex.model.Registry;
import ru.bona.fileindex.model.fileparser.ChannelHandler;
import ru.bona.fileindex.model.range.IntRange;

import java.nio.channels.SeekableByteChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * SpecRegistry
 *
 * @author Kontsur Alex (bona)
 * @since 20.09.14
 */
@SuppressWarnings("PublicStaticArrayField")
public class SpecRegistry extends Registry<SpecControlFile> {

    /*===========================================[ STATIC VARIABLES ]=============*/

    private static final String SPEC_CTRL_FILE = "spec.ctr";

    public static final char[] DELIMETER_SYMBOLS = {
        ' ','\t','\r','\n'
    };
    public static final char[] SPEC_SYMBOLS = {
        '~','!','@','#','$','%','^','&','*','(',')','-','+','=','"',
        '\'','?','>','<','|','{','}','[',']','/','\\','.',',',':',';'
    };

    /*===========================================[ INNER CLASSES ]================*/

    @Inject
    protected void init(Injector injector, ChannelWorkerProvider workerProvider, Configuration configuration) {
        controlFile = injector.getInstance(SpecControlFile.class);
        String indexPath = configuration.getIndexPath();
        channelWorker = workerProvider.getChannelWorker(indexPath + SPEC_CTRL_FILE);
        channelWorker.processInputChannel(new ChannelHandler() {
            @Override
            public void handle(SeekableByteChannel channel) throws Exception {
                if (channel.size() > 0) {
                    controlFile.readExternal(channel);
                }
            }
        });
    }

    @Override
    public SpecControlFile getControlFile() {
        return controlFile;
    }

    public static String cleanSource(String source) {
        String clearSource = source;
        for (char c : SPEC_SYMBOLS) {
            clearSource = clearSource.replace(c, ' ');
        }
        for (char c : DELIMETER_SYMBOLS) {
            clearSource = clearSource.replace(c, ' ');
        }
        return clearSource;
    }

    public Map<Number, IntRange> getFrameInfosByChar(char c) {
        return controlFile.getCharacterInfoMap(c);
    }

    public Map<Number, IntRange> removeFrameInfosByFile(Number fileNum) {
        return controlFile.removeFrameInfosByFile(fileNum);
    }

    public void register(char c, Map<Number, IntRange> frameMap) {
        controlFile.put(c, frameMap);
    }

    private static boolean specChar(char c) {
        for (char source : SPEC_SYMBOLS) {
            if (source == c) {
                return true;
            }
        }
        return false;
    }

    private static List<Character> getSpecChars(String sequence) {
        List<Character> chars = new ArrayList<>();
        for (int i = 0; i < sequence.length(); i++) {
            char c = sequence.charAt(i);
            if (specChar(c)) {
                chars.add(c);
            }
        }
        return chars;
    }

    public static boolean isDelimeterChar(char c) {
        for (char terminateSymbol : DELIMETER_SYMBOLS) {
            if (terminateSymbol == c) {
                return true;
            }
        }
        return false;
    }

    public static boolean isSpecChar(char c) {
        for (char sp : SPEC_SYMBOLS) {
            if (sp == c) {
                return true;
            }
        }
        return false;
    }

    public void delete(Character c, Number fileNum) {
        controlFile.removeFrameInfosByCharAndFile(c, fileNum);
    }

    public IntRange getPreviousFrame(char c, Number fileNum) {
        Map<Number, IntRange> regionInfo = getFrameInfosByChar(c);
        if (regionInfo != null) {
            IntRange frameInfo = regionInfo.get(fileNum);
            if (frameInfo != null) {
                return frameInfo;
            }
        }
        return null;
    }

    //public static final char[] unusual = {
    //    '§','¶','†','‡','•','—','–','…','√','∫','∂','∑','∏','¿','¡','«','»','‹','›','Þ','þ','ß','Ѿ',
    //    '±','÷','∗','·','×','⊗','⊕','≅','≈','∝','≡','≠','≤','≥','∈','∉','∩','∪','⊂','⊃','⊆','⊇','∅',
    //    '¬','∧','∨','∃','∀','⇐','⇒','⇑','⇓','⇔','←','→','↑','↓','↔','↕','↵','¹','²','³','¼','½','¾','°'
    //    ,'′','″','∞','‰','∇','ℵ','ℑ','℘','ℜ','∴','♔','♕','♖','♗','♘','♙','♚','♛','♜','♝','♞','♟',
    //    '™','©','®','¢','€','¥','£','¤','♠','♣','♥','♦','ª','´','¨','˜','‾','¦','◊','∠','⊥','ƒ','⁄','ˆ','★',
    //    '☭','♪','☂','☠','☛','⇆','█','░','▒','▓','►','◄','▼','▲','▌','▄','■','▀','▐','�','Α','Β',
    //    'Γ','Δ','Ε','Ζ','α','β','γ','δ','ε','ζ','Η','Θ','Ι','Κ','Λ','Μ','Ν','η','θ','ι','κ','λ','μ','ν','Ξ','Ο',
    //    'Π','Ρ','Σ','ξ','ο','π','ρ','σ','ς','Τ','Υ','Φ','Χ','Ψ','Ω','τ','υ','φ','χ','ψ','ω','ʈ','ɖ','ɟ','ɢ','ʔ',
    //    'ɡ','ɱ','ɳ','ɲ','ŋ','ɴ','ʙ','ʀ','ɾ','ɽ','ɸ','ð','ʃ','ʒ','ʂ','ʐ','ʝ','ɣ','ʁ','ħ','ʕ','ɦ','ʋ','ɹ','ɻ','ɰ',
    //    'ɨ','ʉ','ɯ','ɪ','ʏ','ʊ','ɘ','ɵ','ɤ','ə','ɛ','ɜ','ɞ','ʌ','ɔ','æ','ɐ','ɶ','ɑ','ɒ','ʍ','ɥ','ʜ','ʦ','ʧ','ʨ',
    //    'ʣ','ʤ','ʥ','ʢ','ʡ','ɕ','ʑ','ɺ','ɧ','ɓ','ɗ','ʄ','ɠ','ʛ','ɭ','ʎ','ʟ','ɬ','ɮ','ʘ','ǀ','ǃ','ǂ','ǁ','ʼ','ˈ',
    //    'ˌ','ː','ˑ','˘','‖','‿','À','Á','Â','Ã','Ä','Å','Æ','Ą','à','á','â','ã','ä','å','ą','Ç','Ć','Ĉ','Č','ç',
    //    'ć','ĉ','č','È','É','Ê','Ë','Ę','Ě','è','é','ê','ë','ę','ě','Ì','Í','Î','Ï','ì','í','î','ï','Ð','Ñ','ñ',
    //    'Ò','Ó','Ô','Œ','Õ','Ö','Ø','ò','ó','ô','œ','õ','ö','ø','Ù','Ú','Û','Ü','ù','ú','û','ü','Ý','Ÿ','ý','ÿ',
    //};

}
