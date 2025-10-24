
package com.atakmap.android.video.http.rest;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import com.atakmap.android.video.http.VideoSyncClient;
import com.atakmap.comms.NetConnectString;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.foxykeep.datadroid.requestmanager.Request;

/**
 *
 */
public class PostVideoListRequest implements Parcelable {

    private static final String TAG = "PostVideoListRequest";

    private final NetConnectString mServerConnectString;
    private final String mBaseUrl;
    private final String mVideoXml;
    private final int mNotificationId;


    public PostVideoListRequest(NetConnectString serverConnectString,
            String baseUrl, String videoXml,
            int notificationId) {
        mServerConnectString = serverConnectString;
        mBaseUrl = baseUrl;
        mVideoXml = videoXml;
        mNotificationId = notificationId;
    }

    public boolean isValid() {
        return !FileSystemUtils.isEmpty(mBaseUrl) && mNotificationId >= 0;
    }

    public NetConnectString getServerConnectString() {
        return mServerConnectString;
    }

    public String getBaseUrl() {
        return mBaseUrl;
    }

    public String getVideoXml() {
        return mVideoXml;
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
            dest.writeString(mServerConnectString == null ? ""
                    : mServerConnectString.toString());
            dest.writeString(mBaseUrl);
            dest.writeString(mVideoXml);
            dest.writeInt(mNotificationId);
        }
    }

    public static final Creator<PostVideoListRequest> CREATOR = new Creator<PostVideoListRequest>() {

        @Override
        public PostVideoListRequest createFromParcel(Parcel in) {
            return new PostVideoListRequest(in);
        }

        @Override
        public PostVideoListRequest[] newArray(int size) {
            return new PostVideoListRequest[size];
        }
    };

    protected PostVideoListRequest(Parcel in) {
        mServerConnectString = NetConnectString.fromString(in.readString());
        mBaseUrl = in.readString();
        mVideoXml = in.readString();
        mNotificationId = in.readInt();
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
    public Request createPostVideoListRequest() {
        Request request = new Request(VideoSyncClient.REQUEST_TYPE_POST_VIDEOS);
        request.put(PostVideoListOperation.PARAM_REQUEST, this);
        return request;
    }
}
