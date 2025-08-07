package ink.snowland.wkuwku.common;

import androidx.annotation.IntDef;

@IntDef(value = {
        Errors.NO_ERR,
        Errors.ERR,
        Errors.ERR_NOT_FOUND,
        Errors.ERR_NOT_SUPPORTED,
        Errors.ERR_TO_FAST
})
public @interface Errors {
    int NO_ERR              = 0;
    int ERR                 = 1;
    int ERR_NOT_FOUND       = 2;
    int ERR_NOT_SUPPORTED   = 3;
    int ERR_TO_FAST         = 4;
}
