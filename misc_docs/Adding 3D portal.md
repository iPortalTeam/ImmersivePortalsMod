
A 3D portal is not limited to a plane. It can be a sphere, a cube, or any other shape.

Currently, ImmPtl supports scaled box. A scaled box has 6 outer portals. ImmPtl also has the rendering merge functionality to render the visible portals of the scale box at once, to improve rendering performance (in rasterization portal rendering, rendering a portal renders the world once. that's not the case for ray-tracing portal rendering).

However, for other cases like sphere portal, using many different individual portals to approximate the sphere is both inefficient and inaccurate. So there need to be a new kind of portal.

The way that current scaled box is implemented by 6 individual portals instead of a single 3D portal, is that these systems are already tailored for plane portals:
* Clipping in portal world rendering
* Outer entity clipping (in cross-portal entity rendering)
* Cross-portal collision
* Teleportation (including dynamic teleportation)
* Portal animation (portal state)
* Cross-portal block interaction

Having 3D portal means that these systems need to be reworked to support 3D portals.

The downside of having individual portals instead of one single 3D portal:
* Require automatic merging functionality which is error-prone and computation-heavy
* Performance issue. In MC, an entity has a lot of attributes and involves a lot of computation. This cause performance issues. Also reduce performance of chunk tracking, as chunk tracking traverses nearby portal entities.
* Harder to manage. Deleting an end portal scaled box requires deleting 6 portals. 

The most problematic issue is with clipping. However, if we only use 3D portal in cases where there is not anything "behind" the destination side of the portal, such as having far-away scale boxes like what MiniScaled does, then the inner clipping is not needed. If the portal forms an enclosed convex shape, then the outer clipping is also not needed.

For teleportation, the 3D portal must provide a distance function and ray tracing function. If the 3D portal's shape is a mesh, the distance function and ray tracing function could be implemented using mesh algorithms.

In cross portal collision, cross portal collision requires collision box clipping. The 3D portal could also provide a collision box clipping function for this side and other side.

### Should the width, height and orientation be decoupled from portal shape for animation?

If we decouple width, height, and orientation from shape, and let them be the portal's attribute instead of the shape's attribute, then the portal animation won't require modifying the shape.

But that involves spatial scaling in ray tracing which is unintuitive and could cause trouble sometimes. We can make the ray tracing related to width, height and orientation.