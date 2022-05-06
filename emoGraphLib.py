saveToFile = True

import sys
import matplotlib
if saveToFile:
   matplotlib.use('Agg')   # to generate png output, must be before importing matplotlib.pyplot
import matplotlib.pyplot as plt
import matplotlib.patches as mpatches
import numpy as np
import math
import csv
import os
from pathlib import Path
#import pprint
#import statistics
#import scipy.stats as scistats


def loadCSV(csvfile):
    """ To load a csv-file. 
    Entries (cells) should be separated by commas. Return a list of rows.
    """
    # need to use a correct character encoding.... latin-1 does it
    with open(csvfile, encoding='latin-1') as file:
      content = csv.DictReader(file, delimiter=',')
      rows = []
      for row in content: rows.append(row)
      return rows


def mkHeatMapWorker(dataset,
        basename,
        pfun,
        whiteVal=1,
        width=64,
        height=64,
        scale=1,
        dir=".",
        colorscheme="black",
        graphtitle="heatmap"):
    """ The worker function to construct a visual heatmap from emotion traces.
    Parameters
    -------------
    dataset : a list of rows representing the data obtained from emotion traces. 
    Each row is a disctionary of 
    attribute-name and its value. The values are assumed to be non-negative. 
    Each row is assumed to contain attributes x,y,t; (x,y) represents position,
    and t represents time.

    pfun : is a function (e.g. a lamda expression) that maps each row to a value.
    E.g. it could be the function lambda r : float(r['fear']). This function 
    determines the values that will be plotted onto the produced map.
    
    basename : the map will be saved in a file named basename.png.
    
    dir : the directory where the map will be placed.
    
    whiteVal : the plotted values in the map will be assumed to range in the interval
    of [0...whiteVal]. 0 will be mapped to the color black, and whiteVal to the color
    white.

    scale : positions (x,y) such that round(scale*x),round(sclae*y) have the same value
    are treated as representing the same position. For example scale=0.5 means that
    data from position (0.9,0.9) to be considered as comparable to data from (0,0).

    width : the width of the produced map. 

    height : the height of the produced map.

    graphtitle : a text that will be put as the title of the produced map.
    """
    black = 0
    white = whiteVal
    map = np.zeros((scale*height,scale*width))
    for x in range(0,scale*height):
      for y in range(0,scale*width):
          map[x][y] = -1001

    for r in dataset:
        xx = round(scale*float(r['x']))
        yy = round(scale*float(r['y']))
        # rotate +90 degree
        x = scale*height - yy
        y = xx
        value = pfun(r)
        #print(f"==== val {value}" )
        if map[x][y] < -1000 :
           map[x][y] = value
        else:
           map[x][y] = max(map[x][y],value)
        #print(f"==== val {map[x][y]}" )
        

    for x in range(0,scale*height):
      for y in range(0,scale*width):
          if map[x][y] < -1000 : 
              if colorscheme == "black":  
                  map[x][y] = black 
              else:
                  map[x][y] = 1.1 * white      
    if colorscheme == "black":
        map[scale*height-1][scale*width-1] = white  # for imposing the range to go from black to white
    else:    
        map[scale*height-1][scale*width-1] = black  
       
    ax = plt.gca()
    ax.xaxis.set_visible(False)
    ax.yaxis.set_visible(False)
    plt.imshow(map, cmap='hot', origin='lower', interpolation='nearest')

    plt.title(graphtitle)
    #plt.legend()
    file_ = Path(dir + "/" +  basename +'.png')
    if saveToFile : plt.savefig(file_)
    else : plt.show()

#
# define here the rescaling, if needed; 1 means no rescaling:
#
rescaling = { 
   "hope" : 1, "joy" : 1, "satisfaction" : 1,
   "fear" : 1, "distress" : 1, "disappointment" : 1 
}
#rescaling = { 
#    "hope" : 1, "joy" : 1, "satisfaction" : 1,
#    "fear" : 1/1.25 , "distress" : 4, "disappointment" : 1 
#}

def mkHeatMap(dir,filename,width,height,colorscheme="black"):
    """ The function to construct a visual heatmap from emotion traces.

    This will construct heapmaps for six emotions: hope, joy, satisfaction,
    fear, distress, and dissapointment.

    Parameters
    -------------
    dir : the directory where the map will be placed.

    filename: the name of the trace-file whose data will be plotted to a heatmap.
    
    width : the width of the produced map. 

    height : the height of the produced map.
    """
    basename = filename.rsplit('.')[0]
    file_ = Path(dir + "/" +  filename)
    dataset = data_set1 = loadCSV(file_)
    mkMap = lambda emoType, pfunction: mkHeatMapWorker(dataset=dataset,
            width=width,
            height=height,
            colorscheme=colorscheme,
            pfun = pfunction,
            dir = dir,
            graphtitle = basename + " " + emoType, 
            basename = basename + "_" + emoType)
    mkMap('hope', lambda r: rescaling['hope']*float(r['hope']))
    mkMap('joy', lambda r: rescaling['joy']*float(r['joy']))
    mkMap('satisfaction', lambda r: rescaling['satisfaction']*float(r['satisfaction']))
    mkMap('fear', lambda r: rescaling['fear']*float(r['fear']))
    mkMap('distress', lambda r: rescaling['distress']*float(r['distress']))
    mkMap('disappointment', lambda r: rescaling['disappointment']*float(r['disappointment']))

