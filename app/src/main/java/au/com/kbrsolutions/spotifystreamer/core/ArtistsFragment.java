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
        void artistSearchStarted();
        void artistSearchEnded();
//        void setTracksFragmentEmptyText(String emptyText);
        void showTracksData(String artistName, List<TrackDetails> trackDetails);
    }

    private ArtistsFragmentCallbacks mCallbacks;
    private EditText mSearchText;
    private ListView mListView;
    private TextView mEmptyView;
    private List<ArtistDetails> mArtistsDetailsList;
    private int mArtistsListViewFirstVisiblePosition;
    private ArtistArrayAdapter<TrackDetails> mArtistArrayAdapter;
    private Activity mActivity;
    private boolean mSearchInProgress;
    private String mArtistName;
    private final String ARTIST_NAME = "artist_name";
    private final String ARTISTS_DATA = "artists_data";
    private final String LIST_VIEW_FIRST_VISIBLE_POSITION = "list_view_first_visible_position";

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
        Log.v(LOG_TAG, "onCreateView");
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
        mEmptyView = (TextView) rootView.findViewById(R.id.emptyView);
        mEmptyView.setText(mActivity.getResources()
                .getString(R.string.no_artists_available));

        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(ARTIST_NAME)) {
                mArtistName = savedInstanceState.getString(ARTIST_NAME);
                mSearchText.setText(mArtistName);
            }
            if (savedInstanceState.containsKey(ARTISTS_DATA)) {
                mArtistsDetailsList = savedInstanceState.getParcelableArrayList(ARTISTS_DATA);
            }
            if (savedInstanceState.containsKey(LIST_VIEW_FIRST_VISIBLE_POSITION)) {
                mArtistsListViewFirstVisiblePosition =
                        savedInstanceState.getInt(LIST_VIEW_FIRST_VISIBLE_POSITION);
            }
        }

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
        private final String artistName;
        private final String artistId;

        SelectedArtistRowDetails(String artistName, String artistId) {
            this.artistName = artistName;
            this.artistId = artistId;
        }

    }

    /**
     * Called when user click on an artist row. It will start asynchronous search for the artist's
     * top 10 tracks.
     */
    private void handleArtistRowClicked(int position) {
        ArtistDetails artistDetails = mArtistArrayAdapter.getItem(position);
        TracksDataFetcherWithCallbacks tracksFetcher = new TracksDataFetcherWithCallbacks();
        tracksFetcher.execute(
                new SelectedArtistRowDetails(
                        artistDetails.name,
                        artistDetails.spotifyId));
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
    // TODO: 13/07/2015 thionk about it - it was probably called from activity onSaveInstanceState method
    public synchronized boolean isSearchInProgress() {
        return mSearchInProgress;
    }

    /**
     * Show artists details or empty view if no data found.
     */
    public void showArtistsDetails() {
        mSearchText.setText(mArtistName);
        mSearchText.requestFocus();
        if (mArtistName != null) {
            mSearchText.setSelection(mArtistName.length());
        }
        mArtistArrayAdapter.clear();
        if (mArtistsDetailsList != null) {
            mArtistArrayAdapter.addAll(mArtistsDetailsList);
            mListView.clearChoices();    /* will clear previously selected artist row */
            mArtistArrayAdapter.notifyDataSetChanged(); /* call after clearChoices above */
            mListView.setSelection(mArtistsListViewFirstVisiblePosition);
            if (mArtistArrayAdapter.isEmpty()) {
                mEmptyView.setVisibility(View.VISIBLE);
//                mEmptyView.setText();
            } else {
                mEmptyView.setVisibility(View.GONE);
            }
        }
    }

    /**
     * Save data if configuration changed - device rotation, etc.
     */
    @Override
    public void onSaveInstanceState(Bundle outState) {
        Log.v(LOG_TAG, "onSaveInstanceState");
        mArtistName = mSearchText.getText().toString();
        outState.putString(ARTIST_NAME, mArtistName);
        if (mArtistsDetailsList != null) {
            outState.putParcelableArrayList(ARTISTS_DATA, (ArrayList) mArtistsDetailsList);
        }
        mArtistsListViewFirstVisiblePosition = mListView.getFirstVisiblePosition();
        if (mListView != null) {
            outState.putInt(LIST_VIEW_FIRST_VISIBLE_POSITION, mListView.getFirstVisiblePosition());
        }
        super.onSaveInstanceState(outState);
    }

    /**
     * Starts asynchronous search for artists details.
     */
    private void sendArtistsDataRequestToSpotify(String artistName) {
        mCallbacks.artistSearchStarted();
        setSearchInProgress(true);
        mArtistArrayAdapter.clear();
        mEmptyView.setText(mActivity.getResources().getString(R.string.search_in_progress));
        mEmptyView.setVisibility(View.VISIBLE);
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
                mEmptyView.setText(mActivity.getResources()
                        .getString(R.string.search_unsuccessful_network_problems));
                return;
            } else if (artistsDetailsList == null) {
                mEmptyView.setText(mActivity.getResources()
                        .getString(R.string.search_returned_no_artist_data));
                return;
            }
            mArtistsDetailsList = artistsDetailsList;
            mArtistsListViewFirstVisiblePosition = 0;
            showArtistsDetails();
            setSearchInProgress(false);
            mCallbacks.artistSearchEnded();
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
        private String artistName;
        private List<TrackDetails> results;
        private CountDownLatch callBackResultsCountDownLatch;

        /**
         * When background processing is done and artists data found, calls onPostExecute
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
            mCallbacks.showTracksData(artistName, trackDetails);
        }


        /**
         * Calls SpotifyService.getArtistTopTrack method with SpotifyCallback.
         *
         * The country code is taken from the preferences.
         */
        @Override
        protected List<TrackDetails> doInBackground(SelectedArtistRowDetails... params) {
            String artistId = params[0].artistId;
            artistName = params[0].artistName;

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
