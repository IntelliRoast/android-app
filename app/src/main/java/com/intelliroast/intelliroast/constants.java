package com.intelliroast.intelliroast;

public interface constants {
    // Message types sent from the BluetoothChatService Handler
    int MESSAGE_STATE_CHANGE = 1;
    int MESSAGE_READ = 2;
    int MESSAGE_WRITE = 3;
    int MESSAGE_DEVICE_NAME = 4;
    int MESSAGE_TOAST = 5;
    int MESSAGE_MACHINE_STATE = 6;

    // Key names received from the BluetoothChatService Handler
    String TOAST = "toast";

}
