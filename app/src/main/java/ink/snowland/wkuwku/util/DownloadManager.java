package ink.snowland.wkuwku.util;

import android.os.Handler;
import android.os.Looper;
import android.util.Pair;
import android.util.SparseArray;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import org.wkuwku.interfaces.ActionListener;
import org.wkuwku.interfaces.OnProgressListener;
import org.wkuwku.util.FileUtils;
import org.wkuwku.util.Logger;
import org.wkuwku.util.NumberUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import ink.snowland.wkuwku.common.Callable;
import ink.snowland.wkuwku.common.OnErrorListener;

public class DownloadManager {

    public static final int SESSION_STATE_CANCELED      = 0;
    public static final int SESSION_STATE_PENDING       = 1;
    public static final int SESSION_STATE_CONNECTING    = 2;
    public static final int SESSION_STATE_READING       = 3;
    public static final int SESSION_STATE_COMPLETED     = 3;
    public static final int SESSION_STATE_FAILED        = 4;

    private final static Logger logger = new Logger("DownloadManager");
    private final static ThreadPoolExecutor executor = new ThreadPoolExecutor(
            1,
            3,
            30,
            TimeUnit.SECONDS,
            new LinkedBlockingDeque<>()
    );
    private final static Handler handler = new Handler(Looper.getMainLooper());
    private final static SparseArray<Session> sessions = new SparseArray<>();
    private DownloadManager() {}

    @Nullable
    public static Session getSession(URL url) {
        return sessions.get(url.toString().hashCode());
    }

    @Nullable
    public static Session getSession(String url) {
        return sessions.get(url.hashCode());
    }

    public static Session newRequest(String url, File file) {
        try {
            return newRequest(new URL(url), file);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }
    public static Session newRequest(URL url, File file) {
        try {
            return new Session(url, file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @WorkerThread
    public static void download(Pair<String, File> it) throws IOException {
        try {
            URLConnection conn = new URL(it.first).openConnection();
            conn.setReadTimeout(1000 * 10);
            conn.setConnectTimeout(1000 * 10);
            try (InputStream from = conn.getInputStream()){
                FileUtils.copy(from, it.second);
            }
        } catch (IOException e) {
            FileUtils.delete(it.second);
            throw e;
        }
    }

    public static void download(List<Pair<String, File>> items, @Nullable ActionListener listener) {
        executor.submit(() -> {
            for (Pair<String, File> it : items) {
                try {
                    download(it);
                } catch (IOException e) {
                    if (listener != null) {
                        listener.onFailure(e);
                    } else {
                        e.printStackTrace(System.err);
                    }
                    FileUtils.delete(it.second);
                    return;
                }
            }
            if (listener != null) {
                listener.onSuccess();
            }
        });
    }

    private static void download(@NonNull Session session) {
        if (sessions.indexOfKey(session.id) >= 0) {
            logger.w("Session already in queue %s!", session.url);
            return;
        }
        session.state = SESSION_STATE_PENDING;
        sessions.put(session.id, session);
        if (session.stateHandler != null) {
            session.stateHandler.call(session.state);
        }
        executor.submit(() -> {
            try {
                session.state = DownloadManager.SESSION_STATE_CONNECTING;
                handler.post(() -> {
                    if (session.stateHandler != null) {
                        session.stateHandler.call(session.state);
                    }
                });
                URLConnection conn = session.url.openConnection();
                conn.setReadTimeout(1000 * 10);
                conn.setConnectTimeout(1000 * 10);
                long totalSize = NumberUtils.parseLong(conn.getHeaderField("Content-Length"), 0);
                try (InputStream from = conn.getInputStream();
                     FileOutputStream to = new FileOutputStream(session.file)){
                    session.state = SESSION_STATE_READING;
                    handler.post(() -> {
                        if (session.stateHandler != null) {
                            session.stateHandler.call(session.state);
                        }
                    });
                    FileUtils.copy(from, to, (progress, max) -> {
                        if (session.cancel) {
                            throw new CancellationException();
                        }
                        handler.post(() -> {
                            if (session.progressHandler != null) {
                                if (totalSize == 0) {
                                    session.progressHandler.update(progress, progress);
                                } else {
                                    session.progressHandler.update(progress, totalSize);
                                }
                            }
                        });
                    });
                    session.state = SESSION_STATE_COMPLETED;
                    handler.post(() -> {
                        if (session.stateHandler != null) {
                            session.stateHandler.call(session.state);
                        }
                        if (session.completeHandler != null) {
                            session.completeHandler.call(session.file);
                        }
                    });
                }
            } catch (IOException | CancellationException e) {
                FileUtils.delete(session.file);
                logger.e("Failed to download file from %s", session.url);
                if (e instanceof CancellationException) {
                    session.state = SESSION_STATE_CANCELED;
                } else {
                    session.state = SESSION_STATE_FAILED;
                }
                handler.post(() -> {
                    if (session.stateHandler != null) {
                        session.stateHandler.call(session.state);
                    }
                    if (session.errorHandler != null) {
                        session.errorHandler.error(e);
                    }
                });
            } finally {
                handler.post(() -> {
                    if (session.finallyHandler != null) {
                        session.finallyHandler.run();
                    }
                });
                sessions.remove(session.id);
            }
        });
    }

    public static class Session {
        public final int id;
        private int state;
        private boolean cancel;
        private final URL url;
        private final File file;
        private Callable<File> completeHandler;
        private OnErrorListener errorHandler;
        private OnProgressListener progressHandler;
        private Runnable finallyHandler;
        private Callable<Integer> stateHandler;

        public Session(URL _url, File _file) throws IOException {
            id = _url.toString().hashCode();
            url = _url;
            file = _file;
            cancel = false;
            state = SESSION_STATE_PENDING;
        }

        public Session doOnComplete(Callable<File> r) {
            completeHandler = r;
            return this;
        }

        public Session doOnError(OnErrorListener e) {
            errorHandler = e;
            return this;
        }

        public Session doOnProgressUpdate(OnProgressListener p) {
            progressHandler = p;
            return this;
        }

        public Session donOnStateChanged(Callable<Integer> r) {
            stateHandler = r;
            return this;
        }

        public Session doOnFinally(Runnable r) {
            finallyHandler = r;
            return this;
        }

        public void submit() {
            download(this);
        }

        public int getState() {
            return state;
        }

        public void cancel() {
            cancel = true;
        }
    }
}
