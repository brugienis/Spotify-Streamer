package au.com.kbrsolutions.spotifystreamer.core;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import au.com.kbrsolutions.spotifystreamer.R;
import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Image;
import kaaes.spotify.webapi.android.models.Track;
import kaaes.spotify.webapi.android.models.Tracks;

/**
 * Created by business on 16/06/2015.
 */
public class TracksListFragment extends ListFragment {

    private TracksActivity mActivity;
    private boolean searchStarted;
    private final int BIG_IMAGE_WIDTH = 640;
    private final int SMALL_IMAGE_WIDTH = 200;

    private final static String LOG_TAG = TracksListFragment.class.getSimpleName();

    public TracksListFragment() {}

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.mActivity = (TracksActivity) activity;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Retain this fragment across configuration changes.
        setRetainInstance(true);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
    }

    public void sendArtistsDataRequestToSpotify(String mArtistId) {
        if (isSearchStarted()) {
            return;
        }
        setSearchStarted(true);
        TracksDataFetcher artistsFetcher = new TracksDataFetcher();
        artistsFetcher.execute(mArtistId);
    }

    public synchronized void setSearchStarted(boolean value) {
        searchStarted = value;
    }

    public synchronized boolean isSearchStarted() {
        return searchStarted;
    }

    public class TracksDataFetcher extends AsyncTask<String, Void, List<TrackDetails>> {

        private boolean successfullyAccessedSpotify = true;

        @Override
        protected void onPostExecute(List<TrackDetails> trackDetails) {
            if (!successfullyAccessedSpotify) {
                Toast.makeText(mActivity, mActivity.getResources().getString(R.string.search_unsuccessful_network_problems), Toast.LENGTH_LONG).show();
                return;
            } else if (trackDetails == null) {
                Toast.makeText(mActivity, mActivity.getResources().getString(R.string.search_returned_no_track_data), Toast.LENGTH_LONG).show();
                mActivity.goBack();
                return;
            }
            TrackArrayAdapter<TrackDetails> trackArrayAdapter = (TrackArrayAdapter) getListAdapter();
            trackArrayAdapter.clear();
            trackArrayAdapter.addAll(trackDetails);
        }

        private SpotifyService mSpotifyService;

        @Override
        protected List<TrackDetails> doInBackground(String... params) {
            String artistId = params[0];

            // If there's no artist Id, there's nothing to look up.  Verify size of params.
            if (artistId.length() == 0) {
                return null;
            }
            List<TrackDetails> results = null;
            try {
                if (mSpotifyService == null) {
                    SpotifyApi api = new SpotifyApi();
                    mSpotifyService = api.getService();
                }
                Map<String, Object> m = new HashMap<>();
                m.put("country", "US");
                Tracks tracks = mSpotifyService.getArtistTopTrack(artistId, m);

                List<Track> listedTracks = tracks.tracks;
                List<Image> images;
                int imagesCnt;
                if (listedTracks.size() > 0) {
                    results = new ArrayList<>(listedTracks.size());
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
                    }
                }
            } catch (Exception e) {
                successfullyAccessedSpotify = false;
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
                imageWidth = oneImage.width;
                if (smallImage == null) {
                    if (imageWidth >= SMALL_IMAGE_WIDTH) {
                        smallImage = oneImage.url;
                    }
                }
                if (imageWidth >= BIG_IMAGE_WIDTH) {
                    bigImage = oneImage.url;
                    break;
                }
                prevImage = oneImage;
            }
            if (bigImage == null && prevImage != null) {
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
