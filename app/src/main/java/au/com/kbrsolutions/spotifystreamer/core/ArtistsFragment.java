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
 * Created by business on 20/06/2015.
 */
public class ArtistsFragment extends Fragment {

    interface ArtistsFragmentCallbacks {
//        void onPreExecute();
//        void onProgressUpdate(int percent);
//        void onCancelled();
        void onPostExecute(CharSequence artistName, List<ArtistDetails> artistsDetailsList, int listViewFirstVisiblePosition);
        void showTracks(int selectedArtistRowPosition, List<TrackDetails> trackDetails);
    }

    private ArtistsFragmentCallbacks mCallbacks;
    private List<ArtistDetails> mArtistsDetailsList;
    private EditText mSearchText;
    private ListView mListView;
    private ArtistArrayAdapter<TrackDetails> mArtistArrayAdapter;
    private Activity mActivity;
    private boolean mSearchInProgress;

    private final static String LOG_TAG = ArtistsFragment.class.getSimpleName();

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.mActivity = activity;
        try {
            mCallbacks = (ArtistsFragmentCallbacks) activity;
        } catch (Exception e) {
            throw new RuntimeException(activity.toString() + " must implement ArtistsFragmentCallbacks");
        }
//        Log.v(LOG_TAG, "onAttach - mCallbacks: " + mCallbacks);
    }

    @Override
     public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Retain this fragment across configuration changes.
        setRetainInstance(true);
//        hasOptionsMenu();
    }

    @Override
     public void onDetach() {
        super.onDetach();
        mCallbacks = null;
//        Log.v(LOG_TAG, "onDetach - mCallbacks: " + mCallbacks);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
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
                handleListItemClicked(position);
            }
        });
        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        String searchText = mSearchText.getText().toString();
        Log.v(LOG_TAG, "onResume - mSearchText: " + searchText);
        mSearchText.setSelection(searchText.length());
//        if (imm == null) {
//            imm = (InputMethodManager) mActivity.getSystemService(Context.INPUT_METHOD_SERVICE);
//        }
    }

    @Override
    public void onPause() {
        super.onPause();

        // hide the keyboard in order to avoid getTextBeforeCursor on inactive InputConnection

//        hideKeyboard();
        // from: http://stackoverflow.com/questions/8122625/getextractedtext-on-inactive-inputconnection-warning-on-android

        InputMethodManager inputMethodManager = (InputMethodManager) mActivity.getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(mSearchText.getWindowToken(), 0);
    }

    private boolean handleSearchButtonClicked(int actionId) {
//        Log.v(LOG_TAG, "handleSearchButtonClicked - start");
        boolean handled = false;
        if (actionId == EditorInfo.IME_ACTION_SEARCH) {
            String artistName = mSearchText.getText().toString();
            if (artistName.trim().length() > 0) {
                sendArtistsDataRequestToSpotify(artistName);
                handled = true;
                hideKeyboard();
            } else {
                mSearchText.setText("");
            }
        }
//        Log.v(LOG_TAG, "handleSearchButtonClicked - end");
        return handled;
    }

    private void handleListItemClicked(int position) {
//        Log.v(LOG_TAG, "handleListItemClicked - start");
        TracksDataFetcherWithCallbacks artistsFetcher = new TracksDataFetcherWithCallbacks();
        artistsFetcher.execute(new SelectedArtistRowDetails(mListView.getFirstVisiblePosition(), (mArtistArrayAdapter.getItem(position)).spotifyId));
//        Log.v(LOG_TAG, "handleListItemClicked - end");
    }

    private void hideKeyboard() {

        InputMethodManager inputMethodManager = (InputMethodManager) mActivity.getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(mSearchText.getWindowToken(), 0);
//        if (mSearchText != null && mSearchText.getWindowToken() != null && imm != null) {
//            imm.hideSoftInputFromWindow(mSearchText.getWindowToken(), 0);
//        }
    }

