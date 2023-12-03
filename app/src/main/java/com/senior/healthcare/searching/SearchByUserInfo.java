package com.senior.healthcare.searching;

import static com.senior.healthcare.setting.UserType.*;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.senior.healthcare.HospitalInfo;
import com.senior.healthcare.Main;
import com.senior.healthcare.R;
import com.senior.healthcare.setting.ApplicationSetting;
import com.senior.healthcare.setting.UserType;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import kotlin.jvm.internal.LocalVariableReference;

public class SearchByUserInfo extends Activity {
    private static final String serviceKey = ApplicationSetting.getServiceKey();
    private static final String API_URL = "http://openapi1.nhis.or.kr/openapi/service/rest/HmcSearchService/getRegnHmcList?siDoCd=" + ApplicationSetting.getCityCode() + "&siGunGuCd=" + ApplicationSetting.getVillageCode() + "&numOfRows=300&ServiceKey=" + serviceKey;
    private static boolean isParsingDone;
    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        isParsingDone = false;
        setContentView(R.layout.search_health);
        LinearLayout loadingLayout = findViewById(R.id.loadingLayout);

        UserType userType = ApplicationSetting.getUserType();
        applyRotationAnimation();
        ImageView back_icon = findViewById(R.id.back_icon);
        back_icon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), Main.class);
                startActivity(intent);
            }
        });

        // Thread 실행
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // API에 GET 요청 보내고 XML 결과 받기
                    String xmlData = getXmlFromUrl(API_URL);

                    // XML 파싱하여 hospitalName, hospitalCode 값 추출
                    final List<HospitalInfo> hospitalList = parseXml(xmlData, userType);
                    Log.v("v", "리스트 생성");
                    // UI 업데이트는 메인 쓰레드에서 수행해야 합니다.
                    runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            loadingLayout.setVisibility(View.GONE);
                            // 파싱된 결과를 사용하여 버튼 동적 생성
                            createButtons(hospitalList);
                        }
                    });

                } catch (IOException | XmlPullParserException e) {
                    e.printStackTrace();
                    // 오류 처리
                    Log.e("search", "오류 발생");
                }
            }
        });
        thread.start();
    }

    private String getXmlFromUrl(String urlString) throws IOException {
        StringBuilder result = new StringBuilder();
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;

        try {
            URL url = new URL(urlString);
            urlConnection = (HttpURLConnection) url.openConnection();
            InputStream in = urlConnection.getInputStream();

            reader = new BufferedReader(new InputStreamReader(in));
            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line);
            }
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return result.toString();
    }

    private List<HospitalInfo> parseXml(String xmlData, UserType userType) throws XmlPullParserException, IOException {
        List<HospitalInfo> hospitalList = new ArrayList<>();
        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        factory.setNamespaceAware(true);
        XmlPullParser parser = factory.newPullParser();
        parser.setInput(new java.io.StringReader(xmlData));

        int eventType = parser.getEventType();
        while (eventType != XmlPullParser.END_DOCUMENT) {
            HospitalInfo hospitalInfo = new HospitalInfo();
            if (eventType == XmlPullParser.START_TAG && "item".equals(parser.getName())) {
                while (eventType != XmlPullParser.END_TAG || !"item".equals(parser.getName())) {
                    if (eventType == XmlPullParser.START_TAG) {
                        switch (parser.getName()) {
                            case "hmcNm":
                                hospitalInfo.setHospitalName(parser.nextText());
                                break;
                            case "hmcNo":
                                hospitalInfo.setHospitalCode(parser.nextText());
                                break;
                            case "hmcTelNo":
                                hospitalInfo.setHospitalCode( parser.nextText());
                                break;
                            case "bcExmdChrgTypeCd"://유방암
                                hospitalInfo.setBcExmdChrgTypeCd("1".equals(parser.nextText()));
                                break;
                            case "ccExmdChrgTypeCd"://대장암
                                hospitalInfo.setCcExmdChrgTypeCd("1".equals(parser.nextText()));
                                break;
                            case "cvxcaExmdChrgTypeCd"://자궁경부암
                                hospitalInfo.setCvxcaExmdChrgTypeCd("1".equals(parser.nextText()));
                                break;
                            case "grenChrgTypeCd"://일반 검진
                                hospitalInfo.setGrenChrgTypeCd("1".equals(parser.nextText()));
                                break;
                            case "lvcaExmdChrgTypeCd"://간암 검진
                                hospitalInfo.setLvcaExmdChrgTypeCd("1".equals(parser.nextText()));
                                break;
//                            case "mchkChrgTypeCd"://구강 검진
//                                hospitalInfo.setMchkChrgTypeCd("1".equals(parser.nextText()));
//                                break;
                            case "stmcaExmdChrgTypeCd"://위암 검진
                                hospitalInfo.setStmcaExmdChrgTypeCd("1".equals(parser.nextText()));
                                break;
                        }
                    }
                    eventType = parser.next();
                }//파싱 종료
                Log.v("v", "파싱 종료");
            }
            eventType = parser.next();
            boolean isCanAdd = checkHospitalByUserType(userType,hospitalInfo);
            if(isCanAdd)hospitalList.add(hospitalInfo);
        }
        isParsingDone = true;
        return hospitalList;
    }

    private boolean checkHospitalByUserType(UserType userType, HospitalInfo hospitalInfo){
        Log.v("checkType", userType.toString());
        if (userType == MEN20UP){
            if(hospitalInfo.isGrenChrgTypeCd())return true;
            return false;
        }
        else if (userType == WOMEN20UP){
            if (hospitalInfo.isCvxcaExmdChrgTypeCd() && hospitalInfo.isGrenChrgTypeCd())return true;
            return false;
        }
        else if(userType == MEN40UP){
            if (hospitalInfo.isLvcaExmdChrgTypeCd() && hospitalInfo.isGrenChrgTypeCd() &&
                    hospitalInfo.isStmcaExmdChrgTypeCd()){
                return true;
            }
            return false;
        }
        else if(userType == WOMEN40UP){
            if (hospitalInfo.isLvcaExmdChrgTypeCd() && hospitalInfo.isGrenChrgTypeCd() &&
                    hospitalInfo.isStmcaExmdChrgTypeCd() && hospitalInfo.isGrenChrgTypeCd() &&
            hospitalInfo.isBcExmdChrgTypeCd())return true;
            return false;
        }
        else if(userType == MEN50UP){
            if (hospitalInfo.isLvcaExmdChrgTypeCd() && hospitalInfo.isGrenChrgTypeCd() &&
                    hospitalInfo.isStmcaExmdChrgTypeCd() && hospitalInfo.isCcExmdChrgTypeCd()){
                return true;
            }
            return false;
        }
        else if(userType == WOMEN50UP){
            if (hospitalInfo.isLvcaExmdChrgTypeCd() && hospitalInfo.isGrenChrgTypeCd() &&
                    hospitalInfo.isStmcaExmdChrgTypeCd() && hospitalInfo.isGrenChrgTypeCd() &&
                    hospitalInfo.isBcExmdChrgTypeCd() && hospitalInfo.isCcExmdChrgTypeCd())return true;
            return false;
        }
        return false;
    }

    private void createButtons(List<HospitalInfo> hospitalList) {
        LinearLayout layout = findViewById(R.id.search_health);

        // 이미지 리소스 배열
        int[] buttonBackgrounds = {R.drawable.btn_color1, R.drawable.btn_color2, R.drawable.btn_color3,
                R.drawable.btn_color4, R.drawable.btn_color5, R.drawable.btn_color6};

        int imageIndex = 0; // 이미지 인덱스 변수

        for (final HospitalInfo hospitalInfo : hospitalList) {
            Button button = new Button(this);
            button.setText(hospitalInfo.getHospitalName());

            // 순서대로 이미지 선택
            button.setBackgroundResource(buttonBackgrounds[imageIndex]);

            // 이미지 인덱스 업데이트
            imageIndex = (imageIndex + 1) % buttonBackgrounds.length;

            //버튼 크기 조절
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(920, 185);

            params.setMargins(0, 0, 0, 40);
            button.setLayoutParams(params);

            button.setOnClickListener(view -> {
                ApplicationSetting.setHospitalName(hospitalInfo.getHospitalName());
                ApplicationSetting.setHospitalCode(hospitalInfo.getHospitalCode());

                Intent intent = new Intent(getApplicationContext(), Hospital.class); // 변경된 클래스명으로 수정
                startActivity(intent);
            });

            layout.addView(button);
        }
    }

    private void applyRotationAnimation() {
        ImageView loadingImageView = findViewById(R.id.loadingImageView);
        RotateAnimation rotateAnimation = new RotateAnimation(0, 360, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        rotateAnimation.setDuration(1000);  // 회전 애니메이션의 지속 시간 (밀리초)
        rotateAnimation.setRepeatCount(Animation.INFINITE);  // 무한 반복
        loadingImageView.startAnimation(rotateAnimation);
    }
}

