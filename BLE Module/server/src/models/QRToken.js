const mongoose = require("mongoose");

const qrTokenSchema = new mongoose.Schema({
  class_id: {
    type: String,
    required: true,
  },

  hash: {
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

// TTL index
qrTokenSchema.index({ expires_at: 1 }, { expireAfterSeconds: 0 });

// Fast lookup
qrTokenSchema.index({ class_id: 1, hash: 1 });

module.exports = mongoose.model("QRToken", qrTokenSchema);