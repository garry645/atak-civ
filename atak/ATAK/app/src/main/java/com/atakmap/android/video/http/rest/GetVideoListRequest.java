
package com.atakmap.android.video.http.rest;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import com.atakmap.android.video.http.VideoSyncClient;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.foxykeep.datadroid.requestmanager.Request;

/**
 *
 */
public class GetVideoListRequest implements Parcelable {

    private static final String TAG = "GetVideoListRequest";

    private final String mBaseUrl;
    private final int mNotificationId;
    private final String _serverNCS;

    public GetVideoListRequest(String baseUrl, int notificationId,
            String serverNCS) {
        mBaseUrl = baseUrl;
        mNotificationId = notificationId;
        _serverNCS = serverNCS;
    }

    public boolean isValid() {
        return !FileSystemUtils.isEmpty(mBaseUrl) && mNotificationId >= 0;
    }

    public String getBaseUrl() {
        return mBaseUrl;
    }

    public String getServerConnectString() {
        return _serverNCS;
    }

    public int getNotificationId() {
        return mNotificationId;
    }

    @NonNull
    @Override
    public String toString() {
        if (!isValid())
            return "";

        return mBaseUrl;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        if (isValid()) {
            dest.writeString(mBaseUrl);
            dest.writeInt(mNotificationId);
            dest.writeString(_serverNCS);
        }
    }

    public static final Creator<GetVideoListRequest> CREATOR = new Creator<GetVideoListRequest>() {

        @Override
        public GetVideoListRequest createFromParcel(Parcel in) {
            return new GetVideoListRequest(in);
        }

        @Override
        public GetVideoListRequest[] newArray(int size) {
            return new GetVideoListRequest[size];
        }
    };

    protected GetVideoListRequest(Parcel in) {
        mBaseUrl = in.readString();
        mNotificationId = in.readInt();
        _serverNCS = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * Create the request to get list of videos. Used by an asynch HTTP request Android
     * Service
     *
     * @return The request.
     */
    public Request createGetVideoListRequest() {
        Request request = new Request(VideoSyncClient.REQUEST_TYPE_GET_VIDEOS);
        request.put(GetVideoListOperation.PARAM_QUERY, this);
        return request;
    }
}
