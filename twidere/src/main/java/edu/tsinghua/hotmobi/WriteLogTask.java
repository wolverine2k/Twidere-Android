/*
 *                 Twidere - Twitter client for Android
 *
 *  Copyright (C) 2012-2015 Mariotaku Lee <mariotaku.lee@gmail.com>
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package edu.tsinghua.hotmobi;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import org.mariotaku.twidere.BuildConfig;
import org.mariotaku.twidere.Constants;
import org.mariotaku.twidere.model.UserKey;
import org.mariotaku.twidere.util.JsonSerializer;
import org.mariotaku.twidere.util.Utils;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.Collections;
import java.util.List;

import edu.tsinghua.hotmobi.model.LogModel;

/**
 * Created by mariotaku on 15/8/23.
 */
public class WriteLogTask<T extends LogModel> implements Runnable, Constants {

    private static final byte[] LF = {'\n'};

    private final Context context;
    private final UserKey accountKey;
    private final String type;
    private final List<T> events;
    @Nullable
    private final PreProcessing<T> preProcessing;

    public WriteLogTask(Context context, UserKey accountKey, T event, @Nullable PreProcessing<T> preProcessing) {
        this(context, accountKey, HotMobiLogger.getLogFilename(event), Collections.singletonList(event), preProcessing);
    }

    public WriteLogTask(Context context, UserKey accountKey, String type, List<T> events,
                        @Nullable PreProcessing<T> preProcessing) {
        this.context = context;
        this.accountKey = accountKey;
        this.type = type;
        this.events = events;
        this.preProcessing = preProcessing;
    }

    @Override
    public void run() {
        final SharedPreferences prefs = context.getSharedPreferences(SHARED_PREFERENCES_NAME,
                Context.MODE_PRIVATE);
        if (!prefs.getBoolean(KEY_USAGE_STATISTICS, false)) return;
        RandomAccessFile raf = null;
        FileChannel fc = null;
        try {
            raf = new RandomAccessFile(HotMobiLogger.getLogFile(context, accountKey, type), "rw");
            fc = raf.getChannel();
            final FileLock lock = fc.lock();
            for (T event : events) {
                if (preProcessing != null) {
                    preProcessing.process(event, context);
                }
                if (BuildConfig.DEBUG) {
                    if (accountKey != null) {
                        Log.v(HotMobiLogger.LOGTAG, "Log " + type + " for account " + accountKey + ": " + event);
                    } else {
                        Log.v(HotMobiLogger.LOGTAG, "Log " + type + ": " + event);
                    }
                }
                final String serialize = JsonSerializer.serialize(event);
                if (TextUtils.isEmpty(serialize)) continue;
                final byte[] bytes = serialize.getBytes("UTF-8");
                final long start = raf.length();
                final ByteBuffer bb;
                if (start == 0) {
                    // Don't write line break
                    bb = ByteBuffer.allocate(bytes.length);
                } else {
                    bb = ByteBuffer.allocate(bytes.length + LF.length);
                    bb.put(LF);
                    bb.position(LF.length);
                }
                bb.put(bytes);
                bb.rewind();
                fc.position(start);
                while (bb.hasRemaining()) {
                    fc.write(bb);
                }
            }
            lock.release();
        } catch (IOException e) {
            Log.w(HotMobiLogger.LOGTAG, e);
        } finally {
            Utils.closeSilently(fc);
            Utils.closeSilently(raf);
        }
    }

}
