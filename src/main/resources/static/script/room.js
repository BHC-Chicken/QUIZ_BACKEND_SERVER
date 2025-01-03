window.addEventListener("load", function () {
    let roomsContainer = document.getElementById('roomsContainer');

    let sock = new SockJS('/updateOccupancy');
    let stompClient = Stomp.over(sock);

    stompClient.connect({}, function (frame) {
        console.log('Connected: ' + frame);

        stompClient.subscribe('/pub/occupancy', function (message) {
            let room = JSON.parse(message.body);
            let emptyMessageRow = document.getElementById("empty-message");

            if (room.currentPeople == 0) {
                let roomRow = document.getElementById('room-' + room.roomId);
                if (roomRow) {
                    roomRow.remove();

                    let tableBody = document.getElementById('room-table-body');

                    if (tableBody.children.length === 0 && !emptyMessageRow) {
                        let newEmptyMessageRow = document.createElement('tr');
                        newEmptyMessageRow.id = 'empty-message';
                        newEmptyMessageRow.innerHTML = `
                            <td colspan="7" style="text-align: center;">현재 생성된 방이 없습니다.</td>
                        `;
                        tableBody.appendChild(newEmptyMessageRow);
                    }
                }
            } else {
                let roomRow = document.getElementById('room-' + room.roomId);
                if (!roomRow) {
                    let tableBody = document.getElementById('room-table-body');

                    if (emptyMessageRow) {
                        emptyMessageRow.remove();
                    }

                    roomRow = document.createElement('tr');
                    roomRow.id = 'room-' + room.roomId;
                    roomRow.innerHTML = `
                        <td>${room.roomId}</td>
                        <td>${room.roomName}</td>
                        <td>${room.topicId}</td>
                        <td>${room.maxPeople}</td>
                        <td>${room.quizCount}</td>
                        <td class="current-people">${room.currentPeople}</td>
                        <td><a data-roomid="${room.roomId}" href="/room/${room.roomId}">enter</a></td>
                    `;
                    tableBody.insertBefore(roomRow, tableBody.firstChild);
                } else {
                    roomRow.querySelector('.current-people').textContent = room.currentPeople;
                }
            }
        });
    });
});
