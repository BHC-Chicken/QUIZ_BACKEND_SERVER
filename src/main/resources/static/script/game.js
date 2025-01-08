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
        subscription = stompClient.subscribe("/room/" + roomId, function (res) {
            console.log(res);
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