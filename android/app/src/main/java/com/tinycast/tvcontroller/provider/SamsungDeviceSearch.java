package com.tinycast.tvcontroller.provider;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.samsung.multiscreen.Error;
import com.samsung.multiscreen.Player;
import com.samsung.multiscreen.Result;
import com.samsung.multiscreen.Search;
import com.samsung.multiscreen.Service;
import com.samsung.multiscreen.VideoPlayer;
import com.tinycast.helper.FunctionCall;
import com.tinycast.helper.FunctionCallResult;
import com.tinycast.tvcontroller.SmartDevice;
import com.tinycast.tvcontroller.SmartDeviceSearch;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


public class SamsungDeviceSearch implements SmartDeviceSearch {
    private static final String TAG = SamsungDeviceSearch.class.getSimpleName();

    private Context context;
    private Search search;
    private final ExecutorService bgWorker = Executors.newSingleThreadExecutor();

    public SamsungDeviceSearch(Context context) {
        this.context = context;
    }

    @Override
    public void start(int timeout, final FunctionCall<Void, SmartDevice> cb) {
        bgWorker.submit(new Runnable() {
            @Override
            public void run() {
                if(search != null) return;

                search = Service.search(context);
                search.setOnServiceFoundListener(
                        new Search.OnServiceFoundListener() {
                            @Override
                            public void onFound(Service service) {
                                cb.call(new SamsungSmartDevice(service));
                            }
                        }
                );
                search.setOnServiceLostListener(new Search.OnServiceLostListener() {
                    @Override
                    public void onLost(Service service) {

                    }
                });
                search.start();
            }
        });
    }

    @Override
    public void stop() {
        bgWorker.submit(new Runnable() {
            @Override
            public void run() {
                if(search == null) return;
                search.stop();
                search = null;
            }
        });
    }

    @Override
    public void release() {
        stop();
        bgWorker.shutdown();
    }

    static class SamsungSmartDevice implements SmartDevice, VideoPlayer.OnVideoPlayerListener {
        private Service service;
        private VideoPlayer player;
        private ExecutorService bgWorker;
        private boolean hasNotPlayYet = false;
        private Map<String, Object> playerStatus;

        SamsungSmartDevice(Service service) {
            this.service = service;
        }

        @Override
        public String getId() {
            return service.getId();
        }

        @Override
        public String getName() {
            return String.format("%s [S]", service.getName());
        }

