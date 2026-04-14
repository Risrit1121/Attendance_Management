#include <WiFi.h>
#include <HTTPClient.h>
#include <NimBLEDevice.h>

// ---------------- WIFI ----------------
const char* ssid = "WaitForIt";
const char* password = "azsxdcfv";

// ---------------- SERVER ----------------
String serverURL = "https://ble-qr-microservice.onrender.com/ble/generate-minor";

// ---------------- BEACON ----------------
uint16_t major = 100;
uint16_t minor = 0;

NimBLEAdvertising *pAdvertising;


// Correct UUID bytes
// BEACON_UUID "550e8400-e29b-41d4-a716-446655440000"
// uint8_t uuidBytes[16] = {
//   0x55, 0x0e, 0x84, 0x00,
//   0xe2, 0x9b,
//   0x41, 0xd4,
//   0xa7, 0x16,
//   0x44, 0x66, 0x55, 0x44, 0x00, 0x00
// };
// Custom UUID: "IITH-ATTENDANCE " (Must be exactly 16 bytes)
uint8_t uuidBytes[16] = {
  0x49, 0x49, 0x54, 0x48, // I I T H
  0x2D, 0x41,             // - A
  0x54, 0x54,             // T T
  0x45, 0x4E,             // E N
  0x44, 0x41, 0x4E, 0x43, 0x45, 0x20 // D A N C E [space]
};

// ---------------- WIFI ----------------
void connectWiFi() {

  WiFi.mode(WIFI_STA);
  WiFi.begin(ssid, password);

  Serial.println("Connecting to WiFi...");

  int attempts = 0;

  while (WiFi.status() != WL_CONNECTED) {
    delay(1000);
    Serial.print(".");
    attempts++;

    if (attempts > 20) {
      Serial.println("\nFailed to connect.");
      return;
    }
  }

  Serial.println("\nConnected!");
  Serial.print("IP: ");
  Serial.println(WiFi.localIP());
}


// ---------------- FETCH MINOR ----------------
uint16_t fetchMinor() {

  if (WiFi.status() == WL_CONNECTED) {

    HTTPClient http;

    http.begin(serverURL);
    http.addHeader("Content-Type", "application/json");

    // Send JSON body
    String payload = "{\"major_id\":" + String(major) + "}";

    Serial.println("POST: " + payload);

    int httpCode = http.POST(payload);

    if (httpCode == 200) {
      String response = http.getString();
      Serial.println("Response: " + response);

      // Extract minor
      int keyIndex = response.indexOf("\"minor\":\"");

      if (keyIndex != -1) {
        int start = keyIndex + 9; // length of "minor":" 
        int end = response.indexOf("\"", start);

        String minorStr = response.substring(start, end);

        Serial.println("Parsed minor: " + minorStr);

        http.end();
        return minorStr.toInt();
      }
    }

    http.end();
  }

  return 0;
}


// ---------------- BUILD IBEACON ----------------
std::string createBeaconData(uint16_t major, uint16_t minor) {

  std::string data = "";

  // Apple Company ID
  data += (char)0x4C;
  data += (char)0x00;

  // iBeacon Type + Length
  data += (char)0x02;
  data += (char)0x15;

  // UUID
  data.append((char*)uuidBytes, 16);

  // Major
  data += (char)(major >> 8);
  data += (char)(major & 0xFF);

  // Minor
  data += (char)(minor >> 8);
  data += (char)(minor & 0xFF);

  // TX Power
  data += (char)0xC5;

  return data;
}


// ---------------- START BEACON ----------------
// void startBeacon(uint16_t major, uint16_t minor) {

//   NimBLEAdvertisementData advData;

//   std::string payload = createBeaconData(major, minor);

//   advData.setManufacturerData(payload);

//   pAdvertising->setAdvertisementData(advData);

//   pAdvertising->setMinInterval(0x20);  // ~20ms
//   pAdvertising->setMaxInterval(0x40);  // ~40ms

//   pAdvertising->start();

//   Serial.println("Beacon broadcasting");
// }
void startBeacon(uint16_t major, uint16_t minor) {

    pAdvertising->stop();

    NimBLEAdvertisementData advData;
    std::string payload = createBeaconData(major, minor);

    advData.setManufacturerData(payload);
    advData.setFlags(0x06);  // LE General Discoverable + BR/EDR Not Supported

    // 🔥 Set intervals BEFORE setting data
    pAdvertising->setMinInterval(160);  // 100ms
    pAdvertising->setMaxInterval(320);  // 200ms

    pAdvertising->setAdvertisementData(advData);

    // Disable scan response
    NimBLEAdvertisementData emptyScanRsp;
    pAdvertising->setScanResponseData(emptyScanRsp);

    pAdvertising->start();

    // 🔍 DEBUG PRINTS
    Serial.println("🚀 Beacon started");
    Serial.print("Major: ");
    Serial.println(major);

    Serial.print("Minor: ");
    Serial.println(minor);

    Serial.println("Interval: 100–200 ms");

    // 🔥 Optional: print payload hex (very useful)
    // Serial.print("Payload (hex): ");
    // for (size_t i = 0; i < payload.length(); i++) {
    //     if ((uint8_t)payload[i] < 0x10) Serial.print("0");
    //     Serial.print((uint8_t)payload[i], HEX);
    //     Serial.print(" ");
    // }
    // Serial.println();
}

// ---------------- SETUP ----------------
// void setup() {

//   Serial.begin(115200);

//   connectWiFi();

//   NimBLEDevice::init("ESP32 Beacon");

//   pAdvertising = NimBLEDevice::getAdvertising();

//   minor = fetchMinor();

//   if (minor == 0) minor = 1;

//   startBeacon(major, minor);
// }

void setup() {
    Serial.begin(115200);
    connectWiFi();

    NimBLEDevice::init("ESP32 Beacon");

    // 🔥 THIS IS THE FIX — forces a random address
    // Android 12+ blocks static public MACs (EC:64:...) from non-system apps
    NimBLEDevice::setOwnAddrType(BLE_OWN_ADDR_RANDOM);

    pAdvertising = NimBLEDevice::getAdvertising();

    minor = fetchMinor();
    if (minor == 0) minor = 1;

    startBeacon(major, minor);
}

// ---------------- LOOP ----------------
void loop() {

  delay(30000);  // update every 30s

  uint16_t newMinor = fetchMinor();

  if (newMinor != 0 && newMinor != minor) {

    minor = newMinor;

    pAdvertising->stop();
    startBeacon(major, minor);

    Serial.println("Beacon updated");
  }
}