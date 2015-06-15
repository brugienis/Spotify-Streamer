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
 * Created by business on 13/06/2015.
 */
public class TrackArrayAdapter<T> extends ArrayAdapter<TrackDetails> {

    private List<TrackDetails> objects;
    private TracksActivity mActivity;

    private final String LOG_TAG = TrackArrayAdapter.class.getSimpleName();

    //    public ArtistArrayAdapter(FragmentActivity activity, List<ArtistDetails> objects) {
    public TrackArrayAdapter(TracksActivity activity, List<TrackDetails> objects) {

        super(activity.getApplicationContext(), -1, objects);
        this.mActivity = activity;
        this.objects = objects;
        Log.i(LOG_TAG, "constructor - end - objects.size(): " + objects.size());
    }
    private int widthPx = -1;

    // todo: utilize convertView and Holder
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
//        Log.i(LOG_TAG, "getView - start");
        View v = convertView;
        if (v == null) {
            LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = inflater.inflate(R.layout.tracks_listview_item, parent, false);
        }
        ImageView albumImage = (ImageView) v.findViewById(R.id.trackImageId);

        TextView trackName = (TextView) v.findViewById(R.id.trackNameId);

        TextView albumName = (TextView) v.findViewById(R.id.albumNameId);

        TrackDetails trackDetails = objects.get(position);
//        Log.i(LOG_TAG, "position: " + position + "/" + trackDetails);

        if (trackDetails != null) {
//            Log.i(LOG_TAG, "getView - trackName set to: " + trackDetails.trackName);
            trackName.setText(trackDetails.trackName);
            albumName.setText(trackDetails.albumName);

            if (widthPx == -1) {
                widthPx = (int) mActivity.getResources().getDimension(R.dimen.artist_thumbnail_image_size) -
                        (int) mActivity.getResources().getDimension(R.dimen.artist_thumbnail_image_padding);
            }
            Picasso.with(mActivity.getApplication()).load(trackDetails.albumArtSmallImageUrl).resize(widthPx, widthPx).centerCrop().into(albumImage);
        }

        return v;
    }

}
