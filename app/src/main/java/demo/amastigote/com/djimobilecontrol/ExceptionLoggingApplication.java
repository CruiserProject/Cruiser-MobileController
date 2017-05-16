package demo.amastigote.com.djimobilecontrol;

import android.app.Application;
import android.content.Context;
import android.support.multidex.MultiDex;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.Socket;

/**
 * Created by Trafalgar on 2017/5/16.
 */

public class ExceptionLoggingApplication extends Application {
    protected static ExceptionLoggingApplication exceptionLoggingApplication;
    private Thread.UncaughtExceptionHandler uncaughtExceptionHandler = new Thread.UncaughtExceptionHandler() {
        @Override
        public void uncaughtException(Thread t, Throwable e) {
            String errorMsg = "From Thread " + t.getName() + "throws exception: " + e.getMessage();
            Socket socket;
            try {
                socket = new Socket("192.168.1.102", 3000);
                BufferedWriter bufferedWriter = new BufferedWriter((new OutputStreamWriter(socket.getOutputStream())));
                bufferedWriter.write(errorMsg);
                bufferedWriter.flush();
                bufferedWriter.close();
                socket.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        exceptionLoggingApplication = this;
        Thread.setDefaultUncaughtExceptionHandler(uncaughtExceptionHandler);
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }
}
