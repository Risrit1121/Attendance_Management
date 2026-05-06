/**
 * logBuffer.js
 *
 * Maintains an in-memory circular buffer of the last MAX_LINES log lines,
 * capturing output from console.log / console.warn / console.error.
 *
 * Call initLogBuffer() once at server startup (before any routes) so that
 * all subsequent console calls are captured.
 *
 * GET /admin/logs reads from getRecentLogs() — no disk I/O, no shell command.
 */

'use strict';

const MAX_LINES = 500;

// Ring-buffer: array of { ts, level, text }
const buffer = [];

function push(level, args) {
  const text = args
    .map(a => (typeof a === 'string' ? a : JSON.stringify(a)))
    .join(' ');
  const entry = { ts: new Date().toISOString(), level, text };
  buffer.push(entry);
  if (buffer.length > MAX_LINES) buffer.shift();
}

/**
 * Monkey-patch the three console methods we care about.
 * The original implementations are preserved so output still goes to stdout.
 */
function initLogBuffer() {
  const origLog   = console.log.bind(console);
  const origWarn  = console.warn.bind(console);
  const origError = console.error.bind(console);

  console.log = (...args) => { origLog(...args);   push('info',  args); };
  console.warn  = (...args) => { origWarn(...args);  push('warn',  args); };
  console.error = (...args) => { origError(...args); push('error', args); };
}

/**
 * Returns the last `n` log entries (default 150), newest-last.
 */
function getRecentLogs(n = 150) {
  return buffer.slice(-n);
}

module.exports = { initLogBuffer, getRecentLogs };