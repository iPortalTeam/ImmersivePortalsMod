#version 120

//simulate vanilla fixed pipeline block rendering

uniform mat4 modelView;
uniform mat4 projection;
uniform mat4 textureMatrix;

//uniform vec3 posBase;

varying vec2 uv;
varying vec2 uv2;
varying vec3 pos;
varying vec4 color;
varying float fogDistance;

void main(){
    pos = gl_Vertex.xyz;
    vec4 modelViewed=(modelView * gl_Vertex);
    gl_Position = projection * modelViewed;
    uv = gl_MultiTexCoord0.xy;
    //uv2 = (textureMatrix * gl_MultiTexCoord1).xy;
    uv2 = (gl_MultiTexCoord1.xy+vec2(8, 8))/256;//the texture matrix broke somehow
    color = gl_Color;
    fogDistance=sqrt(modelViewed.x*modelViewed.x+modelViewed.y*modelViewed.y+modelViewed.z*modelViewed.z);
}