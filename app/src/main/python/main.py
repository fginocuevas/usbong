import cv2 as cv
import pandas as pd
import numpy as np
from PIL import Image
import base64
import io
from os.path import dirname, join

def treshcolor(imgData):
    ''' Thresholding '''
    result = " "
    byteCodeData = base64.b64decode(imgData)
    numpyData = np.fromstring(byteCodeData, np.uint8)

    img = cv.imdecode(numpyData, cv.IMREAD_UNCHANGED)

    width = 500
    height = 400
    dimension = (width, height)
    resized = cv.resize(img, dimension,interpolation=cv.INTER_AREA)

    # Adjusting the Brightness of the Image
    adjusted_image = cv.convertScaleAbs(resized,alpha=1, beta=-30)

    # Converting to grayscale
    gray = cv.cvtColor(resized,cv.COLOR_BGR2GRAY)

    # BLUR
    blur = cv.GaussianBlur(gray, (1,1), cv.BORDER_DEFAULT)
    ret, thresh = cv.threshold(blur, 125, 255, cv.THRESH_BINARY)

    threshflat = thresh.flatten()
    i = 0
    x = 0
    y = 0

    # Looping to access the values of thresholded image
    for i in threshflat:
        if i == 0:
            x+=1    #black
        elif i == 255:
            y+=1    #white
    black = x
    white = y

    if (white >=30 and white <=71000) and (black >= 130000 and black <=200000):
        result="Type of soil based on thresholding :\n \n LOAM\n \nLoamy soil a relatively even mix of sand, silt and clay, feels fine-textured and slightly damp. It has ideal characteristics for gardening, lawns and shrubs"

    elif (white >=28000 and white <=200000) and (black >=3000 and black <=180000):
        result="Type of soil based on thresholding:\n \n SANDY\n \nSand is a naturally occurring granular material composed of finely divided rock and mineral particles. A soil containing more than 85% sand-sized particles by mass "



    ''' Color Detection '''
    clicked = False
    r = g = b = x_pos = y_pos = 0

    colorsCsv = join(dirname(__file__), "colors.csv")

    # Reading csv file with pandas and giving names to each column
    index = ["color", "color_name", "hex", "R", "G", "B"]
    csv = pd.read_csv(colorsCsv, names=index, header=None)

    # calculate the average color of each row of our image
    avg_color_per_row = np.average(img, axis=0)

    # calculate the averages of our rows
    avg_colors = np.average(avg_color_per_row, axis=0)

    # avg_color is a tuple in BGR order of the average colors
    # but as float values
    # so, convert that array to integers
    int_averages = np.array(avg_colors, dtype=np.uint8)

    # create a new image of the same height/width as the original
    average_image = np.zeros((height, width, 3), np.uint8)

    # and fill its pixels with our average color
    average_image[:] = int_averages

    # function to calculate minimum distance from all colors and get the most matching color
    def get_color_name(R, G, B):
        global cname
        minimum = 100
        for i in range(len(csv)):
            d = abs(R - int(csv.loc[i, "R"])) + abs(G - int(csv.loc[i, "G"])) + abs(B - int(csv.loc[i, "B"]))
            if d <= minimum:
                minimum = d
                cname = csv.loc[i, "color_name"]
        return cname

    def draw_function(event, x, y, flags, param):
        if event == cv.EVENT_LBUTTONDOWN:
            global r, g, b, x_pos, y_pos, clicked
            clicked = True
            x_pos = x
            y_pos = y

    # finally, show it side-by-side with the original
    # cv.imshow("Avg Color", average_image)
    b, g, r = average_image[0, 0]
    r = int(r)
    g = int(g)
    b = int(b)

    text = get_color_name(r, g, b)

    result+="\n\nColor Detection using Color Detection:\n \n"
    result+=text

    return result

def decodeImage(data):
	decoded_data = base64.b64decode(data)
	np_data = np.fromstring(decoded_data,np.uint8)
	img = cv.imdecode(np_data,cv.IMREAD_UNCHANGED)
	img_gray = cv.cvtColor(img,cv.COLOR_BGR2GRAY)
	pil_im = Image.fromarray(img_gray)

	buff = io.BytesIO()
	pil_im.save(buff,format="PNG")
	img_str = base64.b64encode(buff.getvalue())
	return ""+str(img_str,'utf-8')

