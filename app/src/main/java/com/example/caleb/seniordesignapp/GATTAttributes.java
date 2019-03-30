package com.example.caleb.seniordesignapp;

import java.util.HashMap;

public class GATTAttributes {

    private static HashMap<String, String> attributes = new HashMap();


    public static String HM11 = "0000ffe1-0000-1000-8000-00805f9b34fb"; //This Tells the phone how to access the data from HM-11
    public static String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb"; //This tells the phone how to set up i.e how it will recieve and send data etc.

    static {
        // Sample Services.
        attributes.put("0000ffe0-0000-1000-8000-00805f9b34fb", "HM-11 Service"); //Overarching Identifier that encompasses the HM-11 characteristic named above
        attributes.put("0000180a-0000-1000-8000-00805f9b34fb", "Device Information Service");
        // Sample Characteristics.
        attributes.put(HM11, "HM-11 Message");
        attributes.put("00002a29-0000-1000-8000-00805f9b34fb", "Manufacturer Name String"); //Function of the "Device Information Service
    }

    //Function to retrieve UUID from the Hashmap
    public static String lookup(String uuid, String defaultName) {
        String name = attributes.get(uuid);
        return name == null ? defaultName : name;
    }
}

