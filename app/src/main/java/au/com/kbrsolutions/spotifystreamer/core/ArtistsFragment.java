package au.com.kbrsolutions.spotifystreamer.core;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import au.com.kbrsolutions.spotifystreamer.R;
import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Artist;
import kaaes.spotify.webapi.android.models.ArtistsPager;
import kaaes.spotify.webapi.android.models.Pager;

//import android.app.Fragment;

public class ArtistsFragment extends Fragment {

    private ArrayAdapter<String> mArtistsAdapter;
    private String[] data = {"Beyonce", "Dylan"};
    private ListView listView;
    private TextView sarchText;

    private final String LOG_TAG = ArtistsFragment.class.getSimpleName();

    public ArtistsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        List<String> foundArtists = new ArrayList<String>(Arrays.asList(data));
        mArtistsAdapter =
                new ArrayAdapter<String>(
                        getActivity(), // The current context (this activity)
                        R.layout.list_item_artist, // The name of the layout ID.
                        R.id.list_item_artist, // The ID of the textview to populate.
                        foundArtists);
//                        new ArrayList<String>());

        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_artists, container, false);

        sarchText = (TextView) rootView.findViewById(R.id.searchTextView);
        sarchText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean handled = false;
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    String artistName = sarchText.getText().toString();
                    if (artistName.trim().length() > 0) {
                        sendArtistsDataRequestToSpotify(artistName);
                        handled = true;
                    }
                }
                return handled;
//                if (actionId == R.id.action_start_search) {
//                    Log.v(LOG_TAG, "onCreateView - Done pressed");
//                    return true;
//                }
//                return false;
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

    @Override
    public void onStart() {
        super.onStart();
        sendArtistsDataRequestToSpotify("Beyonce");
    }
//    }

    private void sendArtistsDataRequestToSpotify(String artistName) {
        ArtistsDataFetcher artistsFetcher = new ArtistsDataFetcher();
        artistsFetcher.execute(artistName);
    }

    public class ArtistsDataFetcher extends AsyncTask<String, Void, List<String>> {

        @Override
        protected void onPostExecute(List<String> results) {
            if (results == null) {
                return;
            }
            mArtistsAdapter.clear();
            mArtistsAdapter.addAll(results);
        }

        @Override
        protected List<String> doInBackground(String... params) {

            // If there's no zip code, there's nothing to look up.  Verify size of params.
            if (params.length == 0) {
                return null;
            }
            List<String> results = new ArrayList<String>();
            String artistName = params[0];
            SpotifyApi api = new SpotifyApi();
            SpotifyService spotify = api.getService();
            ArtistsPager artistsPager = spotify.searchArtists(artistName);
            Log.v(LOG_TAG, "handleArtistsData - results: " + artistsPager);
            Pager<Artist> artists = artistsPager.artists;
            List<Artist> artistsList = artists.items;
            int idx = 0;
            if (artistsList.size() > 0) {
                results = new ArrayList<String>(artistsList.size());
                for (Artist artist : artistsList) {
                    results.add(artist.name + " - " + artist.popularity);
//                    Log.v(LOG_TAG, "doInBackground - id/name/popularity: " + artist.id + "/" + artist.name + "/" + artist.popularity);
                }
            }
            return results;

        }
    }

}
