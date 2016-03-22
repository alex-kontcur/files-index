package ru.bona.fileindex.filesholder;

import org.apache.commons.lang.Validate;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * FileInfo
 *
 * @author Kontsur Alex (bona)
 * @since 18.09.14
 */
public class FileInfo {

    /*===========================================[ INSTANCE VARIABLES ]===========*/

    private File file;
    private Number num;
    private long fileSize;
    private String encoding;
    private AtomicBoolean processed;

    /*===========================================[ CONSTRUCTORS ]=================*/

    public FileInfo(File file, String encoding) {
        this(file, encoding, file.exists() ? file.length() : 0);
    }

    public FileInfo(File file, String encoding, long fileSize) {
        Validate.notNull(file);
        Validate.notNull(encoding);

        this.file = file;
        this.encoding = encoding;
        this.fileSize = fileSize;

        processed = new AtomicBoolean(false);
    }

    /*===========================================[ GETTER/SETTER ]================*/

    public boolean getProcessed() {
        return processed.get();
    }

    public void setProcessed(boolean processed) {
        this.processed.set(processed);
    }

    public String getFullPath() {
        return file.getAbsolutePath();
    }

    public File getFile() {
        return file;
    }

    public void setNum(Number num) {
        if (this.num == null) {
            this.num = num;
        }
    }

    public Number getNum() {
        return num;
    }

    public long getFileSize() {
        return fileSize;
    }

    public String getEncoding() {
        return encoding;
    }

    public boolean isExists() {
        return file.exists();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("FileInfo");
        sb.append("{num=").append(num);
        sb.append(", file=").append(file.getName());
        sb.append('}');
        return sb.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        FileInfo fileInfo = (FileInfo) obj;
        if (fileSize != fileInfo.fileSize) {
            return false;
        }
        if (!encoding.equals(fileInfo.encoding)) {
            return false;
        }
        if (!file.equals(fileInfo.file)) {
            return false;
        }
        if (!num.equals(fileInfo.num)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int result = file.hashCode();
        result = 31 * result + num.hashCode();
        result = 31 * result + (int) (fileSize ^ (fileSize >>> 32));
        result = 31 * result + encoding.hashCode();
        return result;
    }
}
