from socket import *
import preProcessing as pc
import os

#IP ='172.31.41.217' #elastic ip address about AWS EC2
IP = '121.173.101.54'
TCP_PORT = 8000#open port for public

serverSocket = socket(AF_INET, SOCK_STREAM)
serverSocket.bind((IP, TCP_PORT))
serverSocket.listen(1)
i=len(os.listdir('test'))+1#save image by applications service

while True:
    print('The server is ready to receive')
    connectionSocket, addr = serverSocket.accept()
    
    file_name='test/hi'+str(i)+'.jpg'#image dir form

    img_file = open(file_name, "wb")
    while True:
        sentence = connectionSocket.recv(1024)
        data = sentence
        img_file.write(sentence)
        if sentence:
            print("recving IMg....")
            print(sentence)
            sentence = connectionSocket.recv(1024)
            img_file.write(sentence)
        else:
            print('Done')
            break
    
    img_file.close()

    label = pc.take_img(file_name)
    connectionSocket.sendall(bytes(label,'UTF-8'))
    i = i+1
    

connectionSocket.close()
