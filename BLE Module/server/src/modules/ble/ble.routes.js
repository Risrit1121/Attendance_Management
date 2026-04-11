const express = require("express");
const router = express.Router();

const { generateMinor, validate } = require("./ble.controller");

router.post("/generate-minor", generateMinor);
router.post("/validate", validate);   // ✅ ADD THIS

module.exports = router;