const AdminBro = require('admin-bro')
const AdminBroExpress = require('@admin-bro/express')

const express = require("express");
const app = express();
const server = require("http").createServer(app);
const logger = require("./app/v1.0.0/config/logger");
const  SubscriptionSecurityUtil = require("./app/v1.0.0/config/SubscriptionSecurityUtil");

var bodyParser = require("body-parser");
const port = process.env.PORT || 3000;
var cors = require("cors");


const socketIo = require('socket.io')
const io = socketIo(server, {
    cors: {
        origin: '*',
    }
})




// parse application/json
app.use(
    bodyParser.json({
        limit: "100mb",
        extended: true,
        parameterLimit: 100000000,
    })
);

// parse application/x-www-form-urlencoded
app.use(
    bodyParser.urlencoded({
        limit: "100mb",
        extended: true,
        parameterLimit: 100000000,
    })
);


require("dotenv").config();
const path = require('path');
const axios = require("axios");
const RecipientEnum = require("./app/v1.0.0/modules/whatsapp/enums/recipient.enum");
const MessageTypeEnum = require("./app/v1.0.0/modules/whatsapp/enums/message-type.enum");
const {getCache, setCache} = require("./app/v1.0.0/modules/whatsapp.cache");
const crypto = require("crypto");
const {request} = require("axios");



//folder for uploads
app.use(express.static(__dirname + "/public"));
app.use('/images', express.static(path.join('images')));
app.use(cors());


// Enable CORS for all HTTP methods
app.use(function(req, res, next) {
    res.header("Access-Control-Allow-Origin", req.get("origin"));
    res.header("Access-Control-Allow-Methods", "GET, PUT, POST, DELETE");
    res.header(
        "Access-Control-Allow-Headers",
        "Origin, X-Requested-With, Content-Type, Accept, Authorization"
    );
    next();
});


app.post('/api/hook-response', (req, res) => {
    generateRequestTimeLog('START OF REQUEST')

    sendMessage()

    const body = req.body;
    console.log(req.body)
    try {
        logger.info(`REQUEST RECEIVED FROM: ${(req.hostname).toUpperCase()} OF TYPE ${(body.type).toUpperCase()}`);

        // Basic validation
        if (!body || !body.type) {
            return res.status(400).send({ error: 'Invalid request' });
        }

        switch (body.type) {
            case 'text':
                handleTextMessage(body);
                break;
            case 'image':
                handleImageMessage(body);
                break;
            case 'document':
                handleDocumentMessage(body);
                break;
            case 'interactive':
                if (body.interactive.type === 'list') {
                    handleListInteractiveMessage(body);
                } else if (body.interactive.type === 'button') {
                    handleButtonInteractiveMessage(body);
                } else if (body.interactive.type === 'flow') {
                    handleFlowInteractiveMessage(body);
                } else {
                    return res.status(400).send({ error: 'Unknown interactive type' });
                }
                break;
            default:
                generateRequestTimeLog('END OF REQUEST - FAILED')
                return res.status(400).send({ error: 'Unknown message type' });
        }


        // Send a response to the client indicating success

        let response = {
            "messaging_product": "whatsapp",
            "contacts": [
                {
                    "input": body.to,
                    "wa_id": body.to
                }
            ],
            "messages": [
                {
                    "id": generateUniqueId()
                }
            ]
        }
        generateRequestTimeLog('END OF REQUEST - SUCCESS');
        res.send(response);
    } catch (error) {
        // If there's an error, handle it
        logger.error("Error receiving request: " + error)

        // Send a response to the client indicating failure
        res.status(500).send('Error sending request to the other server.');
        generateRequestTimeLog('END OF REQUEST - FAILED')
    }
});

app.post('/api/subscribe', (req, res) => {
    generateRequestTimeLog('START OF SUBSCRIBE REQUEST')

    const body = req.body;

    console.log(req.body)

    console.log(req.headers);

    let sharedServiceKey = "01919dd0-022c-7f64-9ac2-ab11ba3984e1";

    let tokenHash = "HvwMJSU+aLNm0AJ5K/BDByXsRM4tpjOqnHFpYy3LL08=";

    try {
        // TODO: decrypt data, get user token, encrypt, generate hash and return hash
        var decryptedData = SubscriptionSecurityUtil.decrypt(body.data, sharedServiceKey);

        console.log(decryptedData);

        generateRequestTimeLog('END OF SUBSCRIBE REQUEST - SUCCESS');

        res.send(tokenHash);
    } catch (error) {
        // If there's an error, handle it
        logger.error("Error receiving request: " + error)

        // Send a response to the client indicating failure
        res.status(500).send('Error sending request to the other server.');
        generateRequestTimeLog('END OF SUBSCRIBE REQUEST - FAILED')
    }
});

async function handleTextMessage(body) {
    console.log('Handling text message:', body);
    let message = {
        id: generateMessageId(),
        time: generateTimestamp(),
        recipient: RecipientEnum.MOBILE_PHONE,
        type: MessageTypeEnum.TEXT,
        content: {
            text: body.text.body,
        }
    }
    sendWebhookResponse(message)
    saveMessage(message);
}

async function handleImageMessage(body) {
    console.log('Handling image message:', body);
    let message = {
        id: generateMessageId(),
        time: generateTimestamp(),
        recipient: RecipientEnum.MOBILE_PHONE,
        type: MessageTypeEnum.IMAGE,
        content: {
            link: body.image.link,
        }
    }

    sendWebhookResponse(message)
    saveMessage(message);
}

