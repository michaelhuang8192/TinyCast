package com.tinycast.tvcontroller.provider;

import android.content.Context;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.media.MediaRouter;
import android.support.v7.media.MediaControlIntent;
import android.support.v7.media.MediaRouteSelector;
import android.util.Log;

import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.framework.CastContext;
import com.google.android.gms.cast.framework.CastSession;
import com.google.android.gms.cast.framework.media.RemoteMediaClient;
import com.google.android.gms.common.api.ResultCallback;
import com.tinycast.helper.FunctionCall;
import com.tinycast.helper.FunctionCallResult;
import com.tinycast.tvcontroller.SmartDevice;
import com.tinycast.tvcontroller.SmartDeviceSearch;

import static android.support.v7.media.MediaRouter.UNSELECT_REASON_DISCONNECTED;


public class ChromeCastSearch implements SmartDeviceSearch {
    private static final String TAG = ChromeCastSearch.class.getSimpleName();

    private final Context context;
    private MediaRouter mediaRouter;
    private MediaRouter.Callback mediaRouterCallback;
    private final Handler handlder;
    private CastContext castContext;

    public ChromeCastSearch(Context context) {
        this.context = context;
        handlder = new Handler(context.getMainLooper());
    }

    @Override
    public void start(int timeout, final FunctionCall<Void, SmartDevice> cb) {
        handlder.post(new Runnable() {
            @Override
            public void run() {
                if(mediaRouter != null) return;

                castContext = CastContext.getSharedInstance(context);

                mediaRouter = MediaRouter.getInstance(context);
                MediaRouteSelector sel = new MediaRouteSelector.Builder()
                        .addControlCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK)
                        .build();

                mediaRouterCallback = new MediaRouter.Callback() {
                    @Override
                    public void onRouteAdded(MediaRouter router, MediaRouter.RouteInfo route) {
                        super.onRouteAdded(router, route);
                        cb.call(new ChromeCastDevice(router, route));
                    }

                    @Override
                    public void onRouteChanged(MediaRouter router, MediaRouter.RouteInfo route) {
                        super.onRouteChanged(router, route);
                        cb.call(new ChromeCastDevice(router, route));
                    }
                };

                //for(MediaRouter.RouteInfo route : mediaRouter.getRoutes()) {
                //    cb.call(new ChromeCastDevice(mediaRouter, route));
                //}

                mediaRouter.addCallback(
                        sel,
                        mediaRouterCallback,
                        MediaRouter.CALLBACK_FLAG_REQUEST_DISCOVERY | MediaRouter.CALLBACK_FLAG_UNFILTERED_EVENTS
                );
            }
        });
    }

    @Override
    public void stop() {
        handlder.post(new Runnable() {
            @Override
            public void run() {
                if(mediaRouter == null) return;

                mediaRouter.removeCallback(mediaRouterCallback);
                mediaRouterCallback = null;
                mediaRouter = null;
            }
        });
    }

    @Override
    public void release() {
        stop();
    }

    class ChromeCastDevice implements SmartDevice {
        private MediaRouter mRouter;
        private MediaRouter.RouteInfo mRoute;

        ChromeCastDevice(MediaRouter router, MediaRouter.RouteInfo route) {
            mRouter = router;
            mRoute = route;
        }

        @Override
        public String getId() {
            return mRoute.getId();
        }

        @Override
        public String getName() {
            return mRoute.getName();
        }

        private RemoteMediaClient getRemoteMediaClient() {
            CastSession session = castContext.getSessionManager().getCurrentCastSession();
            return session == null ? null : session.getRemoteMediaClient();
        }

        private MediaInfo buildMp4Media(String url) {
            if(url == null || url.isEmpty()) return null;

            return new MediaInfo.Builder(url)
                    .setContentType("video/mp4")
                    .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
                    .build();
        }

        @Override
        public void playMedia(final String url, final FunctionCall<Void, FunctionCallResult<Boolean>> cb) {
            final MediaInfo mediaInfo = buildMp4Media(url);

            handlder.post(new Runnable() {
                @Override
                public void run() {
                    try {
                        RemoteMediaClient mRemoteMediaClient = getRemoteMediaClient();

                        if (mRemoteMediaClient != null) {
                            mRemoteMediaClient.load(mediaInfo).setResultCallback(new ResultCallback<RemoteMediaClient.MediaChannelResult>() {
                                @Override
                                public void onResult(@NonNull RemoteMediaClient.MediaChannelResult mediaChannelResult) {
                                    cb.callOnce(FunctionCallResult.asResult(true));
                                }
                            });
                        } else {
                            cb.callOnce(FunctionCallResult.asResult(false));
                        }
                    } catch(Exception e) {
                        cb.callOnce(FunctionCallResult.<Boolean>asError(e));
                    }
                }
            });
        }

        @Override
        public void connect() {
            handlder.post(new Runnable() {
                @Override
                public void run() {
                    Log.i(TAG, "Connect" + mRoute.isSelected() + mRoute.toString());
                    mRouter.selectRoute(mRoute);
                }
            });
        }

        @Override
        public void isConnected(final FunctionCall<Void, FunctionCallResult<Boolean>> cb) {
            handlder.post(new Runnable() {
                @Override
                public void run() {
                    try {
                        CastSession session = castContext.getSessionManager().getCurrentCastSession();
                        cb.callOnce(FunctionCallResult.asResult(session != null ? session.isConnected() : null));
                    } catch(Exception e) {
                        cb.callOnce(FunctionCallResult.<Boolean>asError(e));
                    }
                }
            });
        }

        @Override
        public void isReady(final FunctionCall<Void, FunctionCallResult<Boolean>> cb) {
            isConnected(cb);
        }

        @Override
        public void disconnect() {
            handlder.post(new Runnable() {
                @Override
                public void run() {
                    mRouter.unselect(UNSELECT_REASON_DISCONNECTED);
                }
            });
        }

        @Override
        public boolean isSameDevice(SmartDevice smartDevice) {
            return false;
        }

    }

}
