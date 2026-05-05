const mongoose = require("mongoose");

const majorMinorSchema = new mongoose.Schema({
  major_id: {
    type: String,
    required: true,
  },

  minor: {
    type: String,
    required: true,
  },

  issued_at: {
    type: Date,
    required: true,
  },

  expires_at: {
    type: Date,
    required: true,
  },
});

// TTL index (auto delete expired)
majorMinorSchema.index({ expires_at: 1 }, { expireAfterSeconds: 0 });

// Fast lookup index
majorMinorSchema.index({ major_id: 1, minor: 1 });

module.exports = mongoose.model("MajorMinor", majorMinorSchema);