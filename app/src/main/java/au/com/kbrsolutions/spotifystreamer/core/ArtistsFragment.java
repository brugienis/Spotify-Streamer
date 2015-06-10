package au.com.kbrsolutions.spotifystreamer.core;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
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

//import android.app.Fragment;

public class ArtistsFragment extends ListFragment {

    private ArrayAdapter<String> mArtistsAdapter;
    private ArtistArrayAdapter mArtistArrayAdapter;
//    private String[] data = {"Beyonce", "Dylan"};
    private ListView listView;
    private TextView sarchText;
    private InputMethodManager imm;

    private final String LOG_TAG = ArtistsFragment.class.getSimpleName();

    public ArtistsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
//        List<String> foundArtists = new ArrayList<String>(Arrays.asList(data));
//        mArtistsAdapter =
//                new ArrayAdapter<String>(
//                        getActivity(), // The current context (this activity)
//                        R.layout.list_item_artist, // The name of the layout ID.
//                        R.id.list_item_artist, // The ID of the textview to populate.
//                        new ArrayList<String>());

        List<ArtistDetails> artistsDetails = new ArrayList<>();
        // todo: not sure what to do next
        mArtistArrayAdapter = new ArtistArrayAdapter(getActivity(), artistsDetails);


        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_artists, container, false);

        sarchText = (TextView) rootView.findViewById(R.id.searchTextView);
        sarchText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                return handleSearchButtonClicked(actionId);
            }
        });

        listView = (ListView) rootView.findViewById(R.id.listview_artists);
        listView.setAdapter(mArtistsAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//                Toast.makeText(getActivity(), mForecastAdapter.getItem(position), Toast.LENGTH_SHORT).show();
//                Intent detailIntent = new Intent(getActivity(), DetailActivity.class).putExtra(Intent.EXTRA_TEXT, mForecastAdapter.getItem(position));
//                detailIntent.setData(Uri.parse(mForecastAdapter.getItem(position)));
//                startActivity(detailIntent);
            }
        });

        return rootView;
    }
    
    private boolean handleSearchButtonClicked(int actionId) {
        boolean handled = false;
        if (actionId == EditorInfo.IME_ACTION_SEARCH) {
            String artistName = sarchText.getText().toString();
            if (artistName.trim().length() > 0) {
                sendArtistsDataRequestToSpotify(artistName);
                handled = true;
                hideKeyboard();
            }
        }
        return handled;
    } 

    private void hideKeyboard() {
        // A_MUST: during monkey test got NullPointer Exception
        View view = getView();
        if (view != null && view.getWindowToken() != null && imm != null) {
            imm.hideSoftInputFromWindow(getView().getWindowToken(), 0);
        }
    }

//    @Override
//    public void onStart() {
//        super.onStart();
//        sendArtistsDataRequestToSpotify("Beyonce");
//    }
//    }

    private void sendArtistsDataRequestToSpotify(String artistName) {
        ArtistsDataFetcher artistsFetcher = new ArtistsDataFetcher();
        artistsFetcher.execute(artistName);
    }

    public class ArtistsDataFetcher extends AsyncTask<String, Void, List<ArtistDetails>> {

        @Override
        protected void onPostExecute(List<ArtistDetails> artistsDetails) {
            if (artistsDetails == null) {
                // fixme: show empty view with a message
                Log.v(LOG_TAG, "showing toast");
                Toast.makeText(getActivity(), "No data found", Toast.LENGTH_LONG);
                return;
            }
            List<String> artistsNames = new ArrayList<>(artistsDetails.size());
            for (ArtistDetails artistDetails: artistsDetails) {
                artistsNames.add(artistDetails.name + " - " + artistDetails.spotifyId);
            }
//            mArtistArrayAdapter.clear();
//            mArtistArrayAdapter.addAll(artistsDetails);
            mArtistArrayAdapter = new ArtistArrayAdapter(getActivity(), artistsDetails);
            setListAdapter(mArtistArrayAdapter);
            mArtistArrayAdapter.notifyDataSetChanged();
            //java.lang.RuntimeException: Your content must have a ListView whose id attribute is 'android.R.id.list'
        }

        private SpotifyService spotify;
//        private final String imageSize64 = "64";
//        private final String imageSize300 = "300";
//        private final String imageSize640 = "640";

        @Override
        protected List<ArtistDetails> doInBackground(String... params) {

            // If there's no zip code, there's nothing to look up.  Verify size of params.
            if (params.length == 0) {
                return null;
            }
            List<ArtistDetails> results = null;
            String artistName = params[0];
            if (spotify == null) {
                SpotifyApi api = new SpotifyApi();
                spotify = api.getService();
            }
            ArtistsPager artistsPager = spotify.searchArtists(artistName);
            Log.v(LOG_TAG, "handleArtistsData - results: " + artistsPager);
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
                        thumbnailImageUrl = images.get(imagesCnt - 1).url;
                    }
                    results.add(new ArtistDetails(artist.name, artist.id, thumbnailImageUrl));
                    Log.v(LOG_TAG, "doInBackground - id/name/popularity: " + artist.name + "/" + artist.popularity);
                }
            }
            return results;

        }
    }

}
