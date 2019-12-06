## What is this
It's a minecraft mod.

https://www.curseforge.com/minecraft/mc-mods/immersive-portals-mod

## How to run this code with Intellij IDEA
1. Import the project, enable auto import and let gradle tasks finish
2. Download this jar https://github.com/qouteall/ImmersivePortalsMod/blob/master/minecraft-1.14.4-mapped-net.fabricmc.yarn-2.jar
and use it to replace C:\Users\username\ .gradle\caches\fabric-loom\minecraft-1.14.4-mapped-net.fabricmc.yarn-2.jar (It contains OptiFine stuff)
3. Run  ```gradlew idea``` ```gradlew genSources```

## Briefly explain how this mod works

### Portal entity
A portal entity is one-way and one-faced.
A normal nether portal contains 2 portals in overworld and 2 portals in nether.

You can create a portal by command
```
/summon immersive_portals:portal ~ ~5 ~ {width:4,height:4,axisWX:1,axisWY:0,axisWZ:0,axisHX:0,axisHY:1,axisHZ:0,dimensionTo:-1,destinationX:0,destinationY:129,destinationZ:0}
```
![](https://i.ibb.co/zbC9RW7/1.png)

```immersive_portals:monitoring_nether_portal``` is the portal entity that can disappear when nether portal breaks.

When lighting a nether portal, it will firstly try to link to the obsidian
 frame with same size and direction in 128 range in the other dimension.
You can control the destination of nether portal by building an obsidian frame.

### Rendering
Portal rendering takes place before translucent block rendering.
It uses stencil buffer and does not use additional frame buffer when rendering without shaders.

It firstly draw the portal view area.
When the camera is very close to the portal,
the pixels that are too close to the camera are culled during rasterization.
So it will additionally render a small pyramid-shaped hood around camera.

Drawing view area will increase stencil value by one.
Then it will move the player to another dimension, switch objects for rendering 
and render the world in stencil limitation.
It will then render nested portals.

ChunkRenderDispatcher stores an fixed size array of ChunkRenderer s.
I changed it into a map.

While rendering portal content, it will cull pixels behind portal and it
will do more strict frustum culling to improve performance.

![](https://i.ibb.co/tHJv6ZH/2019-09-05-17-10-47.png)
![](https://i.ibb.co/y8JVVxH/2019-09-05-17-10-53.png)

### Chunk loading
Client chunk manager uses an fixed size array to store chunks.
I changed it into a map.

In server side, it will send redirected packet to players to synchronize world information.
If the packet is not redirected, a chunk data packet of nether may be recognized as overworld chunk data in client.

I made my own chunk loading determination logic. The /forceload command will not work.

### Seamless teleportation
Teleportation on client side happens before rendering (not during ticking).
Teleportation happens when the camera cross the portal (after the player entity crossing the portal).

Client will teleport first and then the server receives the teleport request and do teleportation on server.