def mkAggregateHeatMap(dir,width,height,colorscheme="black"):
    dataset = []
    for filename in os.listdir(dir):
        if(filename.endswith(".csv")):
            file_ = Path(dir + "/" + filename)
            datax = loadCSV(file_)
            #print(filename)
            #print(datax)
            dataset.extend(datax)
    mkMap = lambda emoType, pfunction: mkHeatMapWorker(dataset=dataset,
            width=width,
            height=height,
            colorscheme=colorscheme,
            pfun = pfunction,
            dir = dir,
            graphtitle = emoType, 
            basename =  "aggregate_" + emoType)
    mkMap('hope', lambda r: rescaling['hope']*float(r['hope']))
    mkMap('joy', lambda r: rescaling['joy']*float(r['joy']))
    mkMap('satisfaction', lambda r: rescaling['satisfaction']*float(r['satisfaction']))
    mkMap('fear', lambda r: rescaling['fear']*float(r['fear']))       # rescaling fear
    mkMap('', lambda r: rescaling['distress']*float(r['distress']))  # rescaling distress
    mkMap('disappointment', lambda r: rescaling['disappointment']*float(r['disappointment']))
    mkMap('Negative Emotions', lambda r:  (float(r['fear'])+float(r['distress'])+ float(r['disappointment'])))
    mkMap('Postive Emotions', lambda r: (float(r['hope'])+ float(r['joy'])+ float(r['satisfaction'])))


def findExtremeCases(dir):
    shortestSatLength = 1000000
    shortestSat = ""
    longestSatLength = 0
    longestSat = ""
    shortestDisappointmentLength = 1000000
    shortestDisappointment = ""
    longestDisappointmentLength = 0
    longestDisappointment = ""
    playsWithSat = 0
    playsWithDisappointment = 0
    totDuration = 0
    numOfPlays = 0
    shortestPlay = 1000000
    longestPlay = 0
    totTcLength = 0 
    minTcLength = 1000000
    maxTcLength = 0
    
    for filename in os.listdir(dir):
        
        # abstract test-cases 
        if(filename.endswith(".txt")):
            file_ = Path(dir + "/" + filename)
            with open(file_) as f:
                tcn = len(f.readlines())
                totTcLength = totTcLength + tcn
                if tcn < minTcLength: minTcLength = tcn
                if tcn > maxTcLength: maxTcLength = tcn

        # trace files:
        if(filename.endswith(".csv")):
            file_ = Path(dir + "/" + filename)
            dataset = loadCSV(file_)
            numOfPlays = numOfPlays+1
            N = len(dataset)
            totDuration = totDuration + N
            if N < shortestPlay:
                shortestPlay = N
            if N > longestPlay:
                longestPlay = N    
            satMoment  = -1
            dispMoment = -1
            for r in dataset:
                flag=0
                if (float(r['satisfaction']) >= 1 and satMoment < 0) :
                    satMoment = float(r['t'])
                    playsWithSat = playsWithSat+1
                    flag=1
                if (float(r['disappointment']) >= 1 and dispMoment < 0) :
                    dispMoment = float(r['t'])
                    playsWithDisappointment = playsWithDisappointment+1
                    flag=1
                
            if (satMoment>=0 and satMoment < shortestSatLength) :
                shortestSatLength = satMoment
                shortestSat = filename
            if (satMoment>=0 and satMoment > longestSatLength) :
                longestSatLength = satMoment
                longestSat = filename    
            if (dispMoment>=0 and dispMoment < shortestDisappointmentLength) :
                shortestDisappointmentLength = dispMoment
                shortestDisappointment = filename
            if (dispMoment>=0 and dispMoment > longestDisappointmentLength) :
                longestDisappointmentLength = dispMoment
                longestDisappointment = filename 
            if(flag==0):
                print(f"** #Not terminated       : {filename}")

            
    print(f"** Average tc-length   : {totTcLength/numOfPlays}")
            
    print(f"** Shortest tc         : {minTcLength}")            
    print(f"** Longest tc          : {maxTcLength}")            
    print(f"** Average play-length : {totDuration/numOfPlays}")            
    print(f"** Shortest play       : {shortestPlay}")            
    print(f"** Longest play        : {longestPlay}")            
    print(f"** Shortest play with satisfaction : {shortestSat}, length:{shortestSatLength}")
    print(f"** Longest play with satisfaction  : {longestSat}, length:{longestSatLength}")
    print(f"** Shortest play with disappointment: {shortestDisappointment}, length:{shortestDisappointmentLength}")
    print(f"** Longest play with disappointment : {longestDisappointment}, length:{longestDisappointmentLength}")
    print(f"** #plays with satisfaction         : {playsWithSat}")
    print(f"** #plays with disappointment       : {playsWithDisappointment}")

    

