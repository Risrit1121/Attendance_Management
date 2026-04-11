const mongoose = require("mongoose");

const majorSchema = new mongoose.Schema({
  major_id: String,
  rssi_threshold: Number,
});

const classMappingSchema = new mongoose.Schema({
  class_id: {
    type: String,
    required: true,
    unique: true,
  },

  majors: [majorSchema],
});

module.exports = mongoose.model("ClassMapping", classMappingSchema);