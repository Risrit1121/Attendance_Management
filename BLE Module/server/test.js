require("dotenv").config();
const mongoose = require("mongoose");
const ClassMapping = require("./src/models/ClassMapping");

mongoose.connect(process.env.MONGO_URI).then(async () => {
  console.log("Connected");

  await ClassMapping.create({
    class_id: "CS101",
    majors: [
      { major_id: "abc123", rssi_threshold: -75 },
      { major_id: "def456", rssi_threshold: -80 },
    ],
  });

  console.log("Inserted sample data");

  process.exit();
});