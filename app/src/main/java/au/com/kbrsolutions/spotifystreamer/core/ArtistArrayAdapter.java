package au.com.kbrsolutions.spotifystreamer.core;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.List;

import au.com.kbrsolutions.spotifystreamer.R;

/**
 * Created by business on 9/06/2015.
 */
public class ArtistArrayAdapter<T> extends ArrayAdapter<ArtistDetails> {

    private List<ArtistDetails> objects;
    private ArtistsActivity mActivity;

    private final String LOG_TAG = ArtistArrayAdapter.class.getSimpleName();

//    public ArtistArrayAdapter(FragmentActivity activity, List<ArtistDetails> objects) {
    public ArtistArrayAdapter(ArtistsActivity activity, List<ArtistDetails> objects) {

        super(activity.getApplicationContext(), -1, objects);
        this.mActivity = activity;
        this.objects = objects;
        Log.i(LOG_TAG, "constructor - end - objects.size(): " + objects.size());
    }

    // todo: utilize convertView and Holder
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
//        Log.i(LOG_TAG, "getView - start");
        View v = convertView;
        if (v == null) {
            LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = inflater.inflate(R.layout.artists_listview_item, parent, false);
        }
        ImageView artistImage = (ImageView) v.findViewById(R.id.artistImageId);

        TextView artistName = (TextView) v.findViewById(R.id.artistNameId);

        ArtistDetails artistDetails = objects.get(position);

        if (artistDetails != null) {
//            Log.i(LOG_TAG, "getView - artistName set to: " + artistDetails.name);
            artistName.setText(artistDetails.name);
        } else {
            Log.i(LOG_TAG, "getView - artistName is null");
        }

        int widthPx = (int) mActivity.getResources().getDimension(R.dimen.artist_thumbnail_image_size) -
                (int) mActivity.getResources().getDimension(R.dimen.artist_thumbnail_image_padding);

        Picasso.with(mActivity.getApplication()).load(artistDetails.thumbnailImageUrl).resize(widthPx, widthPx).centerCrop().into(artistImage);

        return v;
    }

}
