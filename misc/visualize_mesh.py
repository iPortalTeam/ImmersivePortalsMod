
# for visualizing 2D mesh
# the mesh is stored in mesh_to_visualize.json

import matplotlib.pyplot as plt
import json
import random

def getRandomColor():
    return (random.random(), random.random(), random.random())

transparent = True

# read the mesh from json file
with open('./misc/mesh_to_visualize.json') as json_file:
    mesh = json.load(json_file)
    points = mesh['points']
    triangles = mesh['triangles']
    print(mesh)

    for triangle in triangles:
        triangle_points = [points[triangle[0]], points[triangle[1]], points[triangle[2]]]
        triangle = plt.Polygon(triangle_points, color=(0,0,0,0.3) if transparent else getRandomColor())
        plt.gca().add_patch(triangle)

    plt.axis('scaled')
    plt.show()



