const express = require("express");
const router = express.Router();
const ClassMapping = require("../../models/ClassMapping");
const Admin = require("../../models/Admin");
const { classMap } = require("../../utils/cache");

// Database auth middleware
const auth = async (req, res, next) => {
  try {
    const { username, password } = req.headers;
    if (!username || !password) {
      return res.status(401).json({ error: "Credentials required" });
    }

    const admin = await Admin.findOne({ username });
    if (!admin || !(await admin.comparePassword(password))) {
      return res.status(401).json({ error: "Invalid credentials" });
    }

    next();
  } catch (err) {
    res.status(500).json({ error: "Auth error" });
  }
};

// Get all mappings
router.get("/mappings", auth, async (req, res) => {
  try {
    const mappings = await ClassMapping.find();
    res.json(mappings);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// Create mapping
router.post("/mappings", auth, async (req, res) => {
  try {
    const mapping = new ClassMapping(req.body);
    await mapping.save();
    
    // Update cache
    const majorMap = new Map();
    mapping.majors.forEach((m) => {
      majorMap.set(String(m.major_id), m.rssi_threshold);
    });
    classMap.set(String(mapping.class_id), majorMap);
    
    res.json(mapping);
  } catch (err) {
    res.status(400).json({ error: err.message });
  }
});

// Update mapping
router.put("/mappings/:id", auth, async (req, res) => {
  try {
    const mapping = await ClassMapping.findByIdAndUpdate(
      req.params.id,
      req.body,
      { new: true }
    );
    
    // Update cache
    const majorMap = new Map();
    mapping.majors.forEach((m) => {
      majorMap.set(String(m.major_id), m.rssi_threshold);
    });
    classMap.set(String(mapping.class_id), majorMap);
    
    res.json(mapping);
  } catch (err) {
    res.status(400).json({ error: err.message });
  }
});

// Delete mapping
router.delete("/mappings/:id", auth, async (req, res) => {
  try {
    const mapping = await ClassMapping.findByIdAndDelete(req.params.id);
    classMap.delete(String(mapping.class_id));
    res.json({ message: "Deleted" });
  } catch (err) {
    res.status(400).json({ error: err.message });
  }
});

module.exports = router;
