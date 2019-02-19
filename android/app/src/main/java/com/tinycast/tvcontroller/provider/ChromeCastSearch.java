package com.tinycast.tvcontroller.provider;

import android.content.Context;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.media.MediaRouter;
import android.support.v7.media.MediaControlIntent;
import android.support.v7.media.MediaRouteSelector;
import android.util.Log;

import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaStatus;
import com.google.android.gms.cast.framework.CastContext;
import com.google.android.gms.cast.framework.CastSession;
import com.google.android.gms.cast.framework.media.RemoteMediaClient;
import com.google.android.gms.common.api.ResultCallback;
import com.tinycast.helper.FunctionCall;
import com.tinycast.helper.FunctionCallResult;
import com.tinycast.tvcontroller.SmartDevice;
import com.tinycast.tvcontroller.SmartDeviceSearch;

import java.util.HashMap;
import java.util.Map;

import static android.support.v7.media.MediaRouter.RouteInfo.DEVICE_TYPE_TV;
import static android.support.v7.media.MediaRouter.UNSELECT_REASON_DISCONNECTED;


public class ChromeCastSearch implements SmartDeviceSearch {
    private static final String TAG = ChromeCastSearch.class.getSimpleName();

    private final Context context;
    private MediaRouter mediaRouter;
    private MediaRouter.Callback mediaRouterCallback;
    private final Handler handler;
    private CastContext castContext;

    public ChromeCastSearch(Context context) {
        this.context = context;
        handler = new Handler(context.getMainLooper());
    }

    @Override
    public void start(int timeout, final FunctionCall<Void, SmartDevice> cb) {
        handler.post(new Runnable() {
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
                        if(route.getDeviceType() != DEVICE_TYPE_TV) return;

                        Log.i("PKT", ">>>>" + route.getId());
                        super.onRouteAdded(router, route);
                        cb.call(new ChromeCastDevice(router, route));
                    }

                    @Override
                    public void onRouteChanged(MediaRouter router, MediaRouter.RouteInfo route) {
                        if(route.getDeviceType() != DEVICE_TYPE_TV) return;

                        Log.i("PKT", ">>>>++" + route.getId()+route.getExtras().);
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
        handler.post(new Runnable() {
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
            return String.format("%s [G]", mRoute.getName());
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
            handler.post(new Runnable() {
                @Override
                public void run() {
                    try {
                        RemoteMediaClient mRemoteMediaClient = getRemoteMediaClient();
                        MediaInfo mediaInfo = buildMp4Media(url);

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
            handler.post(new Runnable() {
                @Override
                public void run() {
                    mRouter.selectRoute(mRoute);
                }
            });
        }

        @Override
        public void isConnected(final FunctionCall<Void, FunctionCallResult<Boolean>> cb) {
            handler.post(new Runnable() {
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
            handler.post(new Runnable() {
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

        @Override
        public void play(final FunctionCall<Void, FunctionCallResult<Boolean>> cb) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    try {
                        RemoteMediaClient mRemoteMediaClient = getRemoteMediaClient();

                        mRemoteMediaClient.play().setResultCallback(new ResultCallback<RemoteMediaClient.MediaChannelResult>() {
                            @Override
                            public void onResult(@NonNull RemoteMediaClient.MediaChannelResult mediaChannelResult) {
                                cb.callOnce(FunctionCallResult.asResult(true));
                            }
                        });

                    } catch(Exception e) {
                        cb.callOnce(FunctionCallResult.<Boolean>asError(e));
                    }
                }
            });
        }

        @Override
        public void pause(final FunctionCall<Void, FunctionCallResult<Boolean>> cb) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    try {
                        RemoteMediaClient mRemoteMediaClient = getRemoteMediaClient();

                        mRemoteMediaClient.pause().setResultCallback(new ResultCallback<RemoteMediaClient.MediaChannelResult>() {
                            @Override
                            public void onResult(@NonNull RemoteMediaClient.MediaChannelResult mediaChannelResult) {
                                cb.callOnce(FunctionCallResult.asResult(true));
                            }
                        });

                    } catch(Exception e) {
                        cb.callOnce(FunctionCallResult.<Boolean>asError(e));
                    }
                }
            });
        }

        @Override
        public void seek(final Integer position, final FunctionCall<Void, FunctionCallResult<Boolean>> cb) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    try {
                        RemoteMediaClient mRemoteMediaClient = getRemoteMediaClient();

                        mRemoteMediaClient.seek(position * 1000).setResultCallback(new ResultCallback<RemoteMediaClient.MediaChannelResult>() {
                            @Override
                            public void onResult(@NonNull RemoteMediaClient.MediaChannelResult mediaChannelResult) {
                                cb.callOnce(FunctionCallResult.asResult(true));
                            }
                        });

                    } catch(Exception e) {
                        cb.callOnce(FunctionCallResult.<Boolean>asError(e));
                    }
                }
            });
        }

        @Override
        public void getStatus(final FunctionCall<Void, FunctionCallResult<Map<String, Object>>> cb) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    try {
                        final RemoteMediaClient mRemoteMediaClient = getRemoteMediaClient();

                        mRemoteMediaClient.requestStatus().setResultCallback(new ResultCallback<RemoteMediaClient.MediaChannelResult>() {
                            @Override
                            public void onResult(@NonNull RemoteMediaClient.MediaChannelResult mediaChannelResult) {
                                Map<String, Object> statusMap = new HashMap<>();

                                MediaStatus status = mRemoteMediaClient.getMediaStatus();
                                if(status != null) {
                                    if(status.getPlayerState() == MediaStatus.PLAYER_STATE_PLAYING)
                                        statusMap.put("state", "playing");
                                    else if(status.getPlayerState() == MediaStatus.PLAYER_STATE_PAUSED)
                                        statusMap.put("state", "paused");
                                    else if(status.getPlayerState() == MediaStatus.PLAYER_STATE_IDLE)
                                        statusMap.put("state", "stop");

                                    statusMap.put("position", status.getStreamPosition() / 1000);

                                    MediaInfo mi = status.getMediaInfo();
                                    if(mi != null)
                                        statusMap.put("duration", mi.getStreamDuration() / 1000);
                                }

                                cb.callOnce(FunctionCallResult.asResult(statusMap));
                            }
                        });

                    } catch(Exception e) {
                        cb.callOnce(FunctionCallResult.<Map<String, Object>>asError(e));
                    }
                }
            });
        }


    }

}
