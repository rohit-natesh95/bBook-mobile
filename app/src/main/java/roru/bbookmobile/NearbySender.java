package roru.bbookmobile;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

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
    private DiscoveryOptions discoveryOptions = new DiscoveryOptions.Builder().
            setStrategy(Strategy.P2P_POINT_TO_POINT).build();
    private int attempt = 0;
    private Context context;
    private Payload payload;
    private final ConnectionLifecycleCallback connectionLifecycleCallback = new ConnectionLifecycleCallback() {
        @Override
        public void onConnectionInitiated(@NonNull String endpointId, @NonNull ConnectionInfo connectionInfo) {
            Nearby.getConnectionsClient(context).acceptConnection(endpointId, payloadCallback);
        }

        @Override
        public void onConnectionResult(@NonNull String endpointId, ConnectionResolution result) {
            switch (result.getStatus().getStatusCode()) {
                case ConnectionsStatusCodes.STATUS_OK:
                    Log.i(TAG, "onConnectionResult: Ok");
                    Nearby.getConnectionsClient(context).sendPayload(endpointId, payload);
                    break;
                case ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED:
                    Log.i(TAG, "onConnectionResult: reject");
                    break;
                case ConnectionsStatusCodes.STATUS_ERROR:
                    Log.i(TAG, "onConnectionResult: error");
                    break;
                default:
                    Log.i(TAG, "onConnectionResult: unknown");
            }
        }

        @Override
        public void onDisconnected(@NonNull String endpointId) {
            Nearby.getConnectionsClient(context).stopDiscovery();
            Nearby.getConnectionsClient(context).stopAllEndpoints();
        }
    };
    private final EndpointDiscoveryCallback endpointDiscoveryCallback = new EndpointDiscoveryCallback() {
        @Override
        public void onEndpointFound(@NonNull String endPointId, @NonNull DiscoveredEndpointInfo discoveredEndpointInfo) {
            Log.i(TAG, "Found end point: " + discoveredEndpointInfo.getEndpointName() + " and :" + endPointId);
            Nearby.getConnectionsClient(context)
                    .requestConnection("bBook Mobile", endPointId, connectionLifecycleCallback)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Log.i(TAG, "Request to adv sent");
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
            Log.i(TAG, "Lost end point");
        }
    };

    NearbySender(Context context) {
        this.context = context;
    }

    void sendData(final String data) {
        attempt++;
        payload = Payload.fromBytes(data.getBytes());
        String SERVICE_ID = "123456";
        Nearby.getConnectionsClient(context)
                .startDiscovery(SERVICE_ID, endpointDiscoveryCallback, discoveryOptions)
                .addOnSuccessListener(
                        new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                attempt = 0;
                                Log.i(TAG, "Starting discovery");
                            }
                        }
                )
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.i(TAG, "Discovery failed");
                                Nearby.getConnectionsClient(context).stopDiscovery();
                                Log.i(TAG, "onFailure: Starting discovery attempt " + attempt);
                                if (attempt < 5)
                                    sendData(data);
                            }
                        }
                );
    }
}
