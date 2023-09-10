package qouteall.imm_ptl.core.render;

import qouteall.imm_ptl.core.portal.PortalLike;

/**
 * It's just a supertype for Portal and PortalGroupToRender.
 * Not using Either to reduce temporary object allocation for normal portal rendering.
 *
 * Unlike {@link PortalLike}, this attaches the information of visible portal entity list for portal group.
 */
public interface PortalRenderable {
    PortalLike getPortalLike();
}
