package com.example.quiz.mapper;

import com.example.quiz.dto.room.request.RoomCreateRequest;
import com.example.quiz.dto.room.response.RoomEnterResponse;
import com.example.quiz.dto.room.response.RoomListResponse;
import com.example.quiz.dto.room.response.RoomResponse;
import com.example.quiz.entity.Room;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper
public interface RoomMapper {
    RoomMapper INSTANCE = Mappers.getMapper(RoomMapper.class);

    @Mapping(target = "removeStatus", constant = "false")
    Room RoomCreateRequestToRoom(RoomCreateRequest request, String masterEmail);

    @Mapping(target = "currentPeople", constant = "1")
    RoomResponse RoomToRoomResponse(Room room);

    RoomListResponse RoomToRoomListResponse(Room room, Integer currentPeople);

    RoomEnterResponse RoomToRoomEnterResponse(Room room);
}
