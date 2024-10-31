const Messenger = require("../model/messenger.model")
const logger = require("./../../../config/logger")
const crypto = require('crypto')
const RecipientEnum = require('../enums/recipient.enum')
const MessageTypeEnum = require('../enums/message-type.enum')
const { getCache, setCache } = require('./../../whatsapp.cache')

exports.hookResponse = async (req, res) => {
    generateRequestTimeLog('START OF REQUEST')



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
                await handleTextMessage(body);
                break;
            case 'image':
                await handleImageMessage(body);
                break;
            case 'document':
                await handleDocumentMessage(body);
                break;
            case 'interactive':
                if (body.interactive.type === 'list') {
                    await handleListInteractiveMessage(body);
                } else if (body.interactive.type === 'button') {
                    await handleButtonInteractiveMessage(body);
                } else if (body.interactive.type === 'flow') {
                    await handleFlowInteractiveMessage(body);
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
};


// Handler functions for different message types

async function handleTextMessage(body) {
    console.log('Handling text message:', body);
    let message = {
        time: generateTimestamp(),
        recipient: RecipientEnum.MOBILE_PHONE,
        type: MessageTypeEnum.TEXT,
        content: {
            text: body.text.body,
        }
    }

    saveMessage(message);

}

async function handleImageMessage(body) {
    console.log('Handling image message:', body);
    let message = {
        time: generateTimestamp(),
        recipient: RecipientEnum.MOBILE_PHONE,
        type: MessageTypeEnum.TEXT,
        content: {
            link: body.image.link,
        }
    }

    saveMessage(message);
}

async function handleDocumentMessage(body) {
    console.log('Handling document message:', body);
    let message = {
        time: generateTimestamp(),
        recipient: RecipientEnum.MOBILE_PHONE,
        type: MessageTypeEnum.TEXT,
        content: {
            link: body.document.link,
            caption: body.document.caption,
        }
    }

    saveMessage(message);
}

async function handleListInteractiveMessage(body) {
    console.log('Handling list interactive message:', body);
    let message = {
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

    saveMessage(message);
}

async function handleButtonInteractiveMessage(body) {
    console.log('Handling button interactive message:', body);
    let message = {
        time: generateTimestamp(),
        recipient: RecipientEnum.MOBILE_PHONE,
        type: MessageTypeEnum.BUTTON,
        content: {
            button: body.interactive.body.text,
            buttons: body.interactive.action.buttons
        }
    }

    saveMessage(message);
}

async function handleFlowInteractiveMessage(body) {
    console.log('Handling flow interactive message:', body);
    let message = {
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
