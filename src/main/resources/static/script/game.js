window.onbeforeunload = disconnect;

let stompClient;
let subscription;

document.addEventListener("DOMContentLoaded", function() {
    connect();
    // 준비 버튼
    setupReadyButton();
});

// WebSocket 연결
function connect() {
    const socket = new SockJS("/game");
    stompClient = Stomp.over(socket);

    const roomId = document.getElementById("roomId").textContent.trim();
    const isAdmin = document.getElementById("isAdmin").textContent.trim() === "true";

    stompClient.connect({}, function (frame) {
        console.log("Connected to WebSocket:", frame);

        roomId = window.location.pathname.split('/')[2];
        console.log("roomId is ", roomId);

        // 구독 한번만 진행
        subscription = stompClient.subscribe(`/pub/room/${roomId}`, function (res) {
            const participant = JSON.parse(res.body);
            console.log("participant is ", participant);

            // 이미 사용자 존재
            if(participant.hasOwnProperty("readyStatus")) {
                handleServerMessage(participant);
            }
            // 처음 입장
            else {
                console.log("Received participants update:", participant);
                updateParticipant(participant);
            }
        });

    });
}

function setupReadyButton() {
    const readyButton = document.getElementById("ready-btn");
    if (!readyButton) return;

    readyButton.addEventListener("click", function() {
        const userId = readyButton.getAttribute("data-user-id");
        console.log("Clicked ready, userId:", userId);

        // 오직 send만, 구독은 재호출하지 않음
        stompClient.send(`/room/${roomId}`, {}, JSON.stringify({ userId: userId }));
        console.log("Sent ready toggle for user:", userId);
    });
}

function disconnect() {
    if (stompClient) {
        stompClient.disconnect();
        console.log("Disconnected from WebSocket");
    }
}

function handleServerMessage(message) {
    // 서버에서 { type: "readyStatus", userId: 123, status: true/false } 라고 보냄 가정
    updateParticipantStatus(message.userId, message.readyStatus);
    if (message.userId === 1) {
        updateGameStatus(message.readyStatus);
    } else {
        console.warn("Unknown message type:", message);
    }
}

function updateParticipantStatus(userId, status) {
    console.log("updateParticipantStatus called!", userId, status);
    const row = document.querySelector(`[data-user-id="${userId}"]`);

    if (!row) {
        console.warn("No row found for userId:", userId);
        return;
    }
    //2) row 자체가 tr이라고 가정
    const statusCell = row.querySelector("td:nth-child(3)");
    console.log(statusCell);
    if (!statusCell) {
        console.warn("No status cell found in row for userId:", userId);
        return;
    }

    // 3) Ready/Not Ready 갱신
    statusCell.innerText = status ? "Ready" : "Not Ready";
    console.log("Updated statusCell for userId:", userId, "to:", status);
}

function updateGameStatus(status) {
    const gameStatusElement = document.getElementById("gameStatus");
    gameStatusElement.innerText = status ? "Started" : "Not Started";
}

function updateParticipant(participant) {
    const participantsTable = document.getElementById("participants");

    // 기존 사용자 확인
    const existingRow = participantsTable.querySelector(`tr[data-user-id="${participant.id}"]`);

    console.log();
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

        // Actions (Optional: Ready 버튼 추가)
        const actionsCell = document.createElement("td");
        const readyBtn = document.createElement("button");
        readyBtn.textContent = "Toggle Ready";
        readyBtn.classList.add("ready-btn");
        readyBtn.setAttribute("data-user-id", participant.id);
        actionsCell.appendChild(readyBtn);
        row.appendChild(actionsCell);

        participantsTable.appendChild(row);
    }
}