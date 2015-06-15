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
public class TracksArrayAdapter<T> extends ArrayAdapter<TrackDetails> {

    private List<TrackDetails> objects;
    private TracksActivity mActivity;

    private final String LOG_TAG = TracksArrayAdapter.class.getSimpleName();

    //    public ArtistArrayAdapter(FragmentActivity activity, List<ArtistDetails> objects) {
    public TracksArrayAdapter(TracksActivity activity, List<TrackDetails> objects) {

        super(activity.getApplicationContext(), -1, objects);
        this.mActivity = activity;
        this.objects = objects;
        Log.i(LOG_TAG, "constructor - end - objects.size(): " + objects.size());
        for (TrackDetails trackDetails:
             objects) {
            Log.i(LOG_TAG, trackDetails.trackName);

        }
    }

    // todo: utilize convertView and Holder
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Log.i(LOG_TAG, "getView - start");
        View v = convertView;
        if (v == null) {
            LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = inflater.inflate(R.layout.tracks_listview_item, parent, false);
        }
        ImageView artistImage = (ImageView) v.findViewById(R.id.trackImageId);

        TextView artistName = (TextView) v.findViewById(R.id.trackNameId);

        TrackDetails trackDetails = objects.get(position);

        if (trackDetails != null) {
            Log.i(LOG_TAG, "getView - artistName set to: " + trackDetails.trackName);
            artistName.setText(trackDetails.trackName);
        } else {
            Log.i(LOG_TAG, "getView - artistName is null");
        }

        int widthPx = (int) mActivity.getResources().getDimension(R.dimen.artist_thumbnail_image_size) -
                (int) mActivity.getResources().getDimension(R.dimen.artist_thumbnail_image_padding);

        Picasso.with(mActivity.getApplication()).load(trackDetails.albumThumbnailImageUrl).resize(widthPx, widthPx).centerCrop().into(artistImage);

        return v;
    }

}
