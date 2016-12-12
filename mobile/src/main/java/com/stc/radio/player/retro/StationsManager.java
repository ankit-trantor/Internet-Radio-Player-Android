package com.stc.radio.player.retro;

import android.support.v4.media.MediaMetadataCompat;

import com.stc.radio.player.model.MusicProviderSource;

import java.util.Iterator;

import timber.log.Timber;

import static com.stc.radio.player.retro.StationsManager.PLAYLISTS.DI;
import static com.stc.radio.player.retro.StationsManager.PLAYLISTS.RADIOTUNES;
import static com.stc.radio.player.retro.StationsManager.PLAYLISTS.ROCK;
import static com.stc.radio.player.retro.StationsManager.PLAYLISTS.SOMA;

/**
 * Created by artem on 10/15/16.
 */

public abstract class StationsManager  implements MusicProviderSource {
	public static String getArtUrl(String s1) {
		return "http://somafm.com/img3/" + s1+ "-400.jpg";
	}
	public static String getArtUrl(ParsedPlaylistItem item){
		String s = item.getImages().getDefault();
		//Timber.w("arturl=%s",s);
		if(s==null) Timber.e("ERROR null");
		else return "http:"+s.replace("{?size,height,width,quality,pad}", "?size=200x200");
		return "null";
	}
	public  static String getUrl(String playlist, String key) {
		int num=1;//new Random().nextInt(1)+1;
		if(playlist.contains(SOMA))
			return "http://ice" + num + ".somafm.com/" + key + "-128-aac";

		String base="";
		if(playlist.equals(DI) ) base+="di.fm"+"/"+key+"_hi";
		else if (playlist.equals(RADIOTUNES) )base+="radiotunes.com"+"/"+key+"_hi";
		else if ( playlist.equals(ROCK))base+="rockradio.com"+"/"+key;
		else base+=playlist+".com"+"/"+key;
		return "http://prem"+num+"."+base+"?"+ SettingsProvider.getToken();
	}

	@Override
	public Iterator<MediaMetadataCompat> iterator() {
		return null;
	}

	/**
	 * Created by artem on 10/13/16.
	 */

	public static class PLAYLISTS {
		public static final String DI="difm";
		public static final String RADIOTUNES="radiotunes";
		public static final String JAZZ="jazzradio";
		public static final String ROCK="rockradio";
		public static final String CLASSIC="classicalradio";
		public static final String FAV="favorite";
		public static final String SOMA="somafm";
	}
	public static final String[] somaStations = {
			"groovesalad",
			"dronezone",
			"spacestation",
			"secretagent",
			"lush",
			"u80s",
			"deepspaceone",
			"beatblender",
			"defcon",
			"seventies",
			"folkfwd",
			"bootliquor",
			"suburbsofgoa",
			"poptron",
			"thistle",
			"fluid",
			"digitalis",
			"illstreet",
			"7soul",
			"brfm",
			"cliqhop",
			"doomed",
			"earwaves",
	};
}
