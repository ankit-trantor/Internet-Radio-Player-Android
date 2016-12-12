/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.stc.radio.player.model;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.RatingCompat;
import android.util.Log;

import com.activeandroid.query.From;
import com.activeandroid.query.Select;
import com.stc.radio.player.R;
import com.stc.radio.player.db.DBMediaItem;
import com.stc.radio.player.utils.LogHelper;
import com.stc.radio.player.utils.MediaIDHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import timber.log.Timber;

import static android.support.v4.media.MediaMetadataCompat.METADATA_KEY_USER_RATING;
import static com.stc.radio.player.utils.MediaIDHelper.MEDIA_ID_MUSICS_BY_GENRE;
import static com.stc.radio.player.utils.MediaIDHelper.MEDIA_ID_MUSICS_BY_SEARCH;
import static com.stc.radio.player.utils.MediaIDHelper.MEDIA_ID_ROOT;
import static com.stc.radio.player.utils.MediaIDHelper.createMediaID;

/**
 * Simple data provider for music tracks. The actual metadata source is delegated to a
 * MusicProviderSource defined by a constructor argument of this class.
 */
public class MusicProvider {

    private static final String TAG = LogHelper.makeLogTag(MusicProvider.class);

    private MusicProviderSource mSource;

    // Categorized caches for music track data:
    private ConcurrentMap<String, List<MediaMetadataCompat>> mMusicListByGenre;
    private final ConcurrentMap<String, MutableMediaMetadata> mMusicListById;

    private final Set<String> mFavoriteTracks;

    enum State {
        NON_INITIALIZED, INITIALIZING, INITIALIZED
    }

    private volatile State mCurrentState = State.NON_INITIALIZED;

    public interface Callback {
        void onMusicCatalogReady(boolean success);
    }

