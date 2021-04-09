from keras.preprocessing.image import ImageDataGenerator, array_to_img, img_to_array, load_img
import os
import cv2
import shutil


datagen = ImageDataGenerator(#generator setting
                rotation_range=40,
                width_shift_range=0.2,
                height_shift_range=0.2,
                shear_range=0.2,
                zoom_range=0.2,
                horizontal_flip=True,
                fill_mode='nearest')

n=20# value that how many images are generated by generator

for k in os.listdir('r_data'):

    q=os.listdir('r_data/'+k)
    samples=0
    leng=len(q)

    print(q)

    for j in q:


        img = load_img('r_data/'+k+'/'+j)  # PIL 이미지
        x = img_to_array(img)  # (3, 150, 150) 크기의 NumPy 배열
        x = x.reshape((1,) + x.shape)  # (1, 3, 150, 150) 크기의 NumPy 배열
        
        # 아래 .flow() 함수는 임의 변환된 이미지를 배치 단위로 생성해서
            # 지정된 `preview/` 폴더에 저장합니다.
        i = 0
        if samples/(leng*n) < 0.85:#원본 예제의 권장수치에 맞춘 값.
            for batch in datagen.flow(x, batch_size=16,
                                            save_to_dir='f_data/train/'+k, save_prefix=k, save_format='jpeg'):
                i += 1
            
                print(k+", train,"+j+","+str(leng))
                samples += 1

                if i >= n:
                    break  # 이미지 20장을 생성하고 
        else:
            shutil.move('r_data/'+k+'/'+j, 'f_data/validation/'+k+'/'+j)