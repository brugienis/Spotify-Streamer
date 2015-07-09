/*
 * Copyright (C) 2013 The Android Open Source Project
 */

package au.com.kbrsolutions.spotifystreamer.core;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import au.com.kbrsolutions.spotifystreamer.R;
import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyCallback;
import kaaes.spotify.webapi.android.SpotifyError;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Artist;
import kaaes.spotify.webapi.android.models.ArtistsPager;
import kaaes.spotify.webapi.android.models.Image;
import kaaes.spotify.webapi.android.models.Pager;
import kaaes.spotify.webapi.android.models.Track;
import kaaes.spotify.webapi.android.models.Tracks;
import retrofit.client.Response;

/**
 * Retrieves artists data using search text entered by the user.
 */
public class ArtistsFragment extends Fragment {

    /**
     * Declares callback methods that have to be implemented by parent Activity
     */
    interface ArtistsFragmentCallbacks {
        void showTracksData(int selectedArtistRowPosition, List<TrackDetails> trackDetails);
    }

    private ArtistsFragmentCallbacks mCallbacks;
    private EditText mSearchText;
    private ListView mListView;
    private List<ArtistDetails> mArtistsDetailsList;
    private ArtistArrayAdapter<TrackDetails> mArtistArrayAdapter;
    private Activity mActivity;
    private boolean mSearchInProgress;
    private String mArtistName;

