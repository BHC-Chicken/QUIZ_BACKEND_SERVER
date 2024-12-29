document.querySelectorAll('a').forEach(anchor =>
    anchor.addEventListener('click', (event) => {
        let roomId = event.target.dataset.roomid;
        event.preventDefault();

        fetch(`/room/${roomId}`)
            .then(response => {

                if (!response.ok) {
                    throw new Error(`HTTP error! status: ${response.status}`);
                }
                window.location.href = `/room/${roomId}`;
            })
            .catch(error => {
                if (error.message.includes('403')) {
                    alert('권한이 없습니다.');
                } else if (error.message.includes('401')) {
                    alert('중복 입장은 불가능합니다.')
                } else {
                    alert('알 수 없는 오류가 발생했습니다.');
                }
            });
    })
);

window.addEventListener("load", function () {
    let roomsContainer = document.getElementById('roomsContainer');

    let sock = new SockJS('/updateOccupancy');
    let stompClient = Stomp.over(sock);

    stompClient.connect({}, function (frame) {
        console.log('Connected: ' + frame);

        stompClient.subscribe('/pub/occupancy', function (message) {
            let room = JSON.parse(message.body);

            if (room.currentPeople == 0) {
                let roomRow = document.getElementById('room-' + room.roomId);
                if (roomRow) {
                    roomRow.remove();
                }
            } else {
                let roomRow = document.getElementById('room-' + room.roomId);
                if (!roomRow) {
                    // 방이 없는 경우 새로 추가
                    let tableBody = document.getElementById('room-table-body');
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