    public MusicProvider() {
        this(new RemoteSource());
    }
    public MusicProvider(MusicProviderSource source) {
        mSource = source;
        mMusicListByGenre = new ConcurrentHashMap<>();
        mMusicListById = new ConcurrentHashMap<>();
        mFavoriteTracks = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());
    }

    /**
     * Get an iterator over the list of genres
     *
     * @return genres
     */
    public Iterable<String> getGenres() {
        if (mCurrentState != State.INITIALIZED) {
            return Collections.emptyList();
        }
        return mMusicListByGenre.keySet();
    }

    /**
     * Get an iterator over a shuffled collection of all songs
     */
    public Iterable<MediaMetadataCompat> getShuffledMusic() {
        if (mCurrentState != State.INITIALIZED) {
            return Collections.emptyList();
        }
        List<MediaMetadataCompat> shuffled = new ArrayList<>(mMusicListById.size());
        for (MutableMediaMetadata mutableMetadata: mMusicListById.values()) {
            shuffled.add(mutableMetadata.metadata);
        }
        Collections.shuffle(shuffled);
        return shuffled;
    }
    static Comparator<MediaMetadataCompat>comparator=new Comparator<MediaMetadataCompat>() {
        @Override
        public int compare(MediaMetadataCompat metadata2, MediaMetadataCompat metadata1) {
            boolean b1=metadata1.getRating(METADATA_KEY_USER_RATING).hasHeart();
            boolean b2=metadata2.getRating(METADATA_KEY_USER_RATING).hasHeart();
            String t1=metadata1.getString(MediaMetadataCompat.METADATA_KEY_TITLE);
            String t2=metadata2.getString(MediaMetadataCompat.METADATA_KEY_TITLE);

            if(b1) {
                if(b2) return t2.compareToIgnoreCase(t1);
                else return 1;
            }else {
                if(b2) return -1;
                else return t2.compareToIgnoreCase(t1);
            }
        }

    };
    public Iterable<MediaMetadataCompat> getMusicsById() {
        if (mCurrentState != State.INITIALIZED) {
            return Collections.emptyList();
        }
        List<MediaMetadataCompat> byId = new ArrayList<>(mMusicListById.size());

        for (MutableMediaMetadata mutableMetadata: mMusicListById.values()) {

			byId.add(mutableMetadata.metadata);
		}
		Collections.sort(byId,comparator);
		//if(mFavoriteTracks!=null) Log.w(TAG, "getFavNum: "+ mFavoriteTracks.size());
		return byId;
	}


    public Iterable<MediaMetadataCompat> getMusicsByGenre(String genre) {
        if (mCurrentState != State.INITIALIZED || !mMusicListByGenre.containsKey(genre)) {
            return Collections.emptyList();
        }
        return mMusicListByGenre.get(genre);
    }


	public Iterable<MediaMetadataCompat> searchMusic(String query) {
	    String metadataField= MediaMetadataCompat.METADATA_KEY_TITLE;
        if (mCurrentState != State.INITIALIZED) {
            return Collections.emptyList();
        }
        ArrayList<MediaMetadataCompat> result = new ArrayList<>();
        query = query.toLowerCase(Locale.US);
        for (MutableMediaMetadata track : mMusicListById.values()) {
            if (track.metadata.getString(metadataField).toLowerCase(Locale.US)
                    .contains(query)) {
                result.add(track.metadata);
            }
        }
        return result;
    }


    /**
     * Return the MediaMetadataCompat for the given musicID.
     *
     * @param musicId The unique, non-hierarchical music ID.
     */
    public MediaMetadataCompat getMusic(String musicId) {
        return mMusicListById.containsKey(musicId) ? mMusicListById.get(musicId).metadata : null;
    }

    public synchronized void updateMusicArt(String musicId, Bitmap albumArt, Bitmap icon) {
        MediaMetadataCompat metadata = getMusic(musicId);
        metadata = new MediaMetadataCompat.Builder(metadata)

                // set high resolution bitmap in METADATA_KEY_ALBUM_ART. This is used, for
                // example, on the lockscreen background when the media session is active.
                .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, albumArt)

                // set small version of the album art in the DISPLAY_ICON. This is used on
                // the MediaDescription and thus it should be small to be serialized if
                // necessary
                .putBitmap(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON, icon)

                .build();

        MutableMediaMetadata mutableMetadata = mMusicListById.get(musicId);
        if (mutableMetadata == null) {
            throw new IllegalStateException("Unexpected error: Inconsistent data structures in " +
                    "MusicProvider");
        }

        mutableMetadata.metadata = metadata;
    }

    public void setFavorite(String musicId, boolean favorite) {
        From from = new Select().from(DBMediaItem.class).where("MediaId = ?", musicId);
        if(from.exists()) {
            DBMediaItem item = from.executeSingle();
            item.setFavorite(favorite);
            item.save();
        }else {
            Timber.e("Set Fav failed %s", musicId);
        }
        if (favorite) {
            mFavoriteTracks.add(musicId);
        } else {
            mFavoriteTracks.remove(musicId);
        }

    }

    public boolean isInitialized() {
        return mCurrentState == State.INITIALIZED;
    }

    public boolean isFavorite(String musicId) {
        if(mFavoriteTracks!=null) {
            Log.w(TAG, "getFavNum: "+ mFavoriteTracks.size());
            return mFavoriteTracks.contains(musicId);
        }else return false;

    }

    /**
     * Get the list of music tracks from a server and caches the track information
     * for future reference, keying tracks by musicId and grouping by genre.
     */
    public void retrieveMediaAsync(final Callback callback) {
        LogHelper.d(TAG, "retrieveMediaAsync called");
        if (mCurrentState == State.INITIALIZED) {
            if (callback != null) {
                // Nothing to do, execute callback immediately
                callback.onMusicCatalogReady(true);
            }
            return;
        }

        // Asynchronously load the music catalog in a separate thread
        new AsyncTask<Void, Void, State>() {
            @Override
            protected State doInBackground(Void... params) {
                retrieveMedia();
                return mCurrentState;
            }

            @Override
            protected void onPostExecute(State current) {
                if (callback != null) {
                    callback.onMusicCatalogReady(current == State.INITIALIZED);
                }
            }
        }.execute();
    }

    private synchronized void buildListsByGenre() {
        ConcurrentMap<String, List<MediaMetadataCompat>> newMusicListByGenre = new ConcurrentHashMap<>();

        for (MutableMediaMetadata m : mMusicListById.values()) {
            String genre = m.metadata.getString(MediaMetadataCompat.METADATA_KEY_GENRE);
            List<MediaMetadataCompat> list = newMusicListByGenre.get(genre);
            if (list == null) {
                list = new ArrayList<>();
                newMusicListByGenre.put(genre, list);
            }
            list.add(m.metadata);
        }
        mMusicListByGenre = newMusicListByGenre;
    }

    private synchronized void retrieveMedia() {
        try {
            if (mCurrentState == State.NON_INITIALIZED) {
                mCurrentState = State.INITIALIZING;

                Iterator<MediaMetadataCompat> tracks = mSource.iterator();
                while (tracks.hasNext()) {
                    MediaMetadataCompat item = tracks.next();
                    String musicId = item.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID);
                    mMusicListById.put(musicId, new MutableMediaMetadata(musicId, item));
                    if(item.getRating(METADATA_KEY_USER_RATING) !=null &&
		                    item.getRating(METADATA_KEY_USER_RATING).isRated() &&
		                    item.getRating(METADATA_KEY_USER_RATING).getRatingStyle()== RatingCompat.RATING_HEART &&
		                    item.getRating(METADATA_KEY_USER_RATING).hasHeart())
                        mFavoriteTracks.add(musicId);
                }
                mCurrentState = State.INITIALIZED;
            }
        } finally {
            if (mCurrentState != State.INITIALIZED) {
                mCurrentState = State.NON_INITIALIZED;
            }
        }
    }

    public List<MediaBrowserCompat.MediaItem> getChildren(String mediaId, Resources resources) {
        List<MediaBrowserCompat.MediaItem> mediaItems = new ArrayList<>();
		    for(String s: MediaIDHelper.getHierarchy(mediaId)){
			    if(s.contains(MEDIA_ID_MUSICS_BY_SEARCH)){
				    String query = MediaIDHelper.extractBrowseCategoryValueFromMediaID(mediaId);
				    for (MediaMetadataCompat metadata : getMusicsById()) {
					    mediaItems.add(createMediaItemForSearch(metadata, query));
				    }
				    return mediaItems;
			    }
		    }
	    for (MediaMetadataCompat metadata : getMusicsById()) {
		    mediaItems.add(createMediaItemForRoot(metadata));
	    }
	    return mediaItems;
    }

    private MediaBrowserCompat.MediaItem createBrowsableMediaItemForRoot(Resources resources) {
        MediaDescriptionCompat description = new MediaDescriptionCompat.Builder()
                .setMediaId(MEDIA_ID_MUSICS_BY_GENRE)
                .setTitle(resources.getString(R.string.browse_genres))
                .setSubtitle(resources.getString(R.string.browse_genre_subtitle))
                .setIconUri(Uri.parse("android.resource://" +
                        "com.example.android.uamp/drawable/ic_by_genre"))
                .build();
        return new MediaBrowserCompat.MediaItem(description,
                MediaBrowserCompat.MediaItem.FLAG_BROWSABLE);
    }

    private MediaBrowserCompat.MediaItem createBrowsableMediaItemForGenre(String genre,
                                                                    Resources resources) {
        MediaDescriptionCompat description = new MediaDescriptionCompat.Builder()
                .setMediaId(createMediaID(null, MEDIA_ID_MUSICS_BY_GENRE, genre))
                .setTitle(genre)
                .setSubtitle(resources.getString(
                        R.string.browse_musics_by_genre_subtitle, genre))
                .build();
        return new MediaBrowserCompat.MediaItem(description,
                MediaBrowserCompat.MediaItem.FLAG_BROWSABLE);
    }
	private MediaBrowserCompat.MediaItem createMediaItemForRoot(MediaMetadataCompat metadata) {
		String title = metadata.getString(MediaMetadataCompat.METADATA_KEY_TITLE);
		String source = metadata.getString(MusicProviderSource.CUSTOM_METADATA_TRACK_SOURCE);
		String artUrl=metadata.getString(MediaMetadataCompat.METADATA_KEY_ART_URI);
		String hierarchyAwareMediaID = createMediaID(
				metadata.getDescription().getMediaId(), MEDIA_ID_ROOT, MEDIA_ID_ROOT);
		RatingCompat ratingCompat=metadata.getRating(METADATA_KEY_USER_RATING);

		MediaMetadataCompat copy = BaseRemoteSource.createMetadata(
				hierarchyAwareMediaID,
				source,
				title,
				artUrl,
				ratingCompat
		);
		return new MediaBrowserCompat.MediaItem(copy.getDescription(), MediaBrowserCompat.MediaItem.FLAG_PLAYABLE);

	}

    private MediaBrowserCompat.MediaItem createMediaItem(MediaMetadataCompat metadata) {
        // Since mediaMetadata fields are immutable, we need to create a copy, so we
        // can set a hierarchy-aware mediaID. We will need to know the media hierarchy
        // when we get a onPlayFromMusicID call, so we can create the proper queue based
        // on where the music was selected from (by artist, by genre, random, etc)
        String genre = metadata.getString(MediaMetadataCompat.METADATA_KEY_GENRE);
        String hierarchyAwareMediaID = MediaIDHelper.createMediaID(
                metadata.getDescription().getMediaId(), MEDIA_ID_MUSICS_BY_GENRE, genre);
        MediaMetadataCompat copy = new MediaMetadataCompat.Builder(metadata)
                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, hierarchyAwareMediaID)
                .build();
        return new MediaBrowserCompat.MediaItem(copy.getDescription(),
                MediaBrowserCompat.MediaItem.FLAG_PLAYABLE);

    }
	private MediaBrowserCompat.MediaItem createMediaItemForSearch(MediaMetadataCompat metadata, String query) {
		String title = metadata.getString(MediaMetadataCompat.METADATA_KEY_TITLE);
		String source = metadata.getString(MusicProviderSource.CUSTOM_METADATA_TRACK_SOURCE);
		String artUrl=metadata.getString(MediaMetadataCompat.METADATA_KEY_ART_URI);
		String hierarchyAwareMediaID = createMediaID(
				metadata.getDescription().getMediaId(), MEDIA_ID_MUSICS_BY_SEARCH, query);
		RatingCompat ratingCompat=metadata.getRating(METADATA_KEY_USER_RATING);

		MediaMetadataCompat copy = BaseRemoteSource.createMetadata(
				hierarchyAwareMediaID,
				source,
				title,
				artUrl,
				ratingCompat
		);
		return new MediaBrowserCompat.MediaItem(copy.getDescription(), MediaBrowserCompat.MediaItem.FLAG_PLAYABLE);
	}

}
