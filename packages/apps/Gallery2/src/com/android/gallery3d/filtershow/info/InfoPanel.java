/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.gallery3d.filtershow.info;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.android.gallery3d.R;
import com.android.gallery3d.exif.ExifInterface;
import com.android.gallery3d.exif.ExifTag;
import com.android.gallery3d.filtershow.cache.ImageLoader;
import com.android.gallery3d.filtershow.imageshow.MasterImage;

import java.io.ByteArrayOutputStream;
import java.util.List;

public class InfoPanel extends DialogFragment {
    public static final String FRAGMENT_TAG = "InfoPanel";
    private static final String LOGTAG = "Gallery2/InfoPanel";
    private LinearLayout mMainView;
    private ImageView mImageThumbnail;
    private TextView mImageName;
    private TextView mImageSize;
    private TextView mExifData;

    private String createStringFromIfFound(ExifTag exifTag, int tag, int str) {
        String exifString = "";
        short tagId = exifTag.getTagId();
        if (tagId == ExifInterface.getTrueTagKey(tag)) {
            String label = getActivity().getString(str);
            exifString += "<b>" + label + ": </b>";
            exifString += exifTag.forceGetValueAsString();
            exifString += "<br>";
        }
        return exifString;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (getDialog() != null) {
            getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        }
        Log.d(LOGTAG, " <onCreateView> .............");
        mMainView = (LinearLayout) inflater.inflate(
                R.layout.filtershow_info_panel, null, false);

        mImageThumbnail = (ImageView) mMainView.findViewById(R.id.imageThumbnail);
        Bitmap bitmap = MasterImage.getImage().getFilteredImage();
        if (bitmap != null) {
            Log.d(LOGTAG, " <onCreateView> bitmap  is not null ");
            if (mCopyBitmap != null)
                mCopyBitmap.recycle();
            mCopyBitmap = bitmap.copy(bitmap.getConfig(), true);
        } else if (mCopyBitmap != null) {
            Log.d(LOGTAG, " <onCreateView> getFilteredImage is null ");
            bitmap = mCopyBitmap;
        }
        mImageThumbnail.setImageBitmap(bitmap);

        mImageName = (TextView) mMainView.findViewById(R.id.imageName);
        mImageSize = (TextView) mMainView.findViewById(R.id.imageSize);
        mExifData = (TextView) mMainView.findViewById(R.id.exifData);
        TextView exifLabel = (TextView) mMainView.findViewById(R.id.exifLabel);

        HistogramView histogramView = (HistogramView) mMainView.findViewById(R.id.histogramView);
        histogramView.setBitmap(bitmap);

        Uri uri = MasterImage.getImage().getUri();
        if (uri != null) {
            mUri = uri.toString();
        } else if (mUri != null) {
            uri = Uri.parse(mUri);
        }
        String path = ImageLoader.getLocalPathFromUri(getActivity(), uri);
        Uri localUri = null;
        if (path != null) {
            localUri = Uri.parse(path);
        }

        if (localUri != null) {
            mImageName.setText(localUri.getLastPathSegment());
        }
        Rect originalBounds = MasterImage.getImage().getOriginalBounds();
        if (originalBounds != null) {
            mOriginalBounds = originalBounds;
        } else {
            originalBounds = mOriginalBounds;
        }
        mImageSize.setText("" + originalBounds.width() + " x " + originalBounds.height());

        List<ExifTag> exif = MasterImage.getImage().getEXIF();
        if (exif == null) {
            Log.d(LOGTAG, "<onCreateView>  exif == null");
            exif = ImageLoader.getExif(getActivity().getApplicationContext(),
                    uri);
        }
        String exifString = "";
        boolean hasExifData = false;
        if (exif != null) {
            for (ExifTag tag : exif) {
                exifString += createStringFromIfFound(tag,
                        ExifInterface.TAG_MODEL,
                        R.string.filtershow_exif_model);
                exifString += createStringFromIfFound(tag,
                        ExifInterface.TAG_APERTURE_VALUE,
                        R.string.filtershow_exif_aperture);
                exifString += createStringFromIfFound(tag,
                        ExifInterface.TAG_FOCAL_LENGTH,
                        R.string.filtershow_exif_focal_length);
                exifString += createStringFromIfFound(tag,
                        ExifInterface.TAG_ISO_SPEED_RATINGS,
                        R.string.filtershow_exif_iso);
                exifString += createStringFromIfFound(tag,
                        ExifInterface.TAG_SUBJECT_DISTANCE,
                        R.string.filtershow_exif_subject_distance);
                exifString += createStringFromIfFound(tag,
                        ExifInterface.TAG_DATE_TIME_ORIGINAL,
                        R.string.filtershow_exif_date);
                exifString += createStringFromIfFound(tag,
                        ExifInterface.TAG_F_NUMBER,
                        R.string.filtershow_exif_f_stop);
                exifString += createStringFromIfFound(tag,
                        ExifInterface.TAG_EXPOSURE_TIME,
                        R.string.filtershow_exif_exposure_time);
                exifString += createStringFromIfFound(tag,
                        ExifInterface.TAG_COPYRIGHT,
                        R.string.filtershow_exif_copyright);
                hasExifData = true;
            }
        }
        if (hasExifData) {
            exifLabel.setVisibility(View.VISIBLE);
            Log.d(LOGTAG, "<onCreateView> exifString = "+exifString);
            mExifData.setText(Html.fromHtml(exifString));
        } else {
            exifLabel.setVisibility(View.GONE);
        }
        return mMainView;
    }
    

    // /M: save thumbnail and image name @{
    public static final String KEY_INDEX_INFOPANEL_NAME = "Info_Panel_name";
    public static final String KEY_INDEX_INFOPANEL_BITMAP = "Info_Panel_bitmap";
    public static final String KEY_INDEX_ORIGINALRECT = "Info_Panel__rect";
    public static final String KEY_INDEX_URI = "Info_Panel_uri";
    private Bitmap mCopyBitmap = null;
    private String mCopyImageName = null;
    private Rect mOriginalBounds;
    private String mUri;
    @Override
    public void onSaveInstanceState(Bundle arg0) {
        super.onSaveInstanceState(arg0);
        Log.d(LOGTAG, "<onSaveInstanceState> arg0= "+arg0);
        arg0.putString(KEY_INDEX_INFOPANEL_NAME, mCopyImageName);
        if (mCopyBitmap != null) {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            mCopyBitmap.compress(Bitmap.CompressFormat.JPEG, 100, os);
            arg0.putByteArray(KEY_INDEX_INFOPANEL_BITMAP, os.toByteArray());
        }
        int data[] = { mOriginalBounds.left, mOriginalBounds.top,
                mOriginalBounds.right, mOriginalBounds.bottom };
        arg0.putIntArray(KEY_INDEX_ORIGINALRECT, data);
        arg0.putString(KEY_INDEX_URI, mUri);
    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            byte[] data = savedInstanceState
                    .getByteArray(KEY_INDEX_INFOPANEL_BITMAP);
            if (data != null) {
                mCopyBitmap = BitmapFactory.decodeByteArray(data, 0,
                        data.length);
            }
            int rect[] = savedInstanceState.getIntArray(KEY_INDEX_ORIGINALRECT);
            mOriginalBounds = new Rect(rect[0], rect[1], rect[2], rect[3]);
            mUri = savedInstanceState.getString(KEY_INDEX_URI);
            Log.d(LOGTAG, "<onCreate> savedInstanceState= "+savedInstanceState);
        }
    }
}
