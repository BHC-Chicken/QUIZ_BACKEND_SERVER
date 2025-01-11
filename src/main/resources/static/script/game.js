window.onload = connect;
window.onbeforeunload = disconnect;

let stompClient;
let subscription;

const RECONNECT_DELAY = 5000;
const MAX_RECONNECT_ATTEMPTS = 10;
let reconnectAttempts = 0;

// WebSocket 연결
function connect() {
    const socket = new SockJS("/game");
    stompClient = Stomp.over(socket);

    const roomId = window.location.pathname.split('/')[2];

    stompClient.connect({"heart-beat": "10000,10000"}, function (frame) {
        reconnectAttempts = 0;

        const startButton = document.getElementById("start-btn");
        const readyButton = document.getElementById("ready-btn");

        if (startButton === null) {
            readyButton.addEventListener("click", () => {
                setupReadyButton(roomId, readyButton)
            });
        } else {
            startButton.addEventListener("click", () => {

            });
        }

        subscription = stompClient.subscribe("/pub/room/" + roomId, function (res) {
            console.log("res: ", res);
            const parseData = JSON.parse(res.body);

            if (parseData.hasOwnProperty('roomId')) {
                updateParticipant(parseData);
            } else {
                handleServerMessage(parseData);
            }
        });

    }, function (error) {
        console.log("WebSocket connection error or disconnected: ", error);

        scheduleReconnect();
    });
}

function setupReadyButton(roomId, ready) {
    const userId = ready.getAttribute("data-user-id");
    stompClient.send(`/room/${roomId}/ready`, {}, JSON.stringify({userId: userId}));
}

function handleServerMessage(message) {
    updateParticipantStatus(message.userId, message.readyStatus);

    if (message.userId === 1) {
        updateGameStatus(message.readyStatus);
    } else {
        // console.warn("Unknown message type:", message);
    }
}

function updateParticipantStatus(userId, status) {
    console.log("updateParticipantStatus called!", userId, status);
    const row = document.querySelector(`tr[data-user-id="${userId}"]`);
    console.log("row: ", row)

    if (!row) {
        console.warn("No row found for userId:", userId);

        return;
    }

    const statusCell = row.querySelector("td:nth-child(3)");

    console.log("statusCell: ", statusCell);
    if (!statusCell) {
        console.warn("No status cell found in row for userId:", userId);

        return;
    }

    statusCell.innerText = status ? "Ready" : "Not Ready";
}

function scheduleReconnect() {
    if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
        console.error(`Max reconnect attempts reached (${MAX_RECONNECT_ATTEMPTS}). Giving up.`);

        return;
    }

    reconnectAttempts++;
    console.log(`Attempting to reconnect in ${RECONNECT_DELAY / 1000} seconds...`);

    setTimeout(() => {
        connect();
    }, RECONNECT_DELAY);
}

function updateParticipant(participant) {
    const participantsTable = document.getElementById("participants");

    console.log("part table: ", participantsTable);
    const existingRow = participantsTable.querySelector(`tr[data-user-id="${participant.id}"]`);
    console.log("existing row: ", existingRow);

    if (existingRow) {
        // 기존 사용자가 있으면 Ready Status만 업데이트
        const readyStatusCell = existingRow.querySelector("td:nth-child(3)");
        readyStatusCell.textContent = participant.readyStatus ? "Ready" : "Not Ready";
        readyStatusCell.classList.toggle("ready-true", participant.readyStatus);
        readyStatusCell.classList.toggle("ready-false", !participant.readyStatus);
    } else {
        // 새 사용자 추가
        const row = document.createElement("tr");
        row.setAttribute("data-user-id", participant.id);

        // User ID
        const idCell = document.createElement("td");
        idCell.textContent = participant.id;
        row.appendChild(idCell);

        // Username
        const usernameCell = document.createElement("td");
        usernameCell.textContent = participant.username;
        row.appendChild(usernameCell);

        // Ready Status
        const readyStatusCell = document.createElement("td");
        readyStatusCell.textContent = participant.readyStatus ? "Ready" : "Not Ready";
        readyStatusCell.classList.add(participant.readyStatus ? "ready-true" : "ready-false");
        row.appendChild(readyStatusCell);

        participantsTable.appendChild(row);
    }
}

function scheduleReconnect() {
    if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
        console.error(`Max reconnect attempts reached (${MAX_RECONNECT_ATTEMPTS}). Giving up.`);

        return;
    }

    reconnectAttempts++;
    console.log(`Attempting to reconnect in ${RECONNECT_DELAY / 1000} seconds...`);

    setTimeout(() => {
        connect();
    }, RECONNECT_DELAY);
}

function disconnect() {
    if (stompClient !== null) {
        if (subscription !== null) {
            subscription.unsubscribe();
        }
        stompClient.disconnect();
    }
}