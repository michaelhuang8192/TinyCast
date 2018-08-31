package com.tinycast.tvcontroller.provider;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.samsung.multiscreen.Error;
import com.samsung.multiscreen.Result;
import com.samsung.multiscreen.Search;
import com.samsung.multiscreen.Service;
import com.samsung.multiscreen.VideoPlayer;
import com.tinycast.helper.FunctionCall;
import com.tinycast.helper.FunctionCallResult;
import com.tinycast.tvcontroller.SmartDevice;
import com.tinycast.tvcontroller.SmartDeviceSearch;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


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

    static class SamsungSmartDevice implements SmartDevice {
        private Service service;
        private VideoPlayer player;
        private ExecutorService bgWorker;
        private boolean hasNotPlayYet = false;

        SamsungSmartDevice(Service service) {
            this.service = service;
        }

        @Override
        public String getId() {
            return service.getId();
        }

        @Override
        public String getName() {
            return service.getName();
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

    }
}
