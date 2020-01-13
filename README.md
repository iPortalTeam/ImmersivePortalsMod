## What is this
It's a minecraft mod.

https://www.curseforge.com/minecraft/mc-mods/immersive-portals-mod

## How to run this code without OptiFine
1. Remove things related to "OptiFabric" in build.gradle
2. Remove package com.qouteall.immersive_portals.optifine_compatibility
3. Fix some compile errors manually

## How to run this code with OptiFine
It needs to use OptiFine-patched minecraft jar to replace original minecraft jar.
If you want to know the details, contact me in email or discord.

## Briefly explain how this mod works

### Portal entity
A portal entity is one-way and one-faced.
A normal nether portal contains 2 portals in overworld and 2 portals in nether.

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
and render the world again in stencil limitation.
It will then render nested portals recursively.

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

I made my own chunk loading determination logic.
I tried to use my own chunk ticket to load the chunk but it didn't work as intended.
Currently it invokes vanilla's force loading mechanic to load chunks.
So /forceload command with this mod will not work.

### Seamless teleportation
Teleportation on client side happens before rendering (not during ticking).
Teleportation happens when the camera cross the portal (not after the player entity crossing the portal).

Client will teleport first and then the server receives the teleport request and do teleportation on server.

### Collision
When an entity is halfway in portal then its collision will be specially treated.
It will cut the entity's collision into two pieces, one outside portal and one inside portal.
It firstly do collision test for outer part and then
 switch the entity to the portal destination position and do collision test for inner part.
 
### Global Portals
World wrapping portals and vertical dimension connecting portals are very big.
They are not entities in world like small portals.
They are stored in per dimension global portal storage.