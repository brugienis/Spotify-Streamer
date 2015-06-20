package au.com.kbrsolutions.spotifystreamer.core;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
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
 * Created by business on 19/06/2015.
 */
public class ArtistsListFragment extends ListFragment {

    private List<ArtistDetails> artistsDetailsList;
    private ArtistsActivity mActivity;

    private final static String LOG_TAG = ArtistsListFragment.class.getSimpleName();

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.mActivity = (ArtistsActivity) activity;
    }

//    @Override
//    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
////        return super.onCreateView(inflater, container, savedInstanceState);
//        View rootView = inflater.inflate(R.layout.artists_view, container, false);
//        return rootView;
//    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        Intent detailIntent = new Intent(getActivity(), TracksActivity.class).putExtra(Intent.EXTRA_TEXT, ((ArtistDetails) getListAdapter().getItem(position)).spotifyId);
        startActivity(detailIntent);
    }

    public ArtistsDetailsAndScreenPositionHolder getArtistsDetails() {
//        Log.v(LOG_TAG, "getArtistsDetails - visible pos fst/last: " + getListView().getFirstVisiblePosition() + "/" + getListView().getLastVisiblePosition());
        return new ArtistsDetailsAndScreenPositionHolder(artistsDetailsList, getListView().getFirstVisiblePosition());     //(ArrayList)artistsDetailsList;
    }

    public void showArtistsDetails(List<ArtistDetails> artistsDetailsList, int listViewFirstVisiblePosition) {
        this.artistsDetailsList = artistsDetailsList;
//        Log.v(LOG_TAG, "showArtistsDetails - artistsDetailsList.size: " + artistsDetailsList.size());
        ArtistArrayAdapter mArtistArrayAdapter = (ArtistArrayAdapter) getListAdapter();
        mArtistArrayAdapter.clear();
        mArtistArrayAdapter.addAll(artistsDetailsList);
        getListView().setSelection(listViewFirstVisiblePosition);
    }

    public void sendArtistsDataRequestToSpotify(String artistName) {
//        Log.v(LOG_TAG, "sendArtistsDataRequestToSpotify - start");
        ((ArtistArrayAdapter) getListAdapter()).clear();
        ArtistsDataFetcher artistsFetcher = new ArtistsDataFetcher();
        artistsFetcher.execute(artistName);
    }

    public class ArtistsDataFetcher extends AsyncTask<String, Void, List<ArtistDetails>> {

        private boolean successfullyAccessedSpotify = true;

        @Override
        protected void onPostExecute(List<ArtistDetails> artistsDetailsList) {
            if (!successfullyAccessedSpotify) {
                Toast.makeText(mActivity, mActivity.getResources().getString(R.string.search_unsuccessful_network_problems), Toast.LENGTH_LONG).show();
                return;
            } else if (artistsDetailsList == null) {
                Toast.makeText(mActivity, mActivity.getResources().getString(R.string.search_returned_no_artist_data), Toast.LENGTH_LONG).show();
                return;
            }
            showArtistsDetails(artistsDetailsList, 0);
        }

        SpotifyService mSpotifyService;

        @Override
        protected List<ArtistDetails> doInBackground(String... params) {

            // If there's no artist trackName, there's nothing to look up.  Verify size of params.
            if (params.length == 0) {
                return null;
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
//                Log.v(LOG_TAG, "doInBackground - results: " + artistsPager);
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
                            //                        albumThumbnailImageUrl = images.get(imagesCnt - 2).url;
                            thumbnailImageUrl = getThumbnaiImagelUrl(images);
                            //                        albumThumbnailImageUrl = images.get(1).url;
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
