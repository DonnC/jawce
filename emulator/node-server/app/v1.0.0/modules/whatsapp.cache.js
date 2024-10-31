// cache.js
const NodeCache = require('node-cache');
const myCache = new NodeCache();

// Function to fetch data from the cache
function getCache(key) {
    const value = myCache.get(key);
    if (value) {
        console.log(`Cache hit for key: ${key}`);
    } else {
        console.log(`Cache miss for key: ${key}`);
    }
    return value;
}

// Function to update the cache
function setCache(key, value, ttl = 3600) { // default TTL of 1 hour
    myCache.set(key, value, ttl);
    console.log(`Cache updated for key: ${key}`);
}

module.exports = {
    getCache,
    setCache,
};
