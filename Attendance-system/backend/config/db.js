require('dotenv').config();
const mongoose = require('mongoose');

const connectDB = async () => {
  try {
    // Use MONGO_URI (set in docker-compose) or fall back to MONGODB_URI for local dev
    const mongoUri = process.env.MONGO_URI || process.env.MONGODB_URI;
    if (!mongoUri) {
      throw new Error('MONGO_URI or MONGODB_URI environment variable not set');
    }
    await mongoose.connect(mongoUri, {
      useNewUrlParser: true,
      useUnifiedTopology: true,
    });
    console.log('MongoDB connected successfully');
  } catch (error) {
    console.error('MongoDB connection failed:', error);
    process.exit(1);
  }
};

module.exports = connectDB;
