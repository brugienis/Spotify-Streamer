package au.com.kbrsolutions.spotifystreamer.core;

import android.app.Activity;
import android.app.ListFragment;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Artist;
import kaaes.spotify.webapi.android.models.ArtistsPager;
import kaaes.spotify.webapi.android.models.Image;
import kaaes.spotify.webapi.android.models.Pager;

/**
 * Created by business on 10/06/2015.
 */
public class ArtistListFragment extends ListFragment {

    private ArtistsActivity mActivity;
    private ArtistSelectable selectedArtistHandler;

    private final static String LOG_TAG = ArtistListFragment.class.getSimpleName();

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.mActivity = (ArtistsActivity) activity;
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
//        ((ArtistDetails) getListAdapter().getItem(position)).spotifyId;
        Intent detailIntent = new Intent(getActivity(), TracksActivity.class).putExtra(Intent.EXTRA_TEXT, ((ArtistDetails) getListAdapter().getItem(position)).spotifyId);
        startActivity(detailIntent);
//        mActivity.processClick(position);
//        selectedArtistHandler.handleSelectedArtist();
    }

    public void sendArtistsDataRequestToSpotify(String artistName) {
        ArtistsDataFetcher artistsFetcher = new ArtistsDataFetcher();
        artistsFetcher.execute(artistName);
    }

    public class ArtistsDataFetcher extends AsyncTask<String, Void, List<ArtistDetails>> {

        @Override
        protected void onPostExecute(List<ArtistDetails> artistsDetails) {
            if (artistsDetails == null) {
                Log.v(LOG_TAG, "showing toast");
                Toast.makeText(getActivity(), "No data found", Toast.LENGTH_LONG).show();
                return;
            }
            List<String> artistsNames = new ArrayList<>(artistsDetails.size());
            for (ArtistDetails artistDetails: artistsDetails) {
                artistsNames.add(artistDetails.name + " - " + artistDetails.spotifyId);
            }
//            mArtistArrayAdapter.clear();
//            mArtistArrayAdapter.addAll(artistsDetails);
            ListAdapter mArtistArrayAdapter = getListAdapter();
            mArtistArrayAdapter = new ArtistArrayAdapter(mActivity, artistsDetails);
            setListAdapter(mArtistArrayAdapter);
//            mArtistArrayAdapter.notifyDataSetChanged();
            //java.lang.RuntimeException: Your content must have a ListView whose id attribute is 'android.R.id.list'
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
            if (mSpotifyService == null) {
                SpotifyApi api = new SpotifyApi();
                mSpotifyService = api.getService();
            }
            ArtistsPager artistsPager = mSpotifyService.searchArtists(artistName);
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
//                        albumThumbnailImageUrl = images.get(imagesCnt - 2).url;
                        thumbnailImageUrl = getThumbnaiImagelUrl(images);
//                        albumThumbnailImageUrl = images.get(1).url;
                    }
                    results.add(new ArtistDetails(artist.name, artist.id, thumbnailImageUrl));
                    Log.v(LOG_TAG, "doInBackground - id/trackName/popularity: " + artist.name + "/" + artist.popularity + "/" + thumbnailImageUrl);
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

    interface ArtistSelectable {
        void handleSelectedArtist(String artistId);
    }

    public void setSelectedArtistHandler(ArtistSelectable selectedArtistHandler) {
        this.selectedArtistHandler = selectedArtistHandler;
    }

}
