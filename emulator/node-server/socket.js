const service = {}

const server = require('http').Server()
const io = require('socket.io')(server, {
    cors: {
        origins: ['http://localhost:4200']
    }
});


service.inicializar = () => {
    io.on('connection', (socket) => {
        const idHandShake = socket.id;
        const { email } = socket.handshake.query;

        socket.join(email);

        console.log(`Conexion establecida --> ${idHandShake}`);

        //Este metodo escucha lo que envia el front y tiene la capacidad de emitir hacia otros miembros de la sala.
        socket.on('event', (res) => {
            const data = res
            console.log(data)

            //Envia un mensaje a todos los participantes del room
            socket.to(email).emit('event', data);
        })
    })
    return io;
}


service.emitEvent = async (email, mensaje) => {
    const sockets = await io.in(email).fetchSockets();
    sockets[0].emit('messageFromServer', mensaje);
}
service.emitSesionIniciada = async (email, mensaje) => {
    console.log('email de session iniciada: ', email);
    const sockets = await io.in(email).fetchSockets();
    sockets[0].emit('sessionIniciada', mensaje);
}

module.exports = service;
