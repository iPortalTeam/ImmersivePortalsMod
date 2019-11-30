package com.qouteall.immersive_portals.optifine_compatibility;

import com.mojang.blaze3d.platform.GLX;
import com.qouteall.immersive_portals.Helper;
import net.minecraft.client.util.Untracker;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL20C;
import org.lwjgl.system.MemoryUtil;

import java.nio.IntBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

//When shaders are rendering improperly, it's possibly because
//an uniform variable is not initialized
//check which uniform is zero
public class UniformReport {
    private static IntBuffer sizeBuffer = GLX.make(MemoryUtil.memAllocInt(1), (p_209238_0_) -> {
        Untracker.untrack(MemoryUtil.memAddress(p_209238_0_));
    });
    
    private static IntBuffer dataTypeBuffer = GLX.make(MemoryUtil.memAllocInt(1), (p_209238_0_) -> {
        Untracker.untrack(MemoryUtil.memAddress(p_209238_0_));
    });
    
    
    public static class UniformInfo {
        public int programId;
        public String name;
        public int index;
        public int type;
        
        public UniformInfo(int programId, String name, int index, int type) {
            this.programId = programId;
            this.name = name;
            this.index = index;
            this.type = type;
        }
        
        public void report(Consumer<String> output) {
            reportValue(s -> output.accept(
                String.format(
                    "[%s] %s: %s",
                    index, name, s
                )
            ));
        }
        
        public void reportValue(Consumer<String> output) {
            try {
                switch (type) {
                    case GL11.GL_FLOAT: {
                        float v = GL20.glGetUniformf(programId, index);
                        output.accept(Float.toString(v));
                    }
                    
                    break;
                    case GL20C.GL_FLOAT_VEC2: {
                        float[] v = new float[2];
                        GL20.glGetUniformfv(programId, index, v);
                        output.accept(Arrays.toString(v));
                    }
                    break;
                    case GL20C.GL_FLOAT_VEC3: {
                        float[] v = new float[3];
                        GL20.glGetUniformfv(programId, index, v);
                        output.accept(Arrays.toString(v));
                    }
                    break;
                    case GL20C.GL_FLOAT_VEC4: {
                        float[] v = new float[4];
                        GL20.glGetUniformfv(programId, index, v);
                        output.accept(Arrays.toString(v));
                    }
                    break;
                    case GL11.GL_INT: {
                        int v = GL20.glGetUniformi(programId, index);
                        output.accept(Integer.toString(v));
                    }
                    break;
                    case GL20C.GL_INT_VEC2: {
                        int[] v = new int[2];
                        GL20.glGetUniformiv(programId, index, v);
                        output.accept(Arrays.toString(v));
                    }
                    break;
                    case GL20C.GL_FLOAT_MAT3: {
                        float[] v = new float[9];
                        GL20.glGetUniformfv(programId, index, v);
                        output.accept(Arrays.toString(v));
                    }
                    break;
                    case GL20C.GL_FLOAT_MAT4: {
                        float[] v = new float[16];
                        GL20.glGetUniformfv(programId, index, v);
                        output.accept(Arrays.toString(v));
                    }
                    break;
                    default: {
                        output.accept("Unknown " + type);
                    }
                }
            }
            catch (Exception e) {
                output.accept(
                    String.format(
                        "Error Reporting Uniform %s %s %s %s",
                        name, index, type,
                        e.toString()
                    )
                );
            }
        }
    }
    
    public static void reportUniforms(int programId, Consumer<String> output) {
        int maxIndex = GL20.glGetProgrami(programId, GL20.GL_ACTIVE_UNIFORMS);
        
        List<UniformInfo> uniformInfos = IntStream.range(0, maxIndex)
            .mapToObj(i -> {
                String name = GL20.glGetActiveUniform(
                    programId, i, sizeBuffer, dataTypeBuffer
                );
                Helper.checkGlError();
                return new UniformInfo(
                    programId,
                    name,
                    i,
                    dataTypeBuffer.get(0)
                );
            }).collect(Collectors.toList());
        
        uniformInfos.stream().forEach(uniformInfo -> {
            uniformInfo.report(output);
        });
    }
}