//    public SaveRestoreArtistDetailsHolder getArtistsDetails() {
//        return new SaveRestoreArtistDetailsHolder(mSearchText.getText(), mArtistsDetailsList, mListView.getFirstVisiblePosition());     //(ArrayList)mArtistsDetailsList;
//    }

    public synchronized void setSearchInProgress(boolean value) {
        mSearchInProgress = value;
    }

    public synchronized boolean isSearchInProgress() {
        return mSearchInProgress;
    }

    public int getListViewFirstVisiblePosition() {
        return mListView.getFirstVisiblePosition();
    }

    public void showRestoredArtistsDetails(CharSequence artistName, List<ArtistDetails> artistsDetailsList, int listViewFirstVisiblePosition) {
        mSearchText.setText(artistName);
        mSearchText.requestFocus();
        mSearchText.setSelection(artistName.length());
        this.mArtistsDetailsList = artistsDetailsList;
        showArtistsDetails(listViewFirstVisiblePosition);
    }

    public void showArtistsDetails(int listViewFirstVisiblePosition) {
        mArtistArrayAdapter.clear();
        if (mArtistsDetailsList != null) {
            mArtistArrayAdapter.addAll(mArtistsDetailsList);
            mListView.setSelection(listViewFirstVisiblePosition);
        }
    }

    public void sendArtistsDataRequestToSpotify(String artistName) {
        setSearchInProgress(true);
        mArtistArrayAdapter.clear();
        ArtistsDataFetcherWithCallbacks artistsFetcher = new ArtistsDataFetcherWithCallbacks();
        artistsFetcher.execute(artistName);
    }

    private class SelectedArtistRowDetails {
        private final int listViewFirstVisiblePosition;
        private final String artistId;

        SelectedArtistRowDetails(int listViewFirstVisiblePosition, String artistId) {
            this.listViewFirstVisiblePosition = listViewFirstVisiblePosition;
            this.artistId = artistId;
        }

    }

    public class ArtistsDataFetcherWithCallbacks extends AsyncTask<String, Void, List<ArtistDetails>> {

        private boolean networkProblems;
        private String artistName;
        private SpotifyService mSpotifyService;
        private List<ArtistDetails> results = null;
        private CountDownLatch callBackresultsCountDownLatch;

        @Override
        protected void onPostExecute(List<ArtistDetails> artistsDetailsList) {
//            Log.v(LOG_TAG, "ArtistsDataFetcher.onPostExecute - start - artistsDetailsList: " + artistsDetailsList);
            if (networkProblems) {
                Toast.makeText(mActivity, mActivity.getResources().getString(R.string.search_unsuccessful_network_problems), Toast.LENGTH_LONG).show();
                return;
            } else if (artistsDetailsList == null) {
                Toast.makeText(mActivity, mActivity.getResources().getString(R.string.search_returned_no_artist_data), Toast.LENGTH_LONG).show();
                return;
            }
//            ArtistsFragment.this.mArtistsDetailsList = artistsDetailsList;
//            Log.v(LOG_TAG, "ArtistsDataFetcher.onPostExecute - mCallbacks: " + mCallbacks);
            if (mCallbacks != null) {
                mCallbacks.onPostExecute(artistName, artistsDetailsList, 0);
            }
            setSearchInProgress(false);
        }

        @Override
        protected List<ArtistDetails> doInBackground(String... params) {

            // If there's no artist trackName, there's nothing to look up.  Verify size of params.
            if (params.length == 0) {
                return null;
            }

            // todo: remove after tests
//            try {
//                Log.v(LOG_TAG, "going to sleep");
//                Thread.sleep(5000);
//                Log.v(LOG_TAG, "woked up");
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }

            artistName = params[0];
            if (mSpotifyService == null) {
                SpotifyApi api = new SpotifyApi();
                mSpotifyService = api.getService();
            }
            callBackresultsCountDownLatch = new CountDownLatch(1);
            mSpotifyService.searchArtists(artistName, new SpotifyCallback<ArtistsPager>() {
                @Override
                public void failure(SpotifyError spotifyError) {
//                    Log.v(LOG_TAG, "ArtistsDataFetcherWithCallbacks - failure - spotifyError: " + spotifyError.getMessage());
                    networkProblems = true;
                    callBackresultsCountDownLatch.countDown();
                }

                @Override
                public void success(ArtistsPager artistsPager, Response response) {
//                    Log.v(LOG_TAG, "ArtistsDataFetcherWithCallbacks - success");
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
                            results.add(new ArtistDetails(artist.name, artist.id, thumbnailImageUrl));
//                                Log.v(LOG_TAG, "doInBackground .success- id/trackName/popularity: " + artist.name + "/" + artist.popularity + "/" + thumbnailImageUrl);
                        }
                    }
                    callBackresultsCountDownLatch.countDown();
                }
            });

            try {
                callBackresultsCountDownLatch.await();
                // TODO: 23/06/2015 think about it
            } catch (InterruptedException nothingCanBeDone) {
            }
            return results;

        }

        private final int THUMBNAIL = 100;

        private String getThumbnaiImagelUrl(List<Image> imagesData) {
            int lastImagaDataIdx = imagesData.size() - 1;
            String selectedUrl = imagesData.get(lastImagaDataIdx).url;
            int imageWidth;
            for (int i = lastImagaDataIdx; i > -1; i--) {
                imageWidth = (int) Integer.valueOf(imagesData.get(i).width);
                if (imageWidth > THUMBNAIL) {
                    selectedUrl = imagesData.get(i).url;
                    break;
                }
            }
            return selectedUrl;
        }

    }

    public class TracksDataFetcherWithCallbacks extends AsyncTask<SelectedArtistRowDetails, Void, List<TrackDetails>> {

        private SpotifyService mSpotifyService;
        private boolean networkProblems = true;
        private int listViewFirstVisiblePosition;
        private List<TrackDetails> results;
        private CountDownLatch callBackresultsCountDownLatch;
        private final int BIG_IMAGE_WIDTH = 640;
        private final int SMALL_IMAGE_WIDTH = 200;

        @Override
        protected void onPostExecute(List<TrackDetails> trackDetails) {
            if (!networkProblems) {
                Toast.makeText(mActivity, mActivity.getResources().getString(R.string.search_unsuccessful_network_problems), Toast.LENGTH_LONG).show();
                return;
            } else if (trackDetails == null) {
                Toast.makeText(mActivity, mActivity.getResources().getString(R.string.search_returned_no_track_data), Toast.LENGTH_LONG).show();
                return;
            }
            mCallbacks.showTracks(listViewFirstVisiblePosition, trackDetails);
//            Log.v(LOG_TAG, "TracksDataFetcherWithCallbacks.onPostExecute - trackDetails.size(): " + trackDetails.size());
        }

        @Override
        protected List<TrackDetails> doInBackground(SelectedArtistRowDetails... params) {
            listViewFirstVisiblePosition = params[0].listViewFirstVisiblePosition;
//            String artistId = (mArtistArrayAdapter.getItem(listViewFirstVisiblePosition)).spotifyId;
            String artistId = params[0].artistId;

            // If there's no artist Id, there's nothing to look up.  Verify size of params.
            if (artistId.length() == 0) {
                return null;
            }
            String countryCode = PreferenceManager.getDefaultSharedPreferences(mActivity /* context */).getString(getResources().getString(R.string.pref_country_key), getResources().getString(R.string.pref_country_default));
            if (countryCode.trim().length() == 0) {
                countryCode = getResources().getString(R.string.pref_country_default);
            }
//            try {
                if (mSpotifyService == null) {
                    SpotifyApi api = new SpotifyApi();
                    mSpotifyService = api.getService();
                }
                Map<String, Object> m = new HashMap<>();
//                m.put("country", "US");
                m.put("country", countryCode);

                callBackresultsCountDownLatch = new CountDownLatch(1);
//                Tracks tracks =
                mSpotifyService.getArtistTopTrack(artistId, m, new SpotifyCallback<Tracks>() {
                    @Override
                    public void failure(SpotifyError spotifyError) {
                        networkProblems = true;
                        callBackresultsCountDownLatch.countDown();
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
                                results.add(new TrackDetails(track.name, track.album.name, trackImages.big, trackImages.small, track.preview_url));
                            }
                        }
                        callBackresultsCountDownLatch.countDown();
                    }
                });

            try {
                callBackresultsCountDownLatch.await();
                // TODO: 23/06/2015 think about it
            } catch (InterruptedException nothingCanBeDone) {
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
