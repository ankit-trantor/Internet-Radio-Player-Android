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

import android.support.v4.media.MediaMetadataCompat;

import com.stc.radio.player.retro.Retro;
import com.stc.radio.player.retro.SettingsProvider;
import com.stc.radio.player.retro.StationsManager;
import com.stc.radio.player.utils.LogHelper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;

import timber.log.Timber;

/**
 * Utility class to get a list of MusicTrack's based on a server-side JSON
 * configuration.
 */
public class RemoteSource implements MusicProviderSource {

	private static final String TAG = LogHelper.makeLogTag(RemoteSource.class);

	@Override
	public Iterator<MediaMetadataCompat> iterator() {
		Retro.updateToken();
		SettingsProvider.getToken();
		
		ArrayList<MediaMetadataCompat> allTracks = new ArrayList<>();
		ArrayList<BaseRemoteSource> sources = new ArrayList<>();
		sources.add(new AudioAddictSource(StationsManager.PLAYLISTS.DI));
		sources.add(new AudioAddictSource(StationsManager.PLAYLISTS.CLASSIC));
		sources.add(new AudioAddictSource(StationsManager.PLAYLISTS.JAZZ));
		sources.add(new AudioAddictSource(StationsManager.PLAYLISTS.ROCK));
		sources.add(new AudioAddictSource(StationsManager.PLAYLISTS.RADIOTUNES));
		sources.add(new SomaRemoteSource(StationsManager.PLAYLISTS.SOMA));
		for(BaseRemoteSource source: sources){
			ArrayList<MediaMetadataCompat> sourceTracks = source.getStations();
			if(sourceTracks!=null && !sourceTracks.isEmpty())
				allTracks.addAll(sourceTracks);
			else Timber.e(TAG, "sourceTracks %s empty",source.getName() );
		}
		//Collections.sort(allTracks, comparatorDBMediaItem);
		//allTracks= RatingHelper.sortByRating(allTracks);

		//Timber.w("list size=%d", allTracks.size());
		return allTracks.iterator();
	}
	static Comparator<MediaMetadataCompat> comparatorDBMediaItem=new Comparator<MediaMetadataCompat>() {
		@Override
		public int compare(MediaMetadataCompat item2, MediaMetadataCompat item1) {
			String title1=item1.getString(MediaMetadataCompat.METADATA_KEY_TITLE);
			String title2=item2.getString(MediaMetadataCompat.METADATA_KEY_TITLE);
			return(title1.compareToIgnoreCase(title2));
		}
	};
}



