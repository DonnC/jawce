const MessengerController = require("./../controller/messenger.controller");

module.exports = (app) => {
    app.post("/api/hook-response111", MessengerController.hookResponse);
}
