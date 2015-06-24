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
 * ArrayAdapter used by ArtistsFragment.
 */
public class ArtistArrayAdapter<T> extends ArrayAdapter<ArtistDetails> {

    private List<ArtistDetails> mObjects;
    private Activity mActivity;
    private int mWidthPx = -1;

    private final String LOG_TAG = ArtistArrayAdapter.class.getSimpleName();

    public ArtistArrayAdapter(Activity activity, List<ArtistDetails> objects) {

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
            v = inflater.inflate(R.layout.artists_listview_item, parent, false);

            holder = new ViewHolder();
            holder.artistImage = (ImageView) v.findViewById(R.id.artistImageId);
            holder.artistName = (TextView) v.findViewById(R.id.artistNameId);

            v.setTag(holder);
        } else {
            holder = (ViewHolder) v.getTag();
        }

        ArtistDetails artistDetails = mObjects.get(position);

        if (artistDetails != null) {
            holder.artistName.setText(artistDetails.name);

            if (mWidthPx == -1) {
                mWidthPx = (int) mActivity.getResources().getDimension(R.dimen.artist_thumbnail_image_size) -
                          (int) mActivity.getResources().getDimension(R.dimen.artist_thumbnail_image_padding);
            }

            if (artistDetails.thumbnailImageUrl != null) {
                Picasso.with(
                        mActivity.getApplication())
                        .load(artistDetails.thumbnailImageUrl)
                        .resize(mWidthPx, mWidthPx)
                        .centerCrop().into(holder
                        .artistImage);
            }
        }

        return v;
    }

    public static class ViewHolder {
        public ImageView artistImage;
        public TextView artistName;
    }

}
