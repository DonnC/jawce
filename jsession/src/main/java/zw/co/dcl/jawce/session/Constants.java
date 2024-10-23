package zw.co.dcl.jawce.session;

public class Constants {
    public static final String SESSION_FILE_EXT = ".session";
    public static final String GLOBAL_SESSION_KEY = "global";
    public static final String GLOBAL_SESSION_FILE_NAME = GLOBAL_SESSION_KEY + SESSION_FILE_EXT;

    public static final String FILE_FOLDER = "./file-session";
    public static final String LMDB_FOLDER = "./lmdb-session";
    public static final String SESSION_DB_NAME = "jawceSession";

    public static final String USER_SESSION_CACHE_NAME = "userSessionCache";
    public static final String GLOBAL_SESSION_CACHE_NAME = "globalSessionCache";

    // --- session based constants
    public final static String USER_PROPS_KEY = "kUserProps";
}
