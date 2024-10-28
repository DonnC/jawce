# WhatsApp Local Emulator
This emulator was initially initiated by [@Donald C](https://www.linkedin.com/in/donchinhuru/) 
and a big shout-out to [@Danai Munjemu](https://www.linkedin.com/in/danai-munjemu/) 
for much of the emulator server and mobile preview work.


!!! This is a heavy WIP project !!!

## Getting Started
The emulator has been developed with Express (v4.19.x) for the server and Angular (v16.2.x) with tailwind for the mobile preview

To get started, install project dependencies
```bash
# install mobile client dependencies
cd mobile-preview
npm i

# install server dependencies
cd node-server
npm i
```

The emulator currently supports the following message types:
1. Text
2. Button
3. List

## Running
First verify that your chatbot url is configured properly in the `node-server/.env` file

In your JAWCE / WhatsApp chatbot, make sure you enabled local emulator
```yaml
channel-config:
  test-local: true
  local-ip: "http://localhost:${server.port}"
  local-url: "http://localhost:3000/api/hook-response"
```
Run your local chatbot project

### Run emulator server
```bash
cd node-server
nodemon index.js
```

### Run emulator client
```bash
cd mobile-preview
ng serve
```
