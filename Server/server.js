const express = require('express');
http = require('http');
const mysql = require('mysql');
app = express();
server = http.createServer(app);
io = require('socket.io').listen(server);
app.get('/', (req, res) => {


    res.send('Chat Server is running on port 3000')
});

// create connection to database
const db = mysql.createConnection({
    host: 'localhost',
    user: 'root',
    password: '',
    database: 'socka'
});

// connect to database
db.connect((err) => {
    if (err) {
        throw err;
    }
    console.log('Connected to database');
});
global.db = db;
global.position = 1;
io.on('connection', (socket) => {

    console.log('user connected')

    Colors = {};
    Colors.names = {
        azure: "#f0ffff",
        beige: "#f5f5dc",
        black: "#000000",
        blue: "#0000ff",
        brown: "#a52a2a",
        cyan: "#00ffff",
        darkblue: "#00008b",
        darkcyan: "#008b8b",
        darkgrey: "#a9a9a9",
        darkkhaki: "#bdb76b",
        darkmagenta: "#8b008b",
        darkolivegreen: "#556b2f",
        darkorange: "#ff8c00",
        darkorchid: "#9932cc",
        darkred: "#8b0000",
        darksalmon: "#e9967a",
        darkviolet: "#9400d3",
        fuchsia: "#ff00ff",
        gold: "#ffd700",
        green: "#008000",
        indigo: "#4b0082",
        khaki: "#f0e68c",
        lime: "#00ff00",
        magenta: "#ff00ff",
        maroon: "#800000",
        navy: "#000080",
        olive: "#808000",
        orange: "#ffa500",
        pink: "#ffc0cb",
        purple: "#800080",
        violet: "#800080",
        red: "#ff0000",
        silver: "#c0c0c0",
    };

    function getRandomColor() {
        var result;
        var count = 0;
        for (var prop in Colors.names)
            if (Math.random() < 1 / ++count)
                result = prop;
        return result;
    };

    socket.on('join', function (userNickname) {

        let usernameQuery = "SELECT * FROM `players` where user_name like '" + userNickname + "' ";
        db.query(usernameQuery, (err, result) => {
            if (err) {
                throw err;
            }
            /* check if username exists in db */
            if (result.length > 0) {
                console.log("username " + userNickname + " already exists");

                let msgQuery = "SELECT * FROM `messages`";
                db.query(msgQuery, (err, msg_result) => {
                    if (err) {
                        throw err;
                    }
                    /* check if username exists in db */
                    console.log("length " + msg_result.length);

                    if (msg_result.length > 0) {

                        for (let index = 0; index < msg_result.length; index++) { 
                            const msg = msg_result[index].message;
                            const sender = msg_result[index].sender;

                            let getColourQuery = "SELECT `color` FROM `players` where user_name like '" + sender + "' ";
                            db.query(getColourQuery, (err, color_result) => {
                                if (err) {
                                    throw err;
                                }
                                let message = { "message_from_past": msg, "senderNickname": sender, "color": Colors.names[color_result[0].color], "manshow": userNickname};
                                // send the json to the client who was offline
                                io.emit('message_from_past_event', message);
    
                            });

                        }

                    }
                });

            } else {

                let color = getRandomColor();
                let number = 1;

                let msgQuery = "SELECT color,user_name FROM `players`";
                db.query(msgQuery, (err, colors_result) => {
                    if (err) {
                        throw err;
                    }
                    console.log("length " + colors_result.length);

                    if (colors_result.length > 0) {

                        for (let index = 0; index < colors_result.length; index++) { 
                            const color = colors_result[index].color;
                            const sender = colors_result[index].user_name;

                            let message = { "colors_update": Colors.names[color], "senderNickname": sender, "manshow": sender};
                            // send the json to the client who was offline
                            io.emit('colors_update_event', message);
                        }

                    }
                });
                
                usernameQuery = "INSERT INTO `players`(`user_name`, `color`, `position`) VALUES ('" + userNickname + "', '" + color + "', '" + number + "')";

                // usernameQuery = "INSERT INTO `players`(`user_name`, `color`, `position`) VALUES ('" + userNickname + "', '" + color + "')";
                db.query(usernameQuery, (err, result) => {
                    if (err) {
                        throw err;
                    }
                });
                

                console.log(userNickname + " : has joined the chat " /* +Colors.names[color] */);

                let message = { "senderNickname": userNickname, "random_color": Colors.names[color] }
                io.emit('userjoinedthechat', message);
            }

        });


    });


    socket.on('messagedetection', (senderNickname, messageContent) => {


        console.log(senderNickname + " :" + messageContent);
        //create a json
        let message = { "message": messageContent, "senderNickname": senderNickname }

        //add in db
        usernameQuery = "INSERT INTO `messages`(`message`, `position`, `sender`) VALUES ('" + messageContent + "', '" + global.position + "', '" + senderNickname + "')";
        db.query(usernameQuery, (err, result) => {
            if (err) {
                throw err;
            }
        });
        global.position++;

        // send the json to the client side  
        io.emit('message', message);

    });


    socket.on('disconnected', function (userNickname) {
        console.log(' user ' + userNickname + ' has left ');
        io.emit("userdisconnect", " user " + userNickname + " has left ");

    });



});



server.listen(3000, () => {

    console.log('Node app is running on port 3000');

});
