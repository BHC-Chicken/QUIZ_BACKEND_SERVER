window.onload = connect;
window.onbeforeunload = disconnect;

let stompClient;
let subscription;


document.addEventListener("DOMContentLoaded", function() {
    connect();
    setupReadyButtons();
});

function connect() {
    let socket = new SockJS("/game");
    stompClient = Stomp.over(socket);

    stompClient.connect({}, function (frame) {
        console.log("Connected to WebSocket:", frame);

        let roomId = window.location.pathname.split('/')[2];
        subscription = stompClient.subscribe(`/pub/room/${roomId}/participants`, function (res) {
            console.log(res);
            const participant = JSON.parse(res.body);
            console.log("Received participants update:", participant);
            updateParticipant(participant);
        });
    });
}

function disconnect() {
    if (stompClient !== null) {
        if (subscription !== null) {
            subscription.unsubscribe();
        }
        stompClient.disconnect();
    }
}

function setupReadyButtons() {
    const readyButtons = document.querySelectorAll(".ready-btn");
    readyButtons.forEach((btn) => {
        btn.addEventListener("click", function() {
            const userId = btn.getAttribute("data-user-id");
            const roomId = window.location.pathname.split('/')[2];

            subscription = stompClient.subscribe("/pub/" + roomId, function (res) {
                console.log(res);
                const message = JSON.parse(res.body);
                console.log("user Id is {}", message.userId);
                console.log("readyStatus is {}", message.readyStatus);
                handleServerMessage(message);
            });

            // 1) 서버로 메시지 전송
            // 서버 @MessageMapping("/{id}")와 매칭 → 최종 경로 "/room/{id}"
            stompClient.send(`/room/${roomId}`, {}, JSON.stringify({ userId: userId }));

            console.log("Sent ready toggle for user:", userId);
        });
    });
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
    if (row) {
        // Ready/Not Ready가 표시된 셀(3번째 열) 업데이트
        const statusCell = row.closest("tr").querySelector("td:nth-child(3)");
        if (statusCell) {
            statusCell.innerText = status ? "Ready" : "Not Ready";
        }
    }
}

function updateGameStatus(status) {
    const gameStatusElement = document.getElementById("gameStatus");
    gameStatusElement.innerText = status ? "Started" : "Not Started";
}

function updateParticipant(participant) {
    const participantsTable = document.getElementById("participants");

    // 기존 사용자 확인
    const existingRow = participantsTable.querySelector(`tr[data-user-id="${participant.id}"]`);

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