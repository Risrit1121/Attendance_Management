require("dotenv").config();
const express = require("express");
const connectDB = require("./src/config/db");

const ClassMapping = require("./src/models/ClassMapping");
const { classMap } = require("./src/utils/cache");

const app = express();

// Middleware
app.use(express.json());

// 🔥 Load mappings into cache
const loadClassMappings = async () => {
  const mappings = await ClassMapping.find();

  mappings.forEach((cls) => {
    const majorMap = new Map();

    cls.majors.forEach((m) => {
      majorMap.set(m.major_id, m.rssi_threshold);
    });

    classMap.set(cls.class_id, majorMap);
  });

  console.log("Class mappings loaded into cache");
};

// 🔥 Connect DB AND THEN load cache
connectDB().then(loadClassMappings);

// Routes
const bleRoutes = require("./src/modules/ble/ble.routes");
app.use("/ble", bleRoutes);

const qrRoutes = require("./src/modules/qr/qr.routes");

app.use("/qr", qrRoutes);

// Health check
app.get("/", (req, res) => {
  res.send("BLE Service Running 🚀");
});

// Start server
const PORT = process.env.PORT || 5000;

app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});