        @Override
        public void playMedia(final String url, final FunctionCall<Void, FunctionCallResult<Boolean>> cb) {
            if(bgWorker == null) {
                cb.call(FunctionCallResult.<Boolean>asError(new Exception("Device Not Connected Yet!")));
                return;
            }

            bgWorker.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        player.playContent(Uri.parse(url), new Result<Boolean>() {
                            @Override
                            public void onSuccess(Boolean aBoolean) {
                                cb.callOnce(FunctionCallResult.asResult(true));
                            }

                            @Override
                            public void onError(Error error) {
                                cb.callOnce(FunctionCallResult.<Boolean>asError(new Exception(error.getMessage())));
                            }
                        });
                        hasNotPlayYet = false;

                    } catch(Exception e) {
                        cb.callOnce(FunctionCallResult.<Boolean>asError(e));
                    }
                }
            });
        }

        @Override
        public void disconnect() {
            ExecutorService bgWorker_ = bgWorker;
            if(bgWorker_ == null) return;
            bgWorker = null;

            bgWorker_.submit(new Runnable() {
                @Override
                public void run() {
                    if (player != null) {
                        player.disconnect(false, null);
                        player = null;
                    }
                }
            });
            bgWorker_.shutdown();
        }

        @Override
        public void connect() {
            if(bgWorker == null)
                bgWorker = Executors.newSingleThreadExecutor();

            bgWorker.submit(new Runnable() {
                @Override
                public void run() {
                    player = service.createVideoPlayer("DMP");
                    playerStatus = new HashMap<>();
                    player.addOnMessageListener(SamsungSmartDevice.this);
                    hasNotPlayYet = true;
                }
            });
        }

        @Override
        public void isConnected(final FunctionCall<Void, FunctionCallResult<Boolean>> cb) {
            if(bgWorker == null) {
                cb.callOnce(FunctionCallResult.asResult(false));
                return;
            }

            bgWorker.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        cb.callOnce(FunctionCallResult.asResult(
                                player != null && (hasNotPlayYet || player.isConnected())
                        ));
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
        public boolean isSameDevice(SmartDevice smartDevice) {
            return false;
        }

        @Override
        public void play(final FunctionCall<Void, FunctionCallResult<Boolean>> cb) {
            if(bgWorker == null) {
                cb.callOnce(FunctionCallResult.asResult(false));
                return;
            }

            bgWorker.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        player.play();
                        cb.callOnce(FunctionCallResult.asResult(true));
                    } catch(Exception e) {
                        cb.callOnce(FunctionCallResult.<Boolean>asError(e));
                    }
                }
            });
        }

        @Override
        public void pause(final FunctionCall<Void, FunctionCallResult<Boolean>> cb) {
            if(bgWorker == null) {
                cb.callOnce(FunctionCallResult.asResult(false));
                return;
            }

            bgWorker.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        player.pause();
                        cb.callOnce(FunctionCallResult.asResult(true));
                    } catch(Exception e) {
                        cb.callOnce(FunctionCallResult.<Boolean>asError(e));
                    }
                }
            });
        }

        @Override
        public void seek(final Integer position, final FunctionCall<Void, FunctionCallResult<Boolean>> cb) {
            if(bgWorker == null) {
                cb.callOnce(FunctionCallResult.asResult(false));
                return;
            }

            bgWorker.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        player.seekTo(position, TimeUnit.SECONDS);
                        cb.callOnce(FunctionCallResult.asResult(true));
                    } catch(Exception e) {
                        cb.callOnce(FunctionCallResult.<Boolean>asError(e));
                    }
                }
            });
        }

        @Override
        public void getStatus(final FunctionCall<Void, FunctionCallResult<Map<String, Object>>> cb) {
            if(bgWorker == null) {
                cb.callOnce(FunctionCallResult.<Map<String, Object>>asError(new Exception("SG: Invalid Device")));
                return;
            }

            bgWorker.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        cb.callOnce(FunctionCallResult.asResult(playerStatus));
                    } catch(Exception e) {
                        cb.callOnce(FunctionCallResult.<Map<String, Object>>asError(e));
                    }
                }
            });
        }

        @Override
        public void onBufferingStart() {

        }

        @Override
        public void onBufferingComplete() {

        }

        @Override
        public void onBufferingProgress(int i) {

        }

        @Override
        public void onCurrentPlayTime(final int i) {
            if(bgWorker != null) {
                bgWorker.submit(new Runnable() {
                    @Override
                    public void run() {
                        if (playerStatus != null)
                            playerStatus.put("position", i / 1000);
                    }
                });
            }
        }

        @Override
        public void onStreamingStarted(final int i) {
            if(bgWorker != null) {
                bgWorker.submit(new Runnable() {
                    @Override
                    public void run() {
                        if (playerStatus != null) {
                            playerStatus.put("state", "playing");
                            playerStatus.put("duration", i / 1000);
                        }
                    }
                });
            }
        }

        @Override
        public void onStreamCompleted() {

        }

        @Override
        public void onPlayerInitialized() {

        }

        @Override
        public void onPlayerChange(String s) {

        }

        @Override
        public void onPlay() {
            if(bgWorker != null) {
                bgWorker.submit(new Runnable() {
                    @Override
                    public void run() {
                        if (playerStatus != null) {
                            Object origState = playerStatus.get("state");
                            playerStatus.put("state", origState != null && "playing".equalsIgnoreCase(origState.toString()) ? "paused" : "playing");
                        }
                    }
                });
            }
        }

        @Override
        public void onPause() {
            if(bgWorker != null) {
                bgWorker.submit(new Runnable() {
                    @Override
                    public void run() {
                        if (playerStatus != null)
                            playerStatus.put("state", "paused");
                    }
                });
            }
        }

        @Override
        public void onStop() {
            if(bgWorker != null) {
                bgWorker.submit(new Runnable() {
                    @Override
                    public void run() {
                        if (playerStatus != null)
                            playerStatus.put("state", "stop");
                    }
                });
            }
        }

        @Override
        public void onForward() {

        }

        @Override
        public void onRewind() {

        }

        @Override
        public void onMute() {

        }

        @Override
        public void onUnMute() {

        }

        @Override
        public void onNext() {

        }

        @Override
        public void onPrevious() {

        }

        @Override
        public void onControlStatus(int i, Boolean aBoolean, Player.RepeatMode repeatMode) {

        }

        @Override
        public void onVolumeChange(int i) {

        }

        @Override
        public void onAddToList(JSONObject jsonObject) {

        }

        @Override
        public void onRemoveFromList(JSONObject jsonObject) {

        }

        @Override
        public void onClearList() {

        }

        @Override
        public void onGetList(JSONArray jsonArray) {

        }

        @Override
        public void onRepeat(Player.RepeatMode repeatMode) {

        }

        @Override
        public void onCurrentPlaying(JSONObject jsonObject, String s) {

        }

        @Override
        public void onApplicationResume() {

        }

        @Override
        public void onApplicationSuspend() {

        }

        @Override
        public void onError(Error error) {

        }
    }
}
