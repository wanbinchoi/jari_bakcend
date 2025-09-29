package com.project.jari.client.impl;

import com.project.jari.client.ParkingApiClient;
import com.project.jari.config.SeoulApiProperties;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

@Component
public class ParkingApiClientImpl implements ParkingApiClient {

    private final SeoulApiProperties props;

    public ParkingApiClientImpl(SeoulApiProperties props) {
        this.props = props;
    }

    // open api로 주차장 데이터 호출
    @Override
    public String callPkApi() throws IOException {
        StringBuilder urlBuilder = new StringBuilder(props.getBaseUrl()); /* URL */
        urlBuilder.append("/" + URLEncoder.encode(props.getKey(), "UTF-8"));
        urlBuilder.append("/" + URLEncoder.encode(props.getResponseType(), "UTF-8"));
        urlBuilder.append("/" + URLEncoder.encode(props.getServiceName(), "UTF-8"));
        urlBuilder.append("/" + URLEncoder.encode("1", "UTF-8"));
        urlBuilder.append("/" + URLEncoder.encode("178", "UTF-8"));

        /*
         * 이거는 선택사항임
         * "xx구" 라고 붙이게 되면 해당 구만 조회 가능한데
         * 이 기능 넣어서 지도 왼쪽에 검색창에다가
         * 구 검색해서 해당 구만 조회할 수 있도록 만들어도 ㄱㅊ을듯
         */
        //urlBuilder.append("/" + URLEncoder.encode("마포구", "UTF-8"));

        URL url = new URL(urlBuilder.toString());
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Content-type", "application/json");
        System.out.println("Response code: " + conn.getResponseCode());

        BufferedReader rd;
        if (conn.getResponseCode() >= 200 && conn.getResponseCode() <= 300) {
            rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        } else {
            rd = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
        }

        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = rd.readLine()) != null) {
            sb.append(line);
        }
        rd.close();
        conn.disconnect();

        // 응답데이터 json 변환
        return sb.toString();
    }
}
