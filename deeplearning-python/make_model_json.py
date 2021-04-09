import os
import numpy as np
from keras.preprocessing.image import ImageDataGenerator
from keras.preprocessing import image
from keras.models import Sequential,model_from_json,load_model
from keras.layers import Dropout, Flatten, Dense
from keras import applications
import cv2
from keras import optimizers
import math

# dimensions of our images.
img_width, img_height = 256,256
epochs = 100
batch_size = 32

top_model_weights_path = 'bottleneck_fc_model1.h5'
train_data_dir = 'f_data/train'
validation_data_dir = 'f_data/validation'


def count_labels():# We need to match labels and image data for testing accuracy
    train_label_count=[]
    validation_label_count=[]

    for i in os.listdir(train_data_dir):
        train_label_count.append(len(os.listdir(train_data_dir+'/'+i)))

    for i in os.listdir(validation_data_dir):
        validation_label_count.append(len(os.listdir(validation_data_dir+'/'+i)))

    return train_label_count,validation_label_count






def save_bottlebeck_features():
    datagen = ImageDataGenerator(rescale=1. / 255)
    datagen_train = ImageDataGenerator(                                                                                                          rotation_range=40,                                                                                                      width_shift_range=0.2,                                                                                                  height_shift_range=0.2,                                                                                                 shear_range=0.2,                                                                                                        zoom_range=0.2,                                                                                                         horizontal_flip=True,fill_mode='neareast')
    # build the VGG16 network
    model = applications.VGG16(include_top=False, weights='imagenet')


    generator = datagen.flow_from_directory(
        train_data_dir,
        target_size=(img_width, img_height),
        batch_size=batch_size,
        class_mode=None,
        shuffle=False)

    bottleneck_features_train = model.predict_generator(
        generator, steps=int(math.ceil(nb_train_samples // batch_size)))

    np.save(open('bottleneck_features_train_1.npy', 'wb'),
            bottleneck_features_train)


    generator = datagen.flow_from_directory(
        validation_data_dir,
        target_size=(img_width, img_height),
        batch_size=batch_size,
        class_mode=None,
        shuffle=False)

    bottleneck_features_validation = model.predict_generator(
            generator, steps=int(math.ceil(nb_validation_samples // batch_size)))

    np.save(open('bottleneck_features_validation_1.npy', 'wb'),
            bottleneck_features_validation)



def train_top_model():

    t_cnt , v_cnt = count_labels()
    train_data = np.load('bottleneck_features_train_1.npy')
    train_labels = np.array(
        [0]*(t_cnt[0]) + [1]*(t_cnt[1]) + [2]*(t_cnt[2]) + [3]*(t_cnt[3]) )
    
    validation_data = np.load('bottleneck_features_validation_1.npy')
    validation_labels = np.array(
        [0]*(v_cnt[0]) + [1]*(v_cnt[1]) + [2]*(v_cnt[2]) + [3]*(v_cnt[3]) )

    model = Sequential()
    model.add(Flatten(input_shape=train_data.shape[1:]))
    model.add(Dense(256, activation='relu'))
    model.add(Dropout(0.5))
    model.add(Dense(4, activation='softmax'))#'sigmoid'))
    opt = optimizers.SGD(lr=0.0001)



    model.compile(optimizer=opt,
                  loss='sparse_categorical_crossentropy', metrics=['accuracy'])

    model.fit(train_data, train_labels,
              epochs=epochs,
              batch_size=batch_size,
              validation_data=(validation_data, validation_labels))
    model.save_weights(top_model_weights_path)
    model_json=model.to_json()
    with open("model.json", "w") as json_file:
        json_file.write(model_json)

#save_bottlebeck_features()
train_top_model()