async function handleDocumentMessage(body) {
    console.log('Handling document message:', body);
    let message = {
        id: generateMessageId(),
        time: generateTimestamp(),
        recipient: RecipientEnum.MOBILE_PHONE,
        type: MessageTypeEnum.DOCUMENT,
        content: {
            link: body.document.link,
            caption: body.document.caption,
            filename: body.document.filename
        }
    }

    sendWebhookResponse(message)
    saveMessage(message);
}

async function handleListInteractiveMessage(body) {
    console.log('Handling list interactive message:', body);
    let message = {
        id: generateMessageId(),
        time: generateTimestamp(),
        recipient: RecipientEnum.MOBILE_PHONE,
        type: MessageTypeEnum.LIST,
        content: {
            // Include header only if it exists in the body
            ...(body.interactive.header && { header: body.interactive.header.text }),
            body: body.interactive.body.text,
            // Include footer only if it exists in the body
            ...(body.interactive.footer && { footer: body.interactive.footer.text }),
            button: body.interactive.action.button,
            sections: body.interactive.action.sections
        }
    }

    sendWebhookResponse(message)
    saveMessage(message);
}

async function handleButtonInteractiveMessage(body) {
    console.log('Handling button interactive message:', body);
    let message = {
        id: generateMessageId(),
        time: generateTimestamp(),
        recipient: RecipientEnum.MOBILE_PHONE,
        type: MessageTypeEnum.BUTTON,
        content: {
            button: body.interactive.body.text,
            buttons: body.interactive.action.buttons
        }
    }

    sendWebhookResponse(message)
    saveMessage(message);
}

async function handleFlowInteractiveMessage(body) {
    console.log('Handling flow interactive message:', body);
    let message = {
        id: generateMessageId(),
        time: generateTimestamp(),
        recipient: RecipientEnum.MOBILE_PHONE,
        type: MessageTypeEnum.BUTTON,
        content: {
            header: body.interactive.header.text,
            body: body.interactive.body.text,
            footer: body.interactive.footer.text,
            button: body.interactive.action.button,
            flow: body.interactive.action
        }
    }

    sendWebhookResponse(message)
    saveMessage(message);
}

function saveMessage(message) {
    // Fetch existing messages from cache
    let messages = getCache('messages') || [];

    // Append the new message
    messages.push(message);

    // Update the cache with the new list of messages
    setCache('messages', messages);

    console.log('Messages:', messages);
}

function generateUniqueId() {
    // Generate 32 random bytes
    const randomBytes = crypto.randomBytes(24); // Adjust the number of bytes if needed

    // Convert to base64 string
    const base64String = randomBytes.toString('base64');

    // Replace URL-unsafe characters to make it URL-safe (optional)
    const safeBase64String = base64String.replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');

    // Combine with prefix
    const uniqueId = `wamid.${safeBase64String}`;

    return uniqueId;
}

function generateRequestTimeLog(message) {
    console.log("");
    console.log('<' + '-'.repeat(40) + ' ' + message + ' - ' + generateTimestamp() + ' ' + '-'.repeat(40) + '>');
    console.log("");
}

function generateTimestamp() {
    const currentTime = new Date();
    return currentTime.toLocaleString('en-US', {
        weekday: 'short',
        month: 'short',
        day: '2-digit',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit'
    });
}





// Handle two way communication
io.on('connection', (socket) => {
    console.log('a user connected');

    // Receiving a message from the client
    socket.on('messageFromClient', (message) => {
        console.log('Message from client:', message);
        sendWhatsappWebhook(message)
    });

    // Sending a message to the client
    socket.emit('messageFromServer', 'Hello from the server!');

    socket.on('disconnect', () => {
        console.log('a user disconnected');
    });
});


function sendMessage(){
    io.emit('messageFromServer', 'we acknowledge your message')
}

function sendWebhookResponse(response) {
    io.emit('responseFromWebhook', response)
}

function sendWhatsappWebhook(message) {
    let postData =
        {
            "object": "whatsapp_business_account",
            "entry": [
                {
                    "id": "88569968194134432",
                    "changes": [
                        {
                            "value": {
                                "messaging_product": "whatsapp",
                                "metadata": {
                                    "display_phone_number": "16505553333",
                                    "phone_number_id": "263780728704"
                                },
                                "contacts": [
                                    {
                                        "profile": {
                                            "name": "Kerry Fisher"
                                        },
                                        "wa_id": "263780728704"
                                    }
                                ],
                                "messages": [
                                    {
                                        "from": "16315551234",
                                        "id": "wamid." + generateWhatsAppTimestamp(),
                                        "timestamp": generateWhatsAppTimestamp(),
                                        ...message
                        }
                    ]
                },
                "field": "messages"
                        }
                        ]
                }
                ]
        }

        console.log(JSON.stringify(postData))

    const headers = {
        'x-hub-signature-256': 'dsagsfgfsa',
    };
    try {

        const response = axios.post(process.env.WHATSAPP_ENGINE_URL, postData, {headers: headers});
        console.log('Response:', response.data);
    } catch (error) {
        console.error('Error:', error.message);
    }
}

function generateWhatsAppTimestamp() {
    const now = Date.now();
    return  Math.floor(now / 1000);
}

function generateMessageId() {
    return Math.floor(1000 + Math.random() * 9000);
}



// hookup routes
require("./app/v1.0.0/modules/whatsapp/route/index")(app);


let run = async() => {
    const adminBro = new AdminBro({
        databases: [],
        rootPath: '/admin',
    });
    const router = AdminBroExpress.buildRouter(adminBro)
    app.use(adminBro.options.rootPath, router)

    server.listen(port, () => {
        console.log(`app listening at http://localhost:${port}`);
    });
};

run();

