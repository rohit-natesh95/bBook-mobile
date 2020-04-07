package roru.bbookmobile;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes;
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;
import com.google.android.gms.nearby.connection.DiscoveryOptions;
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;
import com.google.android.gms.nearby.connection.Strategy;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

class NearbySender {

    private final String TAG = "NearbySender";
    private final String SERVICE_ID = "123456";
    private final PayloadCallback payloadCallback = new PayloadCallback() {
        @Override
        public void onPayloadReceived(@NonNull String s, @NonNull Payload payload) {
            Log.i(TAG, "onPayloadReceived: ");
        }

        @Override
        public void onPayloadTransferUpdate(@NonNull String s, @NonNull PayloadTransferUpdate payloadTransferUpdate) {
            Log.i(TAG, "onPayloadTransferUpdate: ");
        }
    };
    DiscoveryOptions discoveryOptions = new DiscoveryOptions.Builder()
            .setStrategy(Strategy.P2P_POINT_TO_POINT).build();
    int attempt = 0;
    private Context context;
    private Payload payload;
    private final ConnectionLifecycleCallback connectionLifecycleCallback = new ConnectionLifecycleCallback() {
        @Override
        public void onConnectionInitiated(String endpointId, ConnectionInfo connectionInfo) {
            // Automatically accept the connection on both sides.
            Nearby.getConnectionsClient(context).acceptConnection(endpointId, payloadCallback);
        }

        @Override
        public void onConnectionResult(String endpointId, ConnectionResolution result) {
            switch (result.getStatus().getStatusCode()) {
                case ConnectionsStatusCodes.STATUS_OK:
                    // We're connected! Can now start sending and receiving data.
                    Log.i(TAG, "onConnectionResult: Ok");

                    Nearby.getConnectionsClient(context).sendPayload(endpointId, payload);

                    break;
                case ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED:
                    Log.i(TAG, "onConnectionResult: reject");
                    // The connection was rejected by one or both sides.
                    break;
                case ConnectionsStatusCodes.STATUS_ERROR:
                    Log.i(TAG, "onConnectionResult: error");
                    // The connection broke before it was able to be accepted.
                    break;
                default:
                    Log.i(TAG, "onConnectionResult: unknown");
                    // Unknown status code
            }
        }

        @Override
        public void onDisconnected(String endpointId) {
            Nearby.getConnectionsClient(context).stopDiscovery();
            Nearby.getConnectionsClient(context).stopAllEndpoints();
        }
    };
    private final EndpointDiscoveryCallback endpointDiscoveryCallback = new EndpointDiscoveryCallback() {
        @Override
        public void onEndpointFound(@NonNull String endPointId, @NonNull DiscoveredEndpointInfo discoveredEndpointInfo) {
            Log.i(TAG, "  -- Found end point: " + discoveredEndpointInfo.getEndpointName() + " and :" + endPointId);

            Nearby.getConnectionsClient(context)
                    .requestConnection("bBook mobile", endPointId, connectionLifecycleCallback)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Log.i(TAG, " -- request to adv sent");
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.i(TAG, " -- fail to request adv");
                        }
                    });

        }

        @Override
        public void onEndpointLost(@NonNull String s) {
            Log.i(TAG, " -- Lost end point");
        }
    };
/*
    @params mode = 0 for sending data
    @params mode = 1 for configuring wifi
 */

    NearbySender(Context context) {
        this.context = context;
    }

    void sendData(final String data, final int mode) {
        String packet;
        if (mode == 0)
            packet = 0 + data;
        else
            packet = 1 + data;
        attempt++;
        payload = Payload.fromBytes(packet.getBytes());
        Nearby.getConnectionsClient(context)
                .startDiscovery(SERVICE_ID, endpointDiscoveryCallback,
                        discoveryOptions)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        attempt = 0;
                        Log.i(TAG, " -- Starting discovery");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.i(TAG, " -- Discovery failed");
                        Nearby.getConnectionsClient(context).stopDiscovery();
                        Log.i(TAG, "onFailure: -- calling again");
                        if (attempt < 5)
                            sendData(data, mode);
                    }
                });
    }
}
