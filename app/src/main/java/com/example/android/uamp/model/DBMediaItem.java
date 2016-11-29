package com.example.android.uamp.model;

import android.support.v4.media.MediaMetadataCompat;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;

import static android.R.attr.duration;
import static android.R.attr.id;
import static com.google.android.gms.analytics.internal.zzy.m;

/**
 * Created by artem on 10/22/16.
 */
@Table(name = "DBMediaItem", id = "_id")
public class DBMediaItem extends Model {

	@Column(name = "MediaId")
	String mediaId;

	@Column(name = "Genre")
	String genre;

	@Column(name = "Name")
	String name;

	@Column(name = "Source")
	String source;

	@Column(name = "IconUri")
	String iconUri;

	@Column(name = "TrackNumber")
	long trackNumber;

	@Column(name = "TotalTrackCount")
	long totalTrackCount;

	@Column(name = "Favorite")
	long favorite;

	public long isFavorite() {
		return favorite;
	}

	public void setFavorite(long favorite) {
		this.favorite = favorite;
	}

	/*
					*/
	public DBMediaItem(MediaMetadataCompat metadata) {
		this.mediaId = metadata.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID);
		this.genre = metadata.getString(MediaMetadataCompat.METADATA_KEY_GENRE);
		this.source = metadata.getString(MusicProviderSource.CUSTOM_METADATA_TRACK_SOURCE);
		this.name = metadata.getString(MediaMetadataCompat.METADATA_KEY_ARTIST);
		this.iconUri = metadata.getString(MediaMetadataCompat.METADATA_KEY_ART_URI);
		this.totalTrackCount = metadata.getLong(MediaMetadataCompat.METADATA_KEY_NUM_TRACKS);
		this.trackNumber = metadata.getLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER);
		this.favorite=metadata.getLong(MusicProviderSource.CUSTOM_METADATA_FAVORITE);
	}
	public MediaMetadataCompat createMetadata() {
		return new MediaMetadataCompat.Builder()
				.putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, mediaId)
				.putString(MusicProviderSource.CUSTOM_METADATA_TRACK_SOURCE, source)
				.putLong(MusicProviderSource.CUSTOM_METADATA_FAVORITE, favorite)
				.putLong(MediaMetadataCompat.METADATA_KEY_DURATION, -1)
				.putLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER, trackNumber)
				.putLong(MediaMetadataCompat.METADATA_KEY_NUM_TRACKS, totalTrackCount)
				.putString(MediaMetadataCompat.METADATA_KEY_GENRE, genre)
				.putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, iconUri)
				.putString(MediaMetadataCompat.METADATA_KEY_ART_URI, iconUri)
				.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON_URI, iconUri)
				.putString(MediaMetadataCompat.METADATA_KEY_ARTIST, name)
				.build();
	}

	public DBMediaItem() {
	}

	public DBMediaItem(String mediaId, String genre, String name, String source, String iconUri, int totalTrackCount, int trackNumber) {
		this.mediaId = mediaId;
		this.genre = genre;
		this.name = name;
		this.source = source;
		this.iconUri = iconUri;
		this.totalTrackCount = totalTrackCount;
		this.trackNumber = trackNumber;
	}
}
