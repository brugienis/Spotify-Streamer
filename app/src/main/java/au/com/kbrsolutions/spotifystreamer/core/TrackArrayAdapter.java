/*
 * Copyright (C) 2013 The Android Open Source Project
 */

package au.com.kbrsolutions.spotifystreamer.core;

import android.app.Activity;
import android.content.Context;
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
 * ArrayAdapter used by TracksFragment.
 */
public class TrackArrayAdapter<T> extends ArrayAdapter<TrackDetails> {

    private List<TrackDetails> mObjects;
    private Activity mActivity;
    private int mWidthPx = -1;

    private final String LOG_TAG = TrackArrayAdapter.class.getSimpleName();

    public TrackArrayAdapter(Activity activity, List<TrackDetails> objects) {

        super(activity.getApplicationContext(), -1, objects);
        this.mActivity = activity;
        this.mObjects = objects;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = convertView;
        ViewHolder holder;
        if (v == null) {
            LayoutInflater inflater =
                    (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = inflater.inflate(R.layout.tracks_listview_item, parent, false);

            holder = new ViewHolder();
            holder.albumImage = (ImageView) v.findViewById(R.id.trackImageId);
            holder.trackName = (TextView) v.findViewById(R.id.trackNameId);
            holder.albumName = (TextView) v.findViewById(R.id.albumNameId);

            v.setTag(holder);
        } else {
            holder = (ViewHolder) v.getTag();
        }

        TrackDetails trackDetails = mObjects.get(position);

        if (trackDetails != null) {
            holder.trackName.setText(trackDetails.trackName);
            holder.albumName.setText(trackDetails.albumName);

            if (mWidthPx == -1) {
                mWidthPx = (int) mActivity.getResources().getDimension(R.dimen.artist_thumbnail_image_size) -
                          (int) mActivity.getResources().getDimension(R.dimen.artist_thumbnail_image_padding);
            }

            if (trackDetails.albumArtSmallImageUrl != null) {
                Picasso.with(mActivity.getApplication())
                        .load(trackDetails.albumArtSmallImageUrl)
                        .resize(mWidthPx, mWidthPx).centerCrop()
                        .into(holder.albumImage);
            }
        }

        return v;
    }

    public static class ViewHolder {
        public ImageView albumImage;
        public TextView trackName;
        public TextView albumName;
    }

}
