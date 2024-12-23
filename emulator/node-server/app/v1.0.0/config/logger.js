const winston = require("winston");


let date = new Date().toISOString();
const logFormat = winston.format.printf(function(info) {
    return `${date}-${info.level}: ${JSON.stringify(info.message, null, 4)}\n`;
});
const logger = winston.createLogger({
    transports: [
        new winston.transports.Console({
            level: "debug",
            format: winston.format.combine(winston.format.colorize(), logFormat)
        }),
        new winston.transports.File({
            filename: '../logs/error.log',
            level: 'error',
            format: winston.format.combine(winston.format.colorize(), logFormat)
        }),
        new winston.transports.File({
            filename: '../logs/info.log',
            level: 'info',
            format: winston.format.combine(winston.format.colorize(), logFormat)
        })
    ]
});

module.exports = logger;

