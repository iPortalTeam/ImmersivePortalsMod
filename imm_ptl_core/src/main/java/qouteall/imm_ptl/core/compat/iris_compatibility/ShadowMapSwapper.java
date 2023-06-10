package qouteall.imm_ptl.core.compat.iris_compatibility;

public class ShadowMapSwapper {
    // only compilable with the next version of Iris
    
//    public static class Storage {
//        public final DepthTexture depthTexture;
//        public final DepthTexture noTranslucents;
//        private int resolution;
//        private ShadowMapSwapper shadowMapSwapper;
//        // does not include shadow color for now
//
//        public Storage(int resolution, ShadowMapSwapper shadowMapSwapper) {
//            depthTexture = new DepthTexture(resolution, resolution, DepthBufferFormat.DEPTH);
//            noTranslucents = new DepthTexture(resolution, resolution, DepthBufferFormat.DEPTH);
//            this.resolution = resolution;
//            this.shadowMapSwapper = shadowMapSwapper;
//        }
//
//        void dispose() {
//            depthTexture.destroy();
//            noTranslucents.destroy();
//        }
//
//        void copyFromIrisShadowRenderTargets() {
//            GL43C.glCopyImageSubData(
//                shadowMapSwapper.shadowRenderTargets.getDepthTexture().getTextureId(),
//                GL43C.GL_TEXTURE_2D,
//                0, 0, 0, 0,
//                depthTexture.getTextureId(),
//                GL43C.GL_TEXTURE_2D,
//                0, 0, 0, 0,
//                resolution, resolution,
//                1
//            );
//
//            GL43C.glCopyImageSubData(
//                shadowMapSwapper.shadowRenderTargets.getDepthTextureNoTranslucents().getTextureId(),
//                GL43C.GL_TEXTURE_2D,
//                0, 0, 0, 0,
//                noTranslucents.getTextureId(),
//                GL43C.GL_TEXTURE_2D,
//                0, 0, 0, 0,
//                resolution, resolution,
//                1
//            );
//        }
//
//        void copyToIrisShadowRenderTargets() {
//            GL43C.glCopyImageSubData(
//                depthTexture.getTextureId(),
//                GL43C.GL_TEXTURE_2D,
//                0, 0, 0, 0,
//                shadowMapSwapper.shadowRenderTargets.getDepthTexture().getTextureId(),
//                GL43C.GL_TEXTURE_2D,
//                0, 0, 0, 0,
//                resolution, resolution,
//                1
//            );
//
//            GL43C.glCopyImageSubData(
//                noTranslucents.getTextureId(),
//                GL43C.GL_TEXTURE_2D,
//                0, 0, 0, 0,
//                shadowMapSwapper.shadowRenderTargets.getDepthTextureNoTranslucents().getTextureId(),
//                GL43C.GL_TEXTURE_2D,
//                0, 0, 0, 0,
//                resolution, resolution,
//                1
//            );
//        }
//
//        void restitute() {
//            shadowMapSwapper.restituteStorage(this);
//        }
//    }
//
//    private static final int storageNumLimit = 3;
//
//    private final int shadowMapResolution;
//    private ShadowRenderTargets shadowRenderTargets;
//    private int createdNum = 0;
//    private final ArrayDeque<Storage> queue = new ArrayDeque<>();
//
//    public ShadowMapSwapper(int resolution, ShadowRenderTargets shadowRenderTargets) {
//        this.shadowMapResolution = resolution;
//        this.shadowRenderTargets = shadowRenderTargets;
//    }
//
//    public void dispose() {
//        Validate.isTrue(queue.size() == createdNum);
//
//        for (Storage storage : queue) {
//            storage.dispose();
//        }
//    }
//
//    @Nullable
//    public Storage acquireStorage() {
//        if (queue.isEmpty()) {
//            if (createdNum < storageNumLimit) {
//                createdNum++;
//                return new Storage(shadowMapResolution, this);
//            }
//            else {
//                return null;
//            }
//        }
//        return queue.pollFirst();
//    }
//
//    void restituteStorage(Storage storage) {
//        if (storage != null) {
//            queue.addFirst(storage);
//        }
//    }
}
