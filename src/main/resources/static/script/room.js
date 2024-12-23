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
                $('#room-' + room.roomId).remove();
            } else {
                $('#room-' + room.roomId + ' .current-people').text(room.currentPeople);
            }
        });
    });
});