    private final static String LOG_TAG = ArtistsFragment.class.getSimpleName();

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.mActivity = activity;
        try {
            mCallbacks = (ArtistsFragmentCallbacks) activity;
        } catch (Exception e) {
            throw new RuntimeException(
                    mActivity.getResources()
                            .getString(R.string.callbacks_not_implemented, activity.toString()));
        }
    }

    @Override
     public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        /* Retain this fragment across configuration changes. */
        setRetainInstance(true);
    }

    @Override
     public void onDetach() {
        super.onDetach();
        mCallbacks = null;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragments_artists_view, container, false);
        List<ArtistDetails> artistsItemsList = new ArrayList<>();

        mSearchText = (EditText) rootView.findViewById(R.id.searchTextView);
        mSearchText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                return handleSearchButtonClicked(actionId);
            }
        });
        mArtistArrayAdapter = new ArtistArrayAdapter<>(getActivity(), artistsItemsList);
        mListView = (ListView) rootView.findViewById(R.id.listview_artists);
        mListView.setAdapter(mArtistArrayAdapter);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                handleArtistRowClicked(position);
            }
        });
        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        String searchText = mSearchText.getText().toString();
        mSearchText.setSelection(searchText.length());
    }

    @Override
    public void onPause() {
        super.onPause();
        // hide the keyboard in order to avoid getTextBeforeCursor on inactive InputConnection
        hideKeyboard();
    }

    /**
     * Called when user pressed search button. Starts search if search text is not empty.
     */
    private boolean handleSearchButtonClicked(int actionId) {
        Log.v(LOG_TAG, "handleSearchButtonClicked - start");
        boolean handled = false;
        if (actionId == EditorInfo.IME_ACTION_SEARCH) {
            mArtistName = mSearchText.getText().toString();
            if (mArtistName.trim().length() > 0) {
                sendArtistsDataRequestToSpotify(mArtistName);
                handled = true;
                hideKeyboard();
            } else {
                mSearchText.setText("");
            }
        }
        return handled;
    }

    /**
     * Contains details needed by the TracksDataFetcherWithCallbacks.
     */
    private class SelectedArtistRowDetails {
        private final int listViewFirstVisiblePosition;
        private final String artistId;

        SelectedArtistRowDetails(int listViewFirstVisiblePosition, String artistId) {
            this.listViewFirstVisiblePosition = listViewFirstVisiblePosition;
            this.artistId = artistId;
        }

    }

    /**
     * Called when user click on an artist row. It will start asynchronous search for the artist's
     * top 10 tracks.
     */
    private void handleArtistRowClicked(int position) {
        TracksDataFetcherWithCallbacks artistsFetcher = new TracksDataFetcherWithCallbacks();
        artistsFetcher.execute(
                new SelectedArtistRowDetails(mListView.getFirstVisiblePosition(),
                        (mArtistArrayAdapter.getItem(position)).spotifyId));
    }

    private void hideKeyboard() {
        InputMethodManager inputMethodManager =
                (InputMethodManager) mActivity.getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(mSearchText.getWindowToken(), 0);
    }

    private synchronized void setSearchInProgress(boolean value) {
        mSearchInProgress = value;
    }

    /**
     * Should be called before trying to show artists data saved in parent activity in the
     * onSaveInstanceState method. If search is in progress don't show previously saved data.
     */
    public synchronized boolean isSearchInProgress() {
        return mSearchInProgress;
    }

    /**
     * Returns data that should be saved in parent activity onSaveInstanceState method.
     */
    public ArtistFragmentSaveData getDataToSave() {
        mArtistName = mSearchText.getText().toString();
        return new ArtistFragmentSaveData(mArtistName, mArtistsDetailsList, mListView.getFirstVisiblePosition());
    }

    /**
     * Called from the parent activity. Contains artists details that will be placed on the screen.
     */
    public void showArtistsDetails(CharSequence artistName, List<ArtistDetails> artistsDetailsList,
                                   int listViewFirstVisiblePosition) {
        mSearchText.setText(artistName);
        mSearchText.requestFocus();
        if (artistName != null) {
            mSearchText.setSelection(artistName.length());
        }
        mArtistArrayAdapter.clear();
        if (artistsDetailsList != null) {
            mArtistArrayAdapter.addAll(artistsDetailsList);
            mListView.setSelection(listViewFirstVisiblePosition);
        }
    }

    /**
     * Starts asynchronous search for artists details.
     */
    private void sendArtistsDataRequestToSpotify(String artistName) {
        setSearchInProgress(true);
        mArtistArrayAdapter.clear();
        ArtistsDataFetcherWithCallbacks artistsFetcher = new ArtistsDataFetcherWithCallbacks();
        artistsFetcher.execute(artistName);
    }

    /**
     * Class that contain code to perform asynchronous search for artists.
     */
    public class ArtistsDataFetcherWithCallbacks
            extends AsyncTask<String, Void, List<ArtistDetails>> {

        private boolean networkProblems;
        private String artistName;
        private SpotifyService mSpotifyService;
        private List<ArtistDetails> results = null;
        private CountDownLatch callBackResultsCountDownLatch;

        /**
         * When background processing id done and artists data found, calls onPostExecute
         * method onn the class that implemented the ArtistsFragmentCallbacks.
         */
        @Override
        protected void onPostExecute(List<ArtistDetails> artistsDetailsList) {
            Context context = mActivity.getApplicationContext();
            if (networkProblems) {
                Toast.makeText(context,
                        mActivity.getResources()
                                .getString(R.string.search_unsuccessful_network_problems),
                        Toast.LENGTH_LONG).show();
                return;
            } else if (artistsDetailsList == null) {
                Toast.makeText(context,
                        mActivity.getResources()
                                .getString(R.string.search_returned_no_artist_data),
                        Toast.LENGTH_LONG).show();
                return;
            }
//            if (mCallbacks != null) {
//                mCallbacks.onPostExecute(artistName, artistsDetailsList, 0);
//            }
            mArtistsDetailsList = artistsDetailsList;
            showArtistsDetails(artistName, artistsDetailsList, 0);
            setSearchInProgress(false);
        }

        /**
         * Calls SpotifyService.searchArtists method with SpotifyCallback.
         */
        @Override
        protected List<ArtistDetails> doInBackground(String... params) {

            // If there's no artist trackName, there's nothing to look up.  Verify size of params.
            if (params.length == 0) {
                return null;
            }

            artistName = params[0];
            if (mSpotifyService == null) {
                SpotifyApi api = new SpotifyApi();
                mSpotifyService = api.getService();
            }
            callBackResultsCountDownLatch = new CountDownLatch(1);
            mSpotifyService.searchArtists(artistName, new SpotifyCallback<ArtistsPager>() {
                @Override
                public void failure(SpotifyError spotifyError) {
                    networkProblems = true;
                    callBackResultsCountDownLatch.countDown();
                }

                @Override
                public void success(ArtistsPager artistsPager, Response response) {
                    Pager<Artist> artists = artistsPager.artists;
                    List<Image> images;
                    List<Artist> artistsList = artists.items;
                    String thumbnailImageUrl;
                    int imagesCnt;
                    if (artistsList.size() > 0) {
                        results = new ArrayList<ArtistDetails>(artistsList.size());
                        for (Artist artist : artistsList) {
                            images = artist.images;
                            imagesCnt = images.size();
                            if (imagesCnt == 0) {
                                thumbnailImageUrl = null;
                            } else {
                                thumbnailImageUrl = getThumbnaiImagelUrl(images);
                            }
                            results.add(
                                    new ArtistDetails(artist.name, artist.id, thumbnailImageUrl));
                        }
                    }
                    callBackResultsCountDownLatch.countDown();
                }
            });

            try {
                callBackResultsCountDownLatch.await();
            } catch (InterruptedException nothingCanBeDone) {
                Toast.makeText(mActivity.getApplicationContext(),
                        mActivity.getResources()
                                .getString(R.string.search_unsuccessful_internal_problems),
                        Toast.LENGTH_LONG).show();
            }
            return results;

        }

        private final int THUMBNAIL_MIN_SIZE = 100;

        /**
         * Returns an URL of the smallest image that has width greater then 100 pixels
         */
        private String getThumbnaiImagelUrl(List<Image> imagesData) {
            int lastImagaDataIdx = imagesData.size() - 1;
            String selectedUrl = imagesData.get(lastImagaDataIdx).url;
            int imageWidth;
            for (int i = lastImagaDataIdx; i > -1; i--) {
                imageWidth = (int) Integer.valueOf(imagesData.get(i).width);
                if (imageWidth > THUMBNAIL_MIN_SIZE) {
                    selectedUrl = imagesData.get(i).url;
                    break;
                }
            }
            return selectedUrl;
        }

    }

    /**
     * Class that contain code to perform asynchronous search for artist's top 10 tracks.
     */
    public class TracksDataFetcherWithCallbacks
            extends AsyncTask<SelectedArtistRowDetails, Void, List<TrackDetails>> {

        private SpotifyService mSpotifyService;
        private boolean networkProblems = true;
        private int listViewFirstVisiblePosition;
        private List<TrackDetails> results;
        private CountDownLatch callBackResultsCountDownLatch;

        /**
         * When background processing id done and artists data found, calls onPostExecute
         * method onn the class that implemented the ArtistsFragmentCallbacks.
         */
        @Override
        protected void onPostExecute(List<TrackDetails> trackDetails) {
            Context context = mActivity.getApplicationContext();
            if (!networkProblems) {
                Toast.makeText(context,
                        mActivity.getResources()
                                .getString(R.string.search_unsuccessful_network_problems),
                        Toast.LENGTH_LONG).show();
                return;
            } else if (trackDetails == null) {
                Toast.makeText(context,
                        mActivity.getResources()
                                .getString(R.string.search_returned_no_track_data),
                        Toast.LENGTH_LONG).show();
                return;
            }
            mCallbacks.showTracksData(listViewFirstVisiblePosition, trackDetails);
        }


        /**
         * Calls SpotifyService.getArtistTopTrack method with SpotifyCallback.
         *
         * The country code is taken from the preferences.
         */
        @Override
        protected List<TrackDetails> doInBackground(SelectedArtistRowDetails... params) {
            listViewFirstVisiblePosition = params[0].listViewFirstVisiblePosition;
            String artistId = params[0].artistId;

            // If there's no artist Id, there's nothing to look up.  Verify size of params.
            if (artistId.length() == 0) {
                return null;
            }

            String countryCode = PreferenceManager.getDefaultSharedPreferences(mActivity)
                    .getString(getResources().getString(R.string.pref_country_key),
                            getResources().getString(R.string.pref_country_default));
            if (countryCode.trim().length() == 0) {
                countryCode = getResources().getString(R.string.pref_country_default);
            }
            if (mSpotifyService == null) {
                SpotifyApi api = new SpotifyApi();
                mSpotifyService = api.getService();
            }
            Map<String, Object> options = new HashMap<>();
            options.put(mActivity.getResources().getString(R.string.country_string), countryCode);

            callBackResultsCountDownLatch = new CountDownLatch(1);
            mSpotifyService.getArtistTopTrack(artistId, options, new SpotifyCallback<Tracks>() {
                @Override
                public void failure(SpotifyError spotifyError) {
                    networkProblems = true;
                    callBackResultsCountDownLatch.countDown();
                }

                @Override
                public void success(Tracks tracks, Response response) {
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
                            results.add(
                                    new TrackDetails(track.name, track.album.name, trackImages.large,
                                            trackImages.small, track.preview_url));
                        }
                    }
                    callBackResultsCountDownLatch.countDown();
                }
            });

            try {
                callBackResultsCountDownLatch.await();
            } catch (InterruptedException nothingCanBeDone) {
                Toast.makeText(mActivity.getApplicationContext(),
                        mActivity.getResources()
                                .getString(R.string.search_unsuccessful_internal_problems),
                        Toast.LENGTH_LONG).show();
            }

            return results;
        }

        private final int LARGE_IMAGE_MIN_WIDTH = 640;
        private final int SMALL_IMAGE_MIN_WIDTH = 200;

        /**
         * Returns TrackImages class which contains URLs of the big and small images.
         */
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
                    if (imageWidth >= SMALL_IMAGE_MIN_WIDTH) {
                        smallImage = oneImage.url;
                    }
                }
                if (imageWidth >= LARGE_IMAGE_MIN_WIDTH) {
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
            public final String large;
            public final String small;
            TrackImages(String large, String small) {
                this.large = large;
                this.small = small;
            }
        }
    }

}
