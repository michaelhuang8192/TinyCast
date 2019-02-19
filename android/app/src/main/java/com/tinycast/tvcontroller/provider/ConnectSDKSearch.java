package com.tinycast.tvcontroller.provider;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import com.connectsdk.core.MediaInfo;
import com.connectsdk.device.ConnectableDevice;
import com.connectsdk.discovery.DiscoveryManager;
import com.connectsdk.discovery.DiscoveryManagerListener;
import com.connectsdk.service.capability.MediaControl;
import com.connectsdk.service.capability.MediaPlayer;
import com.connectsdk.service.capability.listeners.ResponseListener;
import com.connectsdk.service.command.ServiceCommandError;
import com.tinycast.helper.FunctionCall;
import com.tinycast.helper.FunctionCallResult;
import com.tinycast.tvcontroller.SmartDevice;
import com.tinycast.tvcontroller.SmartDeviceSearch;

import java.util.HashMap;
import java.util.Map;


public class ConnectSDKSearch implements SmartDeviceSearch {
    private static final String TAG = ConnectSDKSearch.class.getSimpleName();

    private final Context context;
    private final Handler handler;
    private static DiscoveryManager discoveryManager = null;
    private DiscoveryManagerListener mDiscoveryManagerListener = null;

    public ConnectSDKSearch(Context context) {
        this.context = context;
        handler = new Handler(context.getMainLooper());
    }

    public static boolean isSupportedDevice(ConnectableDevice device) {
        if(device != null && device.getFriendlyName() != null && device.getFriendlyName().toLowerCase().contains("samsung")) {
            return false;
        }
        return true;
    }

    @Override
    public void start(int timeout, final FunctionCall<Void, SmartDevice> cb) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                if(mDiscoveryManagerListener != null) return;

