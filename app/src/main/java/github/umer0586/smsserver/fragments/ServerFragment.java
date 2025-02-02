package github.umer0586.smsserver.fragments;

import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.github.ybq.android.spinkit.SpinKitView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;

import java.net.BindException;
import java.net.UnknownHostException;

import github.umer0586.smsserver.R;
import github.umer0586.smsserver.broadcastreceiver.ServerCallbackProvider;
import github.umer0586.smsserver.services.SMSService;

public class ServerFragment extends Fragment implements ServerCallbackProvider.ServerEventsListener{

    private static final String TAG =  ServerFragment.class.getSimpleName();

    private ServerCallbackProvider serverCallbackProvider;

    // Button at center to start/stop server
    private MaterialButton startButton;

    // Address of server (http://192.168.2.1:8081)
    private TextView serverAddress;

    // Lock icon placed at left of serverAddress
    private AppCompatImageView lockIcon;

    // card view which holds serverAddress and lockIcon
    private CardView cardView;

    //Ripple animation behind startButton
    private SpinKitView pulseAnimation;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        return inflater.inflate(R.layout.fragment_server, container, false);
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);

        Log.d(TAG, "onViewCreated() called with: view = [" + view + "], savedInstanceState = [" + savedInstanceState + "]");

        startButton = view.findViewById(R.id.start_button);
        serverAddress = view.findViewById(R.id.server_address);
        pulseAnimation = view.findViewById(R.id.loading_animation);
        lockIcon = view.findViewById(R.id.lock_icon);
        cardView = view.findViewById(R.id.card_view);

        serverCallbackProvider = new ServerCallbackProvider(getContext());
        serverCallbackProvider.setServerEventsListener(this);
        serverCallbackProvider.registerEvents();
        serverCallbackProvider.checkIfServerIsRunning();

        hidePulseAnimation();
        hideServerAddress();

        // we will use tag to determine last state of button
        startButton.setOnClickListener(v -> {
            if(v.getTag().equals("stopped"))
                startServer();
            else if(v.getTag().equals("started"))
                stopServer();
        });



    }

    private void stopServer()
    {

        Intent intent = new Intent();
        intent.setAction(SMSService.ACTION_STOP_SERVER);

        getContext().sendBroadcast(intent);

    }

    private void startServer()
    {
        Log.d(TAG, "startServer() called");


        WifiManager wifiManager = (WifiManager) getContext().getApplicationContext().getSystemService(getContext().WIFI_SERVICE);

        if(!wifiManager.isWifiEnabled())
        {
            showMessage("Please enable Wi-Fi");
            return;
        }


        Intent intent = new Intent(getContext(),SMSService.class);

        ContextCompat.startForegroundService(getContext(),intent);

    }


    @Override
    public void onPause()
    {
        super.onPause();
        Log.d(TAG, "onPause() called");
        serverCallbackProvider.unregisterEvents();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        Log.d(TAG, "onResume() called");
        serverCallbackProvider.registerEvents();
        serverCallbackProvider.checkIfServerIsRunning();
    }


    private void showServerAddress(final String address, boolean secure)
    {

        cardView.setVisibility(View.VISIBLE);
        serverAddress.setVisibility(View.VISIBLE);

        if (secure)
        {
            lockIcon.setVisibility(View.VISIBLE);
            serverAddress.setText(Html.fromHtml("<font color=\"#5c6bc0\">https://</font>" + address));
        } else
        {
            lockIcon.setVisibility(View.GONE);
            serverAddress.setText("http://" + address);
        }
    }

    private void showPulseAnimation()
    {
        pulseAnimation.setVisibility(View.VISIBLE);
    }

    private void hidePulseAnimation()
    {
        pulseAnimation.setVisibility(View.INVISIBLE);
    }

    private void hideServerAddress()
    {
        cardView.setVisibility(View.GONE);
        serverAddress.setVisibility(View.GONE);
        lockIcon.setVisibility(View.GONE);
    }


    @Override
    public void onDestroyView()
    {
        super.onDestroyView();
        Log.d(TAG, "onDestroyView() called");
        serverCallbackProvider.unregisterEvents();
    }

    @Override
    public void onServerStarted(String IP, int port, boolean isSecure)
    {
        Log.d(TAG, "onServerStarted() called with: IP = [" + IP + "], port = [" + port + "], isSecure = [" + isSecure + "]");
        showServerAddress(IP + ":" + port,isSecure);
        showPulseAnimation();
        startButton.setTag("started");
        startButton.setText("STOP");

        showMessage("SMS server started");

    }

    @Override
    public void onServerStopped()
    {
        hideServerAddress();
        hidePulseAnimation();
        startButton.setTag("stopped");
        startButton.setText("START");

        showMessage("SMS server stopped");
    }

    @Override
    public void onError(Throwable throwable)
    {
        Log.d(TAG, "onError() called with: throwable = [" + throwable + "]");

        if(throwable instanceof BindException)
            showMessage("Address already in use");
        else if(throwable instanceof UnknownHostException )
            showMessage("Unable to obtain IP address");
        else
            showMessage("Unable to start server");

    }

    @Override
    public void onServerAlreadyRunning(String IP, int port, boolean isSecure)
    {
        Log.d(TAG, "onServerAlreadyRunning() called with: IP = [" + IP + "], port = [" + port + "], isSecure = [" + isSecure + "]");
        showServerAddress(IP + ":" + port,isSecure);
        showPulseAnimation();
        startButton.setTag("started");
        startButton.setText("STOP");

        Toast.makeText(getContext(),"server running",Toast.LENGTH_SHORT).show();
    }


    private void showMessage(String message)
    {
        if(getView() != null)
            Snackbar.make(getView(),message,Snackbar.LENGTH_SHORT).show();
        else
            Toast.makeText(getContext(),message,Toast.LENGTH_SHORT).show();
    }
}
