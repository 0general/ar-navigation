from keras.models import model_from_json, load_model
from keras import applications
import os
from keras.preprocessing import image
import numpy as np
from keras.preprocessing.image import ImageDataGenerator



def prediction(img):

                                           
    img = image.img_to_array(img)/255                                                                                   
    img = np.expand_dims(img, axis=0)   

    top_model_weights_path = 'bottleneck_fc_model.h5'

    json_file = open("model.json", "r")
    loaded_model_json = json_file.read()
    json_file.close()
    
    preModel= applications.VGG16(include_top=False, weights='imagenet')
   # img = preModel.predict_generator(img,320)
    img = preModel.predict(img)

    model = model_from_json(loaded_model_json)
    model.load_weights(top_model_weights_path)
    y=model.predict(img)
  #  for i in range(0,len(y)):
 #       print(y[i].argmax())
#    return 0
    print(y)
    return y
    
    
#if __name__ == "__main__":
   # datagen = ImageDataGenerator(rescale = 1./255)

    #img = datagen.flow_from_directory(
    #       'f_data/validation',
     #       target_size = (256,256),
      #      batch_size=1,
       #     class_mode=None)
    
  #  for i in os.listdir('f_data/validation/cu'):
        #img = image.load_img('f_data/validation/cu/'+i, target_size=(256,256))
        #img = image.img_to_array(img)/255
        #img = np.expand_dims(img, axis=0)
        #print(prediction(img))



 #   img = image.load_img('test/hi5.png', target_size=(256,256))
    
  #  img = image.img_to_array(img)/255
    
   # img = np.expand_dims(img, axis=0)

   # print(prediction(img))