                if(discoveryManager == null) {
                    DiscoveryManager.init(context);
                    discoveryManager = DiscoveryManager.getInstance();
                }
                mDiscoveryManagerListener = new DiscoveryManagerListener() {
                    @Override
                    public void onDeviceAdded(DiscoveryManager manager, ConnectableDevice device) {
                        if(isSupportedDevice(device) && device.hasAnyCapability("MediaPlayer.Play.Video")) {
                            cb.call(new ConnectSDKDevice(device));
                        }
                    }

                    @Override
                    public void onDeviceUpdated(DiscoveryManager manager, ConnectableDevice device) {
                        if(isSupportedDevice(device) && device.hasAnyCapability("MediaPlayer.Play.Video")) {
                            cb.call(new ConnectSDKDevice(device));
                        }
                    }

                    @Override
                    public void onDeviceRemoved(DiscoveryManager manager, ConnectableDevice device) {

                    }

                    @Override
                    public void onDiscoveryFailed(DiscoveryManager manager, ServiceCommandError error) {

                    }
                };
                discoveryManager.addListener(mDiscoveryManagerListener);
                discoveryManager.start();
            }
        });
    }

    @Override
    public void stop() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                if(mDiscoveryManagerListener != null) {
                    discoveryManager.removeListener(mDiscoveryManagerListener);
                    mDiscoveryManagerListener = null;
                    discoveryManager.stop();
                }
            }
        });
    }

    @Override
    public void release() {
        stop();
    }

    class ConnectSDKDevice implements SmartDevice {
        private ConnectableDevice connectableDevice;

        ConnectSDKDevice(ConnectableDevice connectableDevice) {
            this.connectableDevice = connectableDevice;
        }

        @Override
        public String getId() {
            return connectableDevice.getId();
        }

        @Override
        public String getName() {
            return String.format("%s [C]", connectableDevice.getFriendlyName());
        }

        private MediaInfo buildMp4Media(String url) {
            if(url == null || url.isEmpty()) return null;

            return new MediaInfo.Builder(url, "video/mp4")
                    .build();
        }

        @Override
        public void playMedia(final String url, final FunctionCall<Void, FunctionCallResult<Boolean>> cb) {

            handler.post(new Runnable() {
                @Override
                public void run() {
                    try {
                        MediaInfo mediaInfo = buildMp4Media(url);
                        connectableDevice.getMediaPlayer().playMedia(
                                mediaInfo, false, new MediaPlayer.LaunchListener() {
                                    @Override
                                    public void onSuccess(MediaPlayer.MediaLaunchObject object) {
                                        cb.callOnce(FunctionCallResult.asResult(true));
                                    }

                                    @Override
                                    public void onError(ServiceCommandError error) {
                                        cb.callOnce(FunctionCallResult.asResult(false));
                                    }
                                }
                        );

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
                    connectableDevice.connect();
                }
            });
        }

        @Override
        public void isConnected(final FunctionCall<Void, FunctionCallResult<Boolean>> cb) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    try {
                        cb.callOnce(FunctionCallResult.asResult(connectableDevice.isConnected()));
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
                    connectableDevice.disconnect();
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
                        connectableDevice.getMediaControl().play(new ResponseListener<Object>() {
                            @Override
                            public void onError(ServiceCommandError error) {
                                cb.callOnce(FunctionCallResult.asResult(false));
                            }

                            @Override
                            public void onSuccess(Object object) {
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
                        connectableDevice.getMediaControl().pause(new ResponseListener<Object>() {
                            @Override
                            public void onError(ServiceCommandError error) {
                                cb.callOnce(FunctionCallResult.asResult(false));
                            }

                            @Override
                            public void onSuccess(Object object) {
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
                        connectableDevice.getMediaControl().seek(position, new ResponseListener<Object>() {
                            @Override
                            public void onError(ServiceCommandError error) {
                                cb.callOnce(FunctionCallResult.asResult(false));
                            }

                            @Override
                            public void onSuccess(Object object) {
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
                        final Map<String, Object> status = new HashMap<>();

                        connectableDevice.getMediaControl().getDuration(new MediaControl.DurationListener() {
                            @Override
                            public void onSuccess(Long duration) {
                                updateStatus(status, "duration", duration == null ? null : duration / 1000, cb);

                            }

                            @Override
                            public void onError(ServiceCommandError error) {
                                updateStatus(status, "duration", null, cb);
                            }
                        });

                        connectableDevice.getMediaControl().getPosition(new MediaControl.PositionListener() {
                            @Override
                            public void onSuccess(Long position) {
                                updateStatus(status, "position", position == null ? null : position / 1000, cb);
                            }

                            @Override
                            public void onError(ServiceCommandError error) {
                                updateStatus(status, "position", null, cb);
                            }
                        });

                        connectableDevice.getMediaControl().getPlayState(new MediaControl.PlayStateListener() {
                            @Override
                            public void onSuccess(MediaControl.PlayStateStatus object) {
                                String state = null;
                                if(object.compareTo(MediaControl.PlayStateStatus.Playing) == 0)
                                    state= "playing";
                                else if(object.compareTo(MediaControl.PlayStateStatus.Paused) == 0)
                                    state= "paused";
                                else if(object.compareTo(MediaControl.PlayStateStatus.Idle) == 0)
                                    state= "stop";

                                updateStatus(status, "state", state, cb);
                            }

                            @Override
                            public void onError(ServiceCommandError error) {
                                updateStatus(status, "state", null, cb);
                            }
                        });

                    } catch(Exception e) {
                        cb.callOnce(FunctionCallResult.<Map<String, Object>>asError(e));
                    }
                }
            });
        }

        private void updateStatus(final Map<String, Object> status, final String key, final Object val, final FunctionCall<Void, FunctionCallResult<Map<String, Object>>> cb) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    status.put(key, val);
                    if(status.size() == 3)
                        cb.callOnce(FunctionCallResult.asResult(status));
                }
            });
        }

    }

}
