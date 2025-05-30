package ink.snowland.wkuwku.exception;

import java.util.Locale;

public class FileChecksumException extends Exception {
    public FileChecksumException(String expected, String actual) {
        super(String.format(Locale.ROOT, "%s is expected, but %s is actual", expected, actual));
    }
}
