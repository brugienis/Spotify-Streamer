package au.com.kbrsolutions.spotifystreamer.core;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import au.com.kbrsolutions.spotifystreamer.R;
import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Artist;
import kaaes.spotify.webapi.android.models.ArtistsPager;
import kaaes.spotify.webapi.android.models.Image;
import kaaes.spotify.webapi.android.models.Pager;

/**
 * Created by business on 20/06/2015.
 */
public class ArtistsFragment extends Fragment {

    interface ArtistsFragmentCallbacks {
//        void onPreExecute();
//        void onProgressUpdate(int percent);
//        void onCancelled();
        void onPostExecute();
    }

    private ArtistsFragmentCallbacks mCallbacks;
    private List<ArtistDetails> artistsDetailsList;
    private TextView sarchText;
    private ListView mListView;
    private ArtistArrayAdapter<TrackDetails> artistArrayAdapter;
//    private InputMethodManager imm;
    private Activity mActivity;
    private boolean searchInProgress;

    private final static String LOG_TAG = ArtistsFragment.class.getSimpleName();

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.mActivity = activity;
        try {
            mCallbacks = (ArtistsFragmentCallbacks) activity;
        } catch (Exception e) {
            throw new RuntimeException("interface not implemented");
        }
        Log.v(LOG_TAG, "onAttach - mCallbacks: " + mCallbacks);
    }

    @Override
     public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Retain this fragment across configuration changes.
        setRetainInstance(true);
    }

    @Override
     public void onDetach() {
        super.onDetach();
        mCallbacks = null;
        Log.v(LOG_TAG, "onDetach - mCallbacks: " + mCallbacks);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragments_artists_view, container, false);
        List<ArtistDetails> artistsItemsList = new ArrayList<>();

        sarchText = (TextView) rootView.findViewById(R.id.searchTextView);
        sarchText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                return handleSearchButtonClicked(actionId);
            }
        });
        artistArrayAdapter = new ArtistArrayAdapter<>(getActivity(), artistsItemsList);
        mListView = (ListView) rootView.findViewById(R.id.listview_artists);
        mListView.setAdapter(artistArrayAdapter);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                Intent detailIntent = new Intent(getActivity(), TracksActivity.class).putExtra(Intent.EXTRA_TEXT, ((ArtistDetails) artistArrayAdapter.getItem(position)).spotifyId);
                startActivity(detailIntent);
            }
        });
//        hideKeyboard();
        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
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
        inputMethodManager.hideSoftInputFromWindow(sarchText.getWindowToken(), 0);
    }

    private boolean handleSearchButtonClicked(int actionId) {
        Log.v(LOG_TAG, "handleSearchButtonClicked - start");
        boolean handled = false;
        if (actionId == EditorInfo.IME_ACTION_SEARCH) {
            String artistName = sarchText.getText().toString();
            if (artistName.trim().length() > 0) {
                sendArtistsDataRequestToSpotify(artistName);
                handled = true;
                hideKeyboard();
            } else {
                sarchText.setText("");
            }
        }
        Log.v(LOG_TAG, "handleSearchButtonClicked - end");
        return handled;
    }

    private void hideKeyboard() {

        InputMethodManager inputMethodManager = (InputMethodManager) mActivity.getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(sarchText.getWindowToken(), 0);
//        if (sarchText != null && sarchText.getWindowToken() != null && imm != null) {
//            imm.hideSoftInputFromWindow(sarchText.getWindowToken(), 0);
//        }
    }

    public SaveRestoreArtistDetailsHolder getArtistsDetails() {
        return new SaveRestoreArtistDetailsHolder(sarchText.getText(), artistsDetailsList, mListView.getFirstVisiblePosition());     //(ArrayList)artistsDetailsList;
    }

    public synchronized void setSearchInProgress(boolean value) {
        searchInProgress = value;
    }

    public synchronized boolean isSearchInProgress() {
        return searchInProgress;
    }

    public void showRestoredArtistsDetails(CharSequence artistName, List<ArtistDetails> artistsDetailsList, int listViewFirstVisiblePosition) {
        sarchText.setText(artistName);
        this.artistsDetailsList = artistsDetailsList;
        showArtistsDetails(listViewFirstVisiblePosition);
    }

    public void showArtistsDetails(int listViewFirstVisiblePosition) {
        artistArrayAdapter.clear();
        if (artistsDetailsList != null) {
            artistArrayAdapter.addAll(artistsDetailsList);
            mListView.setSelection(listViewFirstVisiblePosition);
        }
    }

    public void sendArtistsDataRequestToSpotify(String artistName) {
        setSearchInProgress(true);
        artistArrayAdapter.clear();
        ArtistsDataFetcher artistsFetcher = new ArtistsDataFetcher();
        artistsFetcher.execute(artistName);
    }

    public class ArtistsDataFetcher extends AsyncTask<String, Void, List<ArtistDetails>> {

        private boolean successfullyAccessedSpotify = true;

        @Override
        protected void onPostExecute(List<ArtistDetails> artistsDetailsList) {
            Log.v(LOG_TAG, "ArtistsDataFetcher.onPostExecute - start");
            if (!successfullyAccessedSpotify) {
                Toast.makeText(mActivity, mActivity.getResources().getString(R.string.search_unsuccessful_network_problems), Toast.LENGTH_LONG).show();
                return;
            } else if (artistsDetailsList == null) {
                Toast.makeText(mActivity, mActivity.getResources().getString(R.string.search_returned_no_artist_data), Toast.LENGTH_LONG).show();
                return;
            }
//            showArtistsDetails(artistsDetailsList, 0);
            ArtistsFragment.this.artistsDetailsList = artistsDetailsList;
            Log.v(LOG_TAG, "ArtistsDataFetcher.onPostExecute - mCallbacks: " + mCallbacks);
            if (mCallbacks != null) {
                mCallbacks.onPostExecute();
            }
            setSearchInProgress(false);
        }

        SpotifyService mSpotifyService;

        @Override
        protected List<ArtistDetails> doInBackground(String... params) {

            // If there's no artist trackName, there's nothing to look up.  Verify size of params.
            if (params.length == 0) {
                return null;
            }

            try {
                Log.v(LOG_TAG, "going to sleep");
                Thread.sleep(5000);
                Log.v(LOG_TAG, "woked up");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            List<ArtistDetails> results = null;
            String artistName = params[0];
            try {
                if (mSpotifyService == null) {
                    SpotifyApi api = new SpotifyApi();
                    mSpotifyService = api.getService();
                }
                // if no access to network: java.net.UnknownHostException: Unable to resolve host "api.spotify.com": No address associated with hostname
                ArtistsPager artistsPager = mSpotifyService.searchArtists(artistName);
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
                        //                    Log.v(LOG_TAG, "doInBackground - id/trackName/popularity: " + artist.name + "/" + artist.popularity + "/" + thumbnailImageUrl);
                    }
                }
            } catch (Exception e) {
                Log.v(LOG_TAG, "doInBackground - exception: " + e);
                successfullyAccessedSpotify = false;
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
}
