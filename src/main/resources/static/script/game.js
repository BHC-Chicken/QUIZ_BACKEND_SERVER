// /script/game.js

window.onload = connect;
window.onbeforeunload = disconnect;

let stompClient;
let subscription;

// WebSocket 연결 함수
function connect() {
    let socket = new SockJS("/game"); // 서버의 WebSocket 엔드포인트와 일치해야 합니다.
    stompClient = Stomp.over(socket);

    stompClient.connect({}, function (frame) {
        console.log('Connected: ' + frame);

        let roomId = getRoomIdFromURL(); // URL에서 roomId 추출
        if (!roomId) {
            console.error('Room ID not found in URL');
            return;
        }

        // "/room/{roomId}" 토픽을 구독
        subscription = stompClient.subscribe("/room/" + roomId, function (res) {
            console.log('Received message: ', res.body);
            let response = JSON.parse(res.body);

            // 메시지 타입에 따라 처리
            if (response.type === 'UserLeft') {
                removeUserFromTable(response.userId);
            } else {
                updateUserTable(response);
            }
        });

        // 필요 시 초기 사용자 목록 요청
        // stompClient.send(`/app/${roomId}/getUsers`, {}, {});
    }, function (error) {
        console.error('STOMP error: ', error);
    });

    // 버튼 클릭 이벤트 핸들러 등록
    document.addEventListener('click', function (event) {
        if (event.target.classList.contains('ready-btn')) {
            toggleReadyStatus(event);
        } else if (event.target.classList.contains('start-btn')) {
            startGame(event);
        }
    });
}

// WebSocket 연결 해제 함수
function disconnect() {
    if (stompClient !== null) {
        if (subscription !== null) {
            subscription.unsubscribe();
        }
        stompClient.disconnect();
    }
}

// URL에서 roomId 추출 함수
function getRoomIdFromURL() {
    // 예: URL이 /room/1234 일 경우 1234 추출
    let path = window.location.pathname;
    let parts = path.split('/');
    return parts.length >= 3 ? parts[2] : null;
}

// 사용자 테이블 업데이트 함수
function updateUserTable(response) {
    // ResponseMessage에서 데이터 추출
    let { userId, userName, readyStatus, role } = response;

    // 기존 사용자 행 찾기
    let userRow = document.getElementById(`user-${userId}`);

    if (userRow) {
        // 기존 사용자 정보 업데이트
        userRow.querySelector('.ready-status').textContent = readyStatus ? "Ready" : "Not Ready";
        userRow.querySelector('.ready-status').className = readyStatus ? "ready-status ready-true" : "ready-status ready-false";
        userRow.querySelector('.role').textContent = role;
        userRow.querySelector('.role').className = role === "ADMIN" ? "role admin" : "role";

        // 버튼 업데이트
        let roleCell = userRow.querySelector('.role');
        roleCell.innerHTML = ''; // 기존 버튼 제거

        if (role === "ADMIN") {
            let startButton = document.createElement("button");
            startButton.textContent = "Start Game";
            startButton.className = "start-btn";
            startButton.setAttribute("data-user-id", userId);
            roleCell.appendChild(startButton);
        } else if (role === "USER") {
            let readyButton = document.createElement("button");
            readyButton.textContent = "Toggle Ready";
            readyButton.className = "ready-btn";
            readyButton.setAttribute("data-user-id", userId);
            roleCell.appendChild(readyButton);
        }
    } else {
        // 새로운 사용자 행 추가
        addUserToTable(userId, userName, readyStatus, role);
    }
}

// 사용자 테이블에 새로운 행 추가 함수
function addUserToTable(userId, userName, readyStatus, role) {
    const userTableBody = document.querySelector("#userTable tbody");
    let userRow = document.createElement("tr");
    userRow.id = `user-${userId}`;

    // User ID 셀
    let userIdCell = document.createElement("td");
    userIdCell.textContent = userId;
    userRow.appendChild(userIdCell);

    // User Name 셀
    let userNameCell = document.createElement("td");
    userNameCell.textContent = userName;
    userRow.appendChild(userNameCell);

    // Ready Status 셀
    let readyStatusCell = document.createElement("td");
    readyStatusCell.textContent = readyStatus ? "Ready" : "Not Ready";
    readyStatusCell.className = readyStatus ? "ready-status ready-true" : "ready-status ready-false";
    userRow.appendChild(readyStatusCell);

    // Role 셀
    let roleCell = document.createElement("td");
    roleCell.textContent = role;
    roleCell.className = role === "ADMIN" ? "role admin" : "role";

    // 버튼 추가
    if (role === "ADMIN") {
        let startButton = document.createElement("button");
        startButton.textContent = "Start Game";
        startButton.className = "start-btn";
        startButton.setAttribute("data-user-id", userId);
        roleCell.appendChild(startButton);
    } else if (role === "USER") {
        let readyButton = document.createElement("button");
        readyButton.textContent = "Toggle Ready";
        readyButton.className = "ready-btn";
        readyButton.setAttribute("data-user-id", userId);
        roleCell.appendChild(readyButton);
    }

    userRow.appendChild(roleCell);

    // 테이블 본문에 행 추가
    userTableBody.appendChild(userRow);
}

// Ready Status 토글 함수 (버튼 클릭 시 호출)
function toggleReadyStatus(event) {
    let userId = event.target.getAttribute("data-user-id");
    let roomId = getRoomIdFromURL();

    if (userId && roomId) {
        // 서버에 Ready 상태 토글 요청 보내기
        stompClient.send(`/app/${roomId}/ready`, {}, JSON.stringify({ userId: userId }));
    }
}

// Start Game 함수 (버튼 클릭 시 호출)
function startGame(event) {
    let userId = event.target.getAttribute("data-user-id");
    let roomId = getRoomIdFromURL();

    if (userId && roomId) {
        // 서버에 게임 시작 요청 보내기
        stompClient.send(`/app/${roomId}/start`, {}, JSON.stringify({ userId: userId }));
    }
}

// 사용자 테이블에서 사용자 제거 함수 (사용자 퇴장 시)
function removeUserFromTable(userId) {
    let userRow = document.getElementById(`user-${userId}`);
    if (userRow) {
        userRow.remove();
    }
}