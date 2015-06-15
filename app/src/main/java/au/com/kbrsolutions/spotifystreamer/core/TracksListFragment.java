package au.com.kbrsolutions.spotifystreamer.core;

import android.app.Activity;
import android.os.AsyncTask;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Image;
import kaaes.spotify.webapi.android.models.Track;
import kaaes.spotify.webapi.android.models.Tracks;
import retrofit.http.QueryMap;

/**
 * A placeholder fragment containing a simple view.
 */
public class TracksListFragment extends ListFragment {
//public class TracksListFragment extends ListFragment {

    private String mArtistId;
    private TracksActivity mActivity;

    private final static String LOG_TAG = TracksListFragment.class.getSimpleName();

    public TracksListFragment() {
    }

//    @Override
//    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
//        return inflater.inflate(R.layout.fragment_tracks, container, false);
//    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.mActivity = (TracksActivity) activity;
    }

//    @Override
//    public void onViewCreated(View view, Bundle savedInstanceState) {
//        sendArtistsDataRequestToSpotify(mArtistId);
//    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
    }

    public void setMessage(String message) {
        this.mArtistId = message;
        Log.v(LOG_TAG, "setMessage - got message: " + message);
    }

    public void sendArtistsDataRequestToSpotify(String mArtistId) {
        TracksDataFetcher artistsFetcher = new TracksDataFetcher();
        artistsFetcher.execute(mArtistId);
    }

    public class TracksDataFetcher extends AsyncTask<String, Void, List<TrackDetails>> {

        @Override
        protected void onPostExecute(List<TrackDetails> trackDetails) {
            if (trackDetails == null) {
                Log.v(LOG_TAG, "showing toast");
                Toast.makeText(getActivity(), "No data found", Toast.LENGTH_LONG).show();
                return;
            }
            List<String> artistsNames = new ArrayList<>(trackDetails.size());
            for (TrackDetails artistDetails: trackDetails) {
                artistsNames.add(artistDetails.trackName + " - " + artistDetails.albumThumbnailImageUrl);
            }
//            mArtistArrayAdapter.clear();
//            mArtistArrayAdapter.addAll(trackDetails);
//            TracksArrayAdapter trackArrayAdapter = getListAdapter();
            TrackArrayAdapter trackArrayAdapter = new TrackArrayAdapter(mActivity, trackDetails);
            setListAdapter(trackArrayAdapter);
            trackArrayAdapter.notifyDataSetChanged();
            //java.lang.RuntimeException: Your content must have a ListView whose id attribute is 'android.R.id.list'
        }

        private SpotifyService mSpotifyService;

        @Override
        protected List<TrackDetails> doInBackground(String... params) {

            // If there's no artist trackName, there's nothing to look up.  Verify size of params.
            if (params.length == 0) {
                return null;
            }
            List<TrackDetails> results = null;
            String artistId = params[0];
            if (mSpotifyService == null) {
                SpotifyApi api = new SpotifyApi();
                mSpotifyService = api.getService();
            }
            QueryMap queryMap = null;   //new HashMap<>();
//            queryMap.put("country", new );
//            mSpotifyService.getArtistTopTrack("", queryMap);
            Map<String, Object> m = new HashMap<>();
            m.put("country", "US");
            Tracks tracks = mSpotifyService.getArtistTopTrack(artistId, m);
            //"GET https://api.spotify.com/v1/artists/43ZHCT0cAZBISjO8DG9PnE/top-tracks?country=SE"
            // GET https://api.spotify.com/v1/artists/4gzpq5DPGxSnKTe4SA8HAU%3Fcountry%3DUS/top-tracks

            Log.v(LOG_TAG, "handleArtistsData - results: " + tracks);
            List<Track> listedTracks = tracks.tracks;
            List<Image> images;
            String thumbnailImageUrl;
            int imagesCnt;
            if (listedTracks.size() > 0) {
                results = new ArrayList<TrackDetails>(listedTracks.size());
                for (Track track : listedTracks) {
//                    images = track.images;
//                    imagesCnt = images.size();
//                    if (imagesCnt == 0) {
//                        thumbnailImageUrl = null;
//                    } else {
//                        thumbnailImageUrl = getThumbnaiImagelUrl(images);
//                    }
//                    results.add(new TrackDetails(track.name, track.id, thumbnailImageUrl));
                    results.add(new TrackDetails(track.name, track.album.name, null, null));
//                    Log.v(LOG_TAG, "doInBackground - id/trackName/popularity: " + track.name + "/" + track.popularity + "/" + thumbnailImageUrl);
                    Log.v(LOG_TAG, "doInBackground - id/trackName/popularity: " + track.name + "/" + track.popularity);
                }
            }
            return results;

        }

        private String getThumbnaiImagelUrl(List<Image> imagesData) {
            int lastImagaDataIdx = imagesData.size() - 1;
            String selectedUrl = imagesData.get(lastImagaDataIdx).url;
            int imageWidth;
            for (int i = lastImagaDataIdx; i > -1; i--) {
                imageWidth = (int) Integer.valueOf(imagesData.get(i).width);
                if (imageWidth > 100) {
                    selectedUrl = imagesData.get(i).url;
                    break;
                }
            }
            return selectedUrl;
        }
    }
}
