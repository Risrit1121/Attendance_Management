require("dotenv").config();
const express = require("express");
const connectDB = require("./src/config/db");
const cors = require('cors');

const ClassMapping = require("./src/models/ClassMapping");
const { classMap, activeMinors } = require("./src/utils/cache");
const os = require("os");

const app = express();
app.use(cors());

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


app.use((req, res, next) => {
  const start = Date.now();

  // 📍 IP Address
  const ip =
    req.headers["x-forwarded-for"] ||
    req.socket.remoteAddress ||
    req.ip;

  // 📱 Device / Browser info
  const userAgent = req.headers["user-agent"];

  // 🌍 Additional headers
  const referer = req.headers["referer"] || "Direct";
  const origin = req.headers["origin"] || "Unknown";

  console.log("➡️ REQUEST");

  console.log({
    method: req.method,
    url: req.originalUrl,
    ip,
    userAgent,
    referer,
    origin,
  });

  // 🔥 LOG BODY (IMPORTANT)
  if (req.body && Object.keys(req.body).length > 0) {
    console.log("📦 BODY:", req.body);
  }

  const originalJson = res.json;

  res.json = function (data) {
    const duration = Date.now() - start;

    console.log("⬅️ RESPONSE");
    console.log({
      method: req.method,
      url: req.originalUrl,
      status: res.statusCode,
      time: `${duration}ms`,
      response: data,
    });

    return originalJson.call(this, data);
  };

  next();
});
// Static files
app.use(express.static("public"));

// Routes
const bleRoutes = require("./src/modules/ble/ble.routes");
app.use("/ble", bleRoutes);

const qrRoutes = require("./src/modules/qr/qr.routes");
app.use("/qr", qrRoutes);

const adminRoutes = require("./src/modules/admin/admin.routes");
app.use("/admin", adminRoutes);


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