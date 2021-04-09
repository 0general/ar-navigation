import deeplearnig
import trim
import cv2 as cv
import numpy as np
from PIL import Image
import os
os.environ['DISPLAY'] = ':0'
class find:
    # top left, top right, bottom right, bottom left
    @staticmethod
    def sort_points(points):
        points = np.array(points)
        points = points.astype(np.float32)

        new_points = np.zeros((4, 2), dtype="float32")

        if len(points) == 4:
            s = points.sum(axis=1)
            min_index = np.argmin(s)
            new_points[0] = points[min_index]
            points = np.delete(points, min_index, axis=0)

            s = points.sum(axis=1)
            max_index = np.argmax(s)
            new_points[2] = points[max_index]
            points = np.delete(points, max_index, axis=0)

            if points[0][1] > points[1][1]:
                new_points[1] = points[1]
                new_points[3] = points[0]
            else:
                new_points[1] = points[0]
                new_points[3] = points[1]

        elif len(points) > 4:
            x = np.zeros((len(points)), dtype="float32")
            y = np.zeros((len(points)), dtype="float32")

            for i in range(len(points)):
                x[i] = points[i][0]
                y[i] = points[i][1]

            new_points[0][0] = min(x)
            new_points[0][1] = min(y)
            new_points[1][0] = max(x)
            new_points[1][1] = min(y)
            new_points[2][0] = max(x)
            new_points[2][1] = max(y)
            new_points[3][0] = min(x)
            new_points[3][1] = max(y)
        return new_points

    # 세로 간판은 잡아 늘리는 거 필요없음!!
    @staticmethod
    def transform(img_input, points):
        points = find.sort_points(points)
        topLeft, topRight, bottomRight, bottomLeft = points

        maxWidth = 800
        maxHeight = 300

        dst = np.array([[0, 0], [maxWidth - 1, 0],
                        [maxWidth - 1, maxHeight - 1], [0, maxHeight - 1]],
                       dtype="float32")

        H = cv.getPerspectiveTransform(points, dst)
        img_warped = cv.warpPerspective(img_input, H, (maxWidth, maxHeight))

        return img_warped

    # img = 서버가 넣어준 이미지
    @classmethod
    def explore_img(cls, img):
        img_color = cv.imread(img, cv.IMREAD_COLOR)

        height, width = img_color.shape[:2]

        img_gray = cv.cvtColor(img_color, cv.COLOR_BGR2GRAY)

        # 이진화로 물체와 배경 분리
        # 물체만 영상에 남긴다.
        ret, img_binary = cv.threshold(img_gray, 0, 255,
                                       cv.THRESH_BINARY | cv.THRESH_OTSU)

        # 잡음 제거
        kernel = cv.getStructuringElement(cv.MORPH_RECT, (3, 3))
        img_binary = cv.morphologyEx(img_binary, cv.MORPH_OPEN, kernel)

        # findContour로 컨투어(같은 색,동일한 픽셀값) 검출
        # #cv,RETR_EXTERNAL : 가장 바깥쪽 라인 cv2.CHAIN_APPROX_SIMPLE: 컨투어 라인을 그릴 수 있는 포인트만 반환
        contours, hierarchy = cv.findContours(img_binary, cv.RETR_EXTERNAL,
                                                 cv.CHAIN_APPROX_SIMPLE)
        img_card = None

        for contour in contours:  # 각 컨투어에 대해서

            area = cv.contourArea(contour)  # 면적을 구한다(단위 픽셀)

            if area < 10000:  # 면적 10000이하 제외.  면적 설정은 논의 필요
                continue

            epsilon = 0.02 * cv.arcLength(contour, True)
            approx = cv.approxPolyDP(contour, epsilon, True)

            size = len(approx)

            img_result = img_color.copy()
            cv.drawContours(img_result, [approx], -1, (0, 255, 0), 2);

            
            cv.waitKey(0)

            if cv.isContourConvex(approx):

                hull = cv.convexHull(approx)  # convexHull로 볼록하게 윤곽선 만들기

                points = []

                for p in hull:  # 포인트를 리스트에 입력
                    points.append(p[0])

                if size >= 4:  # 네개의 엣지를 가지면 사각형
                    img_card = find.transform(img_color, points)  # 정면으로 변화

        if (img_card is None):

            contours_xy = np.array(contours)

            x_min, x_max = 0, 0
            value = list()
            for i in range(len(contours_xy)):
                for j in range(len(contours_xy[i])):
                    value.append(contours_xy[i][j][0][0])  # 네번째 괄호가 0일때 x의 값
                    x_min = min(value)
                    x_max = max(value)
            

            # y의 min과 max 찾기
            y_min, y_max = 0, 0
            value = list()
            for i in range(len(contours_xy)):
                for j in range(len(contours_xy[i])):
                    value.append(contours_xy[i][j][0][1])  # 네번째 괄호가 0일때 x의 값
                    y_min = min(value)
                    y_max = max(value)
        

            points = np.zeros((4, 2), dtype="float32")

            points[0][0] = x_min
            points[0][1] = y_min
            points[1][0] = x_max
            points[1][1] = y_min
            points[2][0] = x_max
            points[2][1] = y_max
            points[3][0] = x_min
            points[3][1] = y_max

            img_card = find.transform(img_color, points)

            trim_card = trim.trim(img)[0]
            name = trim.trim(img)[1]

        return img_card

def f(x):#deeplearnig으로 부터 리턴받은 y1, y2는 각 라벨에 대한 모델의 예상확률 numpy array임.
    #y1.argmax()를 이용해서 제일 높은 값을 지니는 인덱스를 추출하고, 해당하는 라벨 string으로 match시켜서 server에 retrun
        return {0:'버거킹',1:'CU',2:'GS25',3:'롯데리아'}[x]

def take_img(img):

    r_img = find.explore_img(img)
    t_img = trim.trim(img)

    r_img = cv.resize(r_img, dsize=(256,256))
    t_img = cv.resize(t_img, dsize=(256,256))

    y1=deeplearnig.prediction(r_img)
    y2=deeplearnig.prediction(t_img)

    if np.max(y1) > np.max(y2):
        return f(y1.argmax())
    else:
        return f(y2.argmax())

 
   
#    img = take_img('test/hi3.jpg')
 #   img= cv.resize(img, dsize=(256,256))
  #  print(deep.prediction(img))
    



