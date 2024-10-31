const crypto = require('crypto');

class SubscriptionSecurityUtil {
    static ALGORITHM = 'aes-128-cbc';
    static MSG_ALGORITHM = 'sha256';

    static generateValidAESKey(sharedKey) {
        const hash = crypto.createHash(SubscriptionSecurityUtil.MSG_ALGORITHM);
        hash.update(sharedKey, 'utf8');
        const key = hash.digest();
        // Use first 16 bytes for AES-128
        return key.slice(0, 16);
    }

    static encrypt(data, sharedKey) {
        const key = SubscriptionSecurityUtil.generateValidAESKey(sharedKey);
        const iv = crypto.randomBytes(16); // Initialization vector
        const cipher = crypto.createCipheriv(SubscriptionSecurityUtil.ALGORITHM, key, iv);
        let encrypted = cipher.update(data, 'utf8', 'base64');
        encrypted += cipher.final('base64');
        return `${iv.toString('base64')}:${encrypted}`;
    }

    static decrypt(encryptedData, sharedKey) {
        const key = SubscriptionSecurityUtil.generateValidAESKey(sharedKey);
        const [ivBase64, encrypted] = encryptedData.split(':');
        const iv = Buffer.from(ivBase64, 'base64');
        const decipher = crypto.createDecipheriv(SubscriptionSecurityUtil.ALGORITHM, key, iv);
        let decrypted = decipher.update(encrypted, 'base64', 'utf8');
        decrypted += decipher.final('utf8');
        return decrypted;
    }

    static generateHash(data) {
        const hash = crypto.createHash(SubscriptionSecurityUtil.MSG_ALGORITHM);
        hash.update(data, 'utf8');
        return hash.digest('base64');
    }

    static verifyHash(data, receivedHash) {
        const generatedHash = SubscriptionSecurityUtil.generateHash(data);
        return generatedHash === receivedHash;
    }
}

module.exports = SubscriptionSecurityUtil;
