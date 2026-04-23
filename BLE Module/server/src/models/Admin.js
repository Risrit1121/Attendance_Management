const mongoose = require("mongoose");
const bcrypt = require("bcrypt");

const adminSchema = new mongoose.Schema({
  username: {
    type: String,
    required: true,
    unique: true,
  },
  password: {
    type: String,
    required: true,
  },
});

adminSchema.methods.comparePassword = async function (password) {
  return bcrypt.compare(password, this.password);
};

adminSchema.statics.createAdmin = async function (username, password) {
  const hashedPassword = await bcrypt.hash(password, 10);
  return this.create({ username, password: hashedPassword });
};

module.exports = mongoose.model("Admin", adminSchema);