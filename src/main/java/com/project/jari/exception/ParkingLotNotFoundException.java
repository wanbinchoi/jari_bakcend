package com.project.jari.exception;

// 주차장을 찾지 못할 때 발생하는 커스텀 예외 생성
public class ParkingLotNotFoundException extends RuntimeException{
    public ParkingLotNotFoundException(String message){
        super(message);
    }
}
