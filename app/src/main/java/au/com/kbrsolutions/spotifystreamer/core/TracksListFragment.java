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

import au.com.kbrsolutions.spotifystreamer.R;
import au.com.kbrsolutions.spotifystreamer.utils.ProgressBarHandler;
import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Image;
import kaaes.spotify.webapi.android.models.Track;
import kaaes.spotify.webapi.android.models.Tracks;

/**
 * A placeholder fragment containing a simple view.
 */
public class TracksListFragment extends ListFragment {
//public class TracksListFragment extends ListFragment {

    private String mArtistId;
    private TracksActivity mActivity;
    private ProgressBarHandler mProgressBarHandler;

    private final int BIG_WIDTH = 640;
    private final int SMALL_WIDTH = 200;

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

    public void sendArtistsDataRequestToSpotify(String mArtistId) {
        setIsRetrievingData(true);
        TracksDataFetcher artistsFetcher = new TracksDataFetcher();
        artistsFetcher.execute(mArtistId);
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.v(LOG_TAG, "onResume - start");
        if (mProgressBarHandler == null) {
            mProgressBarHandler = new ProgressBarHandler(mActivity);
        }
        if (sRetrievingData()) {
            mProgressBarHandler.show();
        }
        Log.v(LOG_TAG, "onResume - end");
    }

    private boolean isRetrievingData;

    private synchronized boolean sRetrievingData() {
        return isRetrievingData;
    }

    private synchronized void setIsRetrievingData(boolean isRetrievingData) {
        this.isRetrievingData = isRetrievingData;
    }

    public class TracksDataFetcher extends AsyncTask<String, Void, List<TrackDetails>> {

        @Override
        protected void onPostExecute(List<TrackDetails> trackDetails) {
            mProgressBarHandler.hide();
            if (trackDetails == null) {
                Log.v(LOG_TAG, "showing toast");
                Toast.makeText(mActivity, mActivity.getResources().getString(R.string.search_returned_no_track_data), Toast.LENGTH_LONG).show();
                mActivity.goBack();
                return;
            }
            List<String> artistsNames = new ArrayList<>(trackDetails.size());
//            for (TrackDetails artistDetails: trackDetails) {
//                artistsNames.add(artistDetails.trackName + " - " + artistDetails.albumThumbnailImageUrl);
//            }
            TrackArrayAdapter trackArrayAdapter = (TrackArrayAdapter) getListAdapter();
//            TrackArrayAdapter trackArrayAdapter = new TrackArrayAdapter(mActivity, trackDetails);
            trackArrayAdapter.clear();
            trackArrayAdapter.addAll(trackDetails);
            setIsRetrievingData(false);
//            setListAdapter(trackArrayAdapter);
//            trackArrayAdapter.notifyDataSetChanged();
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
            Map<String, Object> m = new HashMap<>();
            m.put("country", "US");
            Tracks tracks = mSpotifyService.getArtistTopTrack(artistId, m);
            //"GET https://api.spotify.com/v1/artists/43ZHCT0cAZBISjO8DG9PnE/top-tracks?country=SE"
            // GET https://api.spotify.com/v1/artists/4gzpq5DPGxSnKTe4SA8HAU%3Fcountry%3DUS/top-tracks

            List<Track> listedTracks = tracks.tracks;
            List<Image> images;
            String thumbnailImageUrl;
            int imagesCnt;
            // TrackDetails(String trackName, String albumName, String thumbnailImageUrl, String previewUrl)
            if (listedTracks.size() > 0) {
                results = new ArrayList<TrackDetails>(listedTracks.size());
                for (Track track : listedTracks) {
                    images = track.album.images;
                    imagesCnt = images.size();
                    TrackImages trackImages;
                    if (imagesCnt == 0) {
                        trackImages = new TrackImages(null, null);
                    } else {
                        trackImages = getImagesUrls(images);
                    }
                    results.add(new TrackDetails(track.name, track.album.name, trackImages.big, trackImages.small, track.preview_url));
//                    Log.v(LOG_TAG, "doInBackground - id/trackName/popularity: " + track.name + "/" + track.popularity);
                }
            }
            return results;
        }

        private TrackImages getImagesUrls(List<Image> imagesData) {
            Image prevImage = null;
            String bigImage = null;
            String smallImage = null;
            Image oneImage;
            int imageWidth;
            for (int i = imagesData.size() - 1; i > -1; i--) {
                oneImage = imagesData.get(i);
                imageWidth = (int) Integer.valueOf(oneImage.width);
//                Log.v(LOG_TAG, "getImagesUrls - imageWidth/popularity: " + imageWidth + "/" + oneImage.url);
                if (smallImage == null) {
                    if (imageWidth >= SMALL_WIDTH) {
                        smallImage = oneImage.url;
                    }
                }
                if (bigImage == null) {
                    if (imageWidth >= BIG_WIDTH) {
                        bigImage = oneImage.url;
                        break;
                    }
                }
                prevImage = oneImage;
            }
            if (bigImage == null) {
                bigImage = prevImage.url;
            }
            return new TrackImages(bigImage, smallImage);
        }

        class TrackImages {
            public final String big;
            public final String small;
            TrackImages(String big, String small) {
                this.big = big;
                this.small = small;
            }
        }
    }
}
