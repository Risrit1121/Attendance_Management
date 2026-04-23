require("dotenv").config();
const mongoose = require("mongoose");
const Admin = require("./src/models/Admin");

const initAdmin = async () => {
  try {
    await mongoose.connect(process.env.MONGO_URI);
    console.log("Connected to MongoDB");

    // Check if admin exists
    const existingAdmin = await Admin.findOne({ username: "admin" });
    if (existingAdmin) {
      console.log("Admin user already exists");
      process.exit(0);
    }

    // Create default admin
    await Admin.createAdmin("admin", "adminpass123");

    console.log("✅ Default admin user created:");
    console.log("Username: admin");
    console.log("Password: adminpass123");
    console.log("Please change the password after first login");

    process.exit(0);
  } catch (err) {
    console.error("❌ Error:", err.message);
    process.exit(1);
  }
};

initAdmin();