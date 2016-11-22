/*
 * 				Twidere - Twitter client for Android
 * 
 *  Copyright (C) 2012-2014 Mariotaku Lee <mariotaku.lee@gmail.com>
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

package org.mariotaku.twidere.preference;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.AttributeSet;

import org.mariotaku.twidere.Constants;
import org.mariotaku.twidere.R;
import org.mariotaku.twidere.provider.TwidereDataStore.Activities;
import org.mariotaku.twidere.provider.TwidereDataStore.CachedStatuses;
import org.mariotaku.twidere.provider.TwidereDataStore.Notifications;
import org.mariotaku.twidere.provider.TwidereDataStore.SavedSearches;
import org.mariotaku.twidere.provider.TwidereDataStore.UnreadCounts;

import static org.mariotaku.twidere.provider.TwidereDataStore.CACHE_URIS;
import static org.mariotaku.twidere.provider.TwidereDataStore.DIRECT_MESSAGES_URIS;
import static org.mariotaku.twidere.provider.TwidereDataStore.STATUSES_URIS;

public class ClearDatabasesPreference extends AsyncTaskPreference implements Constants {

    public ClearDatabasesPreference(final Context context) {
        this(context, null);
    }

    public ClearDatabasesPreference(final Context context, final AttributeSet attrs) {
        this(context, attrs, R.attr.preferenceStyle);
    }

    public ClearDatabasesPreference(final Context context, final AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void doInBackground() {
        final Context context = getContext();
        if (context == null) return;
        final ContentResolver resolver = context.getContentResolver();
        for (final Uri uri : STATUSES_URIS) {
            if (CachedStatuses.CONTENT_URI.equals(uri)) {
                continue;
            }
            resolver.delete(uri, null, null);
        }
        for (final Uri uri : DIRECT_MESSAGES_URIS) {
            resolver.delete(uri, null, null);
        }
        for (final Uri uri : CACHE_URIS) {
            resolver.delete(uri, null, null);
        }
        resolver.delete(Activities.AboutMe.CONTENT_URI, null, null);
        resolver.delete(Activities.ByFriends.CONTENT_URI, null, null);
        resolver.delete(Notifications.CONTENT_URI, null, null);
        resolver.delete(UnreadCounts.CONTENT_URI, null, null);
        resolver.delete(SavedSearches.CONTENT_URI, null, null);

        final SharedPreferences prefs = context.getSharedPreferences(TIMELINE_POSITIONS_PREFERENCES_NAME, Context.MODE_PRIVATE);
        final SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        editor.apply();
    }

}
