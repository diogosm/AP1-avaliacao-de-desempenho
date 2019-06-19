import numpy as np
import matplotlib.pyplot as plt
import matplotlib.mlab as ml
import numpy as np
from sklearn.neighbors import NearestNeighbors

eixoX, eixoY = [], []

eX, eY, eZ = [], [], []

pontos = []
with open('report.txt') as fin:
    ind = 0
    for line in fin:
        [time, label, x, y] = line.split(" ")
        pontos.append([float(x), float(y)])
        eX.append(float(x))
        eY.append(float(y))
        ind += 1
        
samples = pontos
neigh = NearestNeighbors(10)
neigh.fit(samples) 
        
for p in pontos:
    z = 1/neigh.kneighbors([p], int(len(pontos)*0.1), return_distance=True)[0].sum()
    eZ.append(z)

ndata = 10
ny, nx = 100, 200
xmin, xmax = np.array(eX).min(), np.array(eX).max()
ymin, ymax = np.array(eY).min(), np.array(eY).max()
# x = np.linspace(1, 10, ndata)
# y = np.linspace(1, 10, ndata)

x = eX
y = eY
z = eZ

xi = np.linspace(xmin, xmax, nx)
yi = np.linspace(ymin, ymax, ny)
zi = ml.griddata(x, y, z, xi, yi, interp='linear')

plt.figure(figsize=(20,20))

plt.contour(xi, yi, zi, 15, linewidths = 0.5, colors = 'k')
plt.pcolormesh(xi, yi, zi, cmap = plt.get_cmap('rainbow'))


plt.colorbar() 
#plt.scatter(x, y, marker = 'o', c = 'b', s = 5, zorder = 10)
plt.xlim(xmin, xmax)
plt.ylim(ymin, ymax)
plt.show()
plt.savefig('contourMap.png')