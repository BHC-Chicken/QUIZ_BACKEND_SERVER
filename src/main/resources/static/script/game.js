window.onload = connect;
window.onbeforeunload = disconnect;

let stompClient;
let subscription;

document.addEventListener("DOMContentLoaded", function () {
    connect();
    //setupReadyButtons();
});

// WebSocket 연결
function connect() {
    const socket = new SockJS("/game");
    stompClient = Stomp.over(socket);

    const roomId = document.getElementById("roomId").textContent.trim();
    const isAdmin = document.getElementById("isAdmin").textContent.trim() === "true";

    stompClient.connect({}, function (frame) {
        console.log("Connected to WebSocket:", frame);

        // 참가자 실시간 업데이트 구독
        stompClient.subscribe(`/pub/room/${roomId}/participants`, function (res) {
            const participants = JSON.parse(res.body);
            console.log("Received participants update:", participants);
            console.log("admin state is {}", isAdmin);
            updateParticipants(participants, isAdmin, roomId);
        });

        // Ready 버튼 설정
        setupReadyButtons(roomId);

        // Start 버튼 설정 (Admin 전용)
        if (isAdmin) {
            setupStartButtons(roomId);
        }
    });
}

// WebSocket 연결 해제
function disconnect() {
    if (stompClient) {
        stompClient.disconnect();
        console.log("Disconnected from WebSocket");
    }
}

function setupReadyButtons(roomId) {
    console.log("room id is :", roomId);
    const readyButtons = document.querySelectorAll(".ready-btn");
    readyButtons.forEach((btn) => {
        btn.addEventListener("click", function () {
            const userId = btn.getAttribute("data-user-id");

            // 서버에 Ready 상태 변경 요청
            stompClient.send(`/room/${roomId}/ready`, {}, JSON.stringify({ userId: userId }));
            console.log("Sent ready toggle for user:", userId);
        });
    });

    // 서버로부터 메시지 받기
    stompClient.subscribe(`/pub/room/${roomId}`, function (res) {
        const responseMessage = JSON.parse(res.body);
        console.log("Received message from server:", responseMessage);

        // 원하는 값 출력
        console.log("Ready Status:", responseMessage.readyStatus);
        console.log("User ID:", responseMessage.userId);
        console.log("Role:", responseMessage.role);
        console.log("Email:", responseMessage.email);
    });
}

// function handleServerMessage(message) {
//     if (message.participants) {
//         console.log("Updating participants list:", message.participants);
//         updateParticipants(message.participants, message.isAdmin); // 서버에서 전체 목록을 받는다고 가정
//     } else {
//         console.warn("Received unknown message format:", message);
//     }
// }

// Start 버튼 설정 (Admin 전용)
function setupStartButtons(roomId) {
    const startButtons = document.querySelectorAll(".start-btn");
    startButtons.forEach((btn) => {
        btn.addEventListener("click", function () {
            const userId = btn.getAttribute("data-user-id");
            stompClient.send(`/room/${roomId}/start`, {}, JSON.stringify({ userId: userId }));
            console.log("Sent start game request by user:", userId);
        });
    });
}

function updateGameStatus(status) {
    const gameStatusElement = document.getElementById("gameStatus");
    gameStatusElement.innerText = status ? "Started" : "Not Started";
}

function updateParticipants(participants, isAdmin, roomId) {
    const participantsTable = document.getElementById("participants");
    participantsTable.innerHTML = ""; // 기존 목록 초기화

    participants.forEach(participant => {
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
        readyStatusCell.textContent = participant.isReadyStatus ? "Ready" : "Not Ready";
        readyStatusCell.classList.add(participant.isReadyStatus ? "ready-true" : "ready-false");
        row.appendChild(readyStatusCell);

        // Actions
        const actionsCell = document.createElement("td");

        if (participant.role === "USER" && !isAdmin) {
            const readyBtn = document.createElement("button");
            readyBtn.textContent = "Toggle Ready";
            readyBtn.classList.add("ready-btn");
            readyBtn.setAttribute("data-user-id", participant.id);
            actionsCell.appendChild(readyBtn);
        }

        if (participant.role === "ADMIN" && isAdmin) {
            const startBtn = document.createElement("button");
            startBtn.textContent = "Start Game";
            startBtn.classList.add("start-btn");
            startBtn.setAttribute("data-user-id", participant.id);
            actionsCell.appendChild(startBtn);
        }

        row.appendChild(actionsCell);
        participantsTable.appendChild(row);
    });

    // 이벤트 리스너 재설정
    setupReadyButtons(roomId);
    if (isAdmin) {
        setupStartButtons(roomId);
    }
}