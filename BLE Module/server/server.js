require("dotenv").config();
const express = require("express");
const connectDB = require("./src/config/db");

const ClassMapping = require("./src/models/ClassMapping");
const { classMap, activeMinors } = require("./src/utils/cache");
const os = require("os");

const app = express();

// Middleware
app.use(express.json());


// 🔥 Load mappings into cache
const loadClassMappings = async () => {
  try {
    // Clear old cache (IMPORTANT)
    classMap.clear();

    const mappings = await ClassMapping.find();

    mappings.forEach((cls) => {
      const majorMap = new Map();

      cls.majors.forEach((m) => {
        majorMap.set(String(m.major_id), m.rssi_threshold);
      });

      classMap.set(String(cls.class_id), majorMap);
    });

    console.log(`✅ Loaded ${mappings.length} class mappings into cache`);
  } catch (err) {
    console.error("❌ Failed to load class mappings:", err);
  }
};


// Routes
const bleRoutes = require("./src/modules/ble/ble.routes");
app.use("/ble", bleRoutes);

const qrRoutes = require("./src/modules/qr/qr.routes");
app.use("/qr", qrRoutes);


// Health check
app.get("/", (req, res) => {
  res.send("BLE Service Running 🚀");
});


// 🔹 Get Local IP
function getLocalIP() {
  const interfaces = os.networkInterfaces();

  for (const name of Object.keys(interfaces)) {
    for (const net of interfaces[name]) {
      if (net.family === "IPv4" && !net.internal) {
        return net.address;
      }
    }
  }

  return "localhost";
}


// 🔥 Start server ONLY after DB + cache ready
const startServer = async () => {
  try {
    await connectDB();
    await loadClassMappings();

    const PORT = process.env.PORT || 8000;

    app.listen(PORT, "0.0.0.0", () => {
      const ip = getLocalIP();

      console.log("🚀 Server running on:");
      console.log(`   Local:   http://localhost:${PORT}`);
      console.log(`   Network: http://${ip}:${PORT}`);
    });

  } catch (err) {
    console.error("❌ Server startup failed:", err);
    process.exit(1);
  }
};

startServer();

setInterval(() => {
  const now = Date.now();

  for (const [major, data] of activeMinors) {
    if (now > data.expires_at.getTime()) {
      activeMinors.delete(major);
    }
  }
}, 30000);