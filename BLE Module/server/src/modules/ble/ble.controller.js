const MajorMinor = require("../../models/MajorMinor");
const { activeMinors, classMap } = require("../../utils/cache");


// 🔹 Generate Minor
exports.generateMinor = async (req, res) => {
  try {
    const { major_id } = req.body;

    if (!major_id) {
      return res.status(400).json({ error: "major_id required" });
    }

    // 1. Generate unique 4-digit minor (avoid rare collisions)
    let minor;
    do {
      minor = Math.floor(1000 + Math.random() * 9000).toString();
    } while (await MajorMinor.findOne({ major_id, minor }));

    const now = new Date();
    const expires_at = new Date(now.getTime() + 45 * 1000);

    // 2. Expire old minors
    await MajorMinor.updateMany(
      { major_id },
      { $set: { expires_at: now } }
    );

    // 3. Insert new minor
    await MajorMinor.create({
      major_id,
      minor,
      issued_at: now,
      expires_at,
    });

    // 4. 🔥 Update cache
    activeMinors.set(major_id, {
      minor,
      expires_at, // Date object
    });

    // 5. Return response
    res.json({
      minor,
      expires_in: 45,
    });

  } catch (err) {
    console.error(err);
    res.status(500).json({ error: "Server error" });
  }
};



// 🔹 Validate (Multi-beacon + strongest RSSI)
exports.validate = async (req, res) => {
  try {
    const { class_id, beacons } = req.body;

    if (!class_id || !Array.isArray(beacons)) {
      return res.status(400).json({ valid: false });
    }

    const classData = classMap.get(class_id);
    if (!classData) return res.json({ valid: false });

    let best = null;

    // 1. Pick strongest valid beacon (O(n))
    for (const b of beacons) {
      if (!classData.has(b.major)) continue;

      if (!best || b.rssi > best.rssi) {
        best = b;
      }
    }

    if (!best) return res.json({ valid: false });

    const { major, minor, rssi } = best;

    const threshold = classData.get(major);

    // 2. Apply RSSI threshold
    if (rssi < threshold - 5) {
      return res.json({ valid: false });
    }

    const now = Date.now();

    // 3. Cache check (FAST PATH)
    const cached = activeMinors.get(major);

    if (
      cached &&
      cached.minor === minor &&
      now <= cached.expires_at.getTime()
    ) {
      return res.json({ valid: true });
    }

    // 4. DB fallback
    const record = await MajorMinor.findOne({
      major_id: major,
      minor,
    });

    if (
      !record ||
      now > record.expires_at.getTime()
    ) {
      return res.json({ valid: false });
    }

    return res.json({ valid: true });

  } catch (err) {
    console.error(err);
    res.status(500).json({ valid: false });
  }
};