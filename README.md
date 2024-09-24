# A Java WhatsApp ChatBot Engine

Check out the [documentation available here](https://docs.page/donnc/jawce)

A Template based Java ChatBot engine using the official WhatsApp Cloud API

## About
JAWCE came out as a means to avoid repetitive WhatsApp chatbot developments. 
Every bot i had to create i had to start to define the stages and battle session management on these bot flows or stages and relook at the WhatsApp API spec.
With this in mind, i started creating some sort of "engine" to abstract the repetitive tasks away from the core business logic of my chatbots.

## Architecture
JAWCE has decoupled the core engine from the session. This is because session management is a crucial component and anyone can have their own  implementations.
`jawce-session` comes with some default SessionManager implementations to provide a head-start for an out-of-the-box experience

## Demo
<table>
   <tr>
      <td> Engine Template</td>
      <td> In Action: WhatsApp</td>
   </tr>
   <tr>
      <td><img width="320" src="/docs/assets/templates.png"></td>
      <td><video width="320" height="640" src="https://github.com/DonnC/jawce/assets/47761288/f1c9754e-5f29-455e-ba57-54cf7338286b"></td>
   </tr>
</table>

You can use any other language / framework of choice for your chatbot logic. The engine supports REST API based hooks so you can run this engine entirely separate from your logic.


## Example Bot
Check out the [Python ChatBot](https://github.com/DonnC/py-jawce-chatbot) template developed and running using this engine.
