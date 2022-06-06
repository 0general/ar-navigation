# ar-navigation
`Deepect` team project
---
 **딥러닝**을 활용한 **AR Navigation**
<br>

## Video
---
> ### 정상 시나리오
<p align="center">
<img width="30%" src="https://user-images.githubusercontent.com/56211221/170048541-f8d6fe58-5b81-4c67-b960-5bc886855348.gif"/>
</p>

> ### 위치 보정 시나리오
<p align="center">
<img width="30%" src="https://user-images.githubusercontent.com/56211221/170048761-c117920f-62d4-4c8e-ab1a-41d68a758e5a.gif"/>
</p>

**Full presentation video on [YouTube](https://youtu.be/LFAATxp6J5M)**
<br>


## Info
---
#### 작품 개요
> **증강현실 기법**과 **영상의 실시간 분석 기술**을 결합하여 만든 어플리케이션으로, **도보 이용자**에게 최적 이동경로를 AR 지표로 안내하는 내비게이션

#### 기대 효과
> + 국내 지원이 불가했던 기존 타사 앱과 달리 국내 사용 지원
> + 국내 보급율이 높은 **Android** 스마트폰 카메라만을 이용하기에 높은 접근성과 쉬운 사용성
> + 실제 시야와 동일한 영상의 실시간 분석과 **AR**지표를 통한 이용자의 위치와 지도의 직관적인 맵핑 
> + **프랜차이즈** 매장의 **간판**을 탐지하여 **딥러닝**을 활용한 즉각적인 안내 정보 개선 
> + 주변 지형지물 인식에 어려움을 겪는 이나 길눈이 어두운 이들에게도 효과적인 길안내

#### 제작 일정
> 총 제작 일정 (졸업 작품 기간) `2019.09.07 ~ 2020.06.10`
<p align="center">
<img width="80%" src="https://user-images.githubusercontent.com/56211221/170040423-58732c8f-e8f1-42d8-b59a-89eea2c7767d.png"/>
</p>

### 구축 시스템
<p align="center">
<img width="80%" src="https://user-images.githubusercontent.com/56211221/170201412-72036198-3b65-4aca-a4ad-06a91acdf220.png"/>
</p>

<br>

## Role
---

1. 애플리케이션을 구동하여 목적지를 설정합니다.
2. Map view에서 길찾기를 위한 AR 모드로 전환합니다. 
3. 사용자의 현재 위치와 목적지까지의 경로에 AR 지표를 뿌려줍니다.
4. 사용자의 현재 위치와 GPS 상의 위치가 불일치한다면   
   좌측 상단에 스티커의 색상이 붉은 색으로 변합니다.
5. 만일 GPS와 현 위치가 불일치하거나, 복잡한 골목과 같이 헷갈리기 쉬운 길에서 사용자가 이동에 어려움을 겪는다면   
   우측 상단의 위치 보정 버튼을 클릭합니다.  
6. 주변에서 쉽게 발견되는 프랜차이즈 간판을 화면상의 안내 칸에 맞추어 촬영합니다.
7. 서버는 전체 이미지에서 사용자가 맞춘 칸에 해당하는 첫 번째 이미지를 크롭하여 추출합니다.
8. 동시에 전체 이미지에서 간판에 해당하는 부분을 직접 찾아서 크롭 후, 두 번째 이미지로 추출합니다.
9. 위 두 이미지를 딥러닝 모델에 입력하여 가장 높은 정확도를 가진 브랜드명을 반환해줍니다.
10. 서버는 브랜드명을 이용하여 현재 사용자의 위치를 찾아낸 다음, 다시 AR 지표를 생성합니다.
11. 사용자는 새로운 AR 지표와 함께 다시금 경로 안내를 받습니다.



> 제작 당시에 사용한 간판 브랜드는 많은 점포를 가지고 있는 프랜차이즈 브랜드 중 5개로,   
> `CU`, `GS25`, `롯데리아`, `맥도날드`, `베스킨라빈스`를 선택했습니다.

<br>
<br>

## Build Guide
---
### Requirements

+ Python 3.7
+ opencv 
+ git
+ T Map API
+ AR Core
+ CNN VGG16
+ numpy
+ json

```
pip install numpy
```
```
pip install opencv-python
```
```
pip install keras
```

<br>

## References
---
> 참고 문헌

+ 권상일, 김의명 "딥러닝을 이용한 판류형 간판의 인식", Journal of the Korean Society of Surveying, Geodesy, Photogrammetry and Cartography (한국측량학회지), pp. 2019-231, 2019
+ 김명성, 김성조, 김동현. "AR을 활용한 실내 내비게이션의 설계", 한국컴퓨터정보학회 하계학술대회, pp. 129-132, 2019.  
+ 김경호, 조성익, 이재식, 원광연, "증강현실 내비게이션의 인지적·행동적 영향에 관한 연구", 한국시뮬레이션학회 논문지, 제18권, 제4호, pp. 9-20, 2009.  
+ 하다윤, 반영환. "모바일 증강현실 네비게이션 디자인을 위한 정보요소 분석 및 사용성 조사", 한국HCI학회 학술대회, 783-785, 2011.  
+ 장시경, 원광연, "RealNavi: 위치기반 증강현실 기술을 이용한 자동차 네비게이션 시스템”, 한국HCI학회 학술대회, pp. 292-297, 2005  
+ 박새록, 이정민, “국내 증강현실 활용 교육 연구 동향", 학습자중심교과교육연구, 제20권, 제11호, pp. 1-23, 2020.  
+ 김정희, "가상, 증강현실 콘텐츠의 동향파악을 위한 국내외 선진 현황 분석", 한국게임학회 논문지, 제17권, 제4호, pp. 7-15, 2017.  
+ 김분희, 이재영. "증강현실 적용 기술 동향" , 한국콘텐츠학지, 14(4), 17-21, 2016.  
+ 한국편의점산업협회. 편의점 사업의 거래 공정화를 위한 자율 규약. 20019년 1월 28일  
+ "Building powerful image classification models using very little data." The Keras Blog. 2016년 6월 5일 수정, 2020년 1월 20일 접속, https://blog.keras.io/building-powerful-image-classification-models-using-very-little-data.html.

<br>

## Awards
---
> ### 2020년도 인천대학교 컴퓨터공학부 졸업작품 발표회
> [은상](http://www.inu.ac.kr/user/indexSub.do?codyMenuSeq=1477369&siteId=isis&dum=dum&boardId=490566&page=1&command=albumView&boardSeq=580200&chkBoxSeq=&categoryId=&categoryDepth=)

> ### 2020년도 융복합지식학회 추계학술대회
> [우수논문상](https://www.inu.ac.kr/user/indexSub.do?codyMenuSeq=99318&siteId=isis&dum=dum&boardId=601367&page=4&command=albumView&boardSeq=605180&chkBoxSeq=&categoryId=&categoryDepth=)

