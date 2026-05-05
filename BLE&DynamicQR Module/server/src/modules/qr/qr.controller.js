const crypto = require("crypto");
const QRToken = require("../../models/QRToken");

const qrCache = new Map();


// 🔹 Generate QR
exports.generateQR = async (req, res) => {
  try {
    const { class_id } = req.body;

    if (!class_id) {
      return res.status(400).json({ error: "class_id required" });
    }

    const hash = crypto.randomBytes(4).toString("hex");

    const now = new Date();
    const expires_at = new Date(now.getTime() + 15000); // 15 sec

    // Expire old tokens
    await QRToken.updateMany(
      { class_id },
      { $set: { expires_at: now } }
    );

    // Insert new token
    await QRToken.create({
      class_id,
      hash,
      issued_at: now,
      expires_at,
    });

    // Update cache
    qrCache.set(class_id, {
      hash,
      issued_at: now.getTime(),
      expires_at: expires_at.getTime(),
    });

    res.json({
      hash,
      expires_in: 15,
    });

  } catch (err) {
    console.error(err);
    res.status(500).json({ error: "Server error" });
  }
};



// 🔹 Validate QR
exports.validateQR = async (req, res) => {
  try {
    const { class_id, hash, timestamp } = req.body;

    if (!class_id || !hash || !timestamp) {
      return res.status(400).json({ valid: false });
    }

    const now = Date.now();

    // 1. Check cache first (FAST)
    const cached = qrCache.get(class_id);

    if (cached) {
      if (
        cached.hash === hash &&
        now <= cached.expires_at &&
        Math.abs(now - timestamp) <= 5000
      ) {
        return res.json({ valid: true });
      }
    }

    // 2. Fallback to DB
    const record = await QRToken.findOne({
      class_id,
      hash,
    });

    if (!record) {
      return res.json({ valid: false });
    }

    if (now > record.expires_at.getTime()) {
      return res.json({ valid: false });
    }

    if (Math.abs(now - timestamp) > 5000) {
      return res.json({ valid: false });
    }

    return res.json({ valid: true });

  } catch (err) {
    console.error(err);
    res.status(500).json({ valid: false });
  }